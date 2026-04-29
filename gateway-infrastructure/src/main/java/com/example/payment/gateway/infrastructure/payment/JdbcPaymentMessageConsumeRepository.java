package com.example.payment.gateway.infrastructure.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.payment.gateway.common.payment.PaymentMessageConsumeRecord;
import com.example.payment.gateway.common.payment.PaymentMessageConsumeRepository;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayMessageConsumeEntity;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayMessageConsumeMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "docker"})
public class JdbcPaymentMessageConsumeRepository implements PaymentMessageConsumeRepository {

  private final GatewayMessageConsumeMapper gatewayMessageConsumeMapper;

  public JdbcPaymentMessageConsumeRepository(GatewayMessageConsumeMapper gatewayMessageConsumeMapper) {
    this.gatewayMessageConsumeMapper = gatewayMessageConsumeMapper;
  }

  @Override
  public void save(PaymentMessageConsumeRecord record) {
    GatewayMessageConsumeEntity existing = gatewayMessageConsumeMapper.selectOne(new LambdaQueryWrapper<GatewayMessageConsumeEntity>()
        .eq(GatewayMessageConsumeEntity::getMessageKey, record.messageKey())
        .last("LIMIT 1"));
    GatewayMessageConsumeEntity entity = existing == null ? new GatewayMessageConsumeEntity() : existing;
    entity.setMessageKey(record.messageKey());
    entity.setBizType(record.bizType());
    entity.setConsumerGroup(record.consumerGroup());
    entity.setPayloadJson(record.payloadJson());
    entity.setConsumeStatus(record.consumeStatus());
    entity.setRetryCount(record.retryCount());
    entity.setDeadLetter(record.deadLetter() ? 1 : 0);
    entity.setLastErrorMessage(record.lastErrorMessage());
    if (existing == null) {
      gatewayMessageConsumeMapper.insert(entity);
    } else {
      gatewayMessageConsumeMapper.updateById(entity);
    }
  }

  @Override
  public Optional<PaymentMessageConsumeRecord> findByMessageKey(String messageKey) {
    GatewayMessageConsumeEntity entity = gatewayMessageConsumeMapper.selectOne(new LambdaQueryWrapper<GatewayMessageConsumeEntity>()
        .eq(GatewayMessageConsumeEntity::getMessageKey, messageKey)
        .last("LIMIT 1"));
    return Optional.ofNullable(entity).map(this::toRecord);
  }

  @Override
  public List<PaymentMessageConsumeRecord> findRetryableFailed(int limit) {
    return gatewayMessageConsumeMapper.selectList(new LambdaQueryWrapper<GatewayMessageConsumeEntity>()
            .eq(GatewayMessageConsumeEntity::getConsumeStatus, "FAILED")
            .orderByAsc(GatewayMessageConsumeEntity::getId)
            .last("LIMIT " + limit))
        .stream()
        .map(this::toRecord)
        .collect(Collectors.toList());
  }

  private PaymentMessageConsumeRecord toRecord(GatewayMessageConsumeEntity entity) {
    return new PaymentMessageConsumeRecord(
        entity.getId(),
        entity.getMessageKey(),
        entity.getBizType(),
        entity.getConsumerGroup(),
        entity.getPayloadJson(),
        entity.getConsumeStatus(),
        entity.getRetryCount() == null ? 0 : entity.getRetryCount(),
        entity.getDeadLetter() != null && entity.getDeadLetter() == 1,
        entity.getLastErrorMessage()
    );
  }
}
