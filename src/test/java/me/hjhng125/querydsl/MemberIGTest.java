package me.hjhng125.querydsl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import me.hjhng125.querydsl.controller.MemberController;
import me.hjhng125.querydsl.repository.MemberJpaRepository;
import me.hjhng125.querydsl.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
public class MemberIGTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    public void memberList() throws Exception {
        mockMvc.perform(get("/v5/members")
        .queryParam("age", "10"))
            .andDo(print());
    }
}
