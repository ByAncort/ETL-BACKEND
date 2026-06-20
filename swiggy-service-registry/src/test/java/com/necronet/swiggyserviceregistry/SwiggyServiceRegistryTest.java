package com.necronet.swiggyserviceregistry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.server.EurekaServerInitializerConfiguration;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SwiggyServiceRegistryTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    void contextHasEurekaServerInitializerConfiguration() {
        assertThat(context.getBeanNamesForType(EurekaServerInitializerConfiguration.class))
                .as("EurekaServerInitializerConfiguration bean should be present")
                .hasSize(1);
    }
}
