package me.hjhng125.querydsl.controller;

import static me.hjhng125.querydsl.model.entity.QMember.member;

import com.querydsl.core.types.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import me.hjhng125.querydsl.model.MemberSearchCondition;
import me.hjhng125.querydsl.model.dto.MemberTeamDTO;
import me.hjhng125.querydsl.model.entity.Member;
import me.hjhng125.querydsl.repository.MemberJpaRepository;
import me.hjhng125.querydsl.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/v1/members")
    public List<MemberTeamDTO> searchMemberV1(MemberSearchCondition condition) {
        return memberJpaRepository.searchByWhereParam(condition);
    }

    @GetMapping("/v2/members")
    public Page<MemberTeamDTO> searchMemberV2(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageSimple(condition, pageable);
    }

    @GetMapping("/v3/members")
    public Page<MemberTeamDTO> searchMemberV3(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageComplex(condition, pageable);
    }

    @GetMapping("/v4/members")
    public Page<MemberTeamDTO> searchMemberV4(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageNoCountQuery(condition, pageable);
    }

    /**
     * spring data의 Sort를 querydsl에 적용하기 <br/> OrderSpecifier 사용 <br/> spring data의 Sort는 하나의 엔터티에서 조회할 경우는 가능하나, join이 포함된 복잡한 쿼리에서 잘 동작하지 않는다. <br/> 그런 경우 파라미터로 직접 받아서 사용한다.
     */

    /**
     * 테이블의 연관관계의 경우 제대로 동작하지 않음 확인 <br/>
     * QuerydslPredicateExecutor를 사용하여 연관관계가 있는 엔터티 조회 시 무한으로 순회하는 현상 발견
     */
    @GetMapping("/v5/members")
    public Iterable<Member> searchMemberV5(MemberSearchCondition condition) {
        return memberRepository.findAll(
            member.age.goe(condition.getAgeGoe())
                .and(member.age.loe(condition.getAgeLoe())));
    }

    @GetMapping("/v6/members")
    public List<Member> searchMemberV6(@QuerydslPredicate(root = Member.class) Predicate predicate) {
        System.out.println("predicate = " + predicate);
        Iterable<Member> all = memberRepository.findAll(predicate);
        List<Member> listAll = new ArrayList<>();
        all.forEach(listAll::add);
        return listAll;
    }
}
