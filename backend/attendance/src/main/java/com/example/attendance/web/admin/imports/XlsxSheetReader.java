package com.example.attendance.web.admin.imports;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class XlsxSheetReader {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private XlsxSheetReader() {}

    static List<SheetRow> read(MultipartFile file) throws IOException {
        List<SheetRow> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> it = sheet.iterator();
            if (!it.hasNext()) return rows;

            Row headerRow = it.next();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(SheetReader.normalize(cellToString(cell)));
            }

            int rowNum = 1;
            while (it.hasNext()) {
                Row row = it.next();
                rowNum++;
                if (isRowBlank(row, headers.size())) continue;
                Map<String, String> cells = new HashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    cells.put(headers.get(i), cell == null ? "" : cellToString(cell));
                }
                rows.add(new SheetRow(rowNum, cells));
            }
        }
        return rows;
    }

    private static boolean isRowBlank(Row row, int cols) {
        for (int i = 0; i < cols; i++) {
            Cell c = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (c != null && !cellToString(c).isBlank()) return false;
        }
        return true;
    }

    private static String cellToString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> formatNumeric(cell);
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> formulaResult(cell);
            case BLANK, ERROR, _NONE -> "";
        };
    }

    private static String formatNumeric(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            LocalDateTime ldt = cell.getDateCellValue().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();

            if (ldt.toLocalDate().getYear() < 1901) {
                return ldt.format(TIME_FMT);
            }
            return ldt.format(DATE_FMT);
        }
        return NumberToTextConverter.toText(cell.getNumericCellValue());
    }

    private static String formulaResult(Cell cell) {
        try {
            return switch (cell.getCachedFormulaResultType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> formatNumeric(cell);
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                default -> "";
            };
        } catch (Exception e) {
            return "";
        }
    }
}
