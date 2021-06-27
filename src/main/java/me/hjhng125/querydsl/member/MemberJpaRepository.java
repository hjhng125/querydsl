package me.hjhng125.querydsl.member;

import static me.hjhng125.querydsl.member.QMember.member;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory jpaQueryFactory;

    @Transactional
    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findByMember = em.find(Member.class, id);
        return Optional.of(findByMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
            .getResultList();
    }

    public List<Member> findAllQuerydsl() {
        return jpaQueryFactory
            .selectFrom(member)
            .fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select m from Member m where m.username = :username", Member.class)
            .setParameter("username", username)
            .getResultList();
    }

    public List<Member> findByUsernameQuerydsl(String username) {
        return jpaQueryFactory
            .selectFrom(member)
            .where(usernameEq(username))
            .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        return member.username.eq(username);
    }
}
