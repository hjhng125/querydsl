package me.hjhng125.querydsl.team;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import me.hjhng125.querydsl.member.Member;

@Getter
@Entity
@ToString(of = {"id", "name"})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

    @Id @GeneratedValue
    @Column(name = "team_id")
    private Long id;

    private String name;

    @OneToMany(mappedBy = "team") // 관계의 주인이 아님.
    private Set<Member> members = new HashSet<>();

    public Team(String name) {
        this.name = name;
    }
}
