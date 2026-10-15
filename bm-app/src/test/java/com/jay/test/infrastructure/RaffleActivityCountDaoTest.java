package com.jay.test.infrastructure;


import com.jay.infrastructure.dao.IRaffleActivityCountDao;
import com.jay.infrastructure.dao.po.RaffleActivityCount;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import jakarta.annotation.Resource;

/**
 * @author Jay
 * @date 2025/7/17 18:28
 * @description TODO
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RaffleActivityCountDaoTest {
    @Resource
    private IRaffleActivityCountDao countDao;

    @Test
    public void test_query() {
        RaffleActivityCount raffleActivityCount = countDao.queryRaffleActivityCountByActivityCountId(11101L);
        log.info("raffleActivityCount={}", raffleActivityCount);
    }
}
