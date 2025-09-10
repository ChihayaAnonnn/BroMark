package com.jay.domain.strategy.service;

import com.jay.domain.strategy.model.entity.RaffleAwardEntity;
import com.jay.domain.strategy.model.entity.RaffleFactorEntity;
import com.jay.domain.strategy.model.entity.StrategyAwardEntity;
import com.jay.domain.strategy.repository.IStrategyRepository;
import com.jay.domain.strategy.service.armory.IStrategyDispatch;
import com.jay.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import com.jay.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.jay.types.enums.ResponseCode;
import com.jay.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Jay
 * @date 2025/6/30 16:40
 * @description 抽奖策略抽象类（模板模式）
 */
@Slf4j
public abstract class AbstractRaffleStrategy implements IRaffleStrategy {

    // 策略仓储服务 -> 提供数据
    protected IStrategyRepository repository;
    // 策略调度服务 -> 负责抽奖处理，通过新增接口的方式，隔离职责，使用方不需要关心抽奖的初始化
    protected IStrategyDispatch dispatch;
    // 抽奖的责任链 -> 从抽奖的规则中，解耦出前置规则为责任链处理
    protected final DefaultChainFactory chainFactory;
    // 抽奖的决策树 -> 负责抽奖中到抽奖后的规则过滤，如抽奖到A奖品ID，之后要做次数的判断和库存的扣减等。
    protected final DefaultTreeFactory treeFactory;

    public AbstractRaffleStrategy(DefaultTreeFactory treeFactory, DefaultChainFactory chainFactory, IStrategyDispatch dispatch, IStrategyRepository repository) {
        this.treeFactory = treeFactory;
        this.chainFactory = chainFactory;
        this.dispatch = dispatch;
        this.repository = repository;
    }

    @Override
    public RaffleAwardEntity performRaffle(RaffleFactorEntity entity) {
        // 1.参数校验
        String userId = entity.getUserId();
        Long strategyId = entity.getStrategyId();
        if (StringUtils.isBlank(userId) || strategyId == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        // 2.责任链抽奖，得到最初的奖品ID【黑名单、权重抽奖直接返回】
        DefaultChainFactory.StrategyAwardVO chainAwardVO = raffleLogicChain(userId, strategyId);
        log.info("抽奖策略 - 责任链 {} {} {} {}", userId, strategyId, chainAwardVO.getAwardId(), chainAwardVO.getLogicModel());
        // 3.当责任链中的特殊规则（如权重抽奖、黑名单抽奖）已经确定了奖品时，就不需要再经过决策树的复杂处理
        if (!chainAwardVO.getLogicModel().equals(DefaultChainFactory.LogicModel.RULE_DEFAULT.getCode())) {
            return buildRaffleAwardEntity(strategyId, chainAwardVO.getAwardId(), chainAwardVO.getAwardRuleValue());
        }

        // 3.对责任链的抽奖结果进行规则过滤（拿到奖品ID后）
        DefaultTreeFactory.StrategyAwardVO treeAwardVO = raffleLogicTree(userId, strategyId, chainAwardVO.getAwardId());
        log.info("抽奖策略 - 规则树 {} {} {} {}", userId, strategyId, treeAwardVO.getAwardId(), treeAwardVO.getAwardRuleValue());

        return buildRaffleAwardEntity(strategyId, treeAwardVO.getAwardId(), treeAwardVO.getAwardRuleValue());
    }

    private RaffleAwardEntity buildRaffleAwardEntity(Long strategyId, Integer awardId, String awardConfig) {
        StrategyAwardEntity strategyAward = repository.queryStrategyAwardEntity(strategyId, awardId);
        return RaffleAwardEntity.builder()
                .awardTitle(strategyAward.getAwardTitle())
                .awardId(awardId)
                .awardConfig(awardConfig)
                .sort(strategyAward.getSort())
                .build();
    }

    /**
     * 抽奖计算，责任链抽象方法
     *
     * @param userId     用户ID
     * @param strategyId 策略ID
     * @return 奖品信息
     */
    public abstract DefaultChainFactory.StrategyAwardVO raffleLogicChain(String userId, Long strategyId);

    /**
     * 抽奖结果过滤，决策树抽象方法
     *
     * @param userId     用户ID
     * @param strategyId 策略ID
     * @param awardId    奖品ID
     * @return 过滤结果【奖品ID，会根据抽奖次数判断、库存判断、兜底兜里返回最终的可获得奖品信息】
     */
    public abstract DefaultTreeFactory.StrategyAwardVO raffleLogicTree(String userId, Long strategyId, Integer awardId);
}
