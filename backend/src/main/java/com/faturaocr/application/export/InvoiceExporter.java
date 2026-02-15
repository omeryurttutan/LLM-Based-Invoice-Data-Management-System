package com.faturaocr.application.export;

import com.faturaocr.application.export.dto.InvoiceExportData;
import com.faturaocr.application.export.ExportFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface InvoiceExporter {
    /**
     * Exports data provided by the iterable batches to the output stream.
     * The exporter is responsible for iterating through the batches and writing
     * them.
     * 
     * @param dataBatches  Iterable returning batches of invoice data
     * @param outputStream Stream to write to
     * @throws IOException If writing fails
     */
    void export(Iterable<List<InvoiceExportData>> dataBatches, OutputStream outputStream) throws IOException;

    /**
     * Gets the format supported by this exporter.
     */
    ExportFormat getFormat();

    /**
     * Gets the content type for HTTP response.
     */
    default String getContentType() {
        return getFormat().getContentType();
    }

    /**
     * Gets the file extension.
     */
    default String getFileExtension() {
        return getFormat().getExtension();
    }
}
