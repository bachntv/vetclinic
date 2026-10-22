package com.bach.vetclinic.controller;

import com.bach.vetclinic.entity.Vaccination;
import com.bach.vetclinic.service.VaccinationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/vaccinations")
public class VaccinationController {
    private final VaccinationService vaccinationService;

    public VaccinationController(VaccinationService vaccinationService) { this.vaccinationService = vaccinationService; }

    @GetMapping
    public List<VaccinationResponse> list(@RequestParam(required = false) Long petId) {
        return vaccinationService.findAll(petId).stream().map(VaccinationResponse::from).toList();
    }

    @GetMapping("/{id}")
    public VaccinationResponse get(@PathVariable Long id) {
        return VaccinationResponse.from(vaccinationService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VaccinationResponse create(@Valid @RequestBody VaccinationRequest request) {
        return VaccinationResponse.from(vaccinationService.create(request.petId(), clean(request.vaccineType()),
                request.administeredAt(), clean(request.lotNumber()), request.nextDueDate(), clean(request.administeredBy()),request.vaccineId(),clean(request.notes())));
    }

    @PutMapping("/{id}")
    public VaccinationResponse update(@PathVariable Long id, @Valid @RequestBody VaccinationRequest request) {
        return VaccinationResponse.from(vaccinationService.update(id, request.petId(), clean(request.vaccineType()),
                request.administeredAt(), clean(request.lotNumber()), request.nextDueDate(), clean(request.administeredBy()),request.vaccineId(),clean(request.notes())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { vaccinationService.delete(id); }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record VaccinationRequest(@NotNull @Positive Long petId,
                                     @Size(max = 120) String vaccineType,
                                     @NotNull LocalDateTime administeredAt,
                                     @Size(max = 100) String lotNumber,
                                     LocalDate nextDueDate,
                                     @Size(max = 150) String administeredBy,@Positive Long vaccineId,@Size(max=5000) String notes) {}

    public record VaccinationResponse(Long id, Long petId, String petName, String vaccineType,
                                      LocalDateTime administeredAt, String lotNumber, LocalDate nextDueDate,
                                      String administeredBy,Long vaccineId,String notes) {
        static VaccinationResponse from(Vaccination vaccination) {
            return new VaccinationResponse(vaccination.getId(), vaccination.getPet().getId(),
                    vaccination.getPet().getName(), vaccination.getVaccineType(), vaccination.getAdministeredAt(),
                    vaccination.getLotNumber(), vaccination.getNextDueDate(), vaccination.getAdministeredBy(),vaccination.getVaccine()==null?null:vaccination.getVaccine().getId(),vaccination.getNotes());
        }
    }
}
