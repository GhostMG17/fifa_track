package com.fifatracker.fifa_tracker.controller;

import com.fifatracker.fifa_tracker.dto.PeriodResultDto;
import com.fifatracker.fifa_tracker.entity.DailyResult;
import com.fifatracker.fifa_tracker.service.ResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultController {
    private final ResultService resultService;

//    @PostMapping("/daily")
//    public ResponseEntity<DailyResult> calculateDailyResult(@RequestParam String date){
//        LocalDate localDate = LocalDate.parse(date);
//        DailyResult result = resultService.calculateDailyResult(localDate);
//        return ResponseEntity.ok(result);
//    }

    @GetMapping("/daily")
    public PeriodResultDto getDaily(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return resultService.calculateDailyStats(date);
    }

    @GetMapping("/weekly")
    public PeriodResultDto getWeekly(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return resultService.calculateWeeklyStats(date);
    }

    @GetMapping("/monthly")
    public PeriodResultDto getMonthly(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return resultService.calculateMonthlyStats(date);
    }

    @GetMapping("/yearly")
    public PeriodResultDto getYearly(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return resultService.calculateYearlyStats(date);
    }



}
