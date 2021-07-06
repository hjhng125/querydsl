package me.hjhng125.querydsl.repository;

import static me.hjhng125.querydsl.model.entity.QMember.member;
import static me.hjhng125.querydsl.model.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import java.util.List;
import me.hjhng125.querydsl.model.MemberSearchCondition;
import me.hjhng125.querydsl.model.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {

    public MemberTestRepository() {
        super(Member.class);
    }

    public List<Member> basicSelect() {
        return select(member)
            .from(member)
            .fetch();
    }

    public List<Member> basicSelectFrom() {
        return selectFrom(member)
            .fetch();
    }

    public Page<Member> applyPagination(MemberSearchCondition condition, Pageable pageable) {
        JPAQuery<Member> query = selectFrom(member)
            .leftJoin(member.team, team)
            .where(usernameEquals(condition.getUsername()),
                teamNameEquals(condition.getTeamName()),
                ageGoe(condition.getAgeGoe()),
                ageLoe(condition.getAgeLoe()));

        List<Member> content = getQuerydsl().applyPagination(pageable, query)
            .fetch();

        return PageableExecutionUtils.getPage(content, pageable, query::fetchCount);
    }

    public Page<Member> customApplyPaginationOnlyContentQuery(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable,
            jpaQueryFactory -> jpaQueryFactory
                .selectFrom(member)
                .leftJoin(member.team, team)
                .where(usernameEquals(condition.getUsername()),
                    teamNameEquals(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe()))
        );
    }

    public Page<Member> customApplyPaginationWithCountQuery(MemberSearchCondition condition, Pageable pageable) {
        return applyPagination(pageable,
            contentQuery -> contentQuery
                .selectFrom(member)
                .where(usernameEquals(condition.getUsername()),
                    teamNameEquals(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe())),
            countQuery -> countQuery
                .select(member.id).from(member)
                .where(usernameEquals(condition.getUsername()),
                    teamNameEquals(condition.getTeamName()),
                    ageGoe(condition.getAgeGoe()),
                    ageLoe(condition.getAgeLoe()))
        );
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
