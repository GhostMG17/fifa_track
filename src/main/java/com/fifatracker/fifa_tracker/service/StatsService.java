package com.fifatracker.fifa_tracker.service;

import com.fifatracker.fifa_tracker.dto.PlayerStatsDto;
import com.fifatracker.fifa_tracker.entity.DailyResult;
import com.fifatracker.fifa_tracker.entity.Match;
import com.fifatracker.fifa_tracker.entity.Player;
import com.fifatracker.fifa_tracker.repository.DailyResultRepository;
import com.fifatracker.fifa_tracker.repository.MatchRepository;
import com.fifatracker.fifa_tracker.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final DailyResultRepository dailyResultRepository;

    public Map<String, Object> getFullSummary() {
        List<Player> players = playerRepository.findAll();
        List<Match> allMatches = matchRepository.findAll();

        List<PlayerStatsDto> stats = players.stream()
                .map(p -> calculateStatsForPlayer(p, allMatches))
                .collect(Collectors.toList());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("players", stats);

        return summary;
    }

    private PlayerStatsDto calculateStatsForPlayer(Player player, List<Match> allMatches) {
        int wins = 0;
        int losses = 0;
        LocalDate lastMatchDate = null;

        for (Match m : allMatches) {
            if (m.getPlayer1().equals(player) || m.getPlayer2().equals(player)) {
                lastMatchDate = (lastMatchDate == null || m.getDate().isAfter(lastMatchDate))
                        ? m.getDate() : lastMatchDate;

                if (m.getPlayer1Score() > m.getPlayer2Score() && m.getPlayer1().equals(player)) wins++;
                else if (m.getPlayer2Score() > m.getPlayer1Score() && m.getPlayer2().equals(player)) wins++;
                else if (m.getPlayer1Score() < m.getPlayer2Score() && m.getPlayer1().equals(player)) losses++;
                else if (m.getPlayer2Score() < m.getPlayer1Score() && m.getPlayer2().equals(player)) losses++;
            }
        }

        double winRate = (wins + losses) == 0 ? 0.0 : (wins * 100.0) / (wins + losses);

        return new PlayerStatsDto(player.getName(), wins, losses, winRate, lastMatchDate);
    }
}
