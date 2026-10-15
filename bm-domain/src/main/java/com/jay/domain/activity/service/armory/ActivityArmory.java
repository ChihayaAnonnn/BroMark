package com.jay.domain.activity.service.armory;


import com.jay.domain.activity.model.entity.ActivitySkuEntity;
import com.jay.domain.activity.repository.IActivityRepository;
import com.jay.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author Jay
 * @date 2025/7/19 15:55
 * @description 装配活动
 */
@Slf4j
@Service
public class ActivityArmory implements IActivityArmory, IActivityDispatch {

    @Resource
    private IActivityRepository repository;

    @Override
    public boolean assembleActivitySkuByActivity(Long activityId) {

        // 预热活动
        repository.queryRaffleActivityByActivityId(activityId);

        List<ActivitySkuEntity> activitySkuEntityList = repository.queryActivitySkuListByActivityId(activityId);
        for (ActivitySkuEntity entity : activitySkuEntityList) {
            cacheActivitySkuStockCount(entity.getSku(), entity.getStockCountSurplus());
            // 预热活动次数
            repository.queryRaffleActivityCountByActivityCountId(entity.getActivityCountId());
        }
        return true;
    }

    @Override
    public boolean assembleActivitySku(Long sku) {
        ActivitySkuEntity activitySkuEntity = repository.queryActivitySku(sku);
        cacheActivitySkuStockCount(sku, activitySkuEntity.getStockCount());
        // 预热活动
        repository.queryRaffleActivityByActivityId(activitySkuEntity.getActivityId());
        // 预热活动次数
        repository.queryRaffleActivityCountByActivityCountId(activitySkuEntity.getActivityCountId());
        return true;
    }

    private void cacheActivitySkuStockCount(Long sku, Integer stockCount) {
        String key = Constants.RedisKey.ACTIVITY_SKU_STOCK_COUNT_KEY + sku;
        repository.cacheActivitySkuStockCount(key, stockCount);
    }

    @Override
    public boolean subtractionActivitySkuStock(Long sku, Date endDateTime) {
        String key = Constants.RedisKey.ACTIVITY_SKU_STOCK_COUNT_KEY + sku;
        return repository.subtractionActivitySkuStock(sku, key, endDateTime);
    }
}
