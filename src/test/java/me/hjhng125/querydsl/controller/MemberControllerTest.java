package me.hjhng125.querydsl.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import me.hjhng125.querydsl.repository.MemberJpaRepository;
import me.hjhng125.querydsl.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    MemberJpaRepository memberJpaRepository;
    @MockBean
    MemberRepository memberRepository;

    @Test
    void searchMemberV5() throws Exception {
        mockMvc.perform(get("/v5/members")
            .queryParam("age", "10"))
            .andDo(print());

    }
}