package com.jay.domain.rebate.service;

import com.jay.domain.activity.service.AdmissionService;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.repository.UserTransactionRunner;
import com.jay.domain.asset.service.*;
import com.jay.domain.rebate.model.*;
import com.jay.domain.rebate.repository.SignInRepository;
import com.jay.domain.task.model.EventEnvelope;
import com.jay.domain.task.repository.OutboxWriter;
import java.time.*;
import java.util.*;

public final class SignInService {
  private final UserTransactionRunner tx;
  private final AssetKernel assets;
  private final QuotaKernel quota;
  private final SignInRepository repo;
  private final AdmissionService admission;
  private final RouteCodec codec;
  private final OrderIdGenerator ids;
  private final OutboxWriter out;
  private final FaultInjector faults;

  public SignInService(
      UserTransactionRunner tx,
      AssetKernel assets,
      QuotaKernel quota,
      SignInRepository repo,
      AdmissionService admission,
      RouteCodec codec,
      OrderIdGenerator ids,
      OutboxWriter out,
      FaultInjector faults) {
    this.tx = tx;
    this.assets = assets;
    this.quota = quota;
    this.repo = repo;
    this.admission = admission;
    this.codec = codec;
    this.ids = ids;
    this.out = out;
    this.faults = faults;
  }

  public SignInResult signIn(SignInCommand c) {
    if (c.targetDate() == null) throw new IllegalArgumentException("targetDate required");
    var identity =
        new RequestIdentity(
            c.userId(),
            "SIGNIN",
            c.requestId(),
            new RequestHasher().hash("SIGNIN", Map.of("date", c.targetDate().toString())));
    var replay = tx.inUser(c.userId(), WorkClass.TRADE, c.deadline(), () -> repo.request(identity));
    if (replay.isPresent()) return replay.get();
    var sameDay =
        tx.inUser(
            c.userId(), WorkClass.TRADE, c.deadline(), () -> repo.day(c.userId(), c.targetDate()));
    if (sameDay.isPresent())
      return tx.inUser(
          c.userId(),
          WorkClass.TRADE,
          c.deadline(),
          () -> {
            assets.lockAccounts(c.userId(), List.of());
            var old = repo.request(identity);
            if (old.isPresent()) return old.get();
            var original = repo.day(c.userId(), c.targetDate()).orElseThrow();
            repo.requestReceipt(identity, original);
            return original;
          });
    var snapshot = admission.forSignIn(c.userId(), c.targetDate(), c.deadline());
    ids.prefetch();
    String order = codec.newOrderId("SIGN", c.userId());
    String event = codec.newEventId();
    var activities =
        snapshot.rewards().stream()
            .filter(x -> x.quota() != null)
            .map(x -> x.activityId())
            .distinct()
            .sorted()
            .toList();
    return tx.inUser(
        c.userId(),
        WorkClass.TRADE,
        c.deadline(),
        () -> {
          var time = assets.lockAccounts(c.userId(), activities);
          var old = repo.request(identity);
          if (old.isPresent()) return old.get();
          var daily = repo.day(c.userId(), c.targetDate());
          if (daily.isPresent()) {
            repo.requestReceipt(identity, daily.get());
            return daily.get();
          }
          if (!time.businessDate().equals(c.targetDate())
              || !snapshot.accepts(time)
              || snapshot.rewards().isEmpty()) {
            var rejected =
                new SignInResult(
                    null, c.targetDate(), "REJECTED", List.of(), "DATE_OR_REWARD_UNAVAILABLE");
            repo.requestReceipt(identity, rejected);
            return rejected;
          }
          var receipts = new ArrayList<SignInResult.RewardReceipt>();
          var seen = new HashSet<String>();
          for (var reward : snapshot.rewards()) {
            if (!seen.add(reward.itemId()))
              throw new IllegalArgumentException("Duplicate reward item");
            var biz =
                new BusinessId(
                    "SIGN_REWARD", order + ":" + reward.itemId(), identity.requestHash());
            var credit =
                reward.credit() == null ? null : assets.credit(c.userId(), biz, reward.credit());
            var grant =
                reward.quota() == null
                    ? null
                    : quota.grant(c.userId(), reward.activityId(), biz, reward.quota(), time);
            receipts.add(new SignInResult.RewardReceipt(reward.itemId(), credit, grant));
            faults.hit("signin.after-reward");
          }
          var result = new SignInResult(order, c.targetDate(), "DELIVERED", receipts, null);
          repo.save(c.userId(), snapshot, time, result);
          repo.requestReceipt(identity, result);
          out.append(
              new EventEnvelope(
                  event,
                  "SignInCompleted",
                  order,
                  new BusinessId("SIGNIN", order, identity.requestHash()),
                  c.userId(),
                  codec.routeUser(c.userId()),
                  identity.requestHash(),
                  1,
                  1,
                  ReceiptJson.write(result)));
          return result;
        });
  }

  public SignInResult get(String user, LocalDate date) {
    return tx.inUser(
        user,
        WorkClass.QUERY,
        new Deadline(Instant.now().plusSeconds(5)),
        () -> repo.day(user, date).orElseThrow(AssetNotFoundException::new));
  }
}
