package com.faturaocr.application.category;

import com.faturaocr.application.category.dto.CategoryResponse;
import com.faturaocr.application.category.dto.CreateCategoryCommand;
import com.faturaocr.application.common.exception.BusinessException;
import com.faturaocr.domain.category.entity.Category;
import com.faturaocr.domain.category.port.CategoryRepository;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private CategoryService categoryService;

    private MockedStatic<CompanyContextHolder> companyContextMock;
    private final UUID COMPANY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        companyContextMock = mockStatic(CompanyContextHolder.class);
        companyContextMock.when(CompanyContextHolder::getCompanyId).thenReturn(COMPANY_ID);
    }

    @AfterEach
    void tearDown() {
        companyContextMock.close();
    }

    @Test
    void createCategory_ShouldSaveAndReturnResponse() {
        CreateCategoryCommand command = new CreateCategoryCommand();
        command.setName("Test Category");
        command.setColor("#FF0000");

        when(categoryRepository.existsByNameAndCompanyId("Test Category", COMPANY_ID)).thenReturn(false);

        Category savedCategory = new Category();
        savedCategory.setId(UUID.randomUUID());
        savedCategory.setName("Test Category");
        savedCategory.setCompanyId(COMPANY_ID);
        savedCategory.setColor("#FF0000");

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse response = categoryService.createCategory(command);

        assertNotNull(response);
        assertEquals(savedCategory.getId(), response.getId());
        assertEquals("Test Category", response.getName());
    }

    @Test
    void createCategory_DuplicateName_ShouldThrowError() {
        CreateCategoryCommand command = new CreateCategoryCommand();
        command.setName("Duplicate");

        when(categoryRepository.existsByNameAndCompanyId("Duplicate", COMPANY_ID)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> categoryService.createCategory(command));
        assertEquals("CATEGORY_NAME_EXISTS", ex.getErrorCode());
    }

    @Test
    void deleteCategory_WithNoInvoices_ShouldSucceed() {
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);
        category.setCompanyId(COMPANY_ID);

        when(categoryRepository.findByIdAndCompanyId(categoryId, COMPANY_ID))
                .thenReturn(java.util.Optional.of(category));
        when(invoiceRepository.countByCategoryId(categoryId)).thenReturn(0L);

        assertDoesNotThrow(() -> categoryService.deleteCategory(categoryId));
        verify(categoryRepository).softDelete(categoryId);
    }

    @Test
    void deleteCategory_WithInvoices_ShouldThrowError() {
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);
        category.setCompanyId(COMPANY_ID);

        when(categoryRepository.findByIdAndCompanyId(categoryId, COMPANY_ID))
                .thenReturn(java.util.Optional.of(category));
        when(invoiceRepository.countByCategoryId(categoryId)).thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> categoryService.deleteCategory(categoryId));
        assertEquals("CATEGORY_HAS_INVOICES", ex.getErrorCode());
    }
}
