package com.faturaocr.infrastructure.export;

import com.faturaocr.application.export.InvoiceExporter;
import com.faturaocr.application.export.dto.InvoiceExportData;
import com.faturaocr.domain.invoice.valueobject.SourceType;
import com.faturaocr.application.export.ExportFormat;
import com.faturaocr.application.export.ExportFormat;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class CsvInvoiceExporter implements InvoiceExporter {

    private static final byte[] BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
    private static final char DELIMITER = ';';
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.CSV;
    }

    @Override
    public void export(Iterable<List<InvoiceExportData>> dataBatches, OutputStream outputStream) throws IOException {
        outputStream.write(BOM);

        try (OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                CSVWriter writer = new CSVWriter(streamWriter,
                        DELIMITER,
                        CSVWriter.DEFAULT_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END)) {

            // Write Header
            writer.writeNext(new String[] {
                    "Fatura No", "Fatura Tarihi", "Vade Tarihi", "Tedarikçi Adı",
                    "Tedarikçi VKN/TCKN", "Tedarikçi Vergi Dairesi", "Alıcı Adı",
                    "Ara Toplam", "KDV Tutarı", "Genel Toplam", "Para Birimi",
                    "Durum", "Kaynak", "Kategori", "Güven Skoru", "LLM Sağlayıcı",
                    "Oluşturan", "Oluşturma Tarihi", "Notlar",
                    "Kalem Açıklama", "Miktar", "Birim", "Birim Fiyat",
                    "KDV Oranı (%)", "Kalem KDV", "Kalem Toplam"
            });

            // Write Data
            for (List<InvoiceExportData> batch : dataBatches) {
                for (InvoiceExportData data : batch) {
                    writer.writeNext(new String[] {
                            safeString(data.getInvoiceNumber()),
                            data.getInvoiceDate() != null ? data.getInvoiceDate().format(DATE_FORMATTER) : "",
                            data.getDueDate() != null ? data.getDueDate().format(DATE_FORMATTER) : "",
                            safeString(data.getSupplierName()),
                            safeString(data.getSupplierTaxNumber()),
                            safeString(data.getSupplierTaxOffice()),
                            safeString(data.getBuyerName()),
                            safeNumber(data.getSubtotal()),
                            safeNumber(data.getTaxAmount()),
                            safeNumber(data.getTotalAmount()),
                            data.getCurrency() != null ? data.getCurrency().name() : "",
                            translateStatus(data.getStatus()),
                            translateSource(data.getSourceType()),
                            safeString(data.getCategoryName()),
                            data.getConfidenceScore() != null ? data.getConfidenceScore().toString() : "",
                            safeString(data.getLlmProvider()),
                            safeString(data.getCreatedBy()),
                            data.getCreatedAt() != null ? data.getCreatedAt().format(DATE_FORMATTER) : "",
                            safeString(data.getNotes()),
                            // Items
                            safeString(data.getItemDescription()),
                            safeNumber(data.getItemQuantity()),
                            safeString(data.getItemUnit()),
                            safeNumber(data.getItemUnitPrice()),
                            safeNumber(data.getItemTaxRate()),
                            safeNumber(data.getItemTaxAmount()),
                            safeNumber(data.getItemTotalAmount())
                    });
                }
                writer.flush(); // Flush after each batch
            }
        }
    }

    private String safeString(String val) {
        return val == null ? "" : val;
    }

    private String safeNumber(Number val) {
        return val == null ? "" : val.toString();
    }

    private String translateStatus(com.faturaocr.domain.invoice.valueobject.InvoiceStatus status) {
        if (status == null)
            return "";
        return switch (status) {
            case PENDING -> "Beklemede";
            case VERIFIED -> "Onaylandı";
            case REJECTED -> "Reddedildi";
            case PROCESSING -> "İşleniyor";
            case FAILED -> "Başarısız";
            default -> status.name();
        };
    }

    private String translateSource(SourceType source) {
        if (source == null)
            return "";
        return switch (source) {
            case LLM -> "LLM Çıkarım";
            case E_INVOICE -> "e-Fatura";
            case MANUAL -> "Manuel";
            default -> source.name();
        };
    }
}
