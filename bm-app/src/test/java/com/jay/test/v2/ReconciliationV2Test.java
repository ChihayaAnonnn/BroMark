package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;
import com.jay.domain.asset.model.*;
import com.jay.domain.rebate.model.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.time.*;
import java.util.*;
import org.junit.Test;

public class ReconciliationV2Test {
  @Test public void missingSigninRewardCannotBeReportedAsCheckedZero() {
    AssetTestSupport.run(h -> {
      var f = new BusinessTestSupport(h);
      h.overrideTime=BusinessTime.at(Instant.parse("2026-10-16T01:00:00Z"));
      String u="recon-sign-"+UUID.randomUUID();
      var result=f.signin.signIn(new SignInCommand(u,"request",h.overrideTime.businessDate(),new Deadline(Instant.now().plusSeconds(60))));
      h.tx(u,()-> {
        var checks=new MySqlSigninReconciliation(h.j.getDataSource());
        assertThat(checks.checkUser(u)).isZero();
        h.j.update("delete from "+MySqlAssetRepository.table("sign_in_reward")+" where sign_in_order_id=? and reward_item_id='credit'",result.orderId());
        assertThat(checks.checkUser(u)).isEqualTo(1);
        return null;
      });
    });
  }
}
