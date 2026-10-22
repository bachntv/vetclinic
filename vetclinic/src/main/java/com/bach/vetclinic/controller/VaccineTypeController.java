package com.bach.vetclinic.controller;
import com.bach.vetclinic.entity.VaccineType;
import com.bach.vetclinic.service.VaccineTypeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
@RestController @RequestMapping("/api/vaccine-types")
public class VaccineTypeController {
    private final VaccineTypeService service;
    public VaccineTypeController(VaccineTypeService service){this.service=service;}
    @GetMapping public List<VaccineType> list(){return service.list();}
    @GetMapping("/{id}") public VaccineType get(@PathVariable Long id){return service.get(id);}
    @PostMapping @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public VaccineType create(@Valid @RequestBody Request r){return service.create(r.name(),r.manufacturer(),r.description());}
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public VaccineType update(@PathVariable Long id,@Valid @RequestBody Request r){return service.update(id,r.name(),r.manufacturer(),r.description());}
    @PatchMapping("/{id}/active") @PreAuthorize("hasRole('ADMIN')")
    public VaccineType active(@PathVariable Long id,@Valid @RequestBody Active r){return service.active(id,r.active());}
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long id){service.active(id,false);}
    public record Request(@NotBlank @Size(max=120) String name,@Size(max=200) String manufacturer,@Size(max=5000) String description){}
    public record Active(@NotNull Boolean active){}
}
