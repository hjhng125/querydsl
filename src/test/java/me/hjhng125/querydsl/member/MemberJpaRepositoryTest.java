package me.hjhng125.querydsl.member;

import static me.hjhng125.querydsl.model.entity.QMember.member;
import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import me.hjhng125.querydsl.config.QuerydslConfig;
import me.hjhng125.querydsl.model.MemberSearchCondition;
import me.hjhng125.querydsl.model.dto.MemberTeamDTO;
import me.hjhng125.querydsl.model.entity.Member;
import me.hjhng125.querydsl.repository.MemberJpaRepository;
import me.hjhng125.querydsl.model.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(QuerydslConfig.class)
class MemberJpaRepositoryTest {

    @Autowired EntityManager em;
    @Autowired JPAQueryFactory queryFactory;
    MemberJpaRepository memberJpaRepository;
    Member test;

    @BeforeEach
    public void beforeEach() {
        memberJpaRepository = new MemberJpaRepository(em, queryFactory);
        test = new Member("member5", 50);
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
    void findById() {
        Member findMember = memberJpaRepository.findById(test.getId()).get();
        assertThat(findMember).isEqualTo(test);
    }

    @Test
    void findAll() {
        List<Member> all = memberJpaRepository.findAll();
        assertThat(all).contains(test);
    }

    @Test
    void findByUsername() {
        List<Member> findMember = memberJpaRepository.findByUsername("member1");
        assertThat(findMember).contains(test);
    }

    @Test
    void findAllQuerydsl() {
        //given

        //when
        List<Member> all = memberJpaRepository.findAllQuerydsl();

        //then
        assertThat(all).contains(test);

    }

    @Test
    void findByUsernameQuerydsl() {
        //given

        //when
        List<Member> findMember = memberJpaRepository.findByUsernameQuerydsl("member1");

        //then
        assertThat(findMember).contains(test);

    }

    @Test
    void searchByBuilderTest() {
        //given
        MemberSearchCondition memberSearchCondition = MemberSearchCondition.builder()
            .ageGoe(35)
            .ageLoe(40)
            .teamName("teamB")
            .build();

        //when
        List<MemberTeamDTO> memberTeamDTOS = memberJpaRepository.searchByBuilder(memberSearchCondition);

        //then
        assertThat(memberTeamDTOS).extracting("username")
            .containsExactly("member4");
    }

    @Test
    void searchByBuilderFetchTeamBTest() {
        //given
        MemberSearchCondition memberSearchCondition = MemberSearchCondition.builder()
            .teamName("teamB")
            .build();

        //when
        List<MemberTeamDTO> memberTeamDTOS = memberJpaRepository.searchByBuilder(memberSearchCondition);

        //then
        assertThat(memberTeamDTOS).extracting("username")
            .containsExactly("member3", "member4");
    }

    @Test
    void searchByWhereParam() {
        //given
        MemberSearchCondition memberSearchCondition = MemberSearchCondition.builder()
            .teamName("teamB")
            .ageGoe(20)
            .ageLoe(40)
            .build();

        //when
        List<MemberTeamDTO> memberTeamDTOS = memberJpaRepository.searchByWhereParam(memberSearchCondition);

        //then
        assertThat(memberTeamDTOS).extracting("username")
            .containsExactly("member3", "member4");
    }

    @Test
    void betweenTest() {
        //given

        //when
        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.between(30, 40))
            .fetch();

        //then
        assertThat(result)
            .extracting("username")
            .containsExactly("member3", "member4");

    }

}