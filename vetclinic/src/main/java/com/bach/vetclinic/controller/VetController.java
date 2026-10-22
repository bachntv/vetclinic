package com.bach.vetclinic.controller;

import com.bach.vetclinic.entity.Vet;
import com.bach.vetclinic.service.VetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/vets")
public class VetController {
    private final VetService vetService;

    public VetController(VetService vetService) { this.vetService = vetService; }

    @GetMapping
    public List<VetResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return (includeInactive ? vetService.findAll() : vetService.findActiveVets()).stream().map(VetResponse::from).toList();
    }
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public VetResponse activate(@PathVariable Long id) { return VetResponse.from(vetService.activate(id)); }

    @GetMapping("/{id}")
    public VetResponse get(@PathVariable Long id) { return VetResponse.from(vetService.findActiveById(id)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public VetResponse create(@Valid @RequestBody VetRequest request) {
        return VetResponse.from(vetService.create(request.name().trim(), clean(request.specialty()),
                clean(request.licenseNo()), clean(request.workHours())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public VetResponse update(@PathVariable Long id, @Valid @RequestBody VetRequest request) {
        return VetResponse.from(vetService.update(id, request.name().trim(), clean(request.specialty()),
                clean(request.licenseNo()), clean(request.workHours())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(@PathVariable Long id) { vetService.deactivate(id); }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record VetRequest(@NotBlank @Size(max = 150) String name,
                             @Size(max = 150) String specialty,
                             @Size(max = 50) String licenseNo,
                             @Size(max = 255) String workHours) {}

    public record VetResponse(Long id, String name, String specialty, String licenseNo, String workHours,
                              boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        static VetResponse from(Vet vet) {
            return new VetResponse(vet.getId(), vet.getName(), vet.getSpecialty(), vet.getLicenseNo(),
                    vet.getWorkHours(), vet.isActive(), vet.getCreatedAt(), vet.getUpdatedAt());
        }
    }
}
