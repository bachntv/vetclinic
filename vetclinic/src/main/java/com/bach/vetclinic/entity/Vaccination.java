package com.bach.vetclinic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vaccinations")
public class Vaccination extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(name = "vaccine_type", nullable = false, length = 120)
    private String vaccineType;

    @Column(name = "administered_at", nullable = false)
    private LocalDateTime administeredAt;

    @Column(name = "lot_number", length = 100)
    private String lotNumber;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "administered_by", length = 150)
    private String administeredBy;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="vaccine_id")
    private VaccineType vaccine;
    @Column(columnDefinition="text") private String notes;

    protected Vaccination() {
    }

    public Vaccination(Pet pet, String vaccineType, LocalDateTime administeredAt) {
        this.pet = pet;
        this.vaccineType = vaccineType;
        this.administeredAt = administeredAt;
    }

    public Pet getPet() { return pet; }
    public String getVaccineType() { return vaccineType; }
    public LocalDateTime getAdministeredAt() { return administeredAt; }
    public String getLotNumber() { return lotNumber; }
    public LocalDate getNextDueDate() { return nextDueDate; }
    public String getAdministeredBy() { return administeredBy; }
    public VaccineType getVaccine(){return vaccine;} public String getNotes(){return notes;}
    public void setCatalog(VaccineType vaccine,String notes){this.vaccine=vaccine;this.notes=notes;}

    public void update(Pet pet, String vaccineType, LocalDateTime administeredAt, String lotNumber,
                       LocalDate nextDueDate, String administeredBy) {
        this.pet = pet;
        this.vaccineType = vaccineType;
        this.administeredAt = administeredAt;
        this.lotNumber = lotNumber;
        this.nextDueDate = nextDueDate;
        this.administeredBy = administeredBy;
    }
}
