package com.bach.vetclinic.controller;

import com.bach.vetclinic.controller.VisitController.VisitResponse;
import com.bach.vetclinic.service.VisitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PetVisitController {
    private final VisitService visitService;

    public PetVisitController(VisitService visitService) { this.visitService = visitService; }

    @GetMapping("/api/pets/{petId}/visits")
    public List<VisitResponse> visits(@PathVariable Long petId,@org.springframework.web.bind.annotation.RequestParam(required=false) java.time.LocalDate from,@org.springframework.web.bind.annotation.RequestParam(required=false) java.time.LocalDate to,@org.springframework.web.bind.annotation.RequestParam(required=false) String q) {
        return visitService.filter(visitService.findAllForPet(petId),from,to,q).stream().map(VisitResponse::from).toList();
    }

    @GetMapping("/api/pets/{petId}/medical-records")
    public List<VisitResponse> medicalRecords(@PathVariable Long petId,@org.springframework.web.bind.annotation.RequestParam(required=false) java.time.LocalDate from,@org.springframework.web.bind.annotation.RequestParam(required=false) java.time.LocalDate to,@org.springframework.web.bind.annotation.RequestParam(required=false) String q) {
        return visitService.filter(visitService.findMedicalRecordsForPet(petId),from,to,q).stream().map(VisitResponse::from).toList();
    }
}
