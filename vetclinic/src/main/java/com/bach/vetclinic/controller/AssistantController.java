package com.bach.vetclinic.controller;
import com.bach.vetclinic.service.AssistantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController @RequestMapping("/api/assistant")
@PreAuthorize("hasAnyRole('ADMIN','VET')")
public class AssistantController {
    private final AssistantService service;
    public AssistantController(AssistantService service){this.service=service;}
    @PostMapping("/ask")
    public AssistantService.Answer ask(@Valid @RequestBody Question q){return service.ask(q.petId(),q.question().trim(),q.generate());}
    public record Question(@NotNull @Positive Long petId,@NotBlank @Size(min=2,max=1000) String question,boolean generate){}
}
