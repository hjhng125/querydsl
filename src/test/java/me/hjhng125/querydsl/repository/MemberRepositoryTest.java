package me.hjhng125.querydsl.repository;

import static me.hjhng125.querydsl.model.entity.QMember.member;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import me.hjhng125.querydsl.config.QuerydslConfig;
import me.hjhng125.querydsl.model.MemberSearchCondition;
import me.hjhng125.querydsl.model.dto.MemberTeamDTO;
import me.hjhng125.querydsl.model.entity.Member;
import me.hjhng125.querydsl.model.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import(QuerydslConfig.class)
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @PersistenceContext
    EntityManager em;

    @BeforeEach
    public void beforeEach() {
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
    void repositoryTest() {
        //given
        Member member5 = new Member("member5", 10);
        Member save = memberRepository.save(member5);

        //when
        Optional<Member> byId = memberRepository.findById(save.getId());
        List<Member> all = memberRepository.findAll();
        List<Member> byUsername = memberRepository.findByUsername("member1");

        //then
        assertThat(byId.get().getId()).isEqualTo(save.getId());
        assertThat(all.size()).isEqualTo(5);
        assertThat(byUsername)
            .extracting("username")
            .containsExactly("member1");
    }

    @Test
    void searchByCustomRepository() {
        //given
        MemberSearchCondition memberSearchCondition = MemberSearchCondition.builder()
            .teamName("teamB")
            .ageGoe(20)
            .ageLoe(40)
            .build();

        //when
        List<MemberTeamDTO> memberTeamDTOS = memberRepository.search(memberSearchCondition);

        //then
        assertThat(memberTeamDTOS).extracting("username")
            .containsExactly("member3", "member4");
    }

    @Test
    void searchPageSimple() {
        //given
        MemberSearchCondition memberSearchCondition = MemberSearchCondition.builder()
            .build();
        PageRequest pageRequest = PageRequest.of(0, 3);

        //when
        Page<MemberTeamDTO> memberTeamDTOPage = memberRepository.searchPageSimple(memberSearchCondition, pageRequest);

        //then
        assertThat(memberTeamDTOPage.getSize()).isEqualTo(3);
        assertThat(memberTeamDTOPage.getContent())
            .extracting("username")
            .containsExactly("member1", "member2", "member3");

    }

    @Test
    void searchPageComplex() {
        //given
        MemberSearchCondition memberSearchCondition = MemberSearchCondition.builder()
            .build();
        PageRequest pageRequest = PageRequest.of(0, 3);

        //when
        Page<MemberTeamDTO> memberTeamDTOPage = memberRepository.searchPageComplex(memberSearchCondition, pageRequest);

        //then
        assertThat(memberTeamDTOPage.getSize()).isEqualTo(3);
        assertThat(memberTeamDTOPage.getContent())
            .extracting("username")
            .containsExactly("member1", "member2", "member3");

    }

    @Test
    void searchPageNoCount() {
        //given
        MemberSearchCondition memberSearchCondition = MemberSearchCondition.builder()
            .build();
        PageRequest pageRequest = PageRequest.of(0, 5);

        //when
        Page<MemberTeamDTO> memberTeamDTOPage = memberRepository.searchPageNoCountQuery(memberSearchCondition, pageRequest);

        //then
        assertThat(memberTeamDTOPage.getSize()).isEqualTo(5);
        assertThat(memberTeamDTOPage.getContent())
            .extracting("username")
            .containsExactly("member1", "member2", "member3", "member4");

    }

    @Test
    void querydslPredicateExecutorTest() {
        Iterable<Member> result = memberRepository.findAll(
            member.age.between(20, 40) // QMember.member
                .and(member.username.eq("member2")));

        result.forEach(System.out::println);
        assertThat(result).extracting("username")
            .containsExactly("member2");
    }

    @Test
    void querydslRepositorySupportTest() {
        MemberSearchCondition memberSearchCondition = MemberSearchCondition.builder()
            .build();
        Page<MemberTeamDTO> result = memberRepository.searchPageSimpleV2(memberSearchCondition, PageRequest.of(0, 10));
        result.forEach(System.out::println);

        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getContent())
            .extracting("username")
            .containsExactly("member1", "member2", "member3", "member4");
    }

    @Test
    void teest() {
        //given
        em.flush();
        em.clear();

        //when
        Member byId = memberRepository.findById(3L).get();
        //then

        System.out.println("member = " + byId);
        System.out.println("team = " + byId.getTeam());
    }

    @Test
    void teest2() {
        //given
        em.flush();
        em.clear();

        //when
        List<Member> byId = memberRepository.findAll();
        //then

        for (Member member1 : byId) {
            System.out.println("member = " + member1);
            System.out.println("team = " + member1.getTeam());
        }
    }
}