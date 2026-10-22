package com.bach.vetclinic.repository;

import com.bach.vetclinic.entity.Invoice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @EntityGraph(attributePaths = {"items", "visit", "visit.pet", "visit.pet.owner", "visit.vet"})
    List<Invoice> findAllByOrderByIssuedAtDesc();

    @EntityGraph(attributePaths = {"items", "visit", "visit.pet", "visit.pet.owner", "visit.vet"})
    Optional<Invoice> findWithDetailsById(Long id);
}
