package com.fifatracker.fifa_tracker.service;

import com.fifatracker.fifa_tracker.dto.PeriodResultDto;
import com.fifatracker.fifa_tracker.dto.PlayerStatsDto;
import com.fifatracker.fifa_tracker.entity.Match;
import com.fifatracker.fifa_tracker.entity.Player;
import com.fifatracker.fifa_tracker.repository.MatchRepository;
import com.fifatracker.fifa_tracker.repository.PlayerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ResultService {

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;

    // -----------------------------
    // Универсальный подсчёт за период
    // -----------------------------
    @Transactional
    public PeriodResultDto calculateStatsForPeriod(LocalDate startDate, LocalDate endDate) {
        List<Match> matches = matchRepository.findAllByDateBetweenOrderByDateDesc(startDate, endDate);

        // Убираем выходные
        matches.removeIf(m -> {
            DayOfWeek d = m.getDate().getDayOfWeek();
            return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
        });

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Нет матчей за рабочие дни в период: " + startDate + " - " + endDate);
        }

        Map<Player, int[]> statsMap = new HashMap<>(); // [wins, losses]

        for (Match m : matches) {
            statsMap.putIfAbsent(m.getPlayer1(), new int[]{0, 0});
            statsMap.putIfAbsent(m.getPlayer2(), new int[]{0, 0});

            if (m.getPlayer1Score() > m.getPlayer2Score()) {
                statsMap.get(m.getPlayer1())[0]++;
                statsMap.get(m.getPlayer2())[1]++;
            } else if (m.getPlayer1Score() < m.getPlayer2Score()) {
                statsMap.get(m.getPlayer1())[1]++;
                statsMap.get(m.getPlayer2())[0]++;
            }
        }

        List<PlayerStatsDto> playerStats = new ArrayList<>();
        Player champion = null;
        Player loser = null;
        int maxScore = Integer.MIN_VALUE;
        int minScore = Integer.MAX_VALUE;

        for (Map.Entry<Player, int[]> entry : statsMap.entrySet()) {
            Player p = entry.getKey();
            int wins = entry.getValue()[0];
            int losses = entry.getValue()[1];
            double winRate = (wins + losses == 0) ? 0.0 : (wins * 100.0 / (wins + losses));

            LocalDate lastMatchDate = matches.stream()
                    .filter(m -> m.getPlayer1().equals(p) || m.getPlayer2().equals(p))
                    .map(Match::getDate)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            playerStats.add(new PlayerStatsDto(p.getName(), wins, losses, winRate, lastMatchDate));

            int score = wins - losses;
            if (score > maxScore) {
                maxScore = score;
                champion = p;
            }
            if (score < minScore) {
                minScore = score;
                loser = p;
            }
        }

        playerStats.sort((a, b) -> Double.compare(b.getWinRate(), a.getWinRate()));

        return new PeriodResultDto(
                startDate,
                endDate,
                playerStats,
                champion != null ? champion.getName() : null,
                loser != null ? loser.getName() : null
        );
    }

    public PeriodResultDto calculateDailyStats(LocalDate date) {
        return calculateStatsForPeriod(date, date);
    }

    public PeriodResultDto calculateWeeklyStats(LocalDate anyDateInWeek) {
        LocalDate start = anyDateInWeek.with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(4); // Пн–Пт
        return calculateStatsForPeriod(start, end);
    }

    public PeriodResultDto calculateMonthlyStats(LocalDate date) {
        LocalDate start = date.withDayOfMonth(1);
        LocalDate end = date.withDayOfMonth(date.lengthOfMonth());
        return calculateStatsForPeriod(start, end);
    }

    public PeriodResultDto calculateYearlyStats(LocalDate date) {
        LocalDate start = date.withDayOfYear(1);
        LocalDate end = date.withDayOfYear(date.lengthOfYear());
        return calculateStatsForPeriod(start, end);
    }
}
