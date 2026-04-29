package com.example.payment.gateway.governance;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"local", "docker"})
public class SentinelScaffoldService {

  @SentinelResource("tech:sentinel:probe")
  public String probe() {
    return "sentinel-probe-ok";
  }
}
