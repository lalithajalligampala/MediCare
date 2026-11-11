
package com.medicare.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Medicine {

    private String id;
    private String name;
    private String dosage;
    private String frequency;
    private List<String> reminderTimes;
    private LocalDate startDate;
    private LocalDate endDate;

    // Existing constructor: keeps old MainApp code compatible
    public Medicine(String name, String dosage,
                    String frequency, String reminderTime,
                    LocalDate startDate, LocalDate endDate) {

        this(
                UUID.randomUUID().toString(),
                name,
                dosage,
                frequency,
                parseTimes(reminderTime),
                startDate,
                endDate
        );
    }

    // Constructor for multiple timings
    public Medicine(String name, String dosage,
                    String frequency, List<String> reminderTimes,
                    LocalDate startDate, LocalDate endDate) {

        this(
                UUID.randomUUID().toString(),
                name,
                dosage,
                frequency,
                reminderTimes,
                startDate,
                endDate
        );
    }

    // Constructor used when loading or editing an existing medicine
    public Medicine(String id, String name, String dosage,
                    String frequency, List<String> reminderTimes,
                    LocalDate startDate, LocalDate endDate) {

        this.id = id;
        this.name = name;
        this.dosage = dosage;
        this.frequency = frequency;
        this.reminderTimes = new ArrayList<>(reminderTimes);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    private static List<String> parseTimes(String times) {

        List<String> result = new ArrayList<>();

        if (times != null && !times.isBlank()) {
            for (String time : times.split(",")) {
                if (!time.isBlank()) {
                    result.add(time.trim());
                }
            }
        }

        return result;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDosage() {
        return dosage;
    }

    public String getFrequency() {
        return frequency;
    }

    public List<String> getReminderTimes() {
        return new ArrayList<>(reminderTimes);
    }

    // Keeps older code compatible.
    // Returns the first timing if one exists.
    public String getReminderTime() {
        return reminderTimes.isEmpty()
                ? ""
                : reminderTimes.get(0);
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
