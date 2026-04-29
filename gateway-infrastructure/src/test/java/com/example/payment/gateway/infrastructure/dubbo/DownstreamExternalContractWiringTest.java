package com.example.payment.gateway.infrastructure.dubbo;

import java.lang.reflect.Field;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.assertj.core.api.Assertions.assertThat;

class DownstreamExternalContractWiringTest {

  @Test
  void shouldAllowSandboxProvidersToBeDisabledForExternalContractMode() {
    assertSandboxSwitch(MockPaymentCreateFacadeImpl.class);
    assertSandboxSwitch(MockPaymentQueryFacadeImpl.class);
    assertSandboxSwitch(MockRefundFacadeImpl.class);
    assertSandboxSwitch(MockDownstreamPaymentStore.class);
    assertSandboxSwitch(MockDownstreamRefundStore.class);
  }

  @Test
  void shouldNotForceDubboConsumersToUseInJvmProvider() throws Exception {
    assertExternalReference(DubboDownstreamPaymentCreateGateway.class, "paymentCreateFacade");
    assertExternalReference(DubboDownstreamPaymentQueryGateway.class, "paymentQueryFacade");
    assertExternalReference(DubboDownstreamRefundCreateGateway.class, "refundFacade");
    assertExternalReference(DubboDownstreamRefundQueryGateway.class, "refundFacade");
  }

  private static void assertSandboxSwitch(Class<?> type) {
    ConditionalOnProperty conditional = type.getAnnotation(ConditionalOnProperty.class);
    assertThat(conditional)
        .as("%s should be guarded by gateway.downstream.sandbox.enabled", type.getSimpleName())
        .isNotNull();
    assertThat(conditional.prefix()).isEqualTo("gateway.downstream.sandbox");
    assertThat(conditional.name()).containsExactly("enabled");
    assertThat(conditional.havingValue()).isEqualTo("true");
    assertThat(conditional.matchIfMissing()).isTrue();
  }

  private static void assertExternalReference(Class<?> type, String fieldName) throws Exception {
    Field field = type.getDeclaredField(fieldName);
    DubboReference reference = field.getAnnotation(DubboReference.class);
    assertThat(reference)
        .as("%s.%s should be a Dubbo reference", type.getSimpleName(), fieldName)
        .isNotNull();
    assertThat(reference.injvm())
        .as("%s.%s must not force local JVM provider in external contract mode", type.getSimpleName(), fieldName)
        .isFalse();
    assertThat(reference.check()).isFalse();
    assertThat(reference.retries()).isZero();
  }
}
