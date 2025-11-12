package com.fifatracker.fifa_tracker.controller;

import com.fifatracker.fifa_tracker.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        return statsService.getFullSummary();
    }
}
