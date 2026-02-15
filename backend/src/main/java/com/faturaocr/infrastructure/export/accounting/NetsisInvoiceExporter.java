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
public class NetsisInvoiceExporter implements InvoiceExporter {

    private final AccountingExportConfig config;
    private final AccountingExportUtils utils;

    @Override
    public ExportFormat getFormat() {
        return ExportFormat.NETSIS;
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
        log.info("Starting Netsis export");

        XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
        try {
            XMLStreamWriter writer = outputFactory
                    .createXMLStreamWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("FATURA_LISTESI");

            for (List<InvoiceExportData> batch : dataBatches) {
                // Group by Invoice Number
                java.util.Map<String, List<InvoiceExportData>> groupedInvoices = batch.stream()
                        .collect(java.util.stream.Collectors.groupingBy(InvoiceExportData::getInvoiceNumber));

                for (java.util.Map.Entry<String, List<InvoiceExportData>> entry : groupedInvoices.entrySet()) {
                    List<InvoiceExportData> invoiceRows = entry.getValue();
                    if (invoiceRows.isEmpty())
                        continue;

                    InvoiceExportData header = invoiceRows.get(0);
                    writeInvoice(writer, header, invoiceRows);
                }
            }

            writer.writeEndElement(); // FATURA_LISTESI
            writer.writeEndDocument();
            writer.flush();
            writer.close();

        } catch (Exception e) {
            log.error("Error generating Netsis XML export", e);
            throw new RuntimeException("Netsis XML export failed", e);
        }
    }

    private void writeInvoice(XMLStreamWriter writer, InvoiceExportData header, List<InvoiceExportData> items)
            throws XMLStreamException {
        writer.writeStartElement("FATURA");

        // Header fields
        writeElement(writer, "FATURA_NO", header.getInvoiceNumber());
        writeElement(writer, "FATURA_TARIHI",
                utils.formatDate(header.getInvoiceDate(), config.getNetsis().getDateFormat()));
        writeElement(writer, "VADE_TARIHI", utils.formatDate(header.getDueDate(), config.getNetsis().getDateFormat()));
        writeElement(writer, "CARI_UNVAN", header.getSupplierName());
        writeElement(writer, "VERGI_NO", header.getSupplierTaxNumber());
        writeElement(writer, "ARA_TOPLAM", utils.formatNumber(header.getSubtotal(), true));
        writeElement(writer, "KDV_TOPLAM", utils.formatNumber(header.getTaxAmount(), true));
        writeElement(writer, "GENEL_TOPLAM", utils.formatNumber(header.getTotalAmount(), true));
        writeElement(writer, "DOVIZ_CINSI",
                utils.translateCurrency(header.getCurrency() != null ? header.getCurrency().name() : "TRY"));
        writeElement(writer, "ACIKLAMA", utils.sanitizeText(header.getNotes(), 100));

        // Items
        writer.writeStartElement("KALEMLER");
        for (InvoiceExportData row : items) {
            if (row.getItemDescription() != null) {
                writeTransaction(writer, row);
            }
        }
        writer.writeEndElement(); // KALEMLER

        writer.writeEndElement(); // FATURA
    }

    private void writeTransaction(XMLStreamWriter writer, InvoiceExportData row) throws XMLStreamException {
        writer.writeStartElement("KALEM");

        writeElement(writer, "STOK_ADI", utils.sanitizeText(row.getItemDescription(), 100));
        writeElement(writer, "MIKTAR", utils.formatNumber(row.getItemQuantity(), true));
        writeElement(writer, "BIRIM", "ADET");
        writeElement(writer, "BIRIM_FIYAT", utils.formatNumber(row.getItemUnitPrice(), true));
        writeElement(writer, "KDV_ORANI", utils.formatNumber(row.getItemTaxRate(), true));
        writeElement(writer, "KDV_TUTARI", utils.formatNumber(row.getItemTaxAmount(), true));
        writeElement(writer, "TUTAR", utils.formatNumber(row.getItemTotalAmount(), true));
        // Product code usage:
        // writeElement(writer, "STOK_KODU", ...);

        writer.writeEndElement(); // KALEM
    }

    private void writeElement(XMLStreamWriter writer, String name, String value) throws XMLStreamException {
        writer.writeStartElement(name);
        if (value != null) {
            writer.writeCharacters(value);
        }
        writer.writeEndElement();
    }
}
