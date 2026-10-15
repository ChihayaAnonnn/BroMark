package com.jay.test.redis;

import com.jay.infrastructure.redis.IRedisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import jakarta.annotation.Resource;

/**
 * @author Jay
 * @date 2025/6/24 22:11
 * @description
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisTest {

    @Resource
    private IRedisService redisService;

    @Autowired
    private RedissonClient redissonClient;
    /**
     * 测试 String 存取
     */
    @Test
    public void testStringOps() {
        redisService.setValue("test:string", "Hello Redis Cluster!");
        String value = redisService.getValue("test:string");
        log.info("String Value: {}", value);
    }
    @Test
    public void findSlot(){
        RKeys keys = redissonClient.getKeys();
        String mKey = "test:string";
        int slot = keys.getSlot(mKey);
        log.info("Slot Value: {}", slot);

        

    }
}
