package com.jay.trigger.listener;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.jay.domain.award.event.SendAwardMessageEvent;
import com.jay.domain.award.model.entity.DistributeAwardEntity;
import com.jay.domain.award.service.IAwardService;
import com.jay.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * @author Jay
 * @date 2025/7/26 11:25
 * @description TODO
 */
@Slf4j
@Component
public class SendAwardCustomer {

    @Resource
    private IAwardService awardService;

    @Value("${spring.rabbitmq.topic.send_award}")
    private String topic;

    @RabbitListener(queuesToDeclare = @Queue(value = "${spring.rabbitmq.topic.send_award}"))
    public void listener(String message) {
        try {
            log.info("监听用户奖品发送消息 topic:{} message:{}", topic, message);

            BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage> eventMessage = JSON.parseObject(message,
                    new TypeReference<BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage>>() {
                    }.getType());

            SendAwardMessageEvent.SendAwardMessage data = eventMessage.getData();
            // 发放奖品
            DistributeAwardEntity distributeAwardEntity = new DistributeAwardEntity();
            distributeAwardEntity.setUserId(data.getUserId());
            distributeAwardEntity.setOrderId(data.getOrderId());
            distributeAwardEntity.setAwardId(data.getAwardId());
            distributeAwardEntity.setAwardConfig(data.getAwardConfig());
            awardService.distributeAward(distributeAwardEntity);

        } catch (Exception e) {
            log.error("监听用户奖品发送消息，消费失败 topic:{} message:{}", topic, message);
            throw e;
        }
    }
}
