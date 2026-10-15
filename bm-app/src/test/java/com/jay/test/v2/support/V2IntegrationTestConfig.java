package com.jay.test.v2.support;

import com.jay.config.V2TransactionConfig;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
@Import(V2TransactionConfig.class)
public class V2IntegrationTestConfig {}
