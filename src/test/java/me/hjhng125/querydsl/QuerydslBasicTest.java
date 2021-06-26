package me.hjhng125.querydsl;

import static com.querydsl.jpa.JPAExpressions.select;
import static me.hjhng125.querydsl.member.QMember.member;
import static me.hjhng125.querydsl.team.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
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
    QMember memberSub;

    @BeforeEach
    void beforeEach() {
        memberSub = new QMember("memberSub");
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
     * 회원 정렬 순서 1. 회원 나이 내림차순 2. 회원 이름 올림차순 회원 이름이 없으면 마지막에 출력(nullsLast)
     */
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
     */
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

    /**
     * 팀 A에 소속된 모든 팀원
     */
    @Test
    void join() {
        //when
        List<Member> teamA = queryFactory.selectFrom(member)
            .join(member.team, team)
            .where(team.name.eq("teamA"))
            .fetch();

        //then
        assertThat(teamA)
            .extracting("username")
            .containsExactly("member1", "member2");

    }

    /**
     * theta join : 연관관계가 없지만 조인해줌 조인을 하려하는 양 쪽 테이블의 모든 데이터를 가져와서 조인하고 where 절에서 필터링. 회원의 이름이 팀 이름과 같은 회원을 조회
     */

    @Test
    void theta_join() {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        //when

        List<Member> eqName = queryFactory
            .selectFrom(member)
            .from(member, team) // 세타 조인은 그냥 from절에 테이블을 나열한다. Cartesian Product, 실제 sql은 cross 조인 발생
            .where(member.username.eq(team.name))
            .fetch();

        //then
        assertThat(eqName)
            .extracting("username") // iterable한 요소에서 특정 field나 property 추출
            .containsExactly("teamA", "teamB");
    }

    /**
     * on
     * JPA 2.1부터 지원
     * 1.조인 대상을 필터링
     * 2. 연관관계 없는 엔터티 외부 조인
     */

    /**
     * 회원과 팀을 조인 팀 이름이 teamA 회원은 모두 조회 JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    void join_on_filtering() {
        //given

        //when
        /*
          inner join 의 경우 on에서 필터링을 하는것과 where에서 필터링하는 것은 같다.
          어떤 것을 사용해도 무방
          하지만 left join 의 경우 where절 에서 필터링 할 경우 필요한 left값이 필터링 될 수 있기에
          on절에서 풀어야 한다.
         */
        List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(member.team, team) // 연관관계가 있는 경우 id가 조인 조건에 들어감
            .on(member.team.name.eq("teamA")) // on절은 조인할 데이터 대상을 줄이는 것(필터링)
            .fetch();
        //then
        result.forEach(System.out::println);
    }

    /**
     * 연관관계가 없는 테이블 조인 팀이름과 회원이름이 같은 회원 외부 조인으로 조회
     */
    @Test
    void join_on_no_relation() {
        //given
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        //when
        List<Tuple> result = queryFactory
            .select(member, team)
            .from(member)
            .leftJoin(team).on(member.username.eq(team.name)) // 연관관계가 없는 경우 일반 조인과 다르게 join절에 엔터티만 들어간다.
            .fetch();

        result.forEach(System.out::println);
        //then
        result.forEach(item -> {
            assertThat(item.get(member.username)).isIn("teamA", "teamB", null);
        });
    }

    /**
     * fetch join sql이 지원하는 기능은 아니고, sql 조인을 활용하여 연관된 엔터티를 sql 쿼리 한번으로 가져오는 방법 주로 JPQL 성능 최적화에서 사용됨.
     */

    @PersistenceUnit // entity manager를 만드는 factory를 주입받기 위한 어노테이션
        EntityManagerFactory emf;

    @Test
    void noFetchJoin() {
        //given
        em.flush();
        em.clear();
        //when
        Member findMember = queryFactory.selectFrom(member)
            .where(member.username.eq("member1"))
            .fetchOne(); // 현재 member 엔터티는 패치 전략이 Lazy기 때문에 member만 조회한다.

        //then
        boolean loaded = emf.getPersistenceUnitUtil()
            .isLoaded(findMember.getTeam()); // isLoaded : 해당 엔터티가 컨텍스트에 로딩되었는지 아닌지 확인할 수 있도록 해줌.

        assertThat(loaded).as("패치 조인 미적용").isFalse();

    }

    /**
     * fetchType.Eager와 fetch join은 같은 쿼리를 발생시킨다. fetchType을 Eager로 설정하면 해당 연관관계에서 발생하는 모든 쿼리에서 join을 통해 가져오므로 특정 상황에서만 한번에 데이터를 가져오고 싶은 경우 fetch join을 사용하면 될 듯하다.
     */
    @Test
    void fetchJoin() {
        //given
        em.flush();
        em.clear();
        //when
        Member findMember = queryFactory.selectFrom(member)
            .join(member.team, team).fetchJoin()
            .where(member.username.eq("member1"))
            .fetchOne(); // 현재 member 엔터티는 패치 전략이 Lazy기 때문에 member만 조회한다.

        //then
        boolean loaded = emf.getPersistenceUnitUtil()
            .isLoaded(findMember.getTeam()); // isLoaded : 해당 엔터티가 컨텍스트에 로딩되었는지 아닌지 확인할 수 있도록 해줌.

        assertThat(loaded).as("패치 조인 미적용").isTrue();

    }

    /**
     * SubQuery
     * com.querydsl.jpa.JPAExpressions 사용
     */

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    void subQuery() {
        //given

        //when
        Member result = queryFactory
            .selectFrom(member)
            .where(member.age.eq(
                select(memberSub.age.max())
                    .from(memberSub)
            ))
            .fetchOne();
        //then
        assertThat(result.getAge()).isEqualTo(40);

    }

    /**
     * 나이가 평균 이상
     */
    @Test
    void subQuery_age_goe_avg() {
        //given

        //when
        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.goe(
                select(memberSub.age.avg())
                    .from(memberSub)
            ))
            .fetch();

        //then
        assertThat(result).extracting("age")
            .containsExactly(30, 40);
    }

    @Test
    void subQuery_in() {
        //given

        //when
        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.in(
                select(memberSub.age)
                    .from(memberSub)
                    .where(memberSub.age.gt(10))
            ))
            .fetch();

        //then
        assertThat(result).extracting("age")
            .containsExactly(20, 30, 40);

    }

    @Test
    void select_subQuery() {
        //given

        //when
        List<Tuple> result = queryFactory
            .select(member.username,
                select(memberSub.age.avg())
                    .from(memberSub))
            .from(member)
            .fetch();

        //then
        result.forEach(System.out::println);
    }

    /**
     * JPA JPQL의 서브쿼리는 from절에서의 서브쿼리가 불가능하다.
     * querydsl은 JPQL의 빌더 역할이기 떄문에 JPQL이 지원하지 않으면 할 수 없다.
     * select, where 절에서의 서브쿼리는 Hibernate가 지원하고,
     * querydsl도 Hibernate를 사용하기 떄문에 가능한 것이다.
     *
     * * 해결방안
     * 1. 서브쿼리를 join으로 변경한다. (가능한 경우)
     * 2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다.
     *      * 성능이 중요하지 않은 경우
     *      * 심각한 성능 저하를 발생하지 않는 경우
     * 3. 위의 사례를 만족하지 못한 경우 nativeSQL을 사용한다.
     *
     * * from절의 서브쿼리를 사용하는 안 좋은 경우 - 한 쿼리가 많은 기능을 지원하는 경우
     *      * 화면에 렌더링할 데이터 포맷을 맞추는 경우
     *      * 비즈니스 로직이 포함된 경우
     * * sql은 데이터를 가져오는 역할만 해야하고 집중해야 한다.
     * * 로직은 필요한 layer 에서 해결해야 한다.
     * * SQL AntiPatterns
    */
}
