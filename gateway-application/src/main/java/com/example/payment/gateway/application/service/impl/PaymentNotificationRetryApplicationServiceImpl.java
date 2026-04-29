package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.payment.PaymentNotificationRetryResponse;
import com.example.payment.gateway.application.service.PaymentNotificationRetryApplicationService;
import com.example.payment.gateway.common.payment.PaymentMessageConsumeRecord;
import com.example.payment.gateway.common.payment.PaymentMessageConsumeRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PaymentNotificationRetryApplicationServiceImpl implements PaymentNotificationRetryApplicationService {

  private final PaymentMessageConsumeRepository paymentMessageConsumeRepository;
  private final PaymentNotificationProcessor paymentNotificationProcessor;
  private final GatewayNotificationProperties properties;

  public PaymentNotificationRetryApplicationServiceImpl(
      PaymentMessageConsumeRepository paymentMessageConsumeRepository,
      PaymentNotificationProcessor paymentNotificationProcessor,
      GatewayNotificationProperties properties
  ) {
    this.paymentMessageConsumeRepository = paymentMessageConsumeRepository;
    this.paymentNotificationProcessor = paymentNotificationProcessor;
    this.properties = properties;
  }

  @Override
  public PaymentNotificationRetryResponse retryFailedNotifications() {
    List<PaymentMessageConsumeRecord> records = paymentMessageConsumeRepository.findRetryableFailed(properties.getRetryLimit());
    int succeededCount = 0;
    int failedCount = 0;
    int deadLetteredCount = 0;
    List<String> messageKeys = new ArrayList<>();
    for (PaymentMessageConsumeRecord record : records) {
      PaymentNotificationProcessor.ProcessingResult result =
          paymentNotificationProcessor.process(record.messageKey(), record.payloadJson());
      if (result.success()) {
        succeededCount++;
      } else {
        failedCount++;
      }
      if (result.deadLetter()) {
        deadLetteredCount++;
      }
      messageKeys.add(record.messageKey());
    }
    return new PaymentNotificationRetryResponse(
        records.size(),
        succeededCount,
        failedCount,
        deadLetteredCount,
        messageKeys
    );
  }
}
