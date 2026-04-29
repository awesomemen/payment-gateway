package com.example.payment.gateway.observability.payment;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PaymentAuditMetricsRecorder {

  private final MeterRegistry meterRegistry;

  public PaymentAuditMetricsRecorder(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void recordSuccess() {
    counter("success").increment();
  }

  public void recordFailure() {
    counter("failure").increment();
  }

  public double currentCount(String result) {
    Counter counter = meterRegistry.find("gateway.payment.audit.count")
        .tag("result", result)
        .counter();
    return counter == null ? 0D : counter.count();
  }

  private Counter counter(String result) {
    return Counter.builder("gateway.payment.audit.count")
        .tag("result", result)
        .register(meterRegistry);
  }
}
