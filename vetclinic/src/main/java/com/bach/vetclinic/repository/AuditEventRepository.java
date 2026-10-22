package com.bach.vetclinic.repository;
import com.bach.vetclinic.entity.AuditEvent;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;
public interface AuditEventRepository extends JpaRepository<AuditEvent,Long> {
 @Query("select a from AuditEvent a where (:action='' or a.action=:action) and (:entityType='' or a.entityType=:entityType) and (:entityId is null or a.entityId=:entityId) and (:q='' or lower(a.actor) like lower(concat('%',:q,'%')))")
 Page<AuditEvent> search(@Param("action")String action,@Param("entityType")String entityType,@Param("entityId")Long entityId,@Param("q")String q,Pageable page);
}
