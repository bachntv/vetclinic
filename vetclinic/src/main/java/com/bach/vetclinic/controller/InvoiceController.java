package com.bach.vetclinic.controller;

import com.bach.vetclinic.entity.Invoice;
import com.bach.vetclinic.entity.InvoiceItem;
import com.bach.vetclinic.entity.InvoiceItemCategory;
import com.bach.vetclinic.entity.InvoiceStatus;
import com.bach.vetclinic.service.InvoiceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@PreAuthorize("hasAnyRole('ADMIN', 'VET', 'RECEPTIONIST')")
public class InvoiceController {
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) { this.invoiceService = invoiceService; }

    @GetMapping
    public List<InvoiceResponse> list() {
        return invoiceService.findAll().stream().map(InvoiceResponse::from).toList();
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable Long id) { return InvoiceResponse.from(invoiceService.findById(id)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@Valid @RequestBody InvoiceRequest request) {
        return InvoiceResponse.from(invoiceService.create(request.visitId(), request.vatRate(), toItemData(request.items())));
    }

    @PutMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public InvoiceResponse updateItems(@PathVariable Long id, @Valid @RequestBody UpdateInvoiceItemsRequest request) {
        return InvoiceResponse.from(invoiceService.updateDraftItems(id, request.vatRate(), toItemData(request.items())));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public InvoiceResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return InvoiceResponse.from(invoiceService.changeStatus(id, request.status()));
    }

    private List<InvoiceService.ItemData> toItemData(List<InvoiceItemRequest> items) {
        return items.stream().map(item -> new InvoiceService.ItemData(item.description(), item.category(),
                item.quantity(), item.unitPrice())).toList();
    }

    public record InvoiceRequest(@NotNull @Positive Long visitId,
                                 @NotNull @DecimalMin("0.0000") @DecimalMax("1.0000") @Digits(integer = 1, fraction = 4) BigDecimal vatRate,
                                 @NotEmpty List<@NotNull @Valid InvoiceItemRequest> items) {}

    public record UpdateInvoiceItemsRequest(@NotNull @DecimalMin("0.0000") @DecimalMax("1.0000") @Digits(integer = 1, fraction = 4) BigDecimal vatRate,
                                            @NotEmpty List<@NotNull @Valid InvoiceItemRequest> items) {}

    public record InvoiceItemRequest(@NotBlank @Size(max = 500) String description,
                                    @NotNull InvoiceItemCategory category,
                                    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantity,
                                    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice) {}
    public record StatusRequest(@NotNull InvoiceStatus status) {}

    public record InvoiceResponse(Long id, Long visitId, Long petId, String petName,
                                  LocalDateTime issuedAt, BigDecimal subtotal, BigDecimal vatRate,
                                  BigDecimal vatAmount, BigDecimal total, InvoiceStatus status,
                                  List<InvoiceItemResponse> items) {
        static InvoiceResponse from(Invoice invoice) {
            return new InvoiceResponse(invoice.getId(), invoice.getVisit().getId(), invoice.getVisit().getPet().getId(),
                    invoice.getVisit().getPet().getName(), invoice.getIssuedAt(), invoice.getSubtotal(),
                    invoice.getVatRate(), invoice.getVatAmount(), invoice.getTotal(), invoice.getStatus(),
                    invoice.getItems().stream().map(InvoiceItemResponse::from).toList());
        }
    }

    public record InvoiceItemResponse(Long id, String description, InvoiceItemCategory category,
                                      BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        static InvoiceItemResponse from(InvoiceItem item) {
            return new InvoiceItemResponse(item.getId(), item.getDescription(), item.getCategory(), item.getQuantity(),
                    item.getUnitPrice(), item.getLineTotal());
        }
    }
}
