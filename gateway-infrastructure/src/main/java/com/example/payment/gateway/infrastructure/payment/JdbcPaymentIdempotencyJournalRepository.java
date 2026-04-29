package com.example.payment.gateway.infrastructure.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.payment.gateway.common.idempotency.PaymentIdempotencyRecord;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayIdempotencyRecordEntity;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayIdempotencyRecordMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "docker"})
public class JdbcPaymentIdempotencyJournalRepository {

  private final GatewayIdempotencyRecordMapper gatewayIdempotencyRecordMapper;

  public JdbcPaymentIdempotencyJournalRepository(GatewayIdempotencyRecordMapper gatewayIdempotencyRecordMapper) {
    this.gatewayIdempotencyRecordMapper = gatewayIdempotencyRecordMapper;
  }

  public Optional<PaymentIdempotencyRecord> find(String merchantId, String idempotencyKey) {
    GatewayIdempotencyRecordEntity entity = gatewayIdempotencyRecordMapper.selectOne(new LambdaQueryWrapper<GatewayIdempotencyRecordEntity>()
        .eq(GatewayIdempotencyRecordEntity::getMerchantId, merchantId)
        .eq(GatewayIdempotencyRecordEntity::getIdempotencyKey, idempotencyKey)
        .last("LIMIT 1"));
    if (entity == null) {
      return Optional.empty();
    }
    if (entity.getProcessStatus() != null && entity.getProcessStatus() == 1) {
      return Optional.of(PaymentIdempotencyRecord.success(
          entity.getMerchantId(),
          entity.getIdempotencyKey(),
          entity.getRequestHash(),
          entity.getResultGatewayPaymentId(),
          entity.getResultStatus(),
          entity.getResultRouteCode(),
          entity.getResponseMessage()
      ));
    }
    return Optional.of(PaymentIdempotencyRecord.failure(
        entity.getMerchantId(),
        entity.getIdempotencyKey(),
        entity.getRequestHash(),
        entity.getResponseCode(),
        500,
        entity.getResponseMessage()
    ));
  }

  public void save(PaymentIdempotencyRecord record, Duration ttl, String requestId, String bizType) {
    GatewayIdempotencyRecordEntity existing = gatewayIdempotencyRecordMapper.selectOne(new LambdaQueryWrapper<GatewayIdempotencyRecordEntity>()
        .eq(GatewayIdempotencyRecordEntity::getIdempotencyKey, record.idempotencyKey())
        .last("LIMIT 1"));
    GatewayIdempotencyRecordEntity entity = existing == null ? new GatewayIdempotencyRecordEntity() : existing;
    entity.setIdempotencyKey(record.idempotencyKey());
    entity.setMerchantId(record.merchantId());
    entity.setRequestId(requestId);
    entity.setBizType(bizType);
    entity.setRequestHash(record.requestFingerprint());
    entity.setExpireAt(LocalDateTime.ofInstant(java.time.Instant.now().plus(ttl), ZoneOffset.UTC));
    entity.setResultProcessedAt(LocalDateTime.now(ZoneOffset.UTC));
    if (record.storedPaymentResult().success()) {
      entity.setProcessStatus(1);
      entity.setResponseCode("SUCCESS");
      entity.setResponseMessage(record.storedPaymentResult().responseMessage());
      entity.setResultGatewayPaymentId(record.storedPaymentResult().gatewayPaymentId());
      entity.setResultStatus(record.storedPaymentResult().paymentStatus());
      entity.setResultRouteCode(record.storedPaymentResult().routeCode());
    } else {
      entity.setProcessStatus(2);
      entity.setResponseCode(record.storedPaymentResult().errorCode());
      entity.setResponseMessage(record.storedPaymentResult().errorMessage());
      entity.setResultGatewayPaymentId(null);
      entity.setResultStatus(null);
      entity.setResultRouteCode(null);
    }
    if (existing == null) {
      gatewayIdempotencyRecordMapper.insert(entity);
    } else {
      gatewayIdempotencyRecordMapper.updateById(entity);
    }
  }
}
