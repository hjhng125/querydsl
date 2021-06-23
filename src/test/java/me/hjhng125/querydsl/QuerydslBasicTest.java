package me.hjhng125.querydsl;

import static me.hjhng125.querydsl.member.QMember.member;
import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import me.hjhng125.querydsl.member.Member;
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

        Member findByUsername = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1")) // 파라미터 바인딩을 하지 않았지만 jdbc에 있는 preparedStatement로 자동으로 파라미터 바인딩함.
            .fetchOne();

        assertThat(findByUsername.getUsername()).isEqualTo("member1");

    }

    @Test
    void search() {
        Member findMember = queryFactory
            .selectFrom(member)
            .where(member.username.eq("member1")
                .and(member.age.eq(10)))
            .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    void searchAndParam() {
        Member findMember = queryFactory
            .selectFrom(member)
            .where(
                member.username.eq("member1"),
                member.age.eq(10)
                // 이 경우와 같이 and()체인이 아닌 and 조건을 ','로 조립할 수 있다.
                // where()는 파라미터를 Predicate... 로 받기 때문에 가능하다.
                // 이런식으로 할 경우 null이 파라미터로 들어왔을 때 무시할 수 있다.
                // null을 무시함으로써 더욱 직관적인 동적쿼리를 만들 수 있다.
            )
            .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    void resultFetch() {
        List<Member> fetch = queryFactory
            .selectFrom(member)
            .fetch();

        Member fetchOne = queryFactory
            .selectFrom(member)
            .where(member.age.eq(10))
            .fetchOne();

        // 아래 두 쿼리는 같다.
        Member fetchLimitOne = queryFactory
            .selectFrom(member)
            .limit(1)
            .fetchFirst();

        Member fetchFirst = queryFactory
            .selectFrom(member)
            .fetchFirst();

        // paging 정보를 제공한다.
        QueryResults<Member> fetchResults = queryFactory
            .selectFrom(member)
            .fetchResults();
        long total = fetchResults.getTotal();
        List<Member> results = fetchResults.getResults();

        long count = queryFactory
            .selectFrom(member)
            .fetchCount();


    }
}
