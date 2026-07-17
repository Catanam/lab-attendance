package com.example.attendance.web.admin.imports;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public final class SheetReader {
    private SheetReader() {}

    public static List<SheetRow> read(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            throw new IOException("Missing filename");
        }
        String lower = name.toLowerCase();
        if (lower.endsWith(".xlsx")) return XlsxSheetReader.read(file);
        if (lower.endsWith(".csv"))  return CsvSheetReader.read(file);
        throw new IOException("Unsupported file extension. Use .csv or .xlsx (got: " + name + ").");
    }

    static String normalize(String s) {
        if (s == null) return "";
        return s.replace("﻿", "").trim().toLowerCase();
    }
}
