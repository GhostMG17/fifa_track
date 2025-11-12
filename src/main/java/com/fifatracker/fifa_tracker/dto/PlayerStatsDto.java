package com.fifatracker.fifa_tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class PlayerStatsDto {
    private String name;
    private int totalWins;
    private int totalLosses;
    private double winRate;
    private LocalDate lastMatchDate;
}
