package com.bach.vetclinic.controller;

import com.bach.vetclinic.entity.Visit;
import com.bach.vetclinic.entity.VisitStatus;
import com.bach.vetclinic.service.VisitService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final VisitService visitService;

    public AppointmentController(VisitService visitService) { this.visitService = visitService; }

    @GetMapping
    public List<AppointmentResponse> list() {
        return visitService.findAllAppointments().stream().map(AppointmentResponse::from).toList();
    }

    @PostMapping("/{id}/follow-up")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','VET')")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse followUp(@PathVariable Long id, @Valid @RequestBody FollowUpRequest request) {
        return AppointmentResponse.from(visitService.followUp(id, request.scheduledAt()));
    }
    public record FollowUpRequest(@NotNull LocalDateTime scheduledAt) {}

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id) { return AppointmentResponse.from(visitService.findById(id)); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(@Valid @RequestBody AppointmentRequest request) {
        return AppointmentResponse.from(visitService.create(request.petId(), request.vetId(), request.scheduledAt(), clean(request.chiefComplaint())));
    }

    @PutMapping("/{id}")
    public AppointmentResponse reschedule(@PathVariable Long id, @Valid @RequestBody AppointmentRequest request) {
        return AppointmentResponse.from(visitService.reschedule(id, request.petId(), request.vetId(), request.scheduledAt(), clean(request.chiefComplaint())));
    }

    @PutMapping("/{id}/status")
    public AppointmentResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return AppointmentResponse.from(visitService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) { visitService.changeStatus(id, VisitStatus.CANCELLED); }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record AppointmentRequest(@NotNull @Positive Long petId, @NotNull @Positive Long vetId,
                                     @NotNull LocalDateTime scheduledAt, @Size(max = 10000) String chiefComplaint) {}
    public record StatusRequest(@NotNull VisitStatus status) {}

    public record AppointmentResponse(Long id, Long petId, String petName, Long vetId, String vetName,
                                      LocalDateTime scheduledAt, VisitStatus status, String chiefComplaint,
                                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        static AppointmentResponse from(Visit visit) {
            return new AppointmentResponse(visit.getId(), visit.getPet().getId(), visit.getPet().getName(),
                    visit.getVet().getId(), visit.getVet().getName(), visit.getScheduledAt(), visit.getStatus(),
                    visit.getChiefComplaint(), visit.getCreatedAt(), visit.getUpdatedAt());
        }
    }
}
