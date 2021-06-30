package me.hjhng125.querydsl.repository;

import static me.hjhng125.querydsl.model.entity.QMember.member;
import static me.hjhng125.querydsl.model.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import me.hjhng125.querydsl.model.MemberSearchCondition;
import me.hjhng125.querydsl.model.dto.MemberTeamDTO;
import me.hjhng125.querydsl.model.dto.QMemberTeamDTO;

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
}
