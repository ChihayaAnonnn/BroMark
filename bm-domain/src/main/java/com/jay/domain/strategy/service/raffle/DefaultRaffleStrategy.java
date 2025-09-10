package com.jay.domain.strategy.service.raffle;

import com.jay.domain.strategy.model.entity.StrategyAwardEntity;
import com.jay.domain.strategy.model.vo.RuleWeightVO;
import com.jay.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.jay.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.jay.domain.strategy.model.vo.tree.RuleTreeVO;
import com.jay.domain.strategy.repository.IStrategyRepository;
import com.jay.domain.strategy.service.AbstractRaffleStrategy;
import com.jay.domain.strategy.service.IRaffleAward;
import com.jay.domain.strategy.service.IRaffleRule;
import com.jay.domain.strategy.service.IRaffleStock;
import com.jay.domain.strategy.service.armory.IStrategyDispatch;
import com.jay.domain.strategy.service.rule.chain.ILogicChain;
import com.jay.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.jay.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.jay.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Jay
 * @date 2025/6/30 22:05
 * @description
 */
@Slf4j
@Service
public class DefaultRaffleStrategy extends AbstractRaffleStrategy implements IRaffleAward, IRaffleStock, IRaffleRule {

    public DefaultRaffleStrategy(DefaultTreeFactory treeFactory, DefaultChainFactory chainFactory, IStrategyDispatch dispatch, IStrategyRepository repository) {
        super(treeFactory, chainFactory, dispatch, repository);
    }

    @Override
    public DefaultChainFactory.StrategyAwardVO raffleLogicChain(String userId, Long strategyId) {
        ILogicChain logicChain = chainFactory.openLogicChain(strategyId);
        return logicChain.logic(userId, strategyId);
    }

    @Override
    public DefaultTreeFactory.StrategyAwardVO raffleLogicTree(String userId, Long strategyId, Integer awardId) {
        StrategyAwardRuleModelVO ruleModelVO = repository.queryStrategyAwardRuleModelVO(strategyId, awardId);
        if (ruleModelVO == null) {
            return DefaultTreeFactory.StrategyAwardVO.builder().awardId(awardId).build();
        }
        RuleTreeVO ruleTreeVO = repository.queryRuleTreeVOByTreeId(ruleModelVO.getRuleModels());
        if (ruleTreeVO == null) {
            throw new RuntimeException("存在抽奖策略配置的规则模型 Key，未在库表 rule_tree、rule_tree_node、rule_tree_line 配置对应的规则树信息 " + ruleModelVO.getRuleModels());
        }
        IDecisionTreeEngine treeEngine = treeFactory.openLogicTree(ruleTreeVO);
        return treeEngine.process(userId, strategyId, awardId);
    }

    @Override
    public StrategyAwardStockKeyVO takeQueueValue() throws InterruptedException {
        return repository.takeQueueValue();
    }

    @Override
    public void updateStrategyAwardStock(Long strategyId, Integer awardId) {
        repository.updateStrategyAwardStock(strategyId, awardId);
    }

    @Override
    public List<StrategyAwardEntity> queryRaffleStrategyAwards(Long strategyId) {
        long start = System.currentTimeMillis();
        try{
            return repository.queryStrategyAwardList(strategyId);
        }finally {
            long end = System.currentTimeMillis();
            log.info("queryRaffleStrategyAwards 执行耗时: {} ms", (end - start));
        }

    }

    @Override
    public List<StrategyAwardEntity> queryRaffleStrategyAwardsByActivityId(Long activityId) {
        Long strategyId = repository.queryStrategyIdByActivityId(activityId);
        return queryRaffleStrategyAwards(strategyId);
    }

    @Override
    public Map<String, Integer> queryAwardRuleLockCount(String... treeIds) {
        return repository.queryAwardRuleLockCount(treeIds);
    }

    @Override
    public List<RuleWeightVO> queryAwardRuleWeight(Long strategyId) {
        return repository.queryAwardRuleWeight(strategyId);
    }


    @Override
    public List<RuleWeightVO> queryAwardRuleWeightByActivityId(Long activityId) {
        Long strategyId = repository.queryStrategyIdByActivityId(activityId);
        return queryAwardRuleWeight(strategyId);
    }
}
