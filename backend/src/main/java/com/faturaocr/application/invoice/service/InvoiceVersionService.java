package com.faturaocr.application.invoice.service;

import com.faturaocr.domain.invoice.dto.InvoiceVersionDto;
import com.faturaocr.domain.invoice.entity.Invoice;
import com.faturaocr.domain.invoice.entity.InvoiceItem;
import com.faturaocr.domain.invoice.entity.InvoiceVersion.ChangeSource;

import java.util.List;
import java.util.UUID;

public interface InvoiceVersionService {

    void createSnapshot(Invoice invoice, List<InvoiceItem> items, ChangeSource source, String changeSummary);

    List<InvoiceVersionDto.Summary> getVersions(UUID invoiceId);

    InvoiceVersionDto.Detail getVersion(UUID invoiceId, Integer versionNumber);

    InvoiceVersionDto.VersionDiff compareVersions(UUID invoiceId, Integer versionFrom, Integer versionTo);

    Invoice revertToVersion(UUID invoiceId, Integer versionNumber);
}
