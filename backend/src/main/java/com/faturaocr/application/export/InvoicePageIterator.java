package com.faturaocr.application.export;

import com.faturaocr.application.export.dto.InvoiceExportData;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceJpaEntity;
import com.faturaocr.infrastructure.persistence.invoice.InvoiceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.faturaocr.infrastructure.persistence.category.CategoryJpaRepository;
import com.faturaocr.infrastructure.persistence.user.UserJpaRepository;
import com.faturaocr.infrastructure.persistence.user.UserJpaEntity;
import com.faturaocr.infrastructure.persistence.category.CategoryJpaEntity;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class InvoicePageIterator implements Iterator<List<InvoiceExportData>> {

    private final InvoiceJpaRepository repository;
    private final InvoiceMapper mapper;
    private final Specification<InvoiceJpaEntity> spec;
    private final boolean includeItems;
    private final UserJpaRepository userRepository;
    private final CategoryJpaRepository categoryRepository;

    private int currentPage = 0;
    private static final int PAGE_SIZE = 500;
    private boolean hasNext = true;

    public InvoicePageIterator(InvoiceJpaRepository repository, InvoiceMapper mapper,
            Specification<InvoiceJpaEntity> spec, boolean includeItems,
            UserJpaRepository userRepository, CategoryJpaRepository categoryRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.spec = spec;
        this.includeItems = includeItems;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public List<InvoiceExportData> next() {
        if (!hasNext) {
            throw new NoSuchElementException();
        }

        PageRequest pageRequest = PageRequest.of(currentPage, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "invoiceDate"));
        Page<InvoiceJpaEntity> page = repository.findAll(spec, pageRequest);

        List<InvoiceJpaEntity> content = page.getContent();

        // Collect IDs for batch fetching
        Set<UUID> userIds = content.stream()
                .map(InvoiceJpaEntity::getCreatedByUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Set<UUID> categoryIds = content.stream()
                .map(InvoiceJpaEntity::getCategoryId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // Fetch dictionaries
        Map<UUID, String> userNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, UserJpaEntity::getFullName));

        Map<UUID, String> categoryNames = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(CategoryJpaEntity::getId, CategoryJpaEntity::getName));

        List<InvoiceExportData> exportDataList = new ArrayList<>();
        for (InvoiceJpaEntity entity : content) {
            Invoice invoice = mapper.toDomain(entity);
            String categoryName = entity.getCategoryId() != null
                    ? categoryNames.getOrDefault(entity.getCategoryId(), "")
                    : "";
            String createdByName = entity.getCreatedByUserId() != null
                    ? userNames.getOrDefault(entity.getCreatedByUserId(), "")
                    : "";

            InvoiceExportData exportData = InvoiceExportData.from(invoice, "", categoryName, createdByName);

            if (includeItems && !invoice.getItems().isEmpty()) {
                for (var item : invoice.getItems()) {
                    exportDataList.add(exportData.withItem(item));
                }
            } else {
                exportDataList.add(exportData);
            }
        }

        currentPage++;
        if (!page.hasNext()) {
            hasNext = false;
        }

        return exportDataList;
    }
}
