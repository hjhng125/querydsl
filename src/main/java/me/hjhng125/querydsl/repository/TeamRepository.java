package me.hjhng125.querydsl.repository;

import me.hjhng125.querydsl.model.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {

}
