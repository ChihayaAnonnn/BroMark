package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.raffle.service.P0AdmissionGate;
import com.jay.domain.raffle.service.P0BacklogSnapshot;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** P0：按活动维度的受理在途许可上限。 */
public class P0ActivityCapV2Test {
  private static final long ACTIVITY = 11L;
  private static final long OTHER_ACTIVITY = 22L;

  @Test
  public void rejectsWhenActivityHasFiveThousandInflightAndReleasesOnLeave() {
    var gate = new P0AdmissionGate();
    gate.update(new P0BacklogSnapshot(0, 0));
    var users = fill(gate, ACTIVITY, P0AdmissionGate.MAX_ACTIVITY_INFLIGHT);
    assertThat(gate.inflight()).isEqualTo(P0AdmissionGate.MAX_ACTIVITY_INFLIGHT);

    /** 第 5,001 个被活动上限拒绝，且不在用户维度留下半截许可。 */
    assertThat(gate.tryEnter("activity-full-user", ACTIVITY)).isFalse();
    assertThat(gate.inflight()).isEqualTo(P0AdmissionGate.MAX_ACTIVITY_INFLIGHT);
    assertThat(gate.tryEnter("activity-full-user", OTHER_ACTIVITY)).isTrue();
    gate.leave("activity-full-user", OTHER_ACTIVITY);

    /** 其他活动不受该活动打满的影响。 */
    assertThat(gate.tryEnter("other-activity-user", OTHER_ACTIVITY)).isTrue();
    gate.leave("other-activity-user", OTHER_ACTIVITY);

    /** 释放一个名额后，同活动可以再进一个，随后再次触顶。 */
    gate.leave(users.get(0));
    assertThat(gate.tryEnter("released-slot-user", ACTIVITY)).isTrue();
    assertThat(gate.tryEnter("overflow-after-release", ACTIVITY)).isFalse();
    assertThat(gate.inflight()).isEqualTo(P0AdmissionGate.MAX_ACTIVITY_INFLIGHT);
  }

  @Test
  public void perUserLimitAndRequestRetryStillWorkWithActivityPermits() {
    var gate = new P0AdmissionGate();
    gate.update(new P0BacklogSnapshot(0, 0));

    assertThat(gate.tryEnter("u1", ACTIVITY, "request-1")).isTrue();
    assertThat(gate.tryEnter("u1", ACTIVITY, "request-1")).isTrue();
    assertThat(gate.tryEnter("u1", ACTIVITY, "request-2")).isFalse();
    /** 跨活动的用户拒绝不能遗留活动许可。 */
    assertThat(gate.tryEnter("u1", OTHER_ACTIVITY)).isFalse();
    assertThat(gate.inflight()).isEqualTo(2);

    /** 单参 leave 兼容旧调用，同时归还用户许可和活动许可。 */
    gate.leave("u1");
    assertThat(gate.inflight()).isOne();
    gate.leave("u1", ACTIVITY);
    assertThat(gate.inflight()).isZero();

    /** 归还干净后同一用户仍可重新受理。 */
    gate.leave("u1");
    assertThat(gate.tryEnter("u1", ACTIVITY)).isTrue();
    gate.leave("u1", ACTIVITY);
    assertThat(gate.tryEnter("u1", ACTIVITY)).isTrue();
    gate.leave("u1");
    assertThat(gate.inflight()).isZero();
  }

  @Test
  public void activityPermitsDropToZeroWithoutBlockingRefill() {
    var gate = new P0AdmissionGate();
    gate.update(new P0BacklogSnapshot(0, 0));
    var users = fill(gate, ACTIVITY, P0AdmissionGate.MAX_ACTIVITY_INFLIGHT);
    users.forEach(gate::leave);
    assertThat(gate.inflight()).isZero();

    /** 计数归零后重新填满，第二次仍然恰好 5,000 个。 */
    var refilled = fill(gate, ACTIVITY, P0AdmissionGate.MAX_ACTIVITY_INFLIGHT);
    assertThat(gate.tryEnter("overflow-again", ACTIVITY)).isFalse();
    refilled.forEach(gate::leave);
    assertThat(gate.tryEnter("after-cleanup", ACTIVITY)).isTrue();
    gate.leave("after-cleanup", ACTIVITY);
    assertThat(gate.inflight()).isZero();
  }

  private static List<String> fill(P0AdmissionGate gate, long activity, int count) {
    var users = new ArrayList<String>(count);
    for (int i = 0; i < count; i++) {
      String user = "activity-cap-" + activity + "-" + i;
      assertThat(gate.tryEnter(user, activity)).as("user %s", user).isTrue();
      users.add(user);
    }
    return users;
  }
}
