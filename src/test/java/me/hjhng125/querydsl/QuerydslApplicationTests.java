package me.hjhng125.querydsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import me.hjhng125.querydsl.entity.Hello;
import me.hjhng125.querydsl.entity.QHello;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
// @Commit // 테스트 종료 시 롤백하지 않음.
class QuerydslApplicationTests {

    @PersistenceContext
    EntityManager entityManager;

    @Test
    void contextLoads() {
        Hello hello = new Hello();
        entityManager.persist(hello);

        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        QHello qHello = QHello.hello;

        Hello result = queryFactory
            .selectFrom(qHello)
            .fetchOne();

        System.out.println(entityManager.contains(hello));
        System.out.println(entityManager.contains(result));
        System.out.println(hello);
        System.out.println(result);
        assertThat(result).isEqualTo(hello);
        assertThat(result.getId()).isEqualTo(hello.getId());
    }

}
