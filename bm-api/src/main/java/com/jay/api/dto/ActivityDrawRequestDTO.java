package com.jay.api.dto;


import lombok.Data;

/**
 * @author Jay
 * @date 2025/7/26 16:01
 * @description 活动抽奖请求对象
 */
@Data
public class ActivityDrawRequestDTO {
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 活动ID
     */
    private Long activityId;
}
