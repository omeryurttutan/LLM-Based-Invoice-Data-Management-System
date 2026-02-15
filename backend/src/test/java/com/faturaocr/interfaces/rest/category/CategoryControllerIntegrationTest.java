package com.faturaocr.interfaces.rest.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faturaocr.application.category.CategoryService;
import com.faturaocr.application.category.dto.CategoryResponse;
import com.faturaocr.interfaces.rest.category.dto.CreateCategoryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @WithMockUser(authorities = "ADMIN")
    void createCategory_ShouldReturnOk_WhenValidRequest() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Test Category");
        request.setDescription("Description");
        request.setColor("#FFFFFF");

        CategoryResponse response = new CategoryResponse();
        response.setId(UUID.randomUUID());
        response.setName("Test Category");

        when(categoryService.createCategory(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
