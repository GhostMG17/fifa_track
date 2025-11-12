package com.fifatracker.fifa_tracker.controller;

import com.fifatracker.fifa_tracker.entity.Player;
import com.fifatracker.fifa_tracker.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerRepository playerRepository;

    @PostMapping
    public ResponseEntity<Player> createPlayer(@RequestBody Player player) {
        return ResponseEntity.ok(playerRepository.save(player));
    }

    @GetMapping("/names")
    public List<String> getAllNames() {
        return playerRepository.findAll()
                .stream()
                .map(Player::getName)
                .toList();
    }
}
