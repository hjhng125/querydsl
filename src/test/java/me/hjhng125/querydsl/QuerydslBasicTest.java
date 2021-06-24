package me.hjhng125.querydsl;

import static me.hjhng125.querydsl.member.QMember.member;
import static me.hjhng125.querydsl.team.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
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

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순
     * 2. 회원 이름 올림차순
     * 회원 이름이 없으면 마지막에 출력(nullsLast)
     * */
    @Test
    void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> ageDesc = queryFactory
            .selectFrom(member)
            .where(member.age.eq(100))
            .orderBy(member.age.desc(), member.username.desc().nullsLast())
            .fetch();

        ageDesc.forEach(System.out::println);

        assertThat(ageDesc.get(0).getUsername()).isEqualTo("member5");
        assertThat(ageDesc.get(1).getUsername()).isEqualTo("member6");
        assertThat(ageDesc.get(2).getUsername()).isEqualTo(null);
    }

    @Test
    void paging1() {
        List<Member> fetch = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1) // 앞에 몇개를 스킵할 것인가?, 0부터 시작, 1이면 1개 스킵
            .limit(2)
            .fetch();

        assertThat(fetch.size()).isEqualTo(2);
    }

    @Test
    void paging2() {

        // 전체 조회수가 필요한 경우.
        QueryResults<Member> results = queryFactory
            .selectFrom(member)
            .orderBy(member.username.desc())
            .offset(1)
            .limit(2)
            .fetchResults();

        assertThat(results.getTotal()).isEqualTo(4);
        assertThat(results.getLimit()).isEqualTo(2);
        assertThat(results.getOffset()).isEqualTo(1);
        assertThat(results.getResults().size()).isEqualTo(2);
    }

    @Test
    void aggregation() {
        List<Tuple> result = queryFactory
            .select(
                member.count(),
                member.age.sum(),
                member.age.avg(),
                member.age.max(),
                member.age.min())
            .from(member)
            .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 연령을 구하기
     * */
    @Test
    void group() {
        //given
        List<Tuple> result = queryFactory
            .select(team.name, member.age.avg())
            .from(member)
            .join(member.team, team)
            .groupBy(team.name)
            .having()
            .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        //when

        //then
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }
}
