package me.hjhng125.querydsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import me.hjhng125.querydsl.member.Member;
import me.hjhng125.querydsl.member.QMember;
import me.hjhng125.querydsl.team.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void beforeEach() {
        queryFactory = new JPAQueryFactory(em); // entityManager를 가지고 데이터를 찾기 위해 필요함
                                                // 동시성 문제 x - 멀티쓰레드 환경에 Thread safe 하다.
                                                // 동시에 EntityManager에 접근해도 동시성 문제가 없다.
                                                // 스프링에서 주입해주는 EntityManager 자체가 이미 Thread safe 하다.

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void startJPQL() {
        // member1 찾기
        String qlString = ""
            + "select m "
            + "from Member m "
            + "where m.username = :username";

        Member member = em.createQuery(qlString, Member.class)
            .setParameter("username", "member1")
            .getSingleResult();

        assertThat(member.getUsername()).isEqualTo("member1");

    }

    @Test
    void startQuerydsl() {
        // member1찾기

        QMember qMember = QMember.member;

        Member member = queryFactory
            .selectFrom(qMember)
            .where(qMember.username.eq("member1")) // 파라미터 바인딩을 하지 않았지만 jdbc에 있는 preparedStatement로 자동으로 파라미터 바인딩함.
            .fetchOne();

        assertThat(member.getUsername()).isEqualTo("member1");

    }
}
