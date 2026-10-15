package com.jay.test.infrastructure;

import com.alibaba.fastjson2.JSON;
import com.jay.domain.strategy.model.vo.tree.RuleTreeVO;
import com.jay.domain.strategy.repository.IStrategyRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import jakarta.annotation.Resource;

/**
 * @author Jay
 * @date 2025/6/27 22:58
 * @description 仓储测试
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class StrategyRepositoryTest {
    @Resource
    private IStrategyRepository repository;

    @Test
    public void assembleLotteryStrategyTest(){
        repository.queryStrategyAwardList(100001L);
    }

    @Test
    public void queryRuleTreeTest(){
        RuleTreeVO ruleTreeVO = repository.queryRuleTreeVOByTreeId("tree_lock");
        log.info(JSON.toJSONString(ruleTreeVO));
    }
}
