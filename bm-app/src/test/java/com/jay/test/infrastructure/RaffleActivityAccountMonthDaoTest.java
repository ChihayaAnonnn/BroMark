package com.jay.test.infrastructure;


import com.jay.infrastructure.dao.IRaffleActivityAccountMonthDao;
import com.jay.infrastructure.dao.po.RaffleActivityAccountMonth;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import jakarta.annotation.Resource;

/**
 * @author Jay
 * @date 2025/7/24 23:19
 * @description TODO
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RaffleActivityAccountMonthDaoTest {
    @Resource
    private IRaffleActivityAccountMonthDao raffleActivityAccountMonthDao;

    @Test
    public void test_queryActivityAccountMonth() {
        RaffleActivityAccountMonth raffleActivityAccountMonth = raffleActivityAccountMonthDao.queryActivityAccountMonth(
                RaffleActivityAccountMonth.builder()
                        .userId("Jay")
                        .activityId(100301L)
                        .month("2025-07")
                        .build()
        );
        log.info("queryActivityAccountMonth:{}", raffleActivityAccountMonth);
    }
}
