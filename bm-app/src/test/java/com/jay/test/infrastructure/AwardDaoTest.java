package com.jay.test.infrastructure;

import com.alibaba.fastjson.JSON;
import com.jay.infrastructure.dao.IAwardDao;
import com.jay.infrastructure.dao.po.Award;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author Jay
 * @date 2025/6/24 22:11
 * @description
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AwardDaoTest {

    @Resource
    private IAwardDao awardDao;

    @Test
    public void queryAllAwardsTest() {
        List<Award> awards = awardDao.queryAwardList();
        log.info("awards: {}", JSON.toJSONString(awards));
    }
}
