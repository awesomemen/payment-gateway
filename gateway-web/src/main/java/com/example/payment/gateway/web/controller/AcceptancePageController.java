package com.example.payment.gateway.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AcceptancePageController {

  @GetMapping({"/acceptance", "/acceptance/"})
  public String redirectToAcceptanceIndex() {
    return "redirect:/acceptance/index.html";
  }
}
