package com.bach.vetclinic.controller;
import com.bach.vetclinic.entity.KnowledgeDocument;
import com.bach.vetclinic.repository.KnowledgeDocumentRepository;
import com.bach.vetclinic.web.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@RestController @RequestMapping("/api/knowledge") @Transactional
public class KnowledgeController {
    private final KnowledgeDocumentRepository repository;
    public KnowledgeController(KnowledgeDocumentRepository repository){this.repository=repository;}
    @GetMapping @Transactional(readOnly=true)
    public List<KnowledgeDocument> list(){return repository.findAllByOrderByUpdatedAtDesc();}
    @PostMapping @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public KnowledgeDocument create(@Valid @RequestBody Request r){return repository.save(new KnowledgeDocument(r.title().trim(),r.content().trim(),r.source()));}
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public KnowledgeDocument update(@PathVariable Long id,@Valid @RequestBody Request r){var d=find(id);d.update(r.title().trim(),r.content().trim(),r.source());return d;}
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){repository.delete(find(id));}
    private KnowledgeDocument find(Long id){return repository.findById(id).orElseThrow(()->new NotFoundException("Không tìm thấy tài liệu"));}
    public record Request(@NotBlank @Size(max=200) String title,@NotBlank @Size(max=100000) String content,@Size(max=1000) String source){}
}
