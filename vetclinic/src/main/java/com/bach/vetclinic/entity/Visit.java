package com.bach.vetclinic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "visits")
public class Visit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vet_id", nullable = false)
    private Vet vet;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VisitStatus status = VisitStatus.SCHEDULED;

    @Column(name = "chief_complaint", columnDefinition = "text")
    private String chiefComplaint;

    @Column(columnDefinition = "text")
    private String diagnosis;
    @Column(columnDefinition="text") private String symptoms;
    public String getSymptoms(){return symptoms;}
    public void setSymptoms(String symptoms){this.symptoms=symptoms;}

    @Column(name = "treatment_notes", columnDefinition = "text")
    private String treatmentNotes;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    protected Visit() {
    }

    public Visit(Pet pet, Vet vet, LocalDateTime scheduledAt, String chiefComplaint) {
        this.pet = pet;
        this.vet = vet;
        this.scheduledAt = scheduledAt;
        this.chiefComplaint = chiefComplaint;
    }

    public Pet getPet() { return pet; }
    public Vet getVet() { return vet; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public VisitStatus getStatus() { return status; }
    public String getChiefComplaint() { return chiefComplaint; }
    public String getDiagnosis() { return diagnosis; }
    public String getTreatmentNotes() { return treatmentNotes; }
    public LocalDate getFollowUpDate() { return followUpDate; }
    public LocalDateTime getReminderSentAt() { return reminderSentAt; }

    public void reschedule(Pet pet, Vet vet, LocalDateTime scheduledAt, String chiefComplaint) {
        this.pet = pet;
        this.vet = vet;
        this.scheduledAt = scheduledAt;
        this.chiefComplaint = chiefComplaint;
    }

    public void setStatus(VisitStatus status) { this.status = status; }

    public void updateMedicalRecord(String diagnosis, String treatmentNotes, LocalDate followUpDate) {
        this.diagnosis = diagnosis;
        this.treatmentNotes = treatmentNotes;
        this.followUpDate = followUpDate;
    }
}
