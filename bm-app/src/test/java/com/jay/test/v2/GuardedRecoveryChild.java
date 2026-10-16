package com.jay.test.v2;

import com.jay.domain.task.model.*;
import com.jay.infrastructure.adapter.repository.v2.*;
import java.nio.file.*;
import java.time.*;
import javax.sql.DataSource;
import org.springframework.transaction.PlatformTransactionManager;

/** 子进程只接生产仓储，不重跑DDL。 */
public final class GuardedRecoveryChild {
  public static void main(String[] args) {
    Java25RouterIntegrationTest.databaseContext()
        .run(
            c -> {
              if (c.getStartupFailure() != null)
                throw new IllegalStateException(c.getStartupFailure());
              var h =
                  new AssetTestSupport(
                      c.getBean(DataSource.class), c.getBean(PlatformTransactionManager.class));
              var ids =
                  new com.jay.infrastructure.id.BlockOrderIdGenerator(
                      h.runner, h.j, "recoverychild");
              ids.prefetch();
              h.codec.withGenerator(ids);
              var service = GuardedWorkflowRecoveryV2Test.service(h);
              try {
                var dir = Path.of(args[2]);
                Files.writeString(dir.resolve(args[0] + ".ready"), "ready");
                long end = System.nanoTime() + Duration.ofSeconds(30).toNanos();
                while (!Files.exists(dir.resolve("go"))) {
                  if (System.nanoTime() > end) throw new IllegalStateException("Barrier timeout");
                  Thread.sleep(10);
                }
                for (String order : args[1].split(",")) {
                  if (args[0].equals("draw")) service.inspect(order);
                  else
                    new com.jay.trigger.job.v2.StockRecoveryJob(
                            h.runner,
                            new MySqlStockRepository(h.j.getDataSource()),
                            new MySqlDrawRepository(h.j.getDataSource()),
                            h.codec,
                            ids,
                            new MySqlOutboxWriter(h.j.getDataSource()),
                            service,
                            GuardedWorkflowRecoveryV2Test.guarded(h))
                        .inspect(order);
                }
              } catch (Exception e) {
                throw new IllegalStateException(e);
              }
            });
  }
}
