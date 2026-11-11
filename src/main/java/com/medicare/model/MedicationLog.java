
package com.medicare.model;

import java.time.LocalDate;
import java.util.UUID;

public class MedicationLog {

    private String id;
    private LocalDate date;
    private String medicineName;
    private String dosage;
    private String status;

    public MedicationLog(LocalDate date, String medicineName,
                         String dosage, String status) {

        this.id = UUID.randomUUID().toString();
        this.date = date;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.status = status;
    }

    // Constructor used when loading existing logs
    public MedicationLog(String id, LocalDate date,
                         String medicineName, String dosage,
                         String status) {

        this.id = id;
        this.date = date;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public String getDosage() {
        return dosage;
    }

    public String getStatus() {
        return status;
    }
}
