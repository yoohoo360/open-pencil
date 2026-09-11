package cn.jongwong.storage;

import java.time.LocalDate;

public final class StorageObjectPaths {

    private StorageObjectPaths() {
    }

    public static String quarterDirectory() {
        return quarterDirectory(LocalDate.now());
    }

    public static String quarterDirectory(LocalDate date) {
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        return date.getYear() + "Q" + quarter;
    }

    public static String sanitizeSegment(String value) {
        if (value == null || value.isBlank()) {
            return "anonymous";
        }
        String cleaned = value.trim().replace('\\', '-').replace('/', '-').replaceAll("\\s+", "-");
        return cleaned.isEmpty() ? "anonymous" : cleaned;
    }

    public static String directory(String kind, String username) {
        return kind + "/" + sanitizeSegment(username) + "/" + quarterDirectory();
    }
}
