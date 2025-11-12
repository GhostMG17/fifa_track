package com.fifatracker.fifa_tracker.repository;

import com.fifatracker.fifa_tracker.entity.DailyResult;
import com.fifatracker.fifa_tracker.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyResultRepository extends JpaRepository<DailyResult,Long> {
    Optional<DailyResult> findByDate(LocalDate date);
    List<DailyResult> findAllByDate(LocalDate date);
}
