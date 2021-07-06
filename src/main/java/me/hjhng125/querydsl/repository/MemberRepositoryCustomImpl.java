package me.hjhng125.querydsl.repository;

import static me.hjhng125.querydsl.model.entity.QMember.member;
import static me.hjhng125.querydsl.model.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Objects;
import me.hjhng125.querydsl.model.MemberSearchCondition;
import me.hjhng125.querydsl.model.dto.MemberTeamDTO;
import me.hjhng125.querydsl.model.dto.QMemberTeamDTO;
import me.hjhng125.querydsl.model.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;

/**
 * naming은 postfix가 Impl 이어야하며
 * EnableJpaRepository 어노테이션의 옵션으로 수정가능
 * <P/>
 *
 * 커스텀 리포지토리를 만들 때 고민 사항
 * 만약 구현한 조회 쿼리가 특정 화면 혹은 API에서만 단독으로 사용되며,
 * 특정 entity를 가져오는 기능이 아닌 경우,
 * 커스텀 리포지토리로 구현하지 않고 조회용 리포지 토리를 구현해보는 것을 생각해보자
 *
 * ex) MemberQueryRepository
 *
 */

/**
 * RepositoryConfigurationDelegate.registerRepositoriesIn()
 * <br/>
 * 자동으로 커스텀 리포지토리 구현체를 찾아 빈으로 등록함.
 */
public class MemberRepositoryCustomImpl extends QuerydslRepositorySupport implements MemberRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    public MemberRepositoryCustomImpl(JPAQueryFactory jpaQueryFactory) {
        super(Member.class);
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<MemberTeamDTO> search(MemberSearchCondition condition) {
        return jpaQueryFactory
            .select(new QMemberTeamDTO(
                member.id,
                member.username,
                member.age,
                team.id,
                team.name
            ))
            .from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEquals(condition.getUsername()),
                teamNameEquals(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe())
            )
            .fetch();
    }

    private BooleanExpression usernameEquals(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEquals(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    @Override
    public Page<MemberTeamDTO> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDTO> results = jpaQueryFactory
            .select(new QMemberTeamDTO(
                member.id,
                member.username,
                member.age,
                team.id,
                team.name
            ))
            .from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEquals(condition.getUsername()),
                teamNameEquals(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe())
            )
            //.orderBy(member.username.desc()) // 만약 order by가 쿼리에 들어간 경우 count쿼리에선 필요없기 때문에 지워진다. - 알아서 최적화 해줌.
            .offset(pageable.getOffset()) // 몇번째까지 스킵하고 몇번째부터 시작할 것인가
            .limit(pageable.getPageSize())
            .fetchResults(); // contents를 가져오는 쿼리와 count를 가져오는 쿼리 두번 날림.

        return new PageImpl<>(results.getResults(), pageable, results.getTotal());

    }

    /**
     * QuerydslRepositorySupport를 사용하면 JPAQueryFactory와 다르게 from()으로 시작한다.<br/>
     * 또한 getQuerydsl()이라는 메소드를 제공하는데, 이는 spring data jpa가 제공하는 Querydsl라는 헬퍼 클래스를 반환한다.<br/>
     * Querydsl 객체는 applyPagination() 메소드를 제공하는데 이는 Pageable을 인자로 받아 내부에서 페이징한다.<br/>
     * 위에서 메소드 체인으로 적용했던 offset(), limit() 코드가 줄어드는 장점이 있다.<br/>
     * 하지만 Sort는 적용할 시 오류가 발생한다.<br/>
     * 또한 from()으로 시작하기에 JPAQueryFactory 보다 명시적이지 않다. <br/>
     */
    @Override
    public Page<MemberTeamDTO> searchPageSimpleV2(MemberSearchCondition condition, Pageable pageable) {
        JPQLQuery<MemberTeamDTO> query = from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEquals(condition.getUsername()),
                teamNameEquals(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe())
            )
            .select(new QMemberTeamDTO(
                member.id,
                member.username,
                member.age,
                team.id,
                team.name
            ))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize());

        JPQLQuery<MemberTeamDTO> memberTeamDTOJPQLQuery = Objects.requireNonNull(getQuerydsl()).applyPagination(pageable, query);

        return new PageImpl<>(memberTeamDTOJPQLQuery.fetch(), pageable, memberTeamDTOJPQLQuery.fetchCount());

    }

    /**
     * 데이터의 내용과 전체 카운트를 별로도 조회하는 방법 <br/>
     * 쿼리 두개를 분리.<br/>
     * 카운트 쿼리는 더 간단한 쿼리로 만들어지는 경우가 있다.<br/>
     * 예를 들어 join이 필요없을 수 있다. <br/>
     * 또한 카운트 쿼리를 먼저 실행하고 값이 0인 경우 컨텐트 쿼리를 날리지 않을 수 있다. <br/>
     * 이런 경우 쿼리를 분리하면 최적화가 된다.
     * @param condition
     * @param pageable
     * @return
     */
    @Override
    public Page<MemberTeamDTO> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDTO> contents = getMemberTeamDTOS(condition, pageable);

        long total = getTotalQuery(condition).fetchCount();

        return new PageImpl<>(contents, pageable, total);
    }

    private List<MemberTeamDTO> getMemberTeamDTOS(MemberSearchCondition condition, Pageable pageable) {
        return jpaQueryFactory
            .select(new QMemberTeamDTO(
                member.id,
                member.username,
                member.age,
                team.id,
                team.name
            ))
            .from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEquals(condition.getUsername()),
                teamNameEquals(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe())
            )
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
    }

    private JPAQuery<Member> getTotalQuery(MemberSearchCondition condition) {
        return jpaQueryFactory
            .select(member)
            .from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEquals(condition.getUsername()),
                teamNameEquals(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe())
            );
    }

    /**
     * 때에 따라 카운트 쿼리를 생략할 수 있다.<br/>
     * 1. 시작 페이지면서 컨텐츠 사이즈가 페이지 사이즈보다 작을 때 <br/>
     * 2. 마지막 페이지일 떄 (offset + 컨텐트 사이즈로 전체 사이즈를 구할)
     * <p/>
     * PageableExecutionUtils.getPage() 함수는<br/>
     * 마지막 인자로 카운트 쿼리 함수를 받는다.<br/>
     * 내부적으로 위처럼 카운트를 생략할 수 있는 경우 함수를 실행하지 않는다.
     * </p>
     * 이 방법이 위의 방법보다 좀 더 최적화된 방법이므로 이 방법을 사용하도록 하자.
     *
     * <p/>
     * if (pageable.isUnpaged() || pageable.getOffset() == 0) {
     * <p/>
     * 			if (pageable.isUnpaged() || pageable.getPageSize() > content.size()) {
     * 				return new PageImpl<>(content, pageable, content.size());
     *                        }
     * <p/>
     * 			return new PageImpl<>(content, pageable, totalSupplier.getAsLong());* 		}
     * <p/>
     * 		if (content.size() != 0 && pageable.getPageSize() > content.size()) {
     * 			return new PageImpl<>(content, pageable, pageable.getOffset() + content.size());
     *        }
     * <p/>
     * 		return new PageImpl<>(content, pageable, totalSupplier.getAsLong());
     */
    public Page<MemberTeamDTO> searchPageNoCountQuery(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDTO> contents = getMemberTeamDTOS(condition, pageable);

        JPAQuery<Member> countQuery = getTotalQuery(condition);

        return PageableExecutionUtils.getPage(contents, pageable, countQuery::fetchCount);
    }

}
