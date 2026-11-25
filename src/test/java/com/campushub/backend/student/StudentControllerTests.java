package com.campushub.backend.student;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository repository;

    @Test
    void returnsFieldLevelConflictForCaseVariantStudentIdAndEmail() throws Exception {
        createStudent("STU-8001", "one8001@example.com");

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(
                                "stu-8001", "Duplicate ID", "other8001@example.com"
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Student ID already exists"))
                .andExpect(jsonPath("$.errors.studentId")
                        .value("Student ID already exists"));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(
                                "STU-8002", "Duplicate Email", "ONE8001@example.com"
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"))
                .andExpect(jsonPath("$.errors.email")
                        .value("Email already exists"));
    }

    @Test
    void deleteReturnsNoContentAndKeepsStudentAvailableForHistory() throws Exception {
        String body = mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(
                                "STU-8003", "Delete Me", "delete8003@example.com"
                        )))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        StudentResponse created = objectMapper.readValue(body, StudentResponse.class);

        mockMvc.perform(delete("/api/students/{id}", created.id()))
                .andExpect(status().isNoContent());

        assertNotNull(repository.findById(created.id()).orElseThrow().getDeletedAt());

        mockMvc.perform(get("/api/students/{id}", created.id()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/students")
                        .param("includeDeleted", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].studentId").value("STU-8003"));
    }

    private void createStudent(String studentId, String email) throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(
                                studentId, "Existing Student", email
                        )))
                .andExpect(status().isCreated());
    }

    private String requestJson(String studentId, String fullName, String email)
            throws Exception {

        return objectMapper.writeValueAsString(
                new StudentRequest(
                        studentId,
                        fullName,
                        email,
                        null,
                        "Computer Science",
                        "BSc Computer Science",
                        LocalDate.of(2024, 9, 1),
                        "ACTIVE"
                )
        );
    }
}
