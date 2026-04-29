package com.example.payment.gateway.infrastructure.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRecord;
import com.example.payment.gateway.common.payment.PaymentExceptionEventRepository;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayExceptionEventEntity;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayExceptionEventMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "docker"})
public class JdbcPaymentExceptionEventRepository implements PaymentExceptionEventRepository {

  private final GatewayExceptionEventMapper gatewayExceptionEventMapper;

  public JdbcPaymentExceptionEventRepository(GatewayExceptionEventMapper gatewayExceptionEventMapper) {
    this.gatewayExceptionEventMapper = gatewayExceptionEventMapper;
  }

  @Override
  public void save(PaymentExceptionEventRecord record) {
    GatewayExceptionEventEntity entity = new GatewayExceptionEventEntity();
    entity.setTraceId(record.traceId());
    entity.setRequestId(record.requestId());
    entity.setMerchantId(record.merchantId());
    entity.setBizType(record.bizType());
    entity.setApiCode(record.apiCode());
    entity.setEventType(record.eventType());
    entity.setEventLevel(record.eventLevel());
    entity.setEventCode(record.eventCode());
    entity.setEventMessage(record.eventMessage());
    entity.setDetailJson(record.detailJson());
    gatewayExceptionEventMapper.insert(entity);
  }

  @Override
  public long countAll() {
    return gatewayExceptionEventMapper.selectCount(new LambdaQueryWrapper<>());
  }

  @Override
  public List<PaymentExceptionEventRecord> findRecent(String merchantId, String requestId, int limit) {
    LambdaQueryWrapper<GatewayExceptionEventEntity> queryWrapper = new LambdaQueryWrapper<GatewayExceptionEventEntity>()
        .orderByDesc(GatewayExceptionEventEntity::getId)
        .last("LIMIT " + limit);
    if (merchantId != null && !merchantId.isBlank()) {
      queryWrapper.eq(GatewayExceptionEventEntity::getMerchantId, merchantId);
    }
    if (requestId != null && !requestId.isBlank()) {
      queryWrapper.eq(GatewayExceptionEventEntity::getRequestId, requestId);
    }
    return gatewayExceptionEventMapper.selectList(queryWrapper).stream()
        .map(entity -> new PaymentExceptionEventRecord(
            entity.getTraceId(),
            entity.getRequestId(),
            entity.getMerchantId(),
            entity.getBizType(),
            entity.getApiCode(),
            entity.getEventType(),
            entity.getEventLevel(),
            entity.getEventCode(),
            entity.getEventMessage(),
            entity.getDetailJson()
        ))
        .collect(Collectors.toList());
  }
}
