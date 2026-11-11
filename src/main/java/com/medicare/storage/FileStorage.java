
package com.medicare.storage;

import com.medicare.model.Medicine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FileStorage {

    private static final Path FILE_PATH =
            Path.of("data", "medicines.txt");

    // Add a new medicine
    public static void saveMedicine(Medicine medicine)
            throws IOException {

        List<Medicine> medicines = loadMedicines();

        medicines.add(medicine);

        saveAllMedicines(medicines);
    }

    // Load all medicines
    public static List<Medicine> loadMedicines()
            throws IOException {

        List<Medicine> medicines = new ArrayList<>();

        if (!Files.exists(FILE_PATH)) {
            return medicines;
        }

        List<String> lines = Files.readAllLines(FILE_PATH);

        for (String line : lines) {

            try {
                String[] parts = line.split("\\|", -1);

                if (parts.length == 7) {

                    // New format with unique ID
                    List<String> times =
                            parseTimes(parts[4]);

                    medicines.add(new Medicine(
                            parts[0],
                            parts[1],
                            parts[2],
                            parts[3],
                            times,
                            LocalDate.parse(parts[5]),
                            LocalDate.parse(parts[6])
                    ));

                } else if (parts.length == 6) {

                    // Old format: preserve existing medicines
                    medicines.add(new Medicine(
                            parts[0],
                            parts[1],
                            parts[2],
                            parts[3],
                            LocalDate.parse(parts[4]),
                            LocalDate.parse(parts[5])
                    ));
                }

            } catch (Exception e) {

                System.out.println(
                        "Skipping invalid medicine record: " + line
                );
            }
        }

        return medicines;
    }

    // Edit an existing medicine
    public static void updateMedicine(Medicine updatedMedicine)
            throws IOException {

        List<Medicine> medicines = loadMedicines();

        boolean found = false;

        for (int i = 0; i < medicines.size(); i++) {

            if (medicines.get(i).getId()
                    .equals(updatedMedicine.getId())) {

                medicines.set(i, updatedMedicine);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IOException("Medicine not found.");
        }

        saveAllMedicines(medicines);
    }

    // Delete a medicine
    public static void deleteMedicine(Medicine medicine)
            throws IOException {

        List<Medicine> medicines = loadMedicines();

        boolean removed = medicines.removeIf(
                item -> item.getId().equals(medicine.getId())
        );

        if (!removed) {
            throw new IOException("Medicine not found.");
        }

        saveAllMedicines(medicines);
    }

    // Write all medicines to the file
    private static void saveAllMedicines(List<Medicine> medicines)
            throws IOException {

        Files.createDirectories(FILE_PATH.getParent());

        List<String> lines = new ArrayList<>();

        for (Medicine medicine : medicines) {

            String timings = String.join(
                    ",",
                    medicine.getReminderTimes()
            );

            lines.add(
                    medicine.getId() + "|" +
                    medicine.getName() + "|" +
                    medicine.getDosage() + "|" +
                    medicine.getFrequency() + "|" +
                    timings + "|" +
                    medicine.getStartDate() + "|" +
                    medicine.getEndDate()
            );
        }

        Files.write(FILE_PATH, lines);
    }

    private static List<String> parseTimes(String timings) {

        List<String> result = new ArrayList<>();

        if (timings != null && !timings.isBlank()) {

            for (String time : timings.split(",")) {

                if (!time.isBlank()) {
                    result.add(time.trim());
                }
            }
        }

        return result;
    }
}
