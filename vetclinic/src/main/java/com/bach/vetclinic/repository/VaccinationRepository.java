package com.bach.vetclinic.repository;

import com.bach.vetclinic.entity.Vaccination;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VaccinationRepository extends JpaRepository<Vaccination, Long> {
    @EntityGraph(attributePaths = {"pet", "vaccine"})
    List<Vaccination> findAllByOrderByAdministeredAtDesc();

    @EntityGraph(attributePaths = {"pet", "vaccine"})
    List<Vaccination> findAllByPetIdOrderByAdministeredAtDesc(Long petId);

    @EntityGraph(attributePaths = {"pet", "vaccine"})
    Optional<Vaccination> findWithPetById(Long id);
}
