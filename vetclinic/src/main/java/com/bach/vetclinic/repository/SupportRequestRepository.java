package com.bach.vetclinic.repository;
import com.bach.vetclinic.entity.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SupportRequestRepository extends JpaRepository<SupportRequest,Long> {
    List<SupportRequest> findAllByOrderByCreatedAtDesc();
    List<SupportRequest> findAllByRequestedByOrderByCreatedAtDesc(String requestedBy);
}
