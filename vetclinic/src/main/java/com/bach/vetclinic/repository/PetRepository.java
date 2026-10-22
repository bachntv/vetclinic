package com.bach.vetclinic.repository;

import com.bach.vetclinic.entity.Pet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
    @EntityGraph(attributePaths="owner")
    @org.springframework.data.jpa.repository.Query("select p from Pet p where (:includeArchived = true or p.archived = false) and (lower(p.name) like lower(concat('%',:q,'%')) or lower(p.species) like lower(concat('%',:q,'%')) or lower(concat(p.owner.firstName,' ',p.owner.lastName)) like lower(concat('%',:q,'%')))")
    org.springframework.data.domain.Page<Pet> search(@org.springframework.data.repository.query.Param("q") String q, @org.springframework.data.repository.query.Param("includeArchived") boolean includeArchived, org.springframework.data.domain.Pageable pageable);
    @Override
    @EntityGraph(attributePaths = "owner")
    List<Pet> findAll();
    @EntityGraph(attributePaths = "owner")
    java.util.Optional<Pet> findWithOwnerById(Long id);
    boolean existsByOwnerIdAndArchivedFalse(Long ownerId);
    boolean existsByOwnerId(Long ownerId);

    @EntityGraph(attributePaths = "owner")
    List<Pet> findAllByArchivedFalseOrderByNameAsc();

    @EntityGraph(attributePaths = "owner")
    List<Pet> findAllByOwnerIdAndArchivedFalseOrderByNameAsc(Long ownerId);

    @EntityGraph(attributePaths = "owner")
    Optional<Pet> findByIdAndArchivedFalse(Long id);
}
