package com.bach.vetclinic.entity;
import jakarta.persistence.*;
@Entity
@Table(name="support_requests")
public class SupportRequest extends BaseEntity {
    @Column(nullable=false,length=255) private String requestedBy;
    @Column(nullable=false,length=200) private String subject;
    @Column(nullable=false,columnDefinition="text") private String description;
    @Column(nullable=false,length=20) private String status="OPEN";
    protected SupportRequest() {}
    public SupportRequest(String email,String subject,String description) { this.requestedBy=email; this.subject=subject; this.description=description; }
    public String getRequestedBy(){return requestedBy;} public String getSubject(){return subject;}
    public String getDescription(){return description;} public String getStatus(){return status;}
    public void setStatus(String status){this.status=status;}
}
