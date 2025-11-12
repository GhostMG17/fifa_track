package com.fifatracker.fifa_tracker.dto;


import lombok.Data;

@Data
public class MatchRequest {
    private String player1Name;
    private String player2Name;
    private int player1Score;
    private int player2Score;
    private String date; // формат "YYYY-MM-DD"
}