package me.hjhng125.querydsl.repository;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import me.hjhng125.querydsl.repository.MemberTestRepository.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

/**
 * QuerydslRepositorySupport는 <br/> 1. Sort를 적용할 시 정상작동하지 않는다. <br/> 2. 제공하는 applyPagination() 메소드도 그닥 훨씬 편하다 와닿지 않았다. 3. from()으로 시작하는 문법이 명시적이지 않았다. <p/>
 * QuerydslRepositorySupport Custom Class를 만들어보자. 이 커스텀 클래스는 <br/> 1. spring data가 제공하는 페이징을 편리하게 해주고 <br/> 2. 페이징과 카운트 쿼리를 분리할 수 있다. <br/> 3. 또한, spring data가 제공하는 Sort를 지원한다.
 * <br/> 4. select(), selectFrom()으로 시작할 수 있다. <br/> 5. EntityManager, QueryFactory를 제공한다.
 */
@Repository
public abstract class Querydsl4RepositorySupport {

    private final Class<?> domainClass;
    private Querydsl querydsl;
    private EntityManager entityManager;
    private JPAQueryFactory jpaQueryFactory;

    public Querydsl4RepositorySupport(Class<?> domainClass) {
        Assert.notNull(domainClass, "Domain class must not be null!");
        this.domainClass = domainClass;
    }

    @Autowired
    public void setEntityManager(EntityManager entityManager) {
        Assert.notNull(entityManager, "EntityManager must not be null!");

        // Sort를 위한 세팅으로 path를 제대로 정해줘야 Sort 버그가 해결된다.
        JpaEntityInformation<?, ?> entityInformation = JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager);
        SimpleEntityPathResolver resolver = SimpleEntityPathResolver.INSTANCE;
        EntityPath<?> path = resolver.createPath(entityInformation.getJavaType());

        this.entityManager = entityManager;
        this.querydsl = new Querydsl(entityManager, new PathBuilder<>(path.getType(), path.getMetadata()));
        this.jpaQueryFactory = new JPAQueryFactory(entityManager);
    }

    @PostConstruct
    public void validate() {
        Assert.notNull(entityManager, "EntityManager must not be null!");
        Assert.notNull(querydsl, "Querydsl must not be null!");
        Assert.notNull(jpaQueryFactory, "QueryFactory must not be null!");
    }

    protected JPAQueryFactory getJpaQueryFactory() {
        return jpaQueryFactory;
    }

    protected Querydsl getQuerydsl() {
        return querydsl;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    protected <T> JPAQuery<T> select(Expression<T> expression) {
        return getJpaQueryFactory().select(expression);
    }

    protected <T> JPAQuery<T> selectFrom(EntityPath<T> path) {
        return getJpaQueryFactory().selectFrom(path);
    }

    protected <T> Page<T> applyPagination(Pageable pageable,
        Function<JPAQueryFactory, JPAQuery<T>> contentQueryFunction) {

        JPAQuery<T> contentQuery = contentQueryFunction.apply(getJpaQueryFactory());
        List<T> content = getQuerydsl().applyPagination(pageable, contentQuery).fetch();

        return PageableExecutionUtils.getPage(content, pageable, contentQuery::fetchCount);
    }

    protected <T, ID> Page<T> applyPagination(Pageable pageable,
        Function<JPAQueryFactory, JPAQuery<T>> contentQueryFunction,
        Function<JPAQueryFactory, JPAQuery<ID>> countQueryFunction) {

        JPAQuery<T> contentQuery = contentQueryFunction.apply(getJpaQueryFactory());
        List<T> content = getQuerydsl().applyPagination(pageable, contentQuery).fetch();

        JPAQuery<ID> countQuery = countQueryFunction.apply(getJpaQueryFactory());

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
    }
}
