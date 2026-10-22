package com.bach.vetclinic.controller;

import com.bach.vetclinic.entity.PetWeightRecord;
import com.bach.vetclinic.service.PetWeightService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/pets/{petId}/weights")
public class PetWeightController {
    private final PetWeightService weightService;

    public PetWeightController(PetWeightService weightService) { this.weightService = weightService; }

    @GetMapping
    public List<WeightResponse> list(@PathVariable Long petId) {
        return weightService.findAllForPet(petId).stream().map(WeightResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WeightResponse record(@PathVariable Long petId, @Valid @RequestBody WeightRequest request) {
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        return WeightResponse.from(weightService.record(petId, request.weightKg(), request.recordedAt(), note));
    }

    public record WeightRequest(@NotNull @DecimalMin("0.01") @Digits(integer = 5, fraction = 2) BigDecimal weightKg,
                                @NotNull LocalDateTime recordedAt, String note) {}
    public record WeightResponse(Long id, Long petId, BigDecimal weightKg, LocalDateTime recordedAt, String note) {
        static WeightResponse from(PetWeightRecord record) {
            return new WeightResponse(record.getId(), record.getPet().getId(), record.getWeightKg(),
                    record.getRecordedAt(), record.getNote());
        }
    }
}
