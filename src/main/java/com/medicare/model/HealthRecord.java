package com.medicare.model;

import java.time.LocalDate;

public class HealthRecord {
    private LocalDate date;
    private double weight;
    private int systolic;
    private int diastolic;

    public HealthRecord(LocalDate date, double weight, int systolic, int diastolic) {
        this.date = date;
        this.weight = weight;
        this.systolic = systolic;
        this.diastolic = diastolic;
    }

    public LocalDate getDate() { return date; }
    public double getWeight() { return weight; }
    public int getSystolic() { return systolic; }
    public int getDiastolic() { return diastolic; }
}
