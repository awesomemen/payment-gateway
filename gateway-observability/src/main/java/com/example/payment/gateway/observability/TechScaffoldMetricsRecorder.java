package com.example.payment.gateway.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class TechScaffoldMetricsRecorder {

  private final Counter probeCounter;

  public TechScaffoldMetricsRecorder(MeterRegistry meterRegistry) {
    this.probeCounter = Counter.builder("gateway.tech.probe.count")
        .description("Number of technical scaffold probe requests")
        .register(meterRegistry);
  }

  public void recordProbe() {
    probeCounter.increment();
  }
}
