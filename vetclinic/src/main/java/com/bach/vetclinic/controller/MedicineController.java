package com.bach.vetclinic.controller;

import com.bach.vetclinic.entity.Medicine;
import com.bach.vetclinic.service.MedicineService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {
    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) { this.medicineService = medicineService; }

    @GetMapping
    public List<MedicineResponse> list() {
        return medicineService.findAll().stream().map(MedicineResponse::from).toList();
    }

    @GetMapping("/{id}")
    public MedicineResponse get(@PathVariable Long id) { return MedicineResponse.from(medicineService.findById(id)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MedicineResponse create(@Valid @RequestBody MedicineRequest request) {
        return MedicineResponse.from(medicineService.create(request.name().trim(), clean(request.activeIngredient()),
                request.form().trim(), request.unit().trim(), request.stockQuantity(), request.reorderLevel(), request.unitPrice(),clean(request.description())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MedicineResponse update(@PathVariable Long id, @Valid @RequestBody MedicineRequest request) {
        return MedicineResponse.from(medicineService.update(id, request.name().trim(), clean(request.activeIngredient()),
                request.form().trim(), request.unit().trim(), request.stockQuantity(), request.reorderLevel(), request.unitPrice(),clean(request.description())));
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public MedicineResponse setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return MedicineResponse.from(medicineService.setActive(id, request.active()));
    }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record MedicineRequest(@NotBlank @Size(max = 160) String name,
                                  @Size(max = 160) String activeIngredient,
                                  @NotBlank @Size(max = 80) String form,
                                  @NotBlank @Size(max = 40) String unit,
                                  @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal stockQuantity,
                                  @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal reorderLevel,
                                  @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice,@Size(max=5000) String description) {}

    public record ActiveRequest(@NotNull Boolean active) {}

    public record MedicineResponse(Long id, String name, String activeIngredient, String form, String unit,
                                   BigDecimal stockQuantity, BigDecimal reorderLevel, BigDecimal unitPrice,
                                   boolean active,String description) {
        static MedicineResponse from(Medicine medicine) {
            return new MedicineResponse(medicine.getId(), medicine.getName(), medicine.getActiveIngredient(),
                    medicine.getForm(), medicine.getUnit(), medicine.getStockQuantity(), medicine.getReorderLevel(),
                    medicine.getUnitPrice(), medicine.isActive(),medicine.getDescription());
        }
    }
}
