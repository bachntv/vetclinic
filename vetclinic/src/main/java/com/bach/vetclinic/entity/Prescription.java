package com.bach.vetclinic.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prescriptions", uniqueConstraints = @UniqueConstraint(name = "uk_prescriptions_visit", columnNames = "visit_id"))
public class Prescription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    @Column(columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionItem> items = new ArrayList<>();

    protected Prescription() {}

    public Prescription(Visit visit, String notes) {
        this.visit = visit;
        this.notes = notes;
    }

    public void addItem(PrescriptionItem item) { items.add(item); }
    public Long getId() { return super.getId(); }
    public Visit getVisit() { return visit; }
    public String getNotes() { return notes; }
    public List<PrescriptionItem> getItems() { return List.copyOf(items); }
}
