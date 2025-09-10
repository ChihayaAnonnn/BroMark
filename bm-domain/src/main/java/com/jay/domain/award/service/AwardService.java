package com.jay.domain.award.service;


import com.jay.domain.award.event.SendAwardMessageEvent;
import com.jay.domain.award.model.aggregate.UserAwardRecordAggregate;
import com.jay.domain.award.model.entity.DistributeAwardEntity;
import com.jay.domain.award.model.entity.TaskEntity;
import com.jay.domain.award.model.entity.UserAwardRecordEntity;
import com.jay.domain.award.model.vo.TaskStateVO;
import com.jay.domain.award.repository.IAwardRepository;
import com.jay.domain.award.service.distribute.IDistributeAward;
import com.jay.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Jay
 * @date 2025/7/26 00:18
 * @description 奖品服务
 */
@Slf4j
@Service
public class AwardService implements IAwardService {

    private final Map<String, IDistributeAward> distributeAwardMap;
    private IAwardRepository repository;
    private SendAwardMessageEvent sendAwardMessageEvent;

    public AwardService(IAwardRepository awardRepository, SendAwardMessageEvent sendAwardMessageEvent, Map<String, IDistributeAward> distributeAwardMap) {
        this.repository = awardRepository;
        this.sendAwardMessageEvent = sendAwardMessageEvent;
        this.distributeAwardMap = distributeAwardMap;
    }


    @Override
    public void saveUserAwardRecord(UserAwardRecordEntity userAwardRecordEntity) {
        // 1.构建消息实体对象
        SendAwardMessageEvent.SendAwardMessage sendAwardMessage = new SendAwardMessageEvent.SendAwardMessage();
        sendAwardMessage.setUserId(userAwardRecordEntity.getUserId());
        sendAwardMessage.setOrderId(userAwardRecordEntity.getOrderId());
        sendAwardMessage.setAwardId(userAwardRecordEntity.getAwardId());
        sendAwardMessage.setAwardTitle(userAwardRecordEntity.getAwardTitle());
        sendAwardMessage.setAwardConfig(userAwardRecordEntity.getAwardConfig());
        BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage> sendAwardMessageEventMessage
                = sendAwardMessageEvent.buildEventMessage(sendAwardMessage);

        // 2.构建任务对象
        TaskEntity taskEntity = TaskEntity.builder()
                .userId(userAwardRecordEntity.getUserId())
                .topic(sendAwardMessageEvent.topic())
                .messageId(sendAwardMessageEventMessage.getId())
                .message(sendAwardMessageEventMessage)
                .state(TaskStateVO.create)
                .build();

        // 3.构建聚合对象
        UserAwardRecordAggregate userAwardRecordAggregate = UserAwardRecordAggregate.builder()
                .userAwardRecordEntity(userAwardRecordEntity)
                .taskEntity(taskEntity)
                .build();

        // 4.存储聚合对象
        repository.saveUserAwardRecord(userAwardRecordAggregate);
    }

    @Override
    public void distributeAward(DistributeAwardEntity distributeAwardEntity) {
        // 奖品Key
        String awardKey = repository.queryAwardKey(distributeAwardEntity.getAwardId());
        if (null == awardKey) {
            log.error("分发奖品，奖品ID不存在。awardKey:{}", awardKey);
            return;
        }
        // 奖品服务
        IDistributeAward distributeAward = distributeAwardMap.get(awardKey);
        if (null == distributeAward) {
            // log.error("分发奖品，对应的服务不存在。awardKey:{}", awardKey);
            // throw new RuntimeException("分发奖品，奖品" + awardKey + "对应的服务不存在");
            return;
        }
        // 发放奖品
        distributeAward.giveOutPrizes(distributeAwardEntity);
    }

}
