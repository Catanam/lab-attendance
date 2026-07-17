package com.example.attendance.web.admin.imports;

import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CsvSheetReader {
    private CsvSheetReader() {}

    static List<SheetRow> read(MultipartFile file) throws IOException {
        List<SheetRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) return rows;
            List<String> headers = parseLine(header);
            for (int i = 0; i < headers.size(); i++) {
                headers.set(i, SheetReader.normalize(headers.get(i)));
            }
            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) continue;
                List<String> cols = parseLine(line);
                Map<String, String> cells = new HashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String v = i < cols.size() ? cols.get(i) : "";
                    cells.put(headers.get(i), v);
                }
                rows.add(new SheetRow(rowNum, cells));
            }
        }
        return rows;
    }

    private static List<String> parseLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i += 2;
                        continue;
                    }
                    inQuotes = false;
                    i++;
                    continue;
                }
                cur.append(c);
                i++;
            } else {
                if (c == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                    i++;
                } else if (c == '"' && cur.length() == 0) {
                    inQuotes = true;
                    i++;
                } else {
                    cur.append(c);
                    i++;
                }
            }
        }
        out.add(cur.toString());
        return out;
    }
}
