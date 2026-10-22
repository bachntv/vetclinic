package com.bach.vetclinic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "vets")
public class Vet extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String specialty;

    @Column(name = "license_no", unique = true, length = 50)
    private String licenseNo;

    @Column(name = "work_hours", length = 255)
    private String workHours;

    @Column(nullable = false)
    private boolean active = true;

    protected Vet() {
    }

    public Vet(String name, String specialty, String licenseNo, String workHours) {
        this.name = name;
        this.specialty = specialty;
        this.licenseNo = licenseNo;
        this.workHours = workHours;
    }

    public String getName() { return name; }
    public String getSpecialty() { return specialty; }
    public String getLicenseNo() { return licenseNo; }
    public String getWorkHours() { return workHours; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public void updateProfile(String name, String specialty, String licenseNo, String workHours) {
        this.name = name;
        this.specialty = specialty;
        this.licenseNo = licenseNo;
        this.workHours = workHours;
    }
}
