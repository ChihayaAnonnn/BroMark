package com.jay.domain.strategy.repository;

import com.jay.domain.strategy.model.entity.StrategyAwardEntity;
import com.jay.domain.strategy.model.entity.StrategyEntity;
import com.jay.domain.strategy.model.entity.StrategyRuleEntity;
import com.jay.domain.strategy.service.armory.algorithm.AliasTable;
import com.jay.domain.strategy.model.vo.RuleWeightVO;
import com.jay.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.jay.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.jay.domain.strategy.model.vo.tree.RuleTreeVO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jay
 * @date 2025/6/27 19:56
 * @description 策略仓储接口
 */
public interface IStrategyRepository {

    List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId);

    void storeStrategyAwardSearchRateTables(String key, Integer rateRange, HashMap<Integer, Integer> shuffledStrategyAwardSearchRateTables);

    int getRateRange(Long strategyId);

    int getRateRange(String key);

    Integer getStrategyAwardAssemble(String key, int rateKey);

    void storeStrategyAliasTable(String key, AliasTable aliasTable);

    AliasTable getStrategyAliasTable(String key);

    StrategyEntity queryStrategyEntityByStrategyId(Long strategyId);

    StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel);

    String queryStrategyRuleValue(Long strategyId, Integer awardId, String ruleModel);

    String queryStrategyRuleValue(Long strategyId, String ruleModel);

    StrategyAwardRuleModelVO queryStrategyAwardRuleModelVO(Long strategyId, Integer awardId);

    RuleTreeVO queryRuleTreeVOByTreeId(String treeId);

    /**
     * 缓存奖品库存
     * @param cacheKey key
     * @param awardCount 库存值
     */
    void cacheStrategyAwardCount(String cacheKey, Integer awardCount);

    /**
     * 缓存key，decr方式扣减库存
     * @param cacheKey key
     * @return 扣减结果
     */
    Boolean subtractionAwardStock(String cacheKey);

    void awardStockConsumeSendQueue(StrategyAwardStockKeyVO stockKeyVO);

    StrategyAwardStockKeyVO takeQueueValue();

    void updateStrategyAwardStock(Long strategyId, Integer awardId);

    StrategyAwardEntity queryStrategyAwardEntity(Long strategyId, Integer awardId);

    Long queryStrategyIdByActivityId(Long activityId);

    Integer queryTodayUserRaffleCount(String userId, Long strategyId);

    Map<String, Integer> queryAwardRuleLockCount(String[] treeIds);

    Integer queryActivityAccountTotalUseCount(String userId, Long strategyId);

    List<RuleWeightVO> queryAwardRuleWeight(Long strategyId);
}
