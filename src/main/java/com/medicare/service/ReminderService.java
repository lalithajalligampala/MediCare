
package com.medicare.service;

import com.medicare.model.Medicine;
import com.medicare.storage.FileStorage;

import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReminderService {

    private ScheduledExecutorService scheduler;

    // Prevents the same reminder from appearing repeatedly.
    private static final Set<String> NOTIFIED_REMINDERS =
            ConcurrentHashMap.newKeySet();

    private static final DateTimeFormatter[] TIME_FORMATS = {
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("H:mm", Locale.ENGLISH)
    };

    // =====================================================
    // START REMINDER SERVICE
    // =====================================================

    public synchronized void start() {

        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {

            Thread thread = new Thread(
                    r,
                    "MediCare-Reminder"
            );

            thread.setDaemon(true);

            return thread;
        });

        // Check reminders every 15 seconds.
        scheduler.scheduleAtFixedRate(
                this::checkReminders,
                0,
                15,
                TimeUnit.SECONDS
        );
    }

    // =====================================================
    // CHECK ALL MEDICINE REMINDERS
    // =====================================================

    private void checkReminders() {

        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        try {

            List<Medicine> medicines =
                    FileStorage.loadMedicines();

            for (Medicine medicine : medicines) {

                // Check whether the medicine is active today.
                if (today.isBefore(medicine.getStartDate())
                        || today.isAfter(medicine.getEndDate())) {

                    continue;
                }

                // Get every reminder timing.
                List<String> reminderTimes =
                        medicine.getReminderTimes();

                for (String time : reminderTimes) {

                    LocalTime reminderTime =
                            parseReminderTime(time);

                    if (reminderTime == null) {
                        continue;
                    }

                    // Check the current hour and minute.
                    if (currentTime.getHour() == reminderTime.getHour()
                            && currentTime.getMinute()
                            == reminderTime.getMinute()) {

                        // Unique key for each medicine and timing.
                        String reminderKey =
                                medicine.getId() + "|" +
                                reminderTime + "|" +
                                today;

                        // Show this reminder only once per day.
                        if (NOTIFIED_REMINDERS.add(reminderKey)) {

                            Platform.runLater(() ->
                                    showReminder(
                                            medicine,
                                            time
                                    )
                            );
                        }
                    }
                }
            }

        } catch (IOException ex) {

            System.out.println(
                    "Reminder check failed: "
                            + ex.getMessage()
            );
        }
    }

    // =====================================================
    // CONVERT STRING TO LOCAL TIME
    // =====================================================

    private LocalTime parseReminderTime(String time) {

        if (time == null || time.isBlank()) {
            return null;
        }

        String normalized = time.trim().replace('.', ':');

        for (DateTimeFormatter formatter : TIME_FORMATS) {

            try {

                return LocalTime.parse(
                        normalized,
                        formatter
                );

            } catch (Exception ignored) {
                // Try the next time format.
            }
        }

        return null;
    }

    // =====================================================
    // DISPLAY REMINDER ALERT
    // =====================================================

    private void showReminder(
            Medicine medicine,
            String reminderTime) {

        Alert alert = new Alert(
                Alert.AlertType.INFORMATION
        );

        alert.setTitle("MediCare Reminder");

        alert.setHeaderText(
                "Medication Reminder"
        );

        alert.setContentText(
                "Medicine: " + medicine.getName() +
                "\nDosage: " + medicine.getDosage() +
                "\nScheduled time: " + reminderTime +
                "\n\nPlease check your prescribed medication schedule."
        );

        alert.show();
    }

    // =====================================================
    // STOP REMINDER SERVICE
    // =====================================================

    public synchronized void stop() {

        if (scheduler != null) {

            scheduler.shutdownNow();

            scheduler = null;
        }
    }
}
