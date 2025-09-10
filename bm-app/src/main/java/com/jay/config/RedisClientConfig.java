package com.jay.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jay
 * @date 2025/6/27 16:54
 * @description Redis 客户端，使用 Redisson <a href="https://github.com/redisson/redisson">Redisson</a>
 */
@Configuration
@EnableConfigurationProperties(RedisClientConfigProperties.class)
public class RedisClientConfig {

    @Bean("redissonClient")
    public RedissonClient redissonClient(ConfigurableApplicationContext applicationContext, RedisClientConfigProperties properties) {
        Config config = new Config();
        // 根据需要可以设定编解码器；https://github.com/redisson/redisson/wiki/4.-%E6%95%B0%E6%8D%AE%E5%BA%8F%E5%88%97%E5%8C%96
        config.setCodec(JsonJacksonCodec.INSTANCE);
        // 单机Redis配置，使用useSingleServer
        // config.useSingleServer()
        //         .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
        //         // .setPassword(properties.getPassword())
        //         .setConnectionPoolSize(properties.getPoolSize())
        //         .setConnectionMinimumIdleSize(properties.getMinIdleSize())
        //         .setIdleConnectionTimeout(properties.getIdleTimeout())
        //         .setConnectTimeout(properties.getConnectTimeout())
        //         .setRetryAttempts(properties.getRetryAttempts())
        //         .setRetryInterval(properties.getRetryInterval())
        //         .setPingConnectionInterval(properties.getPingInterval())
        //         .setKeepAlive(properties.isKeepAlive());

        String[] addressArr = Arrays.stream(properties.getNodes())
                .map(node -> {
                    return "redis://" + node;
                }).toArray(String[]::new);

        config.useClusterServers()
                // 添加集群节点地址，至少写 3 个主节点即可，写全更稳妥
                .addNodeAddress(addressArr)
                //设置密码
                // .setPassword(properties.getPassword())
                // 连接池配置
                .setMasterConnectionPoolSize(properties.getPoolSize())
                .setSlaveConnectionPoolSize(properties.getPoolSize())
                .setMasterConnectionMinimumIdleSize(properties.getMinIdleSize())
                .setSlaveConnectionMinimumIdleSize(properties.getMinIdleSize())

                // 超时相关
                .setIdleConnectionTimeout(properties.getIdleTimeout())
                .setConnectTimeout(properties.getConnectTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval())
                .setPingConnectionInterval(properties.getPingInterval())
                .setKeepAlive(properties.isKeepAlive());

        return Redisson.create(config);
    }

    static class RedisCodec extends BaseCodec {

        private final Encoder encoder = in -> {
            ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
            try {
                ByteBufOutputStream os = new ByteBufOutputStream(out);
                JSON.writeJSONString(os, in, SerializerFeature.WriteClassName);
                return os.buffer();
            } catch (IOException e) {
                out.release();
                throw e;
            } catch (Exception e) {
                out.release();
                throw new IOException(e);
            }
        };

        private final Decoder<Object> decoder = (buf, state) -> JSON.parseObject(new ByteBufInputStream(buf), Object.class);

        @Override
        public Decoder<Object> getValueDecoder() {
            return decoder;
        }

        @Override
        public Encoder getValueEncoder() {
            return encoder;
        }
    }
}
