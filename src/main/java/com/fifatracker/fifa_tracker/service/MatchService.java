package com.fifatracker.fifa_tracker.service;

import com.fifatracker.fifa_tracker.dto.MatchRequest;
import com.fifatracker.fifa_tracker.entity.Match;
import com.fifatracker.fifa_tracker.entity.Player;
import com.fifatracker.fifa_tracker.repository.MatchRepository;
import com.fifatracker.fifa_tracker.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;

    public Match createMatch(MatchRequest request) {
        if (request.getPlayer1Name().equalsIgnoreCase(request.getPlayer2Name())) {
            throw new IllegalArgumentException("Игрок не может играть сам с собой");
        }

        Player player1 = playerRepository.findByName(request.getPlayer1Name())
                .orElseThrow(() -> new IllegalArgumentException("Игрок " + request.getPlayer1Name() + " не найден"));

        Player player2 = playerRepository.findByName(request.getPlayer2Name())
                .orElseThrow(() -> new IllegalArgumentException("Игрок " + request.getPlayer2Name() + " не найден"));

        Match match = new Match();
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setPlayer1Score(request.getPlayer1Score());
        match.setPlayer2Score(request.getPlayer2Score());
        match.setDate(LocalDate.parse(request.getDate()));

        return matchRepository.save(match);
    }
}
