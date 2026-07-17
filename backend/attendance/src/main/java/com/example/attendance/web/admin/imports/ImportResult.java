package com.example.attendance.web.admin.imports;

import java.util.ArrayList;
import java.util.List;

public final class ImportResult {
    private int created = 0;
    private int updated = 0;
    private int skipped = 0;
    private final List<String> errors = new ArrayList<>();

    public void incCreated() { created++; }
    public void incUpdated() { updated++; }
    public void incSkipped() { skipped++; }
    public void error(int row, String msg) { errors.add("Row " + row + ": " + msg); }
    public void error(String msg) { errors.add(msg); }

    public int created() { return created; }
    public int updated() { return updated; }
    public int skipped() { return skipped; }
    public List<String> errors() { return errors; }

    public String summary(String unit) {
        StringBuilder b = new StringBuilder();
        b.append(created).append(" ").append(unit).append(" created");
        if (updated > 0) b.append(", ").append(updated).append(" updated");
        if (skipped > 0) b.append(", ").append(skipped).append(" skipped");
        if (!errors.isEmpty()) {
            b.append(". Errors: ");
            int show = Math.min(errors.size(), 5);
            for (int i = 0; i < show; i++) {
                if (i > 0) b.append("; ");
                b.append(errors.get(i));
            }
            if (errors.size() > show) b.append("; …+").append(errors.size() - show).append(" more");
        }
        return b.toString();
    }

    public boolean hasErrors() { return !errors.isEmpty(); }
}
