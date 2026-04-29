package com.example.payment.gateway.infrastructure.messaging;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.payment.gateway.common.payment.PaymentMqOutboxRecord;
import com.example.payment.gateway.common.payment.PaymentMqOutboxRepository;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayMqOutboxEntity;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayMqOutboxMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "docker"})
public class JdbcPaymentMqOutboxRepository implements PaymentMqOutboxRepository {

  private final GatewayMqOutboxMapper gatewayMqOutboxMapper;

  public JdbcPaymentMqOutboxRepository(GatewayMqOutboxMapper gatewayMqOutboxMapper) {
    this.gatewayMqOutboxMapper = gatewayMqOutboxMapper;
  }

  @Override
  public List<PaymentMqOutboxRecord> findRetryableFailed(Instant now, int limit) {
    LocalDateTime nowUtc = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
    return gatewayMqOutboxMapper.selectList(new LambdaQueryWrapper<GatewayMqOutboxEntity>()
            .eq(GatewayMqOutboxEntity::getSendStatus, 2)
            .and(wrapper -> wrapper.isNull(GatewayMqOutboxEntity::getNextRetryTime)
                .or()
                .le(GatewayMqOutboxEntity::getNextRetryTime, nowUtc))
            .orderByAsc(GatewayMqOutboxEntity::getId)
            .last("LIMIT " + limit))
        .stream()
        .map(this::toRecord)
        .collect(Collectors.toList());
  }

  @Override
  public void markSent(long id, int retryCount) {
    gatewayMqOutboxMapper.update(
        null,
        new LambdaUpdateWrapper<GatewayMqOutboxEntity>()
            .eq(GatewayMqOutboxEntity::getId, id)
            .set(GatewayMqOutboxEntity::getSendStatus, 1)
            .set(GatewayMqOutboxEntity::getRetryCount, retryCount)
            .setSql("next_retry_time = NULL")
            .setSql("last_error_message = NULL")
    );
  }

  @Override
  public void markFailed(long id, int retryCount, Instant nextRetryTime, String lastErrorMessage) {
    gatewayMqOutboxMapper.update(
        null,
        new LambdaUpdateWrapper<GatewayMqOutboxEntity>()
            .eq(GatewayMqOutboxEntity::getId, id)
            .set(GatewayMqOutboxEntity::getSendStatus, 2)
            .set(GatewayMqOutboxEntity::getRetryCount, retryCount)
            .set(GatewayMqOutboxEntity::getNextRetryTime, LocalDateTime.ofInstant(nextRetryTime, ZoneOffset.UTC))
            .set(GatewayMqOutboxEntity::getLastErrorMessage, lastErrorMessage)
    );
  }

  private PaymentMqOutboxRecord toRecord(GatewayMqOutboxEntity entity) {
    return new PaymentMqOutboxRecord(
        entity.getId(),
        entity.getEventKey(),
        entity.getBizType(),
        entity.getTopic(),
        entity.getTag(),
        entity.getMessageKey(),
        entity.getPayloadJson(),
        entity.getSendStatus(),
        entity.getRetryCount(),
        entity.getNextRetryTime() == null ? null : entity.getNextRetryTime().toInstant(ZoneOffset.UTC),
        entity.getLastErrorMessage()
    );
  }
}
