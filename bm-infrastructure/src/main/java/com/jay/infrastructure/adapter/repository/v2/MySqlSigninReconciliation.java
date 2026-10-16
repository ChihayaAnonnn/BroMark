package com.jay.infrastructure.adapter.repository.v2;

import com.jay.domain.rebate.model.SignInResult;
import com.jay.domain.asset.model.*;
import com.jay.domain.asset.service.ReceiptJson;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** 维护窗口内在用户槽事务检查；坏JSON/缺表向上抛错，不能算已检查零差异。 */
public final class MySqlSigninReconciliation {
  private final JdbcTemplate j;
  public MySqlSigninReconciliation(DataSource ds) { j=new JdbcTemplate(ds); }
  public long checkUser(String user) {
    var orders=j.query("select receipt from "+MySqlAssetRepository.table("sign_in_order")+" where user_id=?",
        (r,n)->JsonStore.read(r.getString(1),SignInResult.class),user);
    long bad=0;
    for(var order:orders) {
      var rewards=j.query("select receipt from "+MySqlAssetRepository.table("sign_in_reward")+" where sign_in_order_id=?",
          (r,n)->JsonStore.read(r.getString(1),SignInResult.RewardReceipt.class),order.orderId());
      boolean wrong=rewards.size()!=order.rewards().size() || !new HashSet<>(rewards).equals(new HashSet<>(order.rewards()));
      for(var reward:order.rewards()) {
        if(reward.credit()!=null) {
          var ledger=j.query("select receipt from "+MySqlAssetRepository.table("user_credit_ledger")+" where user_id=? and biz_type=? and biz_id=?",
              (r,n)->JsonStore.read(r.getString(1),AssetReceipt.class),user,reward.credit().bizId().type(),reward.credit().bizId().id());
          wrong |= ledger.size()!=1 || !ledger.getFirst().equals(reward.credit());
        }
        if(reward.quota()!=null) {
          var ledger=j.query("select receipt from "+MySqlAssetRepository.table("user_quota_ledger")+" where user_id=? and activity_id=? and biz_type=? and biz_id=?",
              (r,n)->JsonStore.read(r.getString(1),QuotaReceipt.class),user,reward.quota().activityId(),"GRANT:"+reward.quota().bizId().type(),reward.quota().bizId().id());
          wrong |= ledger.size()!=3 || ledger.stream().anyMatch(x->!x.equals(reward.quota()));
        }
      }
      if(wrong) bad++;
    }
    return bad;
  }
}
