package me.hjhng125.querydsl.controller;

import static me.hjhng125.querydsl.model.entity.QMember.member;

import com.querydsl.core.types.Predicate;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import me.hjhng125.querydsl.model.MemberSearchCondition;
import me.hjhng125.querydsl.model.dto.MemberDto;
import me.hjhng125.querydsl.model.dto.MemberTeamDTO;
import me.hjhng125.querydsl.model.entity.Member;
import me.hjhng125.querydsl.repository.MemberJpaRepository;
import me.hjhng125.querydsl.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
     * OrderSpecifier 사용 <br/> spring data의 Sort는 하나의 엔터티에서 조회할 경우는 가능하나, join이 포함된 복잡한 쿼리에서 잘 동작하지 않는다.
     * <br/> 그런 경우 파라미터로 직접 받아서 사용한다.
     *
     * 컨트롤러에서 엔티티를 응답할 경우 양방향 연관관계에서는 반드시 둘 중 하나에 @JsonIgnore 어노테이션을 붙여서 연결을 끊어줘야 한다. 그렇지 않으면 무한 루프를 돌게 된다. 무한 루프를 해결해도 Proxy 관련 클래스(ByteBuddyInterceptor)의 Type Definition 관련 오류가 발생한다.
     * 결국 Hibernate5 모듈 빈 정의를 작성하여 사용해야 하는데 이 모든것을 하는것보다는 Entity를 DTO로 변환하여 작성하는것이 가장 좋은 방법이다.
     * DTO를 반환해도 N+1 문제가 발생하게 되는데, 그렇다고 fetch 옵션을 EAGER로 바꿔서는 안되고, 모두 LAZY로 유지한 채로 JPQL로 페치조인을 사용하여 쿼리를 개선한다.
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

    @GetMapping("/v7/members/{member_id}")
    public ResponseEntity<Member> searchMemberV7(@PathVariable("member_id") Member member) throws UserPrincipalNotFoundException{
        return ResponseEntity.ok(memberRepository.findById(member.getId()).orElseThrow(
            () -> new UserPrincipalNotFoundException("not found")
        ));
    }
}
