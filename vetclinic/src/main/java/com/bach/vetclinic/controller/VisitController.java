package com.bach.vetclinic.controller;

import com.bach.vetclinic.entity.Visit;
import com.bach.vetclinic.entity.VisitStatus;
import com.bach.vetclinic.service.VisitService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/visits")
public class VisitController {
    private final VisitService visitService;

    public VisitController(VisitService visitService) { this.visitService = visitService; }

    @GetMapping
    public List<VisitResponse> list(@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,@RequestParam(required=false) String q) {
        return visitService.filter(visitService.findAllVisits(),from,to,q).stream().map(VisitResponse::from).toList();
    }
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public VisitResponse create(@Valid @RequestBody VisitRequest request){return VisitResponse.from(visitService.createEncounter(request.petId(),request.vetId(),request.scheduledAt(),clean(request.chiefComplaint())));}
    public record VisitRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long petId,@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long vetId,
                               LocalDateTime scheduledAt,@Size(max=10000) String chiefComplaint){}

    @GetMapping("/{id}")
    public VisitResponse get(@PathVariable Long id) { return VisitResponse.from(visitService.findById(id)); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VET')")
    public VisitResponse updateMedicalRecord(@PathVariable Long id,
                                             @Valid @RequestBody MedicalRecordRequest request) {
        return VisitResponse.from(visitService.updateMedicalRecord(id, clean(request.diagnosis()),
                clean(request.treatmentNotes()), request.followUpDate(),clean(request.symptoms())));
    }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record MedicalRecordRequest(@Size(max = 10000) String diagnosis,
                                       @Size(max = 20000) String treatmentNotes,
                                       LocalDate followUpDate,@Size(max=10000) String symptoms) {}

    public record VisitResponse(Long id, Long petId, String petName, Long vetId, String vetName,
                                LocalDateTime scheduledAt, VisitStatus status, String chiefComplaint,
                                String diagnosis, String treatmentNotes, LocalDate followUpDate,
                                LocalDateTime createdAt, LocalDateTime updatedAt, List<String> warnings,String symptoms) {
        static VisitResponse from(Visit visit) {
            return new VisitResponse(visit.getId(), visit.getPet().getId(), visit.getPet().getName(),
                    visit.getVet().getId(), visit.getVet().getName(), visit.getScheduledAt(), visit.getStatus(),
                    visit.getChiefComplaint(), visit.getDiagnosis(), visit.getTreatmentNotes(), visit.getFollowUpDate(),
                    visit.getCreatedAt(), visit.getUpdatedAt(), allergyWarnings(visit),visit.getSymptoms());
        }
        private static List<String> allergyWarnings(Visit visit) {
            String allergies = visit.getPet().getAllergies(), notes = visit.getTreatmentNotes();
            if(allergies==null || notes==null)return List.of();
            String lower=notes.toLowerCase(java.util.Locale.ROOT);
            return java.util.Arrays.stream(allergies.split("[,;]")).map(String::trim).filter(a->!a.isBlank()&&lower.contains(a.toLowerCase(java.util.Locale.ROOT)))
                    .map(a->"Nội dung điều trị có nhắc đến chất đã ghi nhận dị ứng: "+a).toList();
        }
    }
}
