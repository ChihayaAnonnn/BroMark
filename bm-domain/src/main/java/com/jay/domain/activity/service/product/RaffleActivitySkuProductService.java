package com.jay.domain.activity.service.product;


import com.jay.domain.activity.model.entity.SkuProductEntity;
import com.jay.domain.activity.repository.IActivityRepository;
import com.jay.domain.activity.service.IRaffleActivitySkuProductService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * @author Jay
 * @date 2025/8/8 15:22
 * @description sku商品服务
 */
@Service
public class RaffleActivitySkuProductService implements IRaffleActivitySkuProductService {
    @Resource
    private IActivityRepository repository;

    @Override
    public List<SkuProductEntity> querySkuProductEntityListByActivityId(Long activityId) {
        return repository.querySkuProductEntityListByActivityId(activityId);
    }

}
