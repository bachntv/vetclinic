package com.bach.vetclinic.controller;

import com.bach.vetclinic.entity.Owner;
import com.bach.vetclinic.controller.PetController.PetResponse;
import com.bach.vetclinic.service.OwnerService;
import com.bach.vetclinic.service.PetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/owners")
public class OwnerController {
    private final OwnerService ownerService;
    private final PetService petService;

    public OwnerController(OwnerService ownerService, PetService petService) {
        this.ownerService = ownerService;
        this.petService = petService;
    }

    @GetMapping
    public List<OwnerResponse> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return (includeArchived ? ownerService.findIncludingArchived() : ownerService.findAllActive()).stream().map(OwnerResponse::from).toList();
    }
    @PostMapping("/{id}/activate")
    public OwnerResponse activate(@PathVariable Long id) { return OwnerResponse.from(ownerService.activate(id)); }

    @GetMapping("/{id}")
    public OwnerResponse get(@PathVariable Long id) {
        return OwnerResponse.from(ownerService.findActiveById(id));
    }

    @GetMapping("/{id}/pets")
    public List<PetResponse> pets(@PathVariable Long id) {
        return petService.findAllActiveByOwner(id).stream().map(PetResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerResponse create(@Valid @RequestBody OwnerRequest request) {
        return OwnerResponse.from(ownerService.create(request.firstName().trim(), request.lastName().trim(),
                clean(request.phone()), clean(request.email()), clean(request.address())));
    }

    @PutMapping("/{id}")
    public OwnerResponse update(@PathVariable Long id, @Valid @RequestBody OwnerRequest request) {
        return OwnerResponse.from(ownerService.update(id, request.firstName().trim(), request.lastName().trim(),
                clean(request.phone()), clean(request.email()), clean(request.address())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ownerService.archive(id);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record OwnerRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Size(max = 30) String phone,
            @Email @Size(max = 255) String email,
            @Size(max = 500) String address) {}

    public record OwnerResponse(Long id, String firstName, String lastName, String phone, String email,
                                String address, boolean archived, LocalDateTime createdAt, LocalDateTime updatedAt) {
        static OwnerResponse from(Owner owner) {
            return new OwnerResponse(owner.getId(), owner.getFirstName(), owner.getLastName(), owner.getPhone(),
                    owner.getEmail(), owner.getAddress(), owner.isArchived(), owner.getCreatedAt(), owner.getUpdatedAt());
        }
    }
}
