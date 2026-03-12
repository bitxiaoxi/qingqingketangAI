package com.qingqingketang.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class PingController {

    @GetMapping("/api/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", "qingqingketang");
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }
}
