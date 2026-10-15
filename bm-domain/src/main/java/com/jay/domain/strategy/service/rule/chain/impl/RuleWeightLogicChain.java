package com.jay.domain.strategy.service.rule.chain.impl;

import com.jay.domain.strategy.repository.IStrategyRepository;
import com.jay.domain.strategy.service.armory.IStrategyDispatch;
import com.jay.domain.strategy.service.rule.chain.AbstractLogicChain;
import com.jay.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.jay.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.*;

/**
 * @author Jay
 * @date 2025/7/4 14:31
 * @description 抽奖责任链 - 权重抽奖方法
 */
@Slf4j
@Component("rule_weight")
public class RuleWeightLogicChain extends AbstractLogicChain {

    @Resource
    private IStrategyRepository repository;

    @Resource
    private IStrategyDispatch dispatch;

    /**
     * 权重责任链过滤：<p>
     * 1.权重规则格式;4000:102,103,104,105<p>
     * 2.解析数据格式，判断哪个范围符合用户的特定抽奖范围
     *
     * @param userId     用户ID
     * @param strategyId 策略ID
     * @return 奖品ID
     */
    @Override
    public DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
        log.info("抽奖责任链 - 权重判定开始 userId:{}, strategyId:{}, ruleModel:{}", userId, strategyId, ruleModel());

        String ruleValue = repository.queryStrategyRuleValue(strategyId, null, ruleModel());

        // 1.解析权重策略
        Map<Long, String> weightGroup = parseRuleWeight(ruleValue);
        // 2.判断权重策略是否存在
        if (weightGroup == null || weightGroup.isEmpty()) {
            log.info("抽奖责任链 - 权重判定放行");
            // 2.1.不存在，放行
            return next().logic(userId, strategyId);
        }

        Integer userScore = repository.queryActivityAccountTotalUseCount(userId, strategyId);
        // 2.2.权重规则存在，转换key值，找出最小符合规则
        List<Long> keys = new ArrayList<>(weightGroup.keySet());
        Long fittedKey = keys.stream()
                .filter(key -> userScore >= key)
                .max(Comparator.naturalOrder())
                .orElse(null);

        if (fittedKey != null) {
            Integer awardId = dispatch.getRandomAwardId(strategyId, fittedKey);
            log.info("抽奖责任链 - 权重抽奖接管 userId: {}, strategyId: {}, ruleModel: {}, awardId: {}", userId, strategyId, ruleModel(), awardId);
            return DefaultChainFactory.StrategyAwardVO.builder()
                    .awardId(awardId)
                    .logicModel(ruleModel())
                    .build();
        }

        log.info("抽奖责任链 - 权重判定放行 userId:{}, strategyId:{}, ruleModel:{}", userId, strategyId, ruleModel());
        return next().logic(userId, strategyId);
    }

    @Override
    protected String ruleModel() {
        return DefaultChainFactory.LogicModel.RULE_WEIGHT.getCode();
    }

    private Map<Long, String> parseRuleWeight(String ruleWeights) {
        // 1.分割不同积分的规则（空格）
        String[] weightGroups = ruleWeights.split(Constants.SPACE);
        Map<Long, String> weightMap = new HashMap<>();

        for (String group : weightGroups) {
            // 检查输入
            if (StringUtils.isBlank(group)) {
                return weightMap;
            }
            // 2.分割key和value
            String[] kv = group.split(Constants.COLON);
            if (kv.length != 2) {
                throw new IllegalArgumentException("rule_weight rule_rule invalid input format：" + group);
            }
            // 3.解析值
            weightMap.put(Long.parseLong(kv[0]), group);
        }
        return weightMap;
    }
}
