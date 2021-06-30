package me.hjhng125.querydsl.repository;

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
        Member member1 = new Member("member1", 10);
        Member save = memberRepository.save(member1);

        //when
        Optional<Member> byId = memberRepository.findById(save.getId());
        List<Member> all = memberRepository.findAll();
        List<Member> byUsername = memberRepository.findByUsername("member1");

        //then
        assertThat(byId.get().getId()).isEqualTo(save.getId());
        assertThat(all.size()).isEqualTo(1);
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
}