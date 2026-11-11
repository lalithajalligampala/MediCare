                 package com.medicare.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.medicare.model.HealthRecord;

public class HealthRecordStorage {

    private static final Path FILE_PATH = Path.of("data", "health_records.txt");

    // Save a health record after applying the same basic checks as the UI.
    public static synchronized void saveRecord(HealthRecord record) throws IOException {
        if (record == null || record.getDate() == null) {
            throw new IllegalArgumentException("A record date is required.");
        }
        validateRecord(record);
        Files.createDirectories(FILE_PATH.getParent());
        String data = record.getDate() + "|" + record.getWeight() + "|"
                + record.getSystolic() + "|" + record.getDiastolic()
                + System.lineSeparator();
        Files.writeString(FILE_PATH, data,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND);
    }

    public static synchronized List<HealthRecord> loadRecords() throws IOException {
        List<HealthRecord> records = new ArrayList<>();
        if (!Files.exists(FILE_PATH)) return records;

        for (String line : Files.readAllLines(FILE_PATH)) {
            try {
                String[] parts = line.split("\\|", -1);
                if (parts.length != 4) continue;
                HealthRecord record = new HealthRecord(
                        LocalDate.parse(parts[0]),
                        Double.parseDouble(parts[1]),
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3]));
                // Load legacy entries even if their values fail the new checks,
                // so the user can see and delete previously saved mistakes.
                records.add(record);
            } catch (Exception ex) {
                // Keep the application running if an old/corrupt line exists.
                System.out.println("Invalid health record skipped: " + line);
            }
        }
        return records;
    }

    // Delete exactly one matching row, even if multiple identical entries exist.
    public static synchronized void deleteRecord(HealthRecord record) throws IOException {
        if (record == null) throw new IllegalArgumentException("Select a record to delete.");
        List<HealthRecord> records = loadRecords();
        boolean removed = false;
        for (int i = 0; i < records.size(); i++) {
            if (sameRecord(records.get(i), record)) {
                records.remove(i);
                removed = true;
                break; }
        }
        if (!removed) throw new IOException("The selected health record was not found.");
        writeAll(records);
    }

    // Clear the entire health-record history.
    public static synchronized void deleteAllRecords() throws IOException {
        Files.createDirectories(FILE_PATH.getParent());
        Files.writeString(FILE_PATH, "");
    }

    private static boolean sameRecord(HealthRecord a, HealthRecord b) {
        return a.getDate().equals(b.getDate())
                && Double.compare(a.getWeight(), b.getWeight()) == 0
                && a.getSystolic() == b.getSystolic()
                && a.getDiastolic() == b.getDiastolic();
    }

    private static void writeAll(List<HealthRecord> records) throws IOException {
        Files.createDirectories(FILE_PATH.getParent());
        List<String> lines = new ArrayList<>();
        for (HealthRecord r : records) {
            lines.add(r.getDate() + "|" + r.getWeight() + "|"
                    + r.getSystolic() + "|" + r.getDiastolic());
        }
        Files.write(FILE_PATH, lines);
    }

    private static void validateRecord(HealthRecord record) {
        if (record.getDate() == null) throw new IllegalArgumentException("A record date is required.");
        if (!Double.isFinite(record.getWeight()) || record.getWeight() < 1 || record.getWeight() > 500) {
            throw new IllegalArgumentException("Weight must be between 1 and 500 kg.");
        }
        if (record.getSystolic() < 50 || record.getSystolic() > 300) {
            throw new IllegalArgumentException("Systolic BP must be between 50 and 300 mmHg.");
        }
        if (record.getDiastolic() < 30 || record.getDiastolic() > 200) {
            throw new IllegalArgumentException("Diastolic BP must be between 30 and 200 mmHg.");
        }
        if (record.getSystolic() <= record.getDiastolic()) {
            throw new IllegalArgumentException("Systolic BP must be greater than diastolic BP.");
        }
    }
}
  
