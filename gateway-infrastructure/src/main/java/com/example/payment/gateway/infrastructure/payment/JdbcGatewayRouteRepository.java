package com.example.payment.gateway.infrastructure.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.payment.gateway.common.payment.GatewayRouteDefinition;
import com.example.payment.gateway.common.payment.GatewayRouteRepository;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayRouteConfigEntity;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayRouteConfigMapper;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "docker"})
public class JdbcGatewayRouteRepository implements GatewayRouteRepository {

  private final GatewayRouteConfigMapper gatewayRouteConfigMapper;

  public JdbcGatewayRouteRepository(GatewayRouteConfigMapper gatewayRouteConfigMapper) {
    this.gatewayRouteConfigMapper = gatewayRouteConfigMapper;
  }

  @Override
  public Optional<GatewayRouteDefinition> findRoute(String bizType, String apiCode) {
    GatewayRouteConfigEntity entity = gatewayRouteConfigMapper.selectOne(new LambdaQueryWrapper<GatewayRouteConfigEntity>()
        .eq(GatewayRouteConfigEntity::getBizType, bizType)
        .eq(GatewayRouteConfigEntity::getApiCode, apiCode)
        .eq(GatewayRouteConfigEntity::getStatus, 1)
        .orderByAsc(GatewayRouteConfigEntity::getPriority)
        .last("LIMIT 1"));
    if (entity == null) {
      return Optional.empty();
    }
    return Optional.of(new GatewayRouteDefinition(
        entity.getRouteCode(),
        entity.getBizType(),
        entity.getApiCode(),
        entity.getTargetProtocol(),
        entity.getTargetService(),
        entity.getTargetMethod(),
        entity.getTimeoutMs(),
        entity.getRetryTimes(),
        entity.getSentinelResource()
    ));
  }
}
