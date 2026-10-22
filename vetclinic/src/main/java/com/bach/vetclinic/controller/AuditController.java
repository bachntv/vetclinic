package com.bach.vetclinic.controller;
import com.bach.vetclinic.repository.AuditEventRepository;
import com.bach.vetclinic.entity.AuditEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import java.util.List;
@RestController @RequestMapping("/api/admin/audit") @PreAuthorize("hasRole('ADMIN')")
public class AuditController {
 private final AuditEventRepository repository;
 public AuditController(AuditEventRepository repository){this.repository=repository;}
 @GetMapping public Result list(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(defaultValue="")String action,@RequestParam(defaultValue="")String entityType,@RequestParam(required=false)Long entityId,@RequestParam(defaultValue="")String q){
  if(page<0||size<1||size>100||q.length()>255)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Tham số phân trang hoặc tìm kiếm không hợp lệ");
  var result=repository.search(action,entityType,entityId,q.trim(),PageRequest.of(page,size,Sort.by(Sort.Order.desc("occurredAt"),Sort.Order.desc("id"))));
  return new Result(result.getContent(),result.getTotalElements(),result.getTotalPages(),page);
 }
 public record Result(List<AuditEvent> content,long total,int pages,int page){}
}
