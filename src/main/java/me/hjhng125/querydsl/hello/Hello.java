package me.hjhng125.querydsl.hello;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Hello {

    @Id
    @GeneratedValue
    private Long id;

}
