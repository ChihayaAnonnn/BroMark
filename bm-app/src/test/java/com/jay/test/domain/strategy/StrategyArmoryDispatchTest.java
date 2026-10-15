package com.jay.test.domain.strategy;

import com.jay.domain.strategy.service.armory.IStrategyArmory;
import com.jay.domain.strategy.service.armory.IStrategyDispatch;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import jakarta.annotation.Resource;

/**
 * @author Jay
 * @date 2025/6/27 22:21
 * @description
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class StrategyArmoryDispatchTest {

    @Resource
    private IStrategyArmory strategyArmory;

    @Resource
    private IStrategyDispatch strategyDispatch;

    @Before
    public void setUp() throws Exception {
        boolean success = strategyArmory.assembleLotteryStrategy(100001L);
    }

    @Test
    public void test_getRandomAwardId() {
        log.info("测试结果：{} - 奖品ID值", strategyDispatch.getRandomAwardId(100001L));

    }

    @Test
    public void test_getRandomAwardIdWithRuleWeight() {
        log.info("4000 策略 - 奖品ID值：{}", strategyDispatch.getRandomAwardId(100001L, 4000L));
        log.info("5000 策略 - 奖品ID值：{}", strategyDispatch.getRandomAwardId(100001L, 5000L));
        log.info("6000 策略 - 奖品ID值：{}", strategyDispatch.getRandomAwardId(100001L, 6000L));

    }
}
