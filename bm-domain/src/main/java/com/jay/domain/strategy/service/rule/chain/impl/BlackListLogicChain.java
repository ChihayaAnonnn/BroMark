package com.jay.domain.strategy.service.rule.chain.impl;

import com.jay.domain.strategy.repository.IStrategyRepository;
import com.jay.domain.strategy.service.rule.chain.AbstractLogicChain;
import com.jay.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.jay.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * @author Jay
 * @date 2025/7/4 14:29
 * @description 抽奖责任链 - 黑名单方法
 */
@Slf4j
@Component("rule_blacklist")
public class BlackListLogicChain extends AbstractLogicChain {

    @Resource
    private IStrategyRepository repository;

    @Override
    public DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
        log.info("抽奖责任链 - 黑名单处理开始 userId: {}, strategyId: {}, ruleModel: {}", userId, strategyId, ruleModel());
        String ruleValue = repository.queryStrategyRuleValue(strategyId, ruleModel());
        // 拆分
        String[] splitRuleValues = ruleValue.split(Constants.COLON);
        // 黑名单只能获得的奖品
        Integer awardId = Integer.parseInt(splitRuleValues[0]);
        String[] userBlackIds = splitRuleValues[1].split(Constants.SPLIT);

        for (String userBlackId : userBlackIds) {
            if (userId.equals(userBlackId)) {
                log.info("抽奖责任链 - 黑名单接管 userId: {}, strategyId: {}, ruleModel: {}, awardId: {}", userId, strategyId, ruleModel(), awardId);
                return DefaultChainFactory.StrategyAwardVO.builder()
                        .awardId(awardId)
                        .logicModel(ruleModel())
                        //黑名单 0.01 ~ 1积分
                        .awardRuleValue("0.01,1")
                        .build();
            }
        }
        // 放行
        log.info("抽奖责任链 - 黑名单放行 userId: {}, strategyId: {}, ruleModel: {}", userId, strategyId, ruleModel());
        return next().logic(userId, strategyId);
    }

    @Override
    protected String ruleModel() {
        return DefaultChainFactory.LogicModel.RULE_BLACKLIST.getCode();
    }
}
