package com.bach.vetclinic.repository;

import com.bach.vetclinic.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner, Long> {
    @org.springframework.data.jpa.repository.Query("select o from Owner o where (:includeArchived = true or o.archived = false) and (lower(concat(o.firstName,' ',o.lastName)) like lower(concat('%',:q,'%')) or lower(coalesce(o.email,'')) like lower(concat('%',:q,'%')) or coalesce(o.phone,'') like concat('%',:q,'%'))")
    org.springframework.data.domain.Page<Owner> search(@org.springframework.data.repository.query.Param("q") String q, @org.springframework.data.repository.query.Param("includeArchived") boolean includeArchived, org.springframework.data.domain.Pageable pageable);
    List<Owner> findAllByArchivedFalseOrderByLastNameAscFirstNameAsc();
    Optional<Owner> findByIdAndArchivedFalse(Long id);
}
