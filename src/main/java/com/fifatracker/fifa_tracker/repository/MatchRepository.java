package com.fifatracker.fifa_tracker.repository;

import com.fifatracker.fifa_tracker.entity.Match;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match,Long> {
    List<Match> findByDate(LocalDate date);
    List<Match> findAllByDateBetweenOrderByDateDesc(LocalDate startDate, LocalDate endDate);

    @Query(value = "SELECT m FROM Match m ORDER BY m.date DESC,m.id DESC")
    List<Match> findTopNByOrderByDateDesc(Pageable pageable);

    default List<Match> findTopNByOrderByDateDesc(int limit) {
        return findTopNByOrderByDateDesc(PageRequest.of(0, limit));
    }

}
