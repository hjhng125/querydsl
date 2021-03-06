package me.hjhng125.querydsl.member;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import me.hjhng125.querydsl.model.entity.Member;
import me.hjhng125.querydsl.model.entity.Team;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
class MemberTest {

    @PersistenceContext
    EntityManager em;

    @Test
    public void testEntity() {
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

        // init
        em.flush();
        em.clear();

        List<Member> members = em.createQuery("select m from Member m", Member.class)
            .getResultList();

        members.forEach(member -> {
            System.out.println(member);
            System.out.println(member.getUsername() + "'s team: " + member.getTeam());
        });
    }
}