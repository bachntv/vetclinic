package com.bach.vetclinic.repository;

import com.bach.vetclinic.entity.ClinicService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClinicServiceRepository extends JpaRepository<ClinicService, Long> {
    List<ClinicService> findAllByOrderByNameAsc();
}
