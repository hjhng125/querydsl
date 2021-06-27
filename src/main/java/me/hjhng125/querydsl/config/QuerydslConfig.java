package me.hjhng125.querydsl.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * JPSQueryFactory를 빈으로 등록하여 싱글톤으로 관리할 시 문제가 없을까?
     *
     * * 문제가없다 *
     * JPSQueryFactory는 EntityManager에 의존한다.
     * EntityManager는 스프링과 함께 쓸 경우 동시성 문제와 관계없이 트랜잭션 단위로 분리되어 동작한다.
     *
     * 스프링에서 주입해주는 EntityManager는 사실 실제 영속성 컨텍스트가 아닌 프록시임.
     * 이 객체는 요청이 트랜잭션 단위로 다른 곳에 바인딩 되도록 라우팅 해주는 기능을 포함하고 있다.
     */
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
