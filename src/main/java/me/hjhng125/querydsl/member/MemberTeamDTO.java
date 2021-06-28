package me.hjhng125.querydsl.member;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MemberTeamDTO {

    private final Long memberId;
    private final String username;
    private final int age;

    private final Long teamId;
    private final String teamName;

    @QueryProjection
    public MemberTeamDTO(Long memberId, String username, int age, Long teamId, String teamName) {
        this.memberId = memberId;
        this.username = username;
        this.age = age;
        this.teamId = teamId;
        this.teamName = teamName;
    }
}
