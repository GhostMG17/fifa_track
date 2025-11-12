package com.fifatracker.fifa_tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PeriodResultDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<PlayerStatsDto> players;
    private String champion;
    private String loser;
}
