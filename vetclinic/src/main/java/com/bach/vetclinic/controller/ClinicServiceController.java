package com.bach.vetclinic.controller;

import com.bach.vetclinic.entity.ClinicService;
import com.bach.vetclinic.entity.InvoiceItemCategory;
import com.bach.vetclinic.repository.ClinicServiceRepository;
import com.bach.vetclinic.web.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/services")
@Transactional
public class ClinicServiceController {
    private final ClinicServiceRepository services;
    public ClinicServiceController(ClinicServiceRepository services) { this.services = services; }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ClinicService> list() { return services.findAllByOrderByNameAsc(); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ClinicService create(@Valid @RequestBody Request request) {
        return services.save(new ClinicService(request.name(), request.category(), request.unitPrice(), request.description()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClinicService update(@PathVariable Long id, @Valid @RequestBody Request request) {
        ClinicService service = find(id);
        service.update(request.name(), request.category(), request.unitPrice(), request.description());
        return service;
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ClinicService active(@PathVariable Long id, @Valid @RequestBody Active request) {
        ClinicService service = find(id);
        service.setActive(request.active());
        return service;
    }

    private ClinicService find(Long id) {
        return services.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy dịch vụ"));
    }

    public record Request(@NotBlank @Size(max = 160) String name,
            @NotNull InvoiceItemCategory category,
            @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,
            @Size(max = 1000) String description) {}
    public record Active(@NotNull Boolean active) {}
}
