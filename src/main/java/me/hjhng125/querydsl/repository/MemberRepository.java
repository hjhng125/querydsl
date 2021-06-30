package me.hjhng125.querydsl.repository;

import java.util.List;
import me.hjhng125.querydsl.model.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    List<Member> findByUsername(String username);
}
