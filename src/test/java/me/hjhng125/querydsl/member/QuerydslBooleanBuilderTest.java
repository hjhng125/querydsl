package me.hjhng125.querydsl.member;

import static me.hjhng125.querydsl.member.QMember.member;
import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import me.hjhng125.querydsl.team.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
public class QuerydslBooleanBuilderTest {

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
    void dynamicQuery_boolean_builder() {
        //given
        String username = "member1";
        Integer age = 10;

        //when
        List<Member> result = searchMember(username, age);

        //then
        assertThat(result.size()).isEqualTo(1);

    }

    @Test
    void dynamicQuery_boolean_builder_age_null() {
        //given
        String username = "member1";
        Integer age = null;

        //when
        List<Member> result = searchMember(username, age);

        //then
        assertThat(result.size()).isEqualTo(1);

    }

    /**
     * parameter가 wrapper 타입인 이유는 null 여부에 따라 query가 동적으로 바뀌어야 한다. null인 조건은 where절에 들어가지 않는다.
     */
    private List<Member> searchMember(String username, Integer age) {

        // 만약 특정 조건이 필수여야 한다면 BooleanBuilder 생성시 파라미터로 넘긴다.
        // BooleanBuilder builder = new BooleanBuilder(member.username.eq(username));
        BooleanBuilder builder = new BooleanBuilder();

        if (username != null) {
            builder.and(member.username.eq(username));
        }

        if (age != null) {
            builder.and(member.age.eq(age));
        }

        return queryFactory
            .selectFrom(member)
            .where(builder)
            .fetch();
    }

    @Test
    void dynamicQuery_where_multiple_parameter() {
        //given
        String username = "member1";
        Integer age = 10;

        //when
        List<Member> result = searchMemberWithWhereMultipleParam(username, age);

        //then
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMemberWithWhereMultipleParam(String username, Integer age) {
        return queryFactory
            .selectFrom(member)
            .where( // where 절에 null 인 조건은 무시된다.
                usernameEq(username).and(ageEq(age)))
            .fetch();
    }

    // 아래 두 조건은 특정 이름의 메서드로 묶을 수 있음
    private BooleanExpression usernameEq(String username) {
        return username == null ? null : member.username.eq(username);

    }

    private BooleanExpression ageEq(Integer age) {
        return age == null ? null : member.age.eq(age);

    }
}
