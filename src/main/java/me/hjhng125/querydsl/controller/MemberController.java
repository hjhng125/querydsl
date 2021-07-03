package me.hjhng125.querydsl.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import me.hjhng125.querydsl.model.MemberSearchCondition;
import me.hjhng125.querydsl.model.dto.MemberTeamDTO;
import me.hjhng125.querydsl.repository.MemberJpaRepository;
import me.hjhng125.querydsl.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * spring data의 Sort를 querydsl에 적용하기 <br/>
     * OrderSpecifier 사용 <br/>
     * spring data의 Sort는 하나의 엔터티에서 조회할 경우는 가능하나, join이 포함된 복잡한 쿼리에서
     * 잘 동작하지 않는다. <br/>
     * 그런 경우 파라미터로 직접 받아서 사용한다.
     */
}
