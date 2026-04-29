package com.example.payment.gateway.infrastructure.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.payment.gateway.common.payment.RefundOrderRecord;
import com.example.payment.gateway.common.payment.RefundOrderRepository;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayRefundOrderEntity;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayRefundOrderMapper;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "docker"})
public class JdbcRefundOrderRepository implements RefundOrderRepository {

  private final GatewayRefundOrderMapper gatewayRefundOrderMapper;

  public JdbcRefundOrderRepository(GatewayRefundOrderMapper gatewayRefundOrderMapper) {
    this.gatewayRefundOrderMapper = gatewayRefundOrderMapper;
  }

  @Override
  public void save(RefundOrderRecord record) {
    GatewayRefundOrderEntity existing = gatewayRefundOrderMapper.selectOne(new LambdaQueryWrapper<GatewayRefundOrderEntity>()
        .eq(GatewayRefundOrderEntity::getGatewayRefundId, record.gatewayRefundId())
        .last("LIMIT 1"));
    GatewayRefundOrderEntity entity = existing == null ? new GatewayRefundOrderEntity() : existing;
    entity.setGatewayRefundId(record.gatewayRefundId());
    entity.setMerchantId(record.merchantId());
    entity.setRequestId(record.requestId());
    entity.setGatewayPaymentId(record.gatewayPaymentId());
    entity.setIdempotencyKey(record.idempotencyKey());
    entity.setRouteCode(record.routeCode());
    entity.setTargetService(record.targetService());
    entity.setDownstreamRefundId(record.downstreamRefundId());
    entity.setRefundStatus(record.refundStatus());
    entity.setAmount(record.amount());
    entity.setCurrency(record.currency());
    if (existing == null) {
      gatewayRefundOrderMapper.insert(entity);
    } else {
      gatewayRefundOrderMapper.updateById(entity);
    }
  }

  @Override
  public Optional<RefundOrderRecord> findByGatewayRefundId(String gatewayRefundId) {
    GatewayRefundOrderEntity entity = gatewayRefundOrderMapper.selectOne(new LambdaQueryWrapper<GatewayRefundOrderEntity>()
        .eq(GatewayRefundOrderEntity::getGatewayRefundId, gatewayRefundId)
        .last("LIMIT 1"));
    if (entity == null) {
      return Optional.empty();
    }
    return Optional.of(new RefundOrderRecord(
        entity.getGatewayRefundId(),
        entity.getMerchantId(),
        entity.getRequestId(),
        entity.getGatewayPaymentId(),
        entity.getIdempotencyKey(),
        entity.getRouteCode(),
        entity.getTargetService(),
        entity.getDownstreamRefundId(),
        entity.getRefundStatus(),
        entity.getAmount(),
        entity.getCurrency()
    ));
  }

  @Override
  public Optional<RefundOrderRecord> findByMerchantIdAndRequestId(String merchantId, String requestId) {
    GatewayRefundOrderEntity entity = gatewayRefundOrderMapper.selectOne(new LambdaQueryWrapper<GatewayRefundOrderEntity>()
        .eq(GatewayRefundOrderEntity::getMerchantId, merchantId)
        .eq(GatewayRefundOrderEntity::getRequestId, requestId)
        .last("LIMIT 1"));
    if (entity == null) {
      return Optional.empty();
    }
    return Optional.of(new RefundOrderRecord(
        entity.getGatewayRefundId(),
        entity.getMerchantId(),
        entity.getRequestId(),
        entity.getGatewayPaymentId(),
        entity.getIdempotencyKey(),
        entity.getRouteCode(),
        entity.getTargetService(),
        entity.getDownstreamRefundId(),
        entity.getRefundStatus(),
        entity.getAmount(),
        entity.getCurrency()
    ));
  }
}
