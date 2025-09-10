package com.jay.domain.strategy.service.rule.tree.factory.engine.impl;

import com.jay.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.jay.domain.strategy.model.vo.tree.RuleTreeNodeLineVO;
import com.jay.domain.strategy.model.vo.tree.RuleTreeNodeVO;
import com.jay.domain.strategy.model.vo.tree.RuleTreeVO;
import com.jay.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.jay.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.jay.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * @author Jay
 * @date 2025/7/5 15:36
 * @description 决策树引擎
 */
@Slf4j
public class DecisionTreeEngine implements IDecisionTreeEngine {

    private final Map<String, ILogicTreeNode> logicTreeNodeMap;

    private final RuleTreeVO ruleTreeVO;

    public DecisionTreeEngine(Map<String, ILogicTreeNode> logicTreeNodeMap, RuleTreeVO ruleTreeVO) {
        this.logicTreeNodeMap = logicTreeNodeMap;
        this.ruleTreeVO = ruleTreeVO;
    }

    @Override
    public DefaultTreeFactory.StrategyAwardVO process(String userId, Long strategyId, Integer awardId) {
        DefaultTreeFactory.StrategyAwardVO strategyAwardVO = null;

        // 获取基础信息（树根结点，所有树节点信息）
        String nextNode = ruleTreeVO.getTreeRootNode();
        Map<String, RuleTreeNodeVO> treeNodeMap = ruleTreeVO.getTreeNodeMap();

        // 执行过程
        RuleTreeNodeVO curNode = treeNodeMap.get(nextNode);
        while (nextNode != null) {
            ILogicTreeNode logicTreeNode = logicTreeNodeMap.get(curNode.getRuleKey());
            String ruleValue = curNode.getRuleValue();

            // 树节点执行结果
            DefaultTreeFactory.TreeActionEntity logicEntity = logicTreeNode.logic(userId, strategyId, awardId, ruleValue);
            // 执行结果校验对象
            RuleLogicCheckTypeVO checkType = logicEntity.getRuleLogicCheckType();
            // 策略奖品数据
            strategyAwardVO = logicEntity.getStrategyAwardVO();
            log.info("决策树引擎 - {} treeId:{}, node:{}, code:{}", ruleTreeVO.getTreeName(), ruleTreeVO.getTreeId(), nextNode, checkType.getCode());

            // 获取下一个树节点
            nextNode = nextNode(checkType.getCode(), curNode.getNodeLineVOList());
            curNode = treeNodeMap.get(nextNode);
        }

        return strategyAwardVO;
    }

    /**
     * 获取下一个节点
     *
     * @param matterValue            校验结果
     * @param ruleTreeNodeLineVOList 与子节点的连线
     * @return 下一个节点
     */
    private String nextNode(String matterValue, List<RuleTreeNodeLineVO> ruleTreeNodeLineVOList) {
        if (ruleTreeNodeLineVOList == null || ruleTreeNodeLineVOList.isEmpty()) {
            return null;
        }
        // 找到符合要求的子节点
        for (RuleTreeNodeLineVO nodeLine : ruleTreeNodeLineVOList) {
            if (decisionLogic(matterValue, nodeLine)) {
                return nodeLine.getRuleNodeTo();
            }
        }
        // log.warn("决策树引擎 - nextNode 计算失败，未找到可执行节点！");
        return null;
    }

    /**
     * @param matterValue 校验结果
     * @param nodeLine    子节点的连线
     * @return 是否满足到达子节点的条件
     */
    public boolean decisionLogic(String matterValue, RuleTreeNodeLineVO nodeLine) {
        switch (nodeLine.getRuleLimitType()) {
            case EQUAL:
                return matterValue.equals(nodeLine.getRuleLimitValue().getCode());
            case GT:
            case LT:
            case GE:
            case LE:
            default:
                return false;
        }
    }
}
