package me.hjhng125.querydsl.repository;

import static me.hjhng125.querydsl.model.entity.QMember.member;
import static me.hjhng125.querydsl.model.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import me.hjhng125.querydsl.model.MemberSearchCondition;
import me.hjhng125.querydsl.model.dto.MemberTeamDTO;
import me.hjhng125.querydsl.model.dto.QMemberTeamDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

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

        long total = getTotal(condition);

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

    private long getTotal(MemberSearchCondition condition) {
        return jpaQueryFactory
            .select(member)
            .from(member)
            .leftJoin(member.team, team)
            .where(
                usernameEquals(condition.getUsername()),
                teamNameEquals(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe())
            )
            .fetchCount();
    }
}
