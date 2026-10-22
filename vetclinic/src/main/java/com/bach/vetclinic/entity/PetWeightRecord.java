package com.bach.vetclinic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pet_weight_records")
public class PetWeightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(name = "weight_kg", nullable = false, precision = 7, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(columnDefinition = "text")
    private String note;

    protected PetWeightRecord() {
    }

    public PetWeightRecord(Pet pet, BigDecimal weightKg, LocalDateTime recordedAt, String note) {
        this.pet = pet;
        this.weightKg = weightKg;
        this.recordedAt = recordedAt;
        this.note = note;
    }

    public Long getId() { return id; }
    public Pet getPet() { return pet; }
    public BigDecimal getWeightKg() { return weightKg; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public String getNote() { return note; }
}
