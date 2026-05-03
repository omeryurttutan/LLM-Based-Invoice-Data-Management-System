package com.faturaocr.application.category;

import com.faturaocr.application.category.dto.CategoryResponse;
import com.faturaocr.application.category.dto.CreateCategoryCommand;
import com.faturaocr.application.category.CategoryService;
import com.faturaocr.domain.category.entity.Category;
import com.faturaocr.domain.category.port.CategoryRepository;
import com.faturaocr.domain.invoice.port.InvoiceRepository;
import com.faturaocr.infrastructure.security.CompanyContextHolder;
import com.faturaocr.testutil.TestDataBuilder;
import com.faturaocr.testutil.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private CategoryService categoryService;

    private MockedStatic<CompanyContextHolder> companyContextHolderMock;

    @BeforeEach
    void setUp() {
        companyContextHolderMock = Mockito.mockStatic(CompanyContextHolder.class);
        companyContextHolderMock.when(CompanyContextHolder::getCompanyId).thenReturn(TestFixtures.COMPANY_ID);
    }

    @AfterEach
    void tearDown() {
        companyContextHolderMock.close();
    }

    @Test
    @DisplayName("Should create category successfully")
    void shouldCreateCategorySuccessfully() {
        // Given
        CreateCategoryCommand command = new CreateCategoryCommand();
        command.setName("Utilities");
        command.setDescription("Bills");
        command.setColor("blue");
        command.setIcon("icon");
        command.setParentId(null);

        when(categoryRepository.existsByNameAndCompanyId(eq("Utilities"), eq(TestFixtures.COMPANY_ID)))
                .thenReturn(false);

        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> {
            Category c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        // When
        CategoryResponse response = categoryService.createCategory(command);

        // Then
        assertThat(response.getName()).isEqualTo("Utilities");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should prevent duplicate category name")
    void shouldPreventDuplicateCategoryName() {
        // Given
        CreateCategoryCommand command = new CreateCategoryCommand();
        command.setName("Utilities");
        command.setDescription("Bills");
        command.setColor("blue");
        command.setIcon("icon");
        command.setParentId(null);

        when(categoryRepository.existsByNameAndCompanyId(eq("Utilities"), eq(TestFixtures.COMPANY_ID)))
                .thenReturn(true);

        // When
        Throwable thrown = catchThrowable(() -> categoryService.createCategory(command));

        // Then
        assertThat(thrown).isInstanceOf(com.faturaocr.application.common.exception.BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should delete category if empty")
    void shouldDeleteCategoryIfEmpty() {
        // Given
        UUID id = UUID.randomUUID();
        Category category = new Category();
        category.setId(id);

        when(categoryRepository.findByIdAndCompanyId(id, TestFixtures.COMPANY_ID))
                .thenReturn(Optional.of(category));
        when(invoiceRepository.countByCategoryId(id)).thenReturn(0L);

        // When
        categoryService.deleteCategory(id);

        // Then
        verify(categoryRepository).softDelete(id);
    }

    @Test
    @DisplayName("Should prevent delete if category has invoices")
    void shouldPreventDeleteIfCategoryHasInvoices() {
        // Given
        UUID id = UUID.randomUUID();
        Category category = new Category();
        category.setId(id);

        when(categoryRepository.findByIdAndCompanyId(id, TestFixtures.COMPANY_ID))
                .thenReturn(Optional.of(category));
        when(invoiceRepository.countByCategoryId(id)).thenReturn(5L);

        // When
        Throwable thrown = catchThrowable(() -> categoryService.deleteCategory(id));

        // Then
        assertThat(thrown).isInstanceOf(com.faturaocr.application.common.exception.BusinessException.class)
                .hasMessageContaining("Cannot delete category");
    }
}
