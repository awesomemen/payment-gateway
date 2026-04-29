package com.example.payment.gateway.web.controller;

import com.example.payment.gateway.api.payment.AuditEventEntryResponse;
import com.example.payment.gateway.api.payment.AuditLogEntryResponse;
import com.example.payment.gateway.api.payment.AuditSearchResponse;
import com.example.payment.gateway.api.payment.TransactionDetailResponse;
import com.example.payment.gateway.application.service.AuditSearchApplicationService;
import com.example.payment.gateway.application.service.TransactionQueryApplicationService;
import com.example.payment.gateway.web.advice.GatewayExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

  private MockMvc mockMvc;

  @Mock
  private TransactionQueryApplicationService transactionQueryApplicationService;
  @Mock
  private AuditSearchApplicationService auditSearchApplicationService;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new TransactionController(transactionQueryApplicationService, auditSearchApplicationService))
        .setControllerAdvice(new GatewayExceptionHandler())
        .build();
  }

  @Test
  void shouldReturnTransactionDetail() throws Exception {
    given(transactionQueryApplicationService.query(any()))
        .willReturn(new TransactionDetailResponse("PAY", "REQ-1", "GP123", "DSP123", "SUCCEEDED", "88.50", "CNY", "ROUTE_PAY_CREATE", "svc"));

    mockMvc.perform(post("/api/v1/transactions/detail")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"merchantId":"MCH100001","requestId":"REQ-1","gatewayOrderId":"GP123"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.bizType").value("PAY"));
  }

  @Test
  void shouldReturnAuditSearch() throws Exception {
    given(auditSearchApplicationService.search("MCH100001", "REQ-1", 10))
        .willReturn(new AuditSearchResponse(
            List.of(new AuditLogEntryResponse("trace-1", "REQ-1", "MCH100001", "PAY", "CREATE", "SUCCESS", "SUCCESS", null, null)),
            List.of(new AuditEventEntryResponse("trace-2", "REQ-1", "MCH100001", "PAY", "CREATE", "DOWNSTREAM_TIMEOUT", "WARN", "DOWNSTREAM_TIMEOUT", "timeout"))
        ));

    mockMvc.perform(get("/api/v1/transactions/audit")
            .param("merchantId", "MCH100001")
            .param("requestId", "REQ-1")
            .param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.requestLogs[0].requestId").value("REQ-1"))
        .andExpect(jsonPath("$.data.exceptionEvents[0].eventType").value("DOWNSTREAM_TIMEOUT"));
  }
}
