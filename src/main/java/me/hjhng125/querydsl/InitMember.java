package me.hjhng125.querydsl;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import me.hjhng125.querydsl.model.entity.Member;
import me.hjhng125.querydsl.model.entity.Team;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    @PostConstruct
    public void init() {
        initMemberService.init();;
    }

    /**
     * https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction-declarative-annotations
     * spring 문서에 의하면 spring default proxy mode는 외부 메서드에서 호출되었을 때만 프록시가 동작하고,
     * 해당 객체 내부에 의한 호출에는 @Transactional 어노테이션이 선언되었다해도 런타임 시에 실제 트랜잭션이 동작하지 않을 수 있다고 한.
     * 또한 프록시가 예상되는 동작을 하기 위해선 완전히 초기화가 되어야 하기에
     * @PostConstruct가 선언된 메서드와 같은 초기화 메서드에서 프록시 기능을 의존해선 안된다.
     */
    @Component
    static class InitMemberService {

        @PersistenceContext
        private EntityManager em;

        @Transactional
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i = 0; i < 100; ++i) {
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member"+i, i, selectedTeam));
            }
        }
    }
}
