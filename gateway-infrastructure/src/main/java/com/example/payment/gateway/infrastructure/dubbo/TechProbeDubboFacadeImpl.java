package com.example.payment.gateway.infrastructure.dubbo;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.context.annotation.Profile;

@DubboService(version = "1.0.0")
@Profile({"local", "docker"})
public class TechProbeDubboFacadeImpl implements TechProbeDubboFacade {

  @Override
  public String ping(String payload) {
    return "dubbo-tech-probe:" + payload;
  }
}
