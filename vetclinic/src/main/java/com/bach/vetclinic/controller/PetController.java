package com.bach.vetclinic.controller;

import com.bach.vetclinic.entity.Pet;
import com.bach.vetclinic.service.PetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController {
    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    public List<PetResponse> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return (includeArchived ? petService.findIncludingArchived() : petService.findAllActive()).stream().map(PetResponse::from).toList();
    }
    @PostMapping("/{id}/activate")
    public PetResponse activate(@PathVariable Long id) { return PetResponse.from(petService.activate(id)); }

    @GetMapping("/{id}")
    public PetResponse get(@PathVariable Long id) {
        return PetResponse.from(petService.findActiveById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PetResponse create(@Valid @RequestBody PetRequest request) {
        return PetResponse.from(petService.create(request.ownerId(), request.name().trim(), request.species().trim(),
                clean(request.breed()), clean(request.speciesNote()), request.birthDate(), clean(request.sex()),
                request.weightKg(), clean(request.allergies()), clean(request.chronicConditions())));
    }

    @PutMapping("/{id}")
    public PetResponse update(@PathVariable Long id, @Valid @RequestBody PetRequest request) {
        return PetResponse.from(petService.update(id, request.ownerId(), request.name().trim(), request.species().trim(),
                clean(request.breed()), clean(request.speciesNote()), request.birthDate(), clean(request.sex()),
                request.weightKg(), clean(request.allergies()), clean(request.chronicConditions())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        petService.archive(id);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record PetRequest(
            @NotNull @Positive Long ownerId,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 80) String species,
            @Size(max = 100) String breed,
            @Size(max = 500) String speciesNote,
            LocalDate birthDate,
            @Size(max = 30) String sex,
            @DecimalMin("0.01") @Digits(integer = 5, fraction = 2) BigDecimal weightKg,
            String allergies,
            String chronicConditions) {}

    public record PetResponse(Long id, Long ownerId, String ownerName, String name, String species,
                              String breed, String speciesNote, LocalDate birthDate, String sex,
                              BigDecimal weightKg, String allergies, String chronicConditions, boolean archived,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        static PetResponse from(Pet pet) {
            return new PetResponse(pet.getId(), pet.getOwner().getId(),
                    pet.getOwner().getFirstName() + " " + pet.getOwner().getLastName(), pet.getName(), pet.getSpecies(),
                    pet.getBreed(), pet.getSpeciesNote(), pet.getBirthDate(), pet.getSex(), pet.getWeightKg(),
                    pet.getAllergies(), pet.getChronicConditions(), pet.isArchived(), pet.getCreatedAt(), pet.getUpdatedAt());
        }
    }
}
