package com.bach.vetclinic.repository;

import com.bach.vetclinic.entity.Vet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VetRepository extends JpaRepository<Vet, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Vet v where v.id = :id")
    Optional<Vet> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    List<Vet> findAllByActiveTrueOrderByNameAsc();
    Optional<Vet> findByIdAndActiveTrue(Long id);
    Optional<Vet> findByLicenseNoIgnoreCase(String licenseNo);
}
