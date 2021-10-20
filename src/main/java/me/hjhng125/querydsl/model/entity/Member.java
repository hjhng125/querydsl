package me.hjhng125.querydsl.model.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
/**
 *  ToString()에 team과 같은 연관관계가 들어가면 안된다.
 *  그렇게 되면 ToString()이 호출될때 team의 객체로 갔다가
 *  team에서 다시 ToString()에 의해 member로 무한 루프를 탈 수 있다.
 *  따라서 소유하고 있는 필드만 포함시킨다.
 *  */
@ToString(of = {"id", "username", "age"})

/**
 * JPA를 사용하면 기본 생성자는 필수이다.
 * 그 이유는 조회 시 DB값을 필드에 매핑할 때 Reflection을 사용하기 때문이다.
 * <p/>
 * 하지만 이 기본 생성자가 private 이면 문제가 발생한다.
 * JPA는 매핑된 Entity 를 조회할 때 Eager, Lazy 두가지 옵션이 있다.
 * Eager에서는 상관없지만, Lazy(지연로딩)일 경우
 * 지금 당장 필요로 하지 않는 Entity 필드에 Proxy 객체를 만들어 넣게 된다.
 * <p/>
 * Entity의 프록시는 상속을 통해 만들어 지기 때문에 접근 제한자는 public, protected 여야만 한다.
 * 안정성 측면에서 더 작은 scope인 protected를 사용하는 것이 권장된다.
 * */
@NoArgsConstructor(access = AccessLevel.PROTECTED) // jpa는 기본생성자를 필요로함. Protected 까지 허용.
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String username;
    private int age;

//    @ManyToOne(fetch = FetchType.LAZY)
    @ManyToOne
    @JoinColumn(name = "team_id") // 관계의 주인
    private Team team;

    public Member(String username) {
        this(username, 0);
    }

    public Member(String username, int age) {
        this(username, age, null);
    }

    public Member(String username, int age, Team team) {
        this.username = username;
        this.age = age;
        if (team != null) {
            changeTeam(team);
        }
    }

    public void changeTeam(Team team) {
        // 멤버의 팀을 바꾸면 해당 팀의 멤버도 바꿈.
        this.team = team;
        team.getMembers().add(this);
    }
}
