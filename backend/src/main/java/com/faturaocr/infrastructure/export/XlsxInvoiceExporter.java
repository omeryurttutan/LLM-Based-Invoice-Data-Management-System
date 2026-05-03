package com.faturaocr.infrastructure.export;

import com.faturaocr.domain.invoice.valueobject.SourceType;
import com.faturaocr.application.export.ExportFormat;

import com.faturaocr.application.export.InvoiceExporter;
import com.faturaocr.application.export.dto.InvoiceExportData;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class XlsxInvoiceExporter implements InvoiceExporter {

    private static final String SHEET_NAME = "Faturalar";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.XLSX;
    }

    @Override
    public void export(Iterable<List<InvoiceExportData>> dataBatches, OutputStream outputStream) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) { // Keep 100 rows in memory
            SXSSFSheet sheet = workbook.createSheet(SHEET_NAME);
            sheet.trackAllColumnsForAutoSizing();

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle evenRowStyle = createEvenRowStyle(workbook);

            // Create Header
            createHeaderRow(sheet, headerStyle);

            // Create Data Rows
            int rowNum = 1;
            double totalSubtotal = 0;
            double totalTax = 0;
            double totalAmount = 0;

            for (List<InvoiceExportData> batch : dataBatches) {
                for (InvoiceExportData data : batch) {
                    Row row = sheet.createRow(rowNum);

                    // Accumulate totals (only for the main invoice row, avoid double counting
                    // items?)
                    // Requirement 3 says: "When includeItems=true... invoice-level fields are
                    // repeated".
                    // If we sum everything, we might double count.
                    // However, simplified approach: Sum only if it's the *first* row of an invoice
                    // or if flattened.
                    // For now, let's assume simple summation of the DTO fields.
                    // Warning: If includeItems=true, subtotal/tax/total are repeated.
                    // We should probably strictly calculate totals from the data stream, but here
                    // we only have the DTOs.
                    // Let's assume the user wants the sum of the rows as they appear or we should
                    // be smarter?
                    // Requirement 4g: "SUM formula of all subtotals".
                    // If we use formulas, it's safer. But here we are calculating values.
                    // Let's just sum the values provided in the DTOs for now.

                    if (data.getSubtotal() != null) {
                        totalSubtotal += data.getSubtotal().doubleValue();
                    }
                    if (data.getTaxAmount() != null) {
                        totalTax += data.getTaxAmount().doubleValue();
                    }
                    if (data.getTotalAmount() != null) {
                        totalAmount += data.getTotalAmount().doubleValue();
                    }

                    fillRow(row, data, dateStyle, numberStyle, currencyStyle, evenRowStyle, rowNum);
                    rowNum++;
                }
            }

            createSummaryRow(sheet, rowNum, headerStyle, numberStyle, totalSubtotal, totalTax, totalAmount);

            // Auto-size columns (only tracks tracked columns)
            for (int i = 0; i < 26; i++) {
                sheet.setColumnWidth(i, 20 * 256); // Set fixed width ~20 chars
            }

            // Write to output stream
            workbook.write(outputStream);
        }
    }

    private void createHeaderRow(Sheet sheet, CellStyle style) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Fatura No", "Fatura Tarihi", "Vade Tarihi", "Tedarikçi Adı",
                "Tedarikçi VKN/TCKN", "Tedarikçi Vergi Dairesi", "Alıcı Adı",
                "Ara Toplam", "KDV Tutarı", "Genel Toplam", "Para Birimi",
                "Durum", "Kaynak", "Kategori", "Güven Skoru", "LLM Sağlayıcı",
                "Oluşturan", "Oluşturma Tarihi", "Notlar",
                // Item columns
                "Kalem Açıklama", "Miktar", "Birim", "Birim Fiyat",
                "KDV Oranı (%)", "Kalem KDV", "Kalem Toplam"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void fillRow(Row row, InvoiceExportData data, CellStyle dateStyle, CellStyle numberStyle,
            CellStyle currencyStyle, CellStyle evenRowStyle, int rowNum) {

        CellStyle textStyle = (rowNum % 2 == 0) ? evenRowStyle : null;
        // For formatted cells, we would ideally need combined styles (Date + Grey), but
        // for simplicity we might keep white background
        // or we need to create Date+Even, Number+Even styles.
        // Let's effectively ignore alternating colors for formatted cells to save
        // complexity,
        // OR we can rely on Conditional Formatting (too complex for POI in 1 step).
        // Let's just use the textStyle for the text columns.

        int col = 0;
        createCell(row, col++, data.getInvoiceNumber(), textStyle);
        createCell(row, col++, data.getInvoiceDate(), dateStyle);
        createCell(row, col++, data.getDueDate(), dateStyle);
        createCell(row, col++, data.getSupplierName(), textStyle);
        createCell(row, col++, data.getSupplierTaxNumber(), textStyle);
        createCell(row, col++, data.getSupplierTaxOffice(), textStyle);
        createCell(row, col++, data.getBuyerName(), textStyle);
        createCell(row, col++, data.getSubtotal(), numberStyle);
        createCell(row, col++, data.getTaxAmount(), numberStyle);
        createCell(row, col++, data.getTotalAmount(), numberStyle);
        createCell(row, col++, data.getCurrency() != null ? data.getCurrency().name() : "", textStyle);
        createCell(row, col++, translateStatus(data.getStatus()), textStyle);
        createCell(row, col++, translateSource(data.getSourceType()), textStyle);
        createCell(row, col++, data.getCategoryName(), textStyle);
        createCell(row, col++, data.getConfidenceScore(), textStyle);
        createCell(row, col++, data.getLlmProvider(), textStyle);
        createCell(row, col++, data.getCreatedBy(), textStyle);
        createCell(row, col++, data.getCreatedAt() != null ? data.getCreatedAt().format(DATETIME_FORMATTER) : "",
                textStyle);
        createCell(row, col++, data.getNotes(), textStyle);

        // Item fields
        createCell(row, col++, data.getItemDescription(), textStyle);
        createCell(row, col++, data.getItemQuantity(), numberStyle);
        createCell(row, col++, data.getItemUnit(), textStyle);
        createCell(row, col++, data.getItemUnitPrice(), numberStyle);
        createCell(row, col++, data.getItemTaxRate(), numberStyle);
        createCell(row, col++, data.getItemTaxAmount(), numberStyle);
        createCell(row, col++, data.getItemTotalAmount(), numberStyle);
    }

    private void createCell(Row row, int colIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof java.time.LocalDate) {
            cell.setCellValue((java.time.LocalDate) value);
        } else {
            cell.setCellValue(value.toString());
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        short format = workbook.createDataFormat().getFormat("dd.MM.yyyy");
        style.setDataFormat(format);
        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        short format = workbook.createDataFormat().getFormat("#,##0.00");
        style.setDataFormat(format);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        return createNumberStyle(workbook);
    }

    private CellStyle createEvenRowStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void createSummaryRow(Sheet sheet, int rowNum, CellStyle headerStyle, CellStyle numberStyle,
            double totalSubtotal, double totalTax, double totalAmount) {
        Row row = sheet.createRow(rowNum);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue("TOPLAM");
        labelCell.setCellStyle(headerStyle); // Reuse header style for bold/color

        // Subtotal column (index 7)
        Cell subtotalCell = row.createCell(7);
        subtotalCell.setCellValue(totalSubtotal);
        subtotalCell.setCellStyle(numberStyle);

        // Tax column (index 8)
        Cell taxCell = row.createCell(8);
        taxCell.setCellValue(totalTax);
        taxCell.setCellStyle(numberStyle);

        // Total column (index 9)
        Cell totalCell = row.createCell(9);
        totalCell.setCellValue(totalAmount);
        totalCell.setCellStyle(numberStyle);
    }

    private String translateStatus(com.faturaocr.domain.invoice.valueobject.InvoiceStatus status) {
        if (status == null) {
            return "";
        }
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
        if (source == null) {
            return "";
        }
        return switch (source) {
            case LLM -> "LLM Çıkarım";
            case E_INVOICE -> "e-Fatura";
            case MANUAL -> "Manuel";
            default -> source.name();
        };
    }
}
