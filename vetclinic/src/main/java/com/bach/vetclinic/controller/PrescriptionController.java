package com.bach.vetclinic.controller;

import com.bach.vetclinic.entity.Prescription;
import com.bach.vetclinic.entity.PrescriptionItem;
import com.bach.vetclinic.service.PrescriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@PreAuthorize("hasAnyRole('ADMIN', 'VET')")
public class PrescriptionController {
    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) { this.prescriptionService = prescriptionService; }

    @GetMapping("/by-visit/{visitId}")
    public PrescriptionResponse getByVisit(@PathVariable Long visitId) {
        return PrescriptionResponse.from(prescriptionService.findByVisitId(visitId));
    }
    @GetMapping("/by-pet/{petId}")
    public List<PrescriptionResponse> history(@PathVariable Long petId){return prescriptionService.history(petId).stream().map(PrescriptionResponse::from).toList();}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionResponse create(@Valid @RequestBody PrescriptionRequest request) {
        List<PrescriptionService.ItemData> items = request.items().stream()
                .map(item -> new PrescriptionService.ItemData(item.medicineId(), item.dosage().trim(),
                        item.frequency().trim(), item.durationDays(), item.quantity(), clean(item.instructions())))
                .toList();
        return PrescriptionResponse.from(prescriptionService.create(request.visitId(), clean(request.notes()), items));
    }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    @PutMapping("/by-visit/{visitId}")
    public PrescriptionResponse replace(@PathVariable Long visitId,@Valid @RequestBody ReplaceRequest r){
        var lines=r.items().stream().map(i->new PrescriptionService.ItemData(i.medicineId(),i.dosage().trim(),i.frequency().trim(),i.durationDays(),i.quantity(),clean(i.instructions()))).toList();
        return PrescriptionResponse.from(prescriptionService.replace(visitId,r.revision(),r.reason(),r.notes(),lines));
    }
    @PostMapping("/by-visit/{visitId}/cancel")
    public PrescriptionResponse cancel(@PathVariable Long visitId,@Valid @RequestBody CancelRequest r){return PrescriptionResponse.from(prescriptionService.cancel(visitId,r.revision(),r.reason()));}
    public record ReplaceRequest(@NotNull @jakarta.validation.constraints.Min(0) Long revision,@NotBlank @Size(max=1000) String reason,@Size(max=2000) String notes,@NotEmpty List<@NotNull @Valid PrescriptionItemRequest> items){}
    public record CancelRequest(@NotNull @jakarta.validation.constraints.Min(0) Long revision,@NotBlank @Size(max=1000) String reason){}

    public record PrescriptionRequest(@NotNull @Positive Long visitId,
                                      @Size(max = 2000) String notes,
                                      @NotEmpty List<@NotNull @Valid PrescriptionItemRequest> items) {}

    public record PrescriptionItemRequest(@NotNull @Positive Long medicineId,
                                          @NotBlank @Size(max = 200) String dosage,
                                          @NotBlank @Size(max = 120) String frequency,
                                          @NotNull @Positive Integer durationDays,
                                          @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) @DecimalMin(value = "0.01") BigDecimal quantity,
                                          @Size(max = 500) String instructions) {}

    public record PrescriptionResponse(Long id, Long visitId, String petName, String notes,
                                       List<PrescriptionItemResponse> items,java.time.LocalDateTime prescribedAt,String status,long revision,java.time.LocalDateTime cancelledAt,String cancelReason) {
        static PrescriptionResponse from(Prescription prescription) {
            return new PrescriptionResponse(prescription.getId(), prescription.getVisit().getId(),
                    prescription.getVisit().getPet().getName(), prescription.getNotes(),
                    prescription.getItems().stream().map(PrescriptionItemResponse::from).toList(),prescription.getCreatedAt(),prescription.getStatus(),prescription.getRevision(),prescription.getCancelledAt(),prescription.getCancelReason());
        }
    }

    public record PrescriptionItemResponse(Long id, Long medicineId, String medicineName, String unit,
                                            String dosage, String frequency, Integer durationDays,
                                            BigDecimal quantity, BigDecimal unitPrice, String instructions) {
        static PrescriptionItemResponse from(PrescriptionItem item) {
            return new PrescriptionItemResponse(item.getId(), item.getMedicine().getId(), item.getMedicine().getName(),
                    item.getMedicine().getUnit(), item.getDosage(), item.getFrequency(), item.getDurationDays(),
                    item.getQuantity(), item.getUnitPrice(), item.getInstructions());
        }
    }
}
