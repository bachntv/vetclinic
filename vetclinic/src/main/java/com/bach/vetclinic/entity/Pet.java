package com.bach.vetclinic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pets")
public class Pet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 80)
    private String species;

    @Column(length = 100)
    private String breed;

    @Column(name = "species_note", length = 500)
    private String speciesNote;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 30)
    private String sex;

    @Column(name = "weight_kg", precision = 7, scale = 2)
    private BigDecimal weightKg;

    @Column(columnDefinition = "text")
    private String allergies;

    @Column(name = "chronic_conditions", columnDefinition = "text")
    private String chronicConditions;

    @Column(nullable = false)
    private boolean archived = false;

    protected Pet() {
    }

    public Pet(Owner owner, String name, String species) {
        this.owner = owner;
        this.name = name;
        this.species = species;
    }

    public Owner getOwner() { return owner; }
    public String getName() { return name; }
    public String getSpecies() { return species; }
    public String getBreed() { return breed; }
    public String getSpeciesNote() { return speciesNote; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getSex() { return sex; }
    public BigDecimal getWeightKg() { return weightKg; }
    public String getAllergies() { return allergies; }
    public String getChronicConditions() { return chronicConditions; }
    public boolean isArchived() { return archived; }

    public void update(Owner owner, String name, String species, String breed, String speciesNote,
                       LocalDate birthDate, String sex, BigDecimal weightKg, String allergies,
                       String chronicConditions) {
        this.owner = owner;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.speciesNote = speciesNote;
        this.birthDate = birthDate;
        this.sex = sex;
        this.weightKg = weightKg;
        this.allergies = allergies;
        this.chronicConditions = chronicConditions;
    }

    public void archive() { this.archived = true; }
    public void activate() { this.archived = false; }
    public void recordWeight(BigDecimal weightKg) { this.weightKg = weightKg; }
}
