package me.hjhng125.querydsl;

import static me.hjhng125.querydsl.model.entity.QMember.member;
import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import me.hjhng125.querydsl.model.entity.Member;
import me.hjhng125.querydsl.model.entity.QMember;
import me.hjhng125.querydsl.model.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
public class QuerydslBulkTest {

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

    /**
     * 현재 영속성 컨텍스트에 저장된 엔터티는 <br/>
     * member1, 10 => member1 <br/>
     * member2, 20 => member2 <br/>
     * member3, 30 => member3 <br/>
     * member4, 40 => member4 이다. <br/>
     * <p/>
     * JPQL은 영속성 컨텍스트를 무시하고 DB에 바로 쿼리를 반영한다.<br/>
     * 따라서 영속성 컨텍스트와 DB의 상태가 달라져 버린다.
     */
    @Test
    // @Commit // 강제 커밋을 위한 어노테이션
    void bulk_update() {
        //given

        //when
        long count = queryFactory
            .update(member)
            .set(member.username, "비화원")
            .where(member.age.lt(30))
            .execute();

        /*
         * JPQL은 바로 쿼리를 발생시키기 때문에 select 구문이 발생한다.
         * 가져온 데이터의 id를 확인해보니 이미 영속성 컨텍스트에 있다.
         * 그러면 DB에서 가져온 데이터를 버리고 영속성 컨텍스트의 엔터티를 반환한다.
         * 따라서 이렇게 가져온 값은 영속성 컨텍스트에서 가져온 값이기 때문에
         * 같은 트랜잭션 내에서 최신 값을 얻을 수 없게 된다.
         *
         * 이의 해결방법은 아래와 같다.
         * em.flush();
         * em.clear();
         * 항상 bulk 연산을 진행하면 영속성 컨텍스트를 지워야 올바른 값을 얻을 수 있다.
         */
        em.flush();
        em.clear();

        /*
         * JPQL은 쿼리 발생 전 선수작업으로 flush가 발생한다.
         * 영속성 컨텍스트의 상태가 DB에 반영되기 전 JPQL이 실행되면 서로간의 일관성이 무너지기 때문이다.
         */
        List<Member> result = queryFactory
            .selectFrom(member)
            .fetch();

        //then
        assertThat(result)
            .extracting("username")
            .doesNotContain("비회원");

        result.forEach(System.out::println);
        assertThat(count).isEqualTo(2);
        em.createQuery("select m from Member m", Member.class).getResultList();

    }

    @Test
    void bulk_add() {
        //given

        //when
        long count = queryFactory
            .update(member)
            .set(member.age, member.age.add(1))
            .execute();

        //then
        assertThat(count).isEqualTo(4);

    }

    @Test
    void bulk_multiply() {
        //given

        //when
        long count = queryFactory
            .update(member)
            .set(member.age, member.age.multiply(2))
            .execute();

        //then
        assertThat(count).isEqualTo(4);

    }

    @Test
    void bulk_delete() {
        //given

        //when
        long count = queryFactory
            .delete(member)
            .where(member.age.goe(18))
            .execute();

        //then
        assertThat(count).isEqualTo(3);

    }
}
