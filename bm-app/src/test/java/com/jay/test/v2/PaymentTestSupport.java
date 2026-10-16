package com.jay.test.v2;

import com.jay.domain.asset.model.*;
import com.jay.domain.payment.model.*;
import com.jay.domain.payment.service.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import com.jay.infrastructure.gateway.payment.*;
import com.jay.infrastructure.id.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

final class PaymentTestSupport {
  final AssetTestSupport h;
  final MySqlPaymentRepository repo;
  final SandboxPaymentProvider provider;
  final PaymentService service;
  final PaymentFactService facts;

  PaymentTestSupport(AssetTestSupport h) throws Exception {
    this.h = h;
    ConsumerInboxV2Test.schema(h);
    control(
        () -> {
          try {
            for (String s :
                java.nio.file.Files.readString(
                        java.nio.file.Path.of("../docs/dev-ops/mysql/migrations/005_payment.sql"))
                    .split(";")) if (!s.isBlank()) h.j.execute(s);
          } catch (Exception e) {
            throw new IllegalStateException(e);
          }
          return null;
        });
    var ids = new BlockOrderIdGenerator(h.runner, h.j, "paymenttest");
    ids.prefetch();
    h.codec.withGenerator(ids);
    repo = new MySqlPaymentRepository(h.j.getDataSource());
    provider =
        new SandboxPaymentProvider(
            h.runner, repo, "test", "merchant-it", "isolated-signing-key-at-least-32-bytes");
    service =
        new PaymentService(
            h.runner, repo, provider, h.codec, ids, new MySqlOutboxWriter(h.j.getDataSource()));
    facts =
        new PaymentFactService(h.runner, repo, h.codec, new MySqlOutboxWriter(h.j.getDataSource()));
    control(
        () -> {
          h.j.update(
              "insert into payment_product values('sku',10.00,'CNY',100.00,1) on duplicate key"
                  + " update amount=10.00,credit=100.00,published=1");
          return null;
        });
  }

  <T> T control(Supplier<T> s) {
    return h.runner.inControl(WorkClass.RECOVERY, new Deadline(Instant.now().plusSeconds(30)), s);
  }

  PaymentCommand command(String u, String r, String product) {
    return new PaymentCommand(
        u, r, product, "sandbox", new Deadline(Instant.now().plusSeconds(30)));
  }

  VerifiedPaymentEvent event(
      String o, String e, String t, String type, String amount, String currency) {
    return new VerifiedPaymentEvent(
        "sandbox",
        "merchant-it",
        "test",
        e,
        t,
        o,
        type,
        new BigDecimal(amount),
        currency,
        control(() -> repo.find(o, false)).userId(),
        null);
  }
}
