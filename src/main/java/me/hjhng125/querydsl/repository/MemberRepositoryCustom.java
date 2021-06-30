package me.hjhng125.querydsl.repository;

import java.util.List;
import me.hjhng125.querydsl.model.MemberSearchCondition;
import me.hjhng125.querydsl.model.dto.MemberTeamDTO;

public interface MemberRepositoryCustom {

    List<MemberTeamDTO> search(MemberSearchCondition condition);
}
