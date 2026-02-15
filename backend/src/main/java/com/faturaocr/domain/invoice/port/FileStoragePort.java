package com.faturaocr.domain.invoice.port;

import java.io.InputStream;

public interface FileStoragePort {
    /**
     * Saves a file to the storage.
     * 
     * @param inputStream File content stream
     * @param companyId   Company ID for isolation
     * @param fileName    Original file name
     * @return The full path where the file is stored
     */
    String saveFile(InputStream inputStream, String companyId, String fileName);

    /**
     * Reads a file from storage.
     * 
     * @param filePath Full path to the file
     * @return File content stream
     */
    InputStream readFile(String filePath);

    /**
     * Deletes a file from storage.
     * 
     * @param filePath Full path to the file
     */
    void deleteFile(String filePath);
}
