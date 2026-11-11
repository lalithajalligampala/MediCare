
package com.medicare.storage;

import com.medicare.model.MedicationLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MedicationLogStorage {

    private static final Path FILE_PATH =
            Path.of("data", "medication_history.txt");

    public static List<MedicationLog> loadLogs()
            throws IOException {

        List<MedicationLog> logs = new ArrayList<>();

        if (!Files.exists(FILE_PATH)) {
            return logs;
        }

        for (String line : Files.readAllLines(FILE_PATH)) {

            try {
                String[] parts = line.split("\\|", -1);

                if (parts.length == 5) {

                    // New format with unique ID
                    logs.add(new MedicationLog(
                            parts[0],
                            LocalDate.parse(parts[1]),
                            parts[2],
                            parts[3],
                            parts[4]
                    ));

                } else if (parts.length == 4) {

                    // Old format: preserve existing history
                    logs.add(new MedicationLog(
                            LocalDate.parse(parts[0]),
                            parts[1],
                            parts[2],
                            parts[3]
                    ));

                }

            } catch (Exception e) {
                System.out.println(
                        "Skipping invalid log: " + line
                );
            }
        }

        return logs;
    }

    public static void saveOrUpdateLog(MedicationLog newLog)
            throws IOException {

        List<MedicationLog> logs = loadLogs();

        boolean updated = false;

        for (int i = 0; i < logs.size(); i++) {

            MedicationLog oldLog = logs.get(i);

            if (oldLog.getId().equals(newLog.getId())) {

                logs.set(i, newLog);
                updated = true;
                break;
            }
        }

        if (!updated) {
            logs.add(newLog);
        }

        Files.createDirectories(FILE_PATH.getParent());

        List<String> lines = new ArrayList<>();

        for (MedicationLog log : logs) {

            lines.add(
                    log.getId() + "|" +
                    log.getDate() + "|" +
                    log.getMedicineName() + "|" +
                    log.getDosage() + "|" +
                    log.getStatus()
            );
        }

        Files.write(FILE_PATH, lines);
    }
}
