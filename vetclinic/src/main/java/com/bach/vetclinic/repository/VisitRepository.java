package com.bach.vetclinic.repository;

import com.bach.vetclinic.entity.Visit;
import com.bach.vetclinic.entity.VisitStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    boolean existsByVetIdAndIdNotAndStatusNotAndScheduledAtGreaterThanAndScheduledAtLessThan(
            Long vetId, Long id, VisitStatus status, java.time.LocalDateTime from, java.time.LocalDateTime to);
    @EntityGraph(attributePaths = {"pet", "pet.owner", "vet"})
    List<Visit> findAllByOrderByScheduledAtAsc();

    @EntityGraph(attributePaths = {"pet", "pet.owner", "vet"})
    Optional<Visit> findWithDetailsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"pet", "pet.owner", "vet"})
    @Query("select v from Visit v where v.id = :id")
    Optional<Visit> findWithDetailsByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"pet", "pet.owner", "vet"})
    List<Visit> findAllByPetIdOrderByScheduledAtDesc(Long petId);

    @EntityGraph(attributePaths = {"pet", "pet.owner", "vet"})
    List<Visit> findAllByPetIdAndStatusInOrderByScheduledAtDesc(Long petId, List<VisitStatus> statuses);
}
