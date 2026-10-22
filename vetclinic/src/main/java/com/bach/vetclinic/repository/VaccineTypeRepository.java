package com.bach.vetclinic.repository;
import com.bach.vetclinic.entity.VaccineType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface VaccineTypeRepository extends JpaRepository<VaccineType,Long>{List<VaccineType> findAllByOrderByNameAsc();}
