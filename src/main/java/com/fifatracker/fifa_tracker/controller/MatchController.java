package com.fifatracker.fifa_tracker.controller;

import com.fifatracker.fifa_tracker.dto.MatchRequest;
import com.fifatracker.fifa_tracker.entity.Match;
import com.fifatracker.fifa_tracker.repository.MatchRepository;
import com.fifatracker.fifa_tracker.repository.PlayerRepository;
import com.fifatracker.fifa_tracker.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;

    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<Match> createMatch(@RequestBody MatchRequest request) {
        Match match = matchService.createMatch(request);
        return ResponseEntity.ok(match);
    }

    @GetMapping
    public List<Match> getAll() {
        return matchRepository.findAll();
    }

    // Эндпоинт с лимитом
    @GetMapping("/history")
    public List<Match> getMatchesHistory(@RequestParam(required = false) Integer limit,
                                         @RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate) {

        if (startDate != null && endDate != null) {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            return matchRepository.findAllByDateBetweenOrderByDateDesc(start, end);
        } else if (limit != null) {
            return matchRepository.findTopNByOrderByDateDesc(limit);
        } else {
            return matchRepository.findAll(Sort.by(Sort.Direction.DESC, "date"));
        }
    }
}

