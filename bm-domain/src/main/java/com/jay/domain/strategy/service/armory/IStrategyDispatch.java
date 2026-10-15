package com.jay.domain.strategy.service.armory;


/**
 * @author Jay
 * @date 2025/6/29 14:50
 * @description
 */
public interface IStrategyDispatch {

    /**
     * 获取抽奖策略装配的随机结果
     * @param strategyId
     * @return 抽奖结果
     */
    Integer getRandomAwardId(Long strategyId);

    Integer getRandomAwardId(Long strategyId, Long ruleWeightKey);

    /**
     * 根据策略ID和奖品ID，扣减奖品库存
     * @param strategyId
     * @param awardId
     * @return
     */
    Boolean subtractionAwardStock(Long strategyId, Integer awardId);
}
