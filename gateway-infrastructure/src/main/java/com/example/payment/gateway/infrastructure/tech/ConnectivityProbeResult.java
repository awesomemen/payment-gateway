package com.example.payment.gateway.infrastructure.tech;

public record ConnectivityProbeResult(
    String component,
    String status,
    String detail
) {

  public static ConnectivityProbeResult up(String component, String detail) {
    return new ConnectivityProbeResult(component, "UP", detail);
  }

  public static ConnectivityProbeResult down(String component, String detail) {
    return new ConnectivityProbeResult(component, "DOWN", detail);
  }
}
