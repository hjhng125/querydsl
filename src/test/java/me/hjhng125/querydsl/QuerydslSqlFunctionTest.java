package me.hjhng125.querydsl;

import static me.hjhng125.querydsl.member.QMember.member;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import me.hjhng125.querydsl.member.Member;
import me.hjhng125.querydsl.member.QMember;
import me.hjhng125.querydsl.team.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class QuerydslSqlFunctionTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;
    QMember memberSub;

    @BeforeEach
    void before() {
        queryFactory = new JPAQueryFactory(em);
        memberSub = new QMember("memberSub");

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
    void sql_function_replace() {
        //given

        //when
        List<String> result = queryFactory
            .select(
                Expressions.stringTemplate(
                    "function('replace', {0}, {1}, {2})",
                    member.username,
                    "member",
                    "M"))
            .from(member)
            .fetch();

        //then
        result.forEach(System.out::println);

    }

    @Test
    void sql_function_underCase() {
        //given

        //when
        List<String> result = queryFactory
            .select(member.username)
            .from(member)
            .where(member.username.eq(
                Expressions.stringTemplate(
                    "function('lower', {0})",
                    member.username)))
            .fetch();

        List<String> result2 = queryFactory
            .select(member.username)
            .from(member)
            .where(member.username.eq(member.username.lower())) // 모든 db에서 공통적으로 지원하는 sql function은 querydsl에서 기본적으로 지원한다. (ANSI 표준은 지원한다.)
            .fetch();

        //then
        result.forEach(System.out::println);
        result2.forEach(System.out::println);

    }
}
