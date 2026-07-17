package com.example.attendance.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    public record Resp(int status, String body) {}

    protected Resp postBytes(String path, Map<String, String> headers, byte[] body) {
        try {
            MockHttpServletRequestBuilder req = post(path).content(body == null ? new byte[0] : body);
            headers.forEach((k, v) -> req.header(k, v));
            var res = mockMvc.perform(req).andReturn().getResponse();
            return new Resp(res.getStatus(), res.getContentAsString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
