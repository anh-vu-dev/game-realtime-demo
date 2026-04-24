package com.vu.gamews.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/")
    public String home() {
        return "Realtime Game Backend Demo is running.";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}