package com.fifatracker.fifa_tracker.repository;

import com.fifatracker.fifa_tracker.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player,Long> {
    Optional<Player> findByName(String name);
}
