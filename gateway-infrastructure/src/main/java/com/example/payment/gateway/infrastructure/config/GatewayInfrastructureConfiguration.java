package com.example.payment.gateway.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local", "docker"})
@MapperScan("com.example.payment.gateway.infrastructure.mybatis")
@EnableConfigurationProperties(GatewayTechProperties.class)
public class GatewayInfrastructureConfiguration {
}
