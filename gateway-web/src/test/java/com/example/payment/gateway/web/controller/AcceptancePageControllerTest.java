package com.example.payment.gateway.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AcceptancePageControllerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders
        .standaloneSetup(new AcceptancePageController())
        .build();
  }

  @Test
  void shouldRedirectAcceptanceDirectoryToIndexPage() throws Exception {
    mockMvc.perform(get("/acceptance/"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/acceptance/index.html"));
  }
}
