package com.faturaocr.infrastructure.adapter.web;

import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;

public class CustomMultipartFile implements MultipartFile {

    private final byte[] content;
    private final String name;
    private final String originalFilename;
    private final String contentType;

    public CustomMultipartFile(byte[] content, String name, String originalFilename, String contentType) {
        this.content = Objects.requireNonNull(content, "Content must not be null");
        this.name = Objects.requireNonNull(name, "Name must not be null");
        this.originalFilename = originalFilename;
        this.contentType = contentType;
    }

    @Override
    @NonNull
    @SuppressWarnings("null")
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    @NonNull
    @SuppressWarnings("null")
    public byte[] getBytes() throws IOException {
        return content;
    }

    @Override
    @NonNull
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(@NonNull File dest) throws IOException, IllegalStateException {
        Files.write(dest.toPath(), content);
    }
}
