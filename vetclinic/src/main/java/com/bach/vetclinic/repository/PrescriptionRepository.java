package com.bach.vetclinic.repository;

import com.bach.vetclinic.entity.Prescription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    @EntityGraph(attributePaths = {"visit", "visit.pet", "items", "items.medicine"})
    java.util.List<Prescription> findAllByVisitPetIdOrderByCreatedAtDesc(Long petId);
    @EntityGraph(attributePaths = {"visit", "visit.pet", "items", "items.medicine"})
    Optional<Prescription> findWithDetailsByVisitId(Long visitId);
}
