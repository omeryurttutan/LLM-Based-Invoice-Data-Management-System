package com.faturaocr.infrastructure.export.accounting;

import com.faturaocr.application.export.ExportFormat;
import com.faturaocr.application.export.dto.InvoiceExportData;
import com.faturaocr.application.export.InvoiceExporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MikroInvoiceExporter implements InvoiceExporter {

    private final AccountingExportConfig config;
    private final AccountingExportUtils utils;
    private static final Charset WINDOWS_1254 = Charset.forName("windows-1254");

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.MIKRO;
    }

    @Override
    public String getContentType() {
        return "text/plain; charset=windows-1254";
    }

    @Override
    public String getFileExtension() {
        return "txt";
    }

    @Override
    public void export(Iterable<List<InvoiceExportData>> dataBatches, OutputStream outputStream) {
        log.info("Starting Mikro export");

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, WINDOWS_1254))) {
            String delimiter = config.getMikro().getDelimiter();

            for (List<InvoiceExportData> batch : dataBatches) {
                for (InvoiceExportData row : batch) {
                    writeLine(writer, row, delimiter);
                }
            }
        } catch (Exception e) {
            log.error("Error generating Mikro export", e);
            throw new RuntimeException("Mikro export failed", e);
        }
    }

    private void writeLine(PrintWriter writer, InvoiceExportData row, String delimiter) {
        StringBuilder sb = new StringBuilder();

        // 1. Fatura Tipi (1 = Alış)
        sb.append("1").append(delimiter);

        // 2. Fatura No
        sb.append(utils.sanitizeText(row.getInvoiceNumber(), 20)).append(delimiter);

        // 3. Fatura Tarihi
        sb.append(utils.formatDate(row.getInvoiceDate(), config.getMikro().getDateFormat())).append(delimiter);

        // 4. Vade Tarihi
        sb.append(utils.formatDate(row.getDueDate(), config.getMikro().getDateFormat())).append(delimiter);

        // 5. Cari Hesap Kodu (using VKN)
        sb.append(utils.sanitizeText(row.getSupplierTaxNumber(), 20)).append(delimiter);

        // 6. Cari Hesap Adı
        sb.append(utils.sanitizeText(row.getSupplierName(), 50)).append(delimiter);

        // Items
        if (row.getItemDescription() != null) {
            // 7. Stok Kodu (Description as proxy if code missing)
            sb.append(utils.sanitizeText(row.getItemDescription(), 30)).append(delimiter);

            // 8. Stok Adı
            sb.append(utils.sanitizeText(row.getItemDescription(), 50)).append(delimiter);

            // 9. Miktar
            sb.append(utils.formatNumber(row.getItemQuantity(), true)).append(delimiter);

            // 10. Birim
            sb.append("ADET").append(delimiter);

            // 11. Birim Fiyat
            sb.append(utils.formatNumber(row.getItemUnitPrice(), true)).append(delimiter);

            // 12. KDV Oranı
            sb.append(utils.formatNumber(row.getItemTaxRate(), true)).append(delimiter);

            // 13. KDV Tutarı
            sb.append(utils.formatNumber(row.getItemTaxAmount(), true)).append(delimiter);

            // 14. Satır Toplam
            sb.append(utils.formatNumber(row.getItemTotalAmount(), true)).append(delimiter);
        } else {
            // Fill empty columns for item fields
            sb.append(delimiter.repeat(8));
        }

        // 15. Para Birimi
        sb.append(utils.translateCurrency(row.getCurrency() != null ? row.getCurrency().name() : "TL"))
                .append(delimiter);

        // 16. Açıklama
        sb.append(utils.sanitizeText(row.getNotes(), 50));

        writer.println(sb.toString());
    }
}
