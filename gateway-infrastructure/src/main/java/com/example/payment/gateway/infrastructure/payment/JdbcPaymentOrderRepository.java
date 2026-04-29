package com.example.payment.gateway.infrastructure.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.payment.gateway.common.payment.PaymentOrderRecord;
import com.example.payment.gateway.common.payment.PaymentOrderRepository;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayPaymentOrderEntity;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayPaymentOrderMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "docker"})
public class JdbcPaymentOrderRepository implements PaymentOrderRepository {

  private final GatewayPaymentOrderMapper gatewayPaymentOrderMapper;

  public JdbcPaymentOrderRepository(GatewayPaymentOrderMapper gatewayPaymentOrderMapper) {
    this.gatewayPaymentOrderMapper = gatewayPaymentOrderMapper;
  }

  @Override
  public void save(PaymentOrderRecord record) {
    GatewayPaymentOrderEntity existing = gatewayPaymentOrderMapper.selectOne(new LambdaQueryWrapper<GatewayPaymentOrderEntity>()
        .eq(GatewayPaymentOrderEntity::getGatewayPaymentId, record.gatewayPaymentId())
        .last("LIMIT 1"));
    GatewayPaymentOrderEntity entity = existing == null ? new GatewayPaymentOrderEntity() : existing;
    entity.setGatewayPaymentId(record.gatewayPaymentId());
    entity.setMerchantId(record.merchantId());
    entity.setRequestId(record.requestId());
    entity.setIdempotencyKey(record.idempotencyKey());
    entity.setRouteCode(record.routeCode());
    entity.setTargetService(record.targetService());
    entity.setDownstreamPaymentId(record.downstreamPaymentId());
    entity.setPaymentStatus(record.paymentStatus());
    entity.setAmount(record.amount());
    entity.setCurrency(record.currency());
    if (existing == null) {
      gatewayPaymentOrderMapper.insert(entity);
    } else {
      gatewayPaymentOrderMapper.updateById(entity);
    }
  }

  @Override
  public Optional<PaymentOrderRecord> findByGatewayPaymentId(String gatewayPaymentId) {
    GatewayPaymentOrderEntity entity = gatewayPaymentOrderMapper.selectOne(new LambdaQueryWrapper<GatewayPaymentOrderEntity>()
        .eq(GatewayPaymentOrderEntity::getGatewayPaymentId, gatewayPaymentId)
        .last("LIMIT 1"));
    if (entity == null) {
      return Optional.empty();
    }
    return Optional.of(new PaymentOrderRecord(
        entity.getGatewayPaymentId(),
        entity.getMerchantId(),
        entity.getRequestId(),
        entity.getIdempotencyKey(),
        entity.getRouteCode(),
        entity.getTargetService(),
        entity.getDownstreamPaymentId(),
        entity.getPaymentStatus(),
        entity.getAmount(),
        entity.getCurrency()
    ));
  }

  @Override
  public Optional<PaymentOrderRecord> findByMerchantIdAndRequestId(String merchantId, String requestId) {
    GatewayPaymentOrderEntity entity = gatewayPaymentOrderMapper.selectOne(new LambdaQueryWrapper<GatewayPaymentOrderEntity>()
        .eq(GatewayPaymentOrderEntity::getMerchantId, merchantId)
        .eq(GatewayPaymentOrderEntity::getRequestId, requestId)
        .last("LIMIT 1"));
    if (entity == null) {
      return Optional.empty();
    }
    return Optional.of(new PaymentOrderRecord(
        entity.getGatewayPaymentId(),
        entity.getMerchantId(),
        entity.getRequestId(),
        entity.getIdempotencyKey(),
        entity.getRouteCode(),
        entity.getTargetService(),
        entity.getDownstreamPaymentId(),
        entity.getPaymentStatus(),
        entity.getAmount(),
        entity.getCurrency()
    ));
  }

  @Override
  public List<PaymentOrderRecord> findByPaymentStatus(String paymentStatus, int limit) {
    return gatewayPaymentOrderMapper.selectList(new LambdaQueryWrapper<GatewayPaymentOrderEntity>()
            .eq(GatewayPaymentOrderEntity::getPaymentStatus, paymentStatus)
            .orderByAsc(GatewayPaymentOrderEntity::getId)
            .last("LIMIT " + limit))
        .stream()
        .map(entity -> new PaymentOrderRecord(
            entity.getGatewayPaymentId(),
            entity.getMerchantId(),
            entity.getRequestId(),
            entity.getIdempotencyKey(),
            entity.getRouteCode(),
            entity.getTargetService(),
            entity.getDownstreamPaymentId(),
            entity.getPaymentStatus(),
            entity.getAmount(),
            entity.getCurrency()
        ))
        .collect(Collectors.toList());
  }
}
