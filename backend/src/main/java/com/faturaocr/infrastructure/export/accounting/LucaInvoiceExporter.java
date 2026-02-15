package com.faturaocr.infrastructure.export.accounting;

import com.faturaocr.application.export.ExportFormat;
import com.faturaocr.application.export.dto.InvoiceExportData;
import com.faturaocr.application.export.InvoiceExporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LucaInvoiceExporter implements InvoiceExporter {

    private final AccountingExportConfig config;
    private final AccountingExportUtils utils;

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.LUCA;
    }

    @Override
    public String getContentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public String getFileExtension() {
        return "xlsx";
    }

    @Override
    public void export(Iterable<List<InvoiceExportData>> dataBatches, OutputStream outputStream) {
        log.info("Starting Luca export");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Faturalar");
            createHeader(sheet);

            int rowNum = 1;
            for (List<InvoiceExportData> batch : dataBatches) {
                for (InvoiceExportData row : batch) {
                    createRow(sheet.createRow(rowNum++), row);
                }
            }

            // Autosize columns
            for (int i = 0; i < 16; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
        } catch (IOException e) {
            log.error("Error generating Luca export", e);
            throw new RuntimeException("Luca export failed", e);
        }
    }

    private void createHeader(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Fatura No", "Fatura Tarihi", "Vade Tarihi", "Fatura Tipi",
                "Cari Hesap", "Vergi No", "Vergi Dairesi", "Kalem Açıklaması",
                "Miktar", "Birim", "Birim Fiyat", "KDV Oranı (%)",
                "KDV Tutarı", "Satır Toplam", "Para Birimi", "Açıklama"
        };

        CellStyle headerStyle = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        // Luca's specific light blue color if we wanted to be exact, but simple bold is
        // fine
        // headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        // headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void createRow(Row row, InvoiceExportData data) {
        // A: Fatura No
        row.createCell(0).setCellValue(data.getInvoiceNumber());

        // B: Fatura Tarihi
        row.createCell(1).setCellValue(utils.formatDate(data.getInvoiceDate(), config.getLuca().getDateFormat()));

        // C: Vade Tarihi
        row.createCell(2).setCellValue(utils.formatDate(data.getDueDate(), config.getLuca().getDateFormat()));

        // D: Fatura Tipi
        row.createCell(3).setCellValue("Alış");

        // E: Cari Hesap
        row.createCell(4).setCellValue(data.getSupplierName());

        // F: Vergi No
        row.createCell(5).setCellValue(data.getSupplierTaxNumber());

        // G: Vergi Dairesi
        row.createCell(6).setCellValue(""); // Not currently captured

        if (data.getItemDescription() != null) {
            // H: Kalem Açıklaması
            row.createCell(7).setCellValue(data.getItemDescription());

            // I: Miktar
            row.createCell(8).setCellValue(utils.formatNumber(data.getItemQuantity(), true));

            // J: Birim
            row.createCell(9).setCellValue("ADET");

            // K: Birim Fiyat
            row.createCell(10).setCellValue(utils.formatNumber(data.getItemUnitPrice(), true));

            // L: KDV Oranı
            row.createCell(11).setCellValue(utils.formatNumber(data.getItemTaxRate(), true));

            // M: KDV Tutarı
            row.createCell(12).setCellValue(utils.formatNumber(data.getItemTaxAmount(), true));

            // N: Satır Toplam
            row.createCell(13).setCellValue(utils.formatNumber(data.getItemTotalAmount(), true));
        }

        // O: Para Birimi
        row.createCell(14)
                .setCellValue(utils.translateCurrency(data.getCurrency() != null ? data.getCurrency().name() : "TL"));

        // P: Açıklama
        row.createCell(15).setCellValue(data.getNotes());
    }
}
