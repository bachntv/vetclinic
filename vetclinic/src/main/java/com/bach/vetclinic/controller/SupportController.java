package com.bach.vetclinic.controller;
import com.bach.vetclinic.entity.SupportRequest;
import com.bach.vetclinic.repository.SupportRequestRepository;
import com.bach.vetclinic.web.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@RestController @RequestMapping("/api/support-requests") @Transactional
public class SupportController {
    private final SupportRequestRepository repository;
    public SupportController(SupportRequestRepository repository){this.repository=repository;}
    @GetMapping @Transactional(readOnly=true)
    public List<SupportRequest> list(Authentication auth){
        boolean admin=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));
        return admin?repository.findAllByOrderByCreatedAtDesc():repository.findAllByRequestedByOrderByCreatedAtDesc(auth.getName());
    }
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public SupportRequest create(Authentication auth,@Valid @RequestBody Request request){return repository.save(new SupportRequest(auth.getName(),request.subject().trim(),request.description().trim()));}
    @PatchMapping("/{id}/status") @PreAuthorize("hasRole('ADMIN')")
    public SupportRequest status(@PathVariable Long id,@Valid @RequestBody StatusRequest request){
        SupportRequest item=repository.findById(id).orElseThrow(()->new NotFoundException("Không tìm thấy yêu cầu")); item.setStatus(request.status()); return item;
    }
    public record Request(@NotBlank @Size(max=200) String subject,@NotBlank @Size(max=10000) String description){}
    public record StatusRequest(@NotNull @Pattern(regexp="OPEN|IN_PROGRESS|RESOLVED") String status){}
}
