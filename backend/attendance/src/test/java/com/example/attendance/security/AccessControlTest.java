package com.example.attendance.security;

import com.example.attendance.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccessControlTest extends AbstractIntegrationTest {
    @Test
    @WithMockUser(roles = "PROFESSOR")
    void professorIsForbiddenOnStudents() throws Exception {
        mockMvc.perform(get("/students")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    void professorIsForbiddenOnRegister() throws Exception {
        mockMvc.perform(get("/register")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    void professorIsForbiddenOnImport() throws Exception {
        mockMvc.perform(get("/import")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void staffIsAllowedOnStudents() throws Exception {
        mockMvc.perform(get("/students")).andExpect(status().isOk());
    }

    @Test
    void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/students"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
