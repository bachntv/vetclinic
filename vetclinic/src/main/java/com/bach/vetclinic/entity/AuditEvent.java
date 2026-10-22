package com.bach.vetclinic.entity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="audit_events",indexes={@Index(name="idx_audit_occurred",columnList="occurred_at"),@Index(name="idx_audit_entity",columnList="entity_type,entity_id")})
public class AuditEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="occurred_at",nullable=false) private Instant occurredAt;
 @Column(nullable=false,length=255) private String actor;
 @Column(nullable=false,length=100) private String action;
 @Column(name="entity_type",nullable=false,length=50) private String entityType;
 @Column(name="entity_id",nullable=false) private Long entityId;
 @Column(length=1000) private String reason;
 @Column(name="before_state",columnDefinition="text") private String beforeState;
 @Column(name="after_state",columnDefinition="text") private String afterState;
 protected AuditEvent(){}
 public AuditEvent(String actor,String action,String type,Long entityId,String reason,String before,String after){this.occurredAt=Instant.now();this.actor=actor;this.action=action;this.entityType=type;this.entityId=entityId;this.reason=reason;this.beforeState=before;this.afterState=after;}
 public Long getId(){return id;} public Instant getOccurredAt(){return occurredAt;} public String getActor(){return actor;} public String getAction(){return action;} public String getEntityType(){return entityType;} public Long getEntityId(){return entityId;} public String getReason(){return reason;} public String getBeforeState(){return beforeState;} public String getAfterState(){return afterState;}
}
