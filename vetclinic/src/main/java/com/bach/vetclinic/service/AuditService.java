package com.bach.vetclinic.service;
import com.bach.vetclinic.entity.AuditEvent;
import com.bach.vetclinic.repository.AuditEventRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.json.JsonMapper;

@Service
public class AuditService {
 private final AuditEventRepository repository;
 private final JsonMapper json=JsonMapper.builder().build();
 public AuditService(AuditEventRepository repository){this.repository=repository;}
 @Transactional(propagation=Propagation.MANDATORY)
 public void record(String action,String type,Long id,String reason,Object before,Object after){
  var auth=SecurityContextHolder.getContext().getAuthentication();
  String actor=auth!=null&&auth.isAuthenticated()?auth.getName():"SYSTEM";
  repository.save(new AuditEvent(actor,action,type,id,reason,before==null?null:json.writeValueAsString(before),after==null?null:json.writeValueAsString(after)));
 }
}
