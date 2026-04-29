package com.example.payment.gateway.infrastructure.payment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.payment.gateway.common.payment.PaymentRequestLogRecord;
import com.example.payment.gateway.common.payment.PaymentRequestLogRepository;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayRequestLogEntity;
import com.example.payment.gateway.infrastructure.mybatis.payment.GatewayRequestLogMapper;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"local", "docker"})
public class JdbcPaymentRequestLogRepository implements PaymentRequestLogRepository {

  private final GatewayRequestLogMapper gatewayRequestLogMapper;

  public JdbcPaymentRequestLogRepository(GatewayRequestLogMapper gatewayRequestLogMapper) {
    this.gatewayRequestLogMapper = gatewayRequestLogMapper;
  }

  @Override
  public void save(PaymentRequestLogRecord record) {
    GatewayRequestLogEntity entity = new GatewayRequestLogEntity();
    entity.setTraceId(record.traceId());
    entity.setRequestId(record.requestId());
    entity.setIdempotencyKey(record.idempotencyKey());
    entity.setMerchantId(record.merchantId());
    entity.setAppId(record.appId());
    entity.setBizType(record.bizType());
    entity.setApiCode(record.apiCode());
    entity.setHttpMethod(record.httpMethod());
    entity.setRequestUri(record.requestUri());
    entity.setRequestTime(toLocalDateTime(record.requestTime()));
    entity.setFinishTime(toLocalDateTime(record.finishTime()));
    entity.setDurationMs(record.durationMillis());
    entity.setRouteCode(record.routeCode());
    entity.setTargetService(record.targetService());
    entity.setResponseCode(record.responseCode());
    entity.setResponseStatus(record.responseStatus());
    entity.setErrorType(record.errorType());
    entity.setErrorMessage(record.errorMessage());
    entity.setRequestSummary(record.requestSummaryJson());
    entity.setExtJson(record.extJson());
    gatewayRequestLogMapper.insert(entity);
  }

  @Override
  public long countByResponseStatus(String responseStatus) {
    return gatewayRequestLogMapper.selectCount(new LambdaQueryWrapper<GatewayRequestLogEntity>()
        .eq(GatewayRequestLogEntity::getResponseStatus, responseStatus));
  }

  @Override
  public List<PaymentRequestLogRecord> findRecent(String merchantId, String requestId, int limit) {
    LambdaQueryWrapper<GatewayRequestLogEntity> queryWrapper = new LambdaQueryWrapper<GatewayRequestLogEntity>()
        .orderByDesc(GatewayRequestLogEntity::getId)
        .last("LIMIT " + limit);
    if (merchantId != null && !merchantId.isBlank()) {
      queryWrapper.eq(GatewayRequestLogEntity::getMerchantId, merchantId);
    }
    if (requestId != null && !requestId.isBlank()) {
      queryWrapper.eq(GatewayRequestLogEntity::getRequestId, requestId);
    }
    return gatewayRequestLogMapper.selectList(queryWrapper).stream()
        .map(entity -> new PaymentRequestLogRecord(
            entity.getTraceId(),
            entity.getRequestId(),
            entity.getIdempotencyKey(),
            entity.getMerchantId(),
            entity.getAppId(),
            entity.getBizType(),
            entity.getApiCode(),
            entity.getHttpMethod(),
            entity.getRequestUri(),
            entity.getRequestTime() == null ? null : entity.getRequestTime().toInstant(ZoneOffset.UTC),
            entity.getFinishTime() == null ? null : entity.getFinishTime().toInstant(ZoneOffset.UTC),
            entity.getDurationMs(),
            entity.getRouteCode(),
            entity.getTargetService(),
            entity.getResponseCode(),
            entity.getResponseStatus(),
            entity.getErrorType(),
            entity.getErrorMessage(),
            entity.getRequestSummary(),
            entity.getExtJson()
        ))
        .collect(Collectors.toList());
  }

  private static LocalDateTime toLocalDateTime(java.time.Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
  }
}
