package com.jay.test.v2.support;
import org.springframework.context.annotation.*;
import com.jay.config.V2TransactionConfig;
@Configuration(proxyBeanMethods=false) @Import(V2TransactionConfig.class)
public class V2IntegrationTestConfig {}
