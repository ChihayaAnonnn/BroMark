package com.jay.domain.activity.repository;


import com.jay.domain.activity.model.aggregate.CreateOrderAggregate;
import com.jay.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.jay.domain.activity.model.entity.*;
import com.jay.domain.activity.model.vo.ActivitySkuStockKeyVO;

import java.util.Date;
import java.util.List;

/**
 * @author Jay
 * @date 2025/7/16 16:33
 * @description 活动仓储接口
 */
public interface IActivityRepository {
    ActivitySkuEntity queryActivitySku(Long sku);

    ActivityEntity queryRaffleActivityByActivityId(Long activityId);

    ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId);

    void cacheActivitySkuStockCount(String key, Integer stockCount);

    boolean subtractionActivitySkuStock(Long sku, String key, Date endDateTime);

    void activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO activitySkuStockKeyVO);

    ActivitySkuStockKeyVO takeQueueValue();

    void clearQueue();

    void updateActivitySkuStock(Long sku);

    void clearActivitySkuStock(Long sku);

    UserRaffleOrderEntity queryNoUsedRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity);

    void saveCreatePartakeOrderAggregate(CreateQuotaOrderAggregate createQuotaOrderAggregate);

    ActivityAccountDayEntity queryActivityAccountDay(String userId, Long activityId, String day);

    ActivityAccountMonthEntity queryActivityAccountMonth(String userId, Long activityId, String month);

    ActivityAccountEntity queryActivityAccount(String userId, Long activityId);

    List<ActivitySkuEntity> queryActivitySkuListByActivityId(Long activityId);

    Integer queryRaffleActivityAccountDayPartakeCount(String userId, Long activityId);

    Integer queryRaffleActivityAccountPartakeCount(Long activityId, String userId);

    ActivityAccountEntity queryActivityAccountEntity(Long activityId, String userId);

    void doSaveNoPayOrder(CreateOrderAggregate createOrderAggregate);

    void doSaveCreditPayOrder(CreateOrderAggregate createOrderAggregate);

    void updateOrder(DeliveryOrderEntity deliveryOrderEntity);

    UnpaidActivityOrderEntity queryUnpaidActivityOrder(SkuRechargeEntity skuRechargeEntity);

    List<SkuProductEntity> querySkuProductEntityListByActivityId(Long activityId);

    void createActivityAccount(ActivityAccountEntity activityAccountEntity);
}
