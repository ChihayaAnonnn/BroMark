package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import com.jay.domain.asset.model.BusinessId;
import com.jay.domain.asset.model.RouteSlot;
import com.jay.domain.asset.service.ReceiptJson;
import com.jay.domain.stock.model.ReservationResult;
import com.jay.domain.strategy.model.AwardSnapshot;
import com.jay.domain.task.model.EventEnvelope;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class P0EventContractV2Test {
  @Test
  public void acceptsSchemaTwoAndRejectsUnknown() {
    assertThat(
            new EventEnvelope(
                "e1",
                "ReserveAward",
                "o1",
                new BusinessId("DRAW", "o1", "a".repeat(64)),
                "u1",
                RouteSlot.control(),
                "a".repeat(64),
                1,
                2,
                "{}"))
        .isNotNull();
    assertThatThrownBy(
            () ->
                new EventEnvelope(
                    "e2",
                    "ReserveAward",
                    "o1",
                    new BusinessId("DRAW", "o1", "a".repeat(64)),
                    "u1",
                    RouteSlot.control(),
                    "a".repeat(64),
                    1,
                    3,
                    "{}"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void reservationReceiptRoundTrips() {
    var r =
        new ReservationResult(
            "o1",
            "u1",
            "a".repeat(64),
            "b".repeat(64),
            "CONFIRMED",
            new AwardSnapshot(0, "NONE", null, null, 0, "c".repeat(64)),
            0);
    assertThat(ReceiptJson.read(ReceiptJson.write(r), ReservationResult.class)).isEqualTo(r);
  }

  @Test
  public void keepsSchemaOneAccepted() {
    assertThat(
            new EventEnvelope(
                "e1",
                "ExchangeCompleted",
                "o1",
                new BusinessId("EX", "o1", "a".repeat(64)),
                "u1",
                RouteSlot.control(),
                "a".repeat(64),
                1,
                1,
                "{}"))
        .isNotNull();
  }

  @Test
  public void schema004DeclaresReservedAtAndBacklogIndex() throws Exception {
    String ddl = Files.readString(Path.of("../docs/dev-ops/mysql/migrations/004_user_draw.sql"));
    assertThat(ddl).contains("confirmed_at datetime(6),reserved_at datetime(6)");
    assertThat(ddl)
        .contains(
            "INDEX draw_user(user_id,activity_id),"
                + "INDEX draw_backlog(state,last_progress_at,order_id)");
  }
}
