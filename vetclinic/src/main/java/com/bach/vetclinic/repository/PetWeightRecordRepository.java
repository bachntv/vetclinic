package com.bach.vetclinic.repository;

import com.bach.vetclinic.entity.PetWeightRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetWeightRecordRepository extends JpaRepository<PetWeightRecord, Long> {
    @EntityGraph(attributePaths = "pet")
    List<PetWeightRecord> findAllByPetIdOrderByRecordedAtDesc(Long petId);
}
