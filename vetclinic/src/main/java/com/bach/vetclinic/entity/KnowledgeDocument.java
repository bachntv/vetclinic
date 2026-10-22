package com.bach.vetclinic.entity;
import jakarta.persistence.*;
/** Curated clinic knowledge; patient records remain in their domain tables. */
@Entity @Table(name="knowledge_documents")
public class KnowledgeDocument extends BaseEntity {
    @Column(nullable=false,length=200) private String title;
    @Column(nullable=false,columnDefinition="text") private String content;
    @Column(length=1000) private String source;
    protected KnowledgeDocument(){}
    public KnowledgeDocument(String title,String content,String source){update(title,content,source);}
    public void update(String title,String content,String source){this.title=title;this.content=content;this.source=source;}
    public String getTitle(){return title;} public String getContent(){return content;} public String getSource(){return source;}
}
