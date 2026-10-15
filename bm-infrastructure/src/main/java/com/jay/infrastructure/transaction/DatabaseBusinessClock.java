package com.jay.infrastructure.transaction;
import com.jay.domain.asset.model.BusinessTime;
import com.jay.domain.asset.service.BusinessClock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.sql.DataSource;
import java.time.*;
public final class DatabaseBusinessClock implements BusinessClock {
 private final JdbcTemplate jdbc;public DatabaseBusinessClock(DataSource ds){jdbc=new JdbcTemplate(ds);}
 public BusinessTime now(){if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Business time requires the account transaction");LocalDateTime utc=jdbc.queryForObject("select UTC_TIMESTAMP(6)",LocalDateTime.class);return BusinessTime.at(utc.toInstant(ZoneOffset.UTC));}
}
