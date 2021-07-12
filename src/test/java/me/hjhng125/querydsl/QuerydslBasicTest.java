package me.hjhng125.querydsl;

import static com.querydsl.jpa.JPAExpressions.select;
import static me.hjhng125.querydsl.model.entity.QMember.member;
import static me.hjhng125.querydsl.model.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import me.hjhng125.querydsl.config.QuerydslConfig;
import me.hjhng125.querydsl.model.dto.QMemberDto;
import me.hjhng125.querydsl.model.entity.Member;
import me.hjhng125.querydsl.model.dto.MemberDto;
import me.hjhng125.querydsl.model.entity.QMember;
import me.hjhng125.querydsl.model.entity.Team;
import me.hjhng125.querydsl.model.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(QuerydslConfig.class)
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

        assertThat(ageDesc.get(0).getUsername()).isEqualTo("member6");
        assertThat(ageDesc.get(1).getUsername()).isEqualTo("member5");
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

    @Test
    void distinct() {
        //given
        em.persist(new Member("member5", 10));

        //when
        List<Integer> result = queryFactory
            .select(member.age).distinct()
            .from(member)
            .fetch();

        //then
        result.forEach(System.out::println);

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

    @Test
    void fetchJoinByTeam() {
        //given

        //when
        List<Team> result = queryFactory.selectFrom(team)
            .join(team.members, member).fetchJoin()
            .fetch();

        //then
        for (Team team : result) {
            System.out.println("team = " + team);
            for (Member member : team.getMembers()) {
                System.out.println("member = " + member);
            }
        }
    }

    @Test
    void fetchJoinByTeamDistinct() {
        //given

        //when
        List<Team> result = queryFactory
            .selectFrom(team).distinct()
            .join(team.members, member).fetchJoin()
            .fetch();

        //then
        for (Team team : result) {
            System.out.println("team = " + team);
            for (Member member : team.getMembers()) {
                System.out.println("member = " + member);
            }
        }
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
     * JPA JPQL의 서브쿼리는 from절에서의 서브쿼리가 불가능하다. querydsl은 JPQL의 빌더 역할이기 떄문에 JPQL이 지원하지 않으면 할 수 없다. select, where 절에서의 서브쿼리는 Hibernate가 지원하고, querydsl도 Hibernate를 사용하기 떄문에 가능한 것이다.
     * <p>
     * * 해결방안 1. 서브쿼리를 join으로 변경한다. (가능한 경우) 2. 애플리케이션에서 쿼리를 2번 분리해서 실행한다. * 성능이 중요하지 않은 경우 * 심각한 성능 저하를 발생하지 않는 경우 3. 위의 사례를 만족하지 못한 경우 nativeSQL을 사용한다.
     * <p>
     * * from절의 서브쿼리를 사용하는 안 좋은 경우 - 한 쿼리가 많은 기능을 지원하는 경우 * 화면에 렌더링할 데이터 포맷을 맞추는 경우 * 비즈니스 로직이 포함된 경우 * sql은 데이터를 가져오는 역할만 해야하고 집중해야 한다. * 로직은 필요한 layer 에서 해결해야 한다. * SQL
     * AntiPatterns
     */

    @Test
    void simple_case() {
        //given

        //when
        List<String> result = queryFactory
            .select(member.age
                .when(10).then("열살")
                .when(20).then("스무살")
                .otherwise("기타"))
            .from(member)
            .fetch();

        //then
        result.forEach(System.out::println);
    }

    @Test
    void complex_case() {
        //given

        //when
        List<String> result = queryFactory
            .select(
                new CaseBuilder()
                    .when(member.age.between(0, 20)).then("0 ~ 20살")
                    .when(member.age.between(21, 30)).then("21 ~ 30살")
                    .otherwise("기타")
            )
            .from(member)
            .fetch();

        //then
        result.forEach(System.out::println);

    }

    @Test
    void case_orderBy() {
        //given

        //when
        NumberExpression<Integer> ageCase = new CaseBuilder()
            .when(member.age.between(0, 20)).then(0)
            .when(member.age.between(21, 30)).then(1)
            .otherwise(2);

        List<Tuple> fetch = queryFactory
            .select(member.username, member.age, ageCase)
            .from(member)
            .orderBy(ageCase.desc())
            .fetch();

        //then
        for (Tuple tuple : fetch) {
            System.out.println(tuple.get(member.username) + ", " + "");
        }

    }

    @Test
    void constant() {
        //given

        //when
        List<Tuple> result = queryFactory
            .select(member.username, Expressions.constant("A")) // 결과에서만 상수를 받고 JPQL, sql에서는 발생하지 않는다.
            .from(member)
            .fetch();

        //then
        result.forEach(System.out::println);
    }

    @Test
    void concat() {
        //given

        //when
        // {username}_{age}
        List<String> result = queryFactory
            .select(member.username.concat("_").concat(member.age.stringValue())) // stringValue()는 문자가 아닌 다른 타입들을 문자로 캐스팅해준다. Enum을 처리할 때 유용
            .from(member)
            .where(member.username.eq("member1"))
            .fetch();

        //then
        result.forEach(System.out::println);

    }

    /**
     * Projection : select절에 어떤 데이터를 가져올지 대상을 지정하는 것
     */

    /**
     * 대상이 하나인 경우 해당 타입으로 지정할 수 있다.
     * username : String
     *
     * 대상이 여러개인 경우 튜플이나 DTO로 받아야 한다.
     */

    /**
     * Tuple은 querydsl이 지원하는(종속적인) 객체이다. 따라서 Tuple을 controller나 service 계층까지 가져가는 것은 타 계층에까지 repository가 JPA, querydsl을 의존하게 만들며, 내부 구현 기술이 어떤 것인지 알리게 된다. Spring에서 추구하는 설계는 내부 구현
     * 기술을 최대한 숨기는 것(의존도를 낮추는 것)에 있다. 이렇게 의존도가 놓은 설계는 결국 repository가 다른 기술을 쓰게 되면 같이 수정되어야 한다.
     * <p>
     * 결국 이러한 문제를 없애기 위해 계층 간 데이터를 리턴할 때는 DTO로 변환하여 던지는 것이 의존관계를 없앨 수 있는 방법이다.
     */
    @Test
    void fetch_username() {
        //given

        //when
        List<String> result = queryFactory
            .select(member.username)
            .from(member)
            .fetch();

        //then
        result.forEach(System.out::println);
    }

    @Test
    void fetch_tuple() {
        //given

        //when
        List<Tuple> result = queryFactory
            .select(member.age, member.username)
            .from(member)
            .fetch();

        //then
        result.forEach(item -> {
            System.out.println(item.get(member.age));
            System.out.println(item.get(member.username));
        });

    }

    /**
     * JPQL에서 아래와 같이 DTO를 사용하는 것은 불가능하다. select m from Member m", MemberDto.class) m 은 entity의 alias이기 때문
     * <p>
     * 따라서 DTO를 사용하고 싶다면 1. DTO 객체에 projection으로 가져올 필드를 파라미터로 받는 생성자를 정의해야 한다. 2. 아래 테스트와 같이 해당 DTO를 new로 생성하듯 명시해야 한다. * JPQL에서 제공하는 new operation 문법
     * <p>
     * * 순수 JPA에서 DTO를 사용하기 위해 new 명령어를 사용해야함. * DTO의 모든 패키지 명을 명시해야함 * 생성자 방식만 지원 (field 주입, setter 주입 불가)
     */
    @Test
    void fetch_dto_by_JPQL() {
        //given

        //when
        List<MemberDto> resultList =
            em.createQuery("select new me.hjhng125.querydsl.model.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();
        //then

        resultList.forEach(System.out::println);
    }

    /**
     * querydsl에서 DTO로 반환하는 방법 1. 프로퍼티 접근 - setter 2. 필드 직접 접근 3. 생성자 사용
     */
    @Test
    void fetch_dto_by_querydsl_by_property() {
        //given

        //when
        List<MemberDto> result = queryFactory
            .select(Projections.bean(MemberDto.class,
                member.username, // setter를 사용하는 property와 이름이 같아야 한다.
                member.age))
            .from(member)
            .fetch();

        //then
        result.forEach(System.out::println);

    }

    @Test
    void fetch_dto_by_querydsl_by_field() {
        //given

        //when
        List<MemberDto> result = queryFactory
            .select(Projections.fields(MemberDto.class,
                member.username, // getter, setter가 없어도 필드에 바로 주입됨,
                member.age))
            .from(member)
            .fetch();

        //then
        result.forEach(System.out::println);

    }

    @Test
    void fetch_userDto_by_querydsl_by_field() {
        //given

        //when
        List<UserDto> result = queryFactory
            .select(Projections.fields(UserDto.class,
                member.username.as("name"), // field 명이 맞아야 하기에 alias를 사용해야함.
                member.age))
            .from(member)
            .fetch();

        //then
        result.forEach(System.out::println);

    }

    @Test
    void fetch_userDto_with_scala_subQuery_by_querydsl_by_field() {
        //given

        //when
        List<UserDto> result = queryFactory
            .select(Projections.fields(UserDto.class,
                member.username.as("name"), // field 명이 맞아야 하기에 alias를 사용해야함.
                ExpressionUtils.as(
                    JPAExpressions
                        .select(memberSub.age.max())
                        .from(memberSub), "age")
                // field 주입을 해야하는데 서브쿼리를 쓰고 싶은 경우 ExpressionUtils를 사용해야 한다. 서브쿼리는 field이름과 안맞으니깐 그 자체를 alias한다고 생각하자. 서브쿼리가 아닌 단순 필드도 할 수 있으나 그냥 as가 간편함.
            ))
            .from(member)
            .fetch();

        //then
        result.forEach(System.out::println);

    }

    @Test
    void fetch_dto_by_querydsl_by_constructor() {
        //given

        //when
        List<MemberDto> result = queryFactory
            .select(Projections.constructor(MemberDto.class,
                member.username, // 생성자를 통한 주입은 projection하는 값들의 순서가 맞아야 한다.
                member.age))
            .from(member)
            .fetch();

        //then
        result.forEach(System.out::println);

    }

    @Test
    void fetch_uesrDto_by_querydsl_by_constructor() {
        //given

        //when
        List<UserDto> result = queryFactory
            .select(Projections.constructor(UserDto.class, // 생성자를 통한 주입은 생성자의 타입만을 보기 때문에 필드명이 달라도 된다.
                member.username, // 생성자를 통한 주입은 projection하는 값들의 순서가 맞아야 한다.
                member.age))
            .from(member)
            .fetch();

        //then
        result.forEach(System.out::println);

    }

    /**
     * Q-Type Dto를 new 연산자를 통해(생성자 그대로) 주입받을 수 있기 때문에 IDE의 도움을 받아 훨씬 안정적으로 값을 받을 수 있다. 만약 타입이 안맞을 경우 컴파일 시점에 오류를 잡을 수 있다.
     * <p>
     * Projections.constructor 방식은 타입이 다르거나, dto에 선언되지 않은 타입을 추가로 넣었을 때 컴파일 시점에 오류를 잡지 못하고, 런타임 시점에 잡는다는 단점이 있다.
     * <p>
     * 하지만 Q-Type을 무조건 선언해야하며, DTO 자체가 querydsl에 심히 의존하게 된다는 단점이 생긴다. 따라서 querydsl에서 다른 기술로 대체하게 되면 DTO에 문제가 된다. DTO는 service, controller 계층, 혹은 API를 통해 외부로 나가는 등 여러 계층에서 사용하는데 모든
     * 곳에 영향이 생긴다. DTO 자체가 순수성을 잃는다.
     * <p>
     * 따라서 DTO가 순수하길 원한다면 이 방법은 고려되어야 한다.
     */
    @Test
    void fetch_by_queryProjection() {
        //given

        //when
        List<MemberDto> result = queryFactory
            .select(new QMemberDto(
                member.username,
                member.age
            ))
            .from(member)
            .fetch();

        //then
        result.forEach(System.out::println);

    }
}
