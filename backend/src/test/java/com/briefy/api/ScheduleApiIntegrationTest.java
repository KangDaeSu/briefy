package com.briefy.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ScheduleApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18");

    @Autowired WebApplicationContext wac;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    MockMvc mockMvc;
    Cookie jwtCookie;

    @BeforeEach
    void setup() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        String email = "it-" + UUID.randomUUID() + "@briefy.test";
        String body = objectMapper.writeValueAsString(
                Map.of("email", email, "name", "Tester", "password", "pass1234"));

        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String setCookie = res.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).startsWith("jwt=");
        String token = setCookie.split(";")[0].split("=", 2)[1];
        jwtCookie = new Cookie("jwt", token);
    }

    @Test
    void createAndRetrieveSchedule() throws Exception {
        OffsetDateTime start = OffsetDateTime.now().plusDays(1).withNano(0);
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "통합테스트 회의",
                "startTime", start.toString(),
                "endTime", start.plusHours(1).toString()
        ));

        MvcResult createRes = mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(jwtCookie).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("통합테스트 회의"))
                .andReturn();

        @SuppressWarnings("unchecked")
        String scheduleId = (String) ((Map<String, Object>)
                objectMapper.readValue(createRes.getResponse().getContentAsString(), Map.class)
                        .get("data")).get("id");

        mockMvc.perform(get("/api/v1/schedules/" + scheduleId).cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(scheduleId))
                .andExpect(jsonPath("$.data.title").value("통합테스트 회의"));
    }

    @Test
    void updateSchedule() throws Exception {
        OffsetDateTime start = OffsetDateTime.now().plusDays(2).withNano(0);
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "원본 제목",
                "startTime", start.toString(),
                "endTime", start.plusHours(1).toString()
        ));
        MvcResult createRes = mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(jwtCookie).content(createBody))
                .andReturn();

        @SuppressWarnings("unchecked")
        String scheduleId = (String) ((Map<String, Object>)
                objectMapper.readValue(createRes.getResponse().getContentAsString(), Map.class)
                        .get("data")).get("id");

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "title", "수정된 제목",
                "startTime", start.toString(),
                "endTime", start.plusHours(2).toString()
        ));

        mockMvc.perform(patch("/api/v1/schedules/" + scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(jwtCookie).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정된 제목"));
    }

    @Test
    void deleteSchedule() throws Exception {
        OffsetDateTime start = OffsetDateTime.now().plusDays(3).withNano(0);
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "삭제할 일정",
                "startTime", start.toString(),
                "endTime", start.plusHours(1).toString()
        ));
        MvcResult createRes = mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(jwtCookie).content(createBody))
                .andReturn();

        @SuppressWarnings("unchecked")
        String scheduleId = (String) ((Map<String, Object>)
                objectMapper.readValue(createRes.getResponse().getContentAsString(), Map.class)
                        .get("data")).get("id");

        mockMvc.perform(delete("/api/v1/schedules/" + scheduleId).cookie(jwtCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/schedules/" + scheduleId).cookie(jwtCookie))
                .andExpect(status().isNotFound());
    }

    @Test
    void listSchedules_returnsOwnEvents() throws Exception {
        OffsetDateTime start = OffsetDateTime.now().plusDays(4).withNano(0);
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "목록 테스트 일정",
                "startTime", start.toString(),
                "endTime", start.plusHours(1).toString()
        ));
        mockMvc.perform(post("/api/v1/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(jwtCookie).content(createBody))
                .andExpect(status().isCreated());

        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to = from.plusMonths(1);

        mockMvc.perform(get("/api/v1/schedules")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .cookie(jwtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].title", hasItem("목록 테스트 일정")));
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/schedules"))
                .andExpect(status().isUnauthorized());
    }
}
