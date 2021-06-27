package me.hjhng125.querydsl.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import me.hjhng125.querydsl.config.QuerydslConfig;
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
    Member member;

    @BeforeEach
    public void beforeEach() {
        memberJpaRepository = new MemberJpaRepository(em, queryFactory);
        member = new Member("member1", 10);
        memberJpaRepository.save(member);
    }

    @Test
    void findById() {
        Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);
    }

    @Test
    void findAll() {
        List<Member> all = memberJpaRepository.findAll();
        assertThat(all).contains(member);
    }

    @Test
    void findByUsername() {
        List<Member> findMember = memberJpaRepository.findByUsername("member1");
        assertThat(findMember).contains(member);
    }

    @Test
    void findAllQuerydsl() {
        //given

        //when
        List<Member> all = memberJpaRepository.findAllQuerydsl();

        //then
        assertThat(all).contains(member);

    }

    @Test
    void findByUsernameQuerydsl() {
        //given

        //when
        List<Member> findMember = memberJpaRepository.findByUsernameQuerydsl("member1");

        //then
        assertThat(findMember).contains(member);

    }

}