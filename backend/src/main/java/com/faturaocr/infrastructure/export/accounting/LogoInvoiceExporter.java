package com.faturaocr.infrastructure.export.accounting;

import com.faturaocr.application.export.ExportFormat;
import com.faturaocr.application.export.dto.InvoiceExportData;
import com.faturaocr.application.export.InvoiceExporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogoInvoiceExporter implements InvoiceExporter {

    private final AccountingExportConfig config;
    private final AccountingExportUtils utils;

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.LOGO;
    }

    @Override
    public String getContentType() {
        return "application/xml";
    }

    @Override
    public String getFileExtension() {
        return "xml";
    }

    @Override
    public void export(Iterable<List<InvoiceExportData>> dataBatches, OutputStream outputStream) {
        log.info("Starting Logo export");

        XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        try {
            XMLStreamWriter writer = outputFactory
                    .createXMLStreamWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("INVOICES");

            for (List<InvoiceExportData> batch : dataBatches) {
                // Group by Invoice Number to reconstruct hierarchy for this batch
                // Since invoices are paginated, an invoice will not be split across batches
                java.util.Map<String, List<InvoiceExportData>> groupedInvoices = batch.stream()
                        .collect(java.util.stream.Collectors.groupingBy(InvoiceExportData::getInvoiceNumber));

                for (java.util.Map.Entry<String, List<InvoiceExportData>> entry : groupedInvoices.entrySet()) {
                    List<InvoiceExportData> invoiceRows = entry.getValue();
                    if (invoiceRows.isEmpty()) {
                        continue;
                    }

                    // Use the first row for header data
                    InvoiceExportData header = invoiceRows.get(0);
                    writeInvoice(writer, header, invoiceRows);
                }
            }

            writer.writeEndElement(); // INVOICES
            writer.writeEndDocument();
            writer.flush();
            writer.close();

        } catch (Exception e) {
            log.error("Error generating Logo XML export", e);
            throw new RuntimeException("Logo XML export failed", e);
        }
    }

    private void writeInvoice(XMLStreamWriter writer, InvoiceExportData header, List<InvoiceExportData> items)
            throws XMLStreamException {
        writer.writeStartElement("INVOICE");

        // Header fields
        writeElement(writer, "NUMBER", header.getInvoiceNumber());
        writeElement(writer, "DATE", utils.formatDate(header.getInvoiceDate(), config.getLogo().getDateFormat()));
        writeElement(writer, "PAYMENT_DATE", utils.formatDate(header.getDueDate(), config.getLogo().getDateFormat()));
        writeElement(writer, "ARP_CODE", header.getSupplierTaxNumber());
        writeElement(writer, "TAX_NR", header.getSupplierTaxNumber());
        writeElement(writer, "TOTAL", utils.formatNumber(header.getSubtotal(), true));
        writeElement(writer, "TOTAL_VAT", utils.formatNumber(header.getTaxAmount(), true));
        writeElement(writer, "GROSS_TOTAL", utils.formatNumber(header.getTotalAmount(), true));
        // Fix currency translation
        writeElement(writer, "CURR_TRANS",
                utils.translateCurrencyForLogo(header.getCurrency() != null ? header.getCurrency().name() : "TRY"));
        writeElement(writer, "NOTES1", utils.sanitizeText(header.getNotes(), 50));

        // Items
        writer.writeStartElement("TRANSACTIONS");
        for (InvoiceExportData row : items) {
            // Only write transaction if it has item data (check if itemDescription is
            // present)
            if (row.getItemDescription() != null) {
                writeTransaction(writer, row);
            }
        }
        writer.writeEndElement(); // TRANSACTIONS

        writer.writeEndElement(); // INVOICE
    }

    private void writeTransaction(XMLStreamWriter writer, InvoiceExportData row) throws XMLStreamException {
        writer.writeStartElement("TRANSACTION");

        writeElement(writer, "DESCRIPTION", utils.sanitizeText(row.getItemDescription(), 100));
        writeElement(writer, "QUANTITY", utils.formatNumber(row.getItemQuantity(), true));
        writeElement(writer, "PRICE", utils.formatNumber(row.getItemUnitPrice(), true));
        writeElement(writer, "VAT_RATE", utils.formatNumber(row.getItemTaxRate(), true));
        writeElement(writer, "VAT_AMOUNT", utils.formatNumber(row.getItemTaxAmount(), true));
        writeElement(writer, "TOTAL", utils.formatNumber(row.getItemTotalAmount(), true));
        writeElement(writer, "UNIT_CODE", "ADET");

        writer.writeEndElement(); // TRANSACTION
    }

    private void writeElement(XMLStreamWriter writer, String name, String value) throws XMLStreamException {
        writer.writeStartElement(name);
        if (value != null) {
            writer.writeCharacters(value);
        }
        writer.writeEndElement();
    }
}
