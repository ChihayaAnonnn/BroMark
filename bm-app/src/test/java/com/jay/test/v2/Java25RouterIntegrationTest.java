package com.jay.test.v2;

import static org.assertj.core.api.Assertions.*;

import cn.bugstack.middleware.db.router.*;
import cn.bugstack.middleware.db.router.config.DataSourceAutoConfig;
import com.jay.infrastructure.dao.IUserCreditOrderDao;
import com.jay.infrastructure.dao.po.UserCreditOrder;
import java.math.BigDecimal;
import javax.sql.DataSource;
import org.junit.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public class Java25RouterIntegrationTest {
  @Configuration(proxyBeanMethods = false)
  @MapperScan("com.jay.infrastructure.dao")
  public static class DaoConfig {}

  public static ApplicationContextRunner databaseContext() {
    String host = System.getenv().getOrDefault("V2_MYSQL_HOST", "host.docker.internal:13316");
    String password =
        java.util.Objects.requireNonNull(
            System.getenv("V2_MYSQL_PASSWORD"), "Set V2_MYSQL_PASSWORD for isolated MySQL");
    return new ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                DataSourceAutoConfig.class,
                AopAutoConfiguration.class,
                MybatisAutoConfiguration.class))
        .withUserConfiguration(DaoConfig.class)
        .withPropertyValues(
            "mini-db-router.jdbc.datasource.dbCount=2",
            "mini-db-router.jdbc.datasource.tbCount=4",
            "mini-db-router.jdbc.datasource.routerKey=userId",
            "mini-db-router.jdbc.datasource.default=db00",
            "mini-db-router.jdbc.datasource.list=db01,db02",
            "mini-db-router.jdbc.datasource.global.username=root",
            "mini-db-router.jdbc.datasource.global.password=" + password,
            "mini-db-router.jdbc.datasource.global.driver-class-name=com.mysql.cj.jdbc.Driver",
            "mini-db-router.jdbc.datasource.global.pool.maximum-pool-size=8",
            "mini-db-router.jdbc.datasource.global.pool.minimum-idle=0",
            "mini-db-router.jdbc.datasource.db00.url=jdbc:mysql://"
                + host
                + "/router_control?useSSL=false&allowPublicKeyRetrieval=true",
            "mini-db-router.jdbc.datasource.db01.url=jdbc:mysql://"
                + host
                + "/router_one?useSSL=false&allowPublicKeyRetrieval=true",
            "mini-db-router.jdbc.datasource.db02.url=jdbc:mysql://"
                + host
                + "/router_two?useSSL=false&allowPublicKeyRetrieval=true",
            "mybatis.mapper-locations=classpath:/mybatis/mapper/*.xml");
  }

  @Test
  public void controlDaoUsesConfiguredControlDatabase() {
    databaseContext().run(c -> {
      assertThat(c).hasNotFailed();
      JdbcTemplate jdbc = new JdbcTemplate(c.getBean(DataSource.class));
      jdbc.execute("create table if not exists raffle_activity(activity_id bigint primary key,activity_name varchar(64),activity_desc varchar(64),begin_date_time datetime,end_date_time datetime,strategy_id bigint,state varchar(16)) ENGINE=InnoDB");
      jdbc.update("delete from raffle_activity where activity_id=987654321");
      jdbc.update("insert into raffle_activity(activity_id,strategy_id) values(987654321,123456789)");
      assertThat(c.getBean(com.jay.infrastructure.dao.IRaffleActivityDao.class).queryStrategyIdByActivityId(987654321L)).isEqualTo(123456789L);
    });
  }

  @Test
  public void actualCreditDaoRollsBackAcrossEveryShard() {
    databaseContext()
        .run(
            c -> {
              assertThat(c).hasNotFailed();
              JdbcTemplate j = new JdbcTemplate(c.getBean(DataSource.class));
              TransactionTemplate tx = c.getBean(TransactionTemplate.class);
              IUserCreditOrderDao dao = c.getBean(IUserCreditOrderDao.class);
              assertThat(j.queryForObject("select database()", String.class))
                  .isEqualTo("router_control");
              for (int db = 1; db <= 2; db++)
                for (int tb = 0; tb < 4; tb++) {
                  DBContextHolder.setDBKey(String.format("%02d", db));
                  DBContextHolder.setTBKey(String.format("%03d", tb));
                  try {
                    String table = "user_credit_order_" + String.format("%03d", tb);
                    j.execute(
                        "create table if not exists "
                            + table
                            + "(id bigint auto_increment primary key,user_id varchar(64),order_id"
                            + " varchar(64),trade_name varchar(64),trade_type"
                            + " varchar(16),trade_amount decimal(18,2),out_business_no"
                            + " varchar(64),create_time datetime,update_time datetime)"
                            + " ENGINE=InnoDB");
                    j.update("delete from " + table);
                    UserCreditOrder order = new UserCreditOrder();
                    order.setUserId("migration-test");
                    order.setOrderId("o1");
                    order.setTradeName("probe");
                    order.setTradeType("forward");
                    order.setTradeAmount(new BigDecimal("1.00"));
                    order.setOutBusinessNo("b1");
                    assertThatThrownBy(
                            () ->
                                tx.executeWithoutResult(
                                    s -> {
                                      dao.insert(order);
                                      throw new IllegalStateException("fault");
                                    }))
                        .isInstanceOf(IllegalStateException.class);
                    assertThat(j.queryForObject("select count(*) from " + table, Integer.class))
                        .isZero();
                    tx.executeWithoutResult(s -> dao.insert(order));
                    assertThat(
                            j.queryForObject("select trade_amount from " + table, BigDecimal.class))
                        .isEqualByComparingTo("1.00");
                  } finally {
                    DBContextHolder.clearDBKey();
                    DBContextHolder.clearTBKey();
                  }
                }
            });
  }
}
