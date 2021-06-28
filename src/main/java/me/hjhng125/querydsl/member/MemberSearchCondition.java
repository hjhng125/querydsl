package me.hjhng125.querydsl.member;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberSearchCondition {

    private final String username;
    private final String teamName;
    private final Integer ageGoe;
    private final Integer ageLoe;
}
