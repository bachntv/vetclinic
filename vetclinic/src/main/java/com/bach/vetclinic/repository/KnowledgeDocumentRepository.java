package com.bach.vetclinic.repository;
import com.bach.vetclinic.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument,Long>{List<KnowledgeDocument> findAllByOrderByUpdatedAtDesc();}
