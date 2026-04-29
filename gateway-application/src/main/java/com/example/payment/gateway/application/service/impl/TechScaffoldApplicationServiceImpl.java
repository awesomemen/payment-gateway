package com.example.payment.gateway.application.service.impl;

import com.example.payment.gateway.api.tech.TechComponentStatusResponse;
import com.example.payment.gateway.api.tech.TechScaffoldStatusResponse;
import com.example.payment.gateway.application.service.TechScaffoldApplicationService;
import com.example.payment.gateway.application.tech.TechConnectivityCollector;
import com.example.payment.gateway.infrastructure.tech.ConnectivityProbeResult;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class TechScaffoldApplicationServiceImpl implements TechScaffoldApplicationService {

  private final ObjectProvider<TechConnectivityCollector> techConnectivityCollectorProvider;
  private final Environment environment;

  public TechScaffoldApplicationServiceImpl(
      ObjectProvider<TechConnectivityCollector> techConnectivityCollectorProvider,
      Environment environment
  ) {
    this.techConnectivityCollectorProvider = techConnectivityCollectorProvider;
    this.environment = environment;
  }

  @Override
  public TechScaffoldStatusResponse currentStatus() {
    TechConnectivityCollector collector = techConnectivityCollectorProvider.getIfAvailable();
    if (collector == null) {
      return new TechScaffoldStatusResponse(
          "payment-gateway",
          "BOOTSTRAP",
          List.of(new TechComponentStatusResponse("scaffold", "DISABLED", "collector active only on local/docker profiles"))
      );
    }
    return new TechScaffoldStatusResponse(
        "payment-gateway",
        currentStage(),
        collector.collect().stream()
            .map(this::toResponse)
            .toList()
    );
  }

  private String currentStage() {
    String[] profiles = environment.getActiveProfiles();
    return profiles.length == 0 ? "BOOTSTRAP" : "TECH_SCAFFOLD(" + String.join(",", profiles) + ")";
  }

  private TechComponentStatusResponse toResponse(ConnectivityProbeResult result) {
    return new TechComponentStatusResponse(result.component(), result.status(), result.detail());
  }
}
