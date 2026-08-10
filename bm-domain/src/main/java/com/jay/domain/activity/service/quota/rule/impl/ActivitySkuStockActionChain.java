package com.jay.domain.activity.service.quota.rule.impl;


import com.jay.domain.activity.model.entity.ActivityCountEntity;
import com.jay.domain.activity.model.entity.ActivityEntity;
import com.jay.domain.activity.model.entity.ActivitySkuEntity;
import com.jay.domain.activity.model.vo.ActivitySkuStockKeyVO;
import com.jay.domain.activity.repository.IActivityRepository;
import com.jay.domain.activity.service.armory.IActivityDispatch;
import com.jay.domain.activity.service.quota.rule.AbstractActionChain;
import com.jay.types.enums.ResponseCode;
import com.jay.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Jay
 * @date 2025/7/17 20:34
 * @description TODO
 */
@Slf4j
@Component("activity_sku_stock_action")
public class ActivitySkuStockActionChain extends AbstractActionChain {

    @Resource
    private IActivityRepository repository;
    @Resource
    private IActivityDispatch dispatch;

    @Override
    public boolean action(ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity) {
        log.info("活动责任链-商品库存处理【校验&扣减】开始。");
        // 扣减库存
        boolean status = dispatch.subtractionActivitySkuStock(activitySkuEntity.getSku(), activityEntity.getEndDateTime());
        // 扣减成功
        if (status) {
            log.info("活动责任链-商品库存处理【校验&扣减】成功。sku:{} activityId:{}", activitySkuEntity.getSku(), activityEntity.getActivityId());
            // 写入延迟队列
            repository.activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO.builder()
                    .sku(activitySkuEntity.getSku())
                    .activityId(activityEntity.getActivityId())
                    .build());

            return true;
        }

        throw new AppException(ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getCode(), ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getInfo());
    }
}
