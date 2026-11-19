package com.bach.vetclinic.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "clinic_services")
public class ClinicService extends BaseEntity {
    @Column(nullable = false, length = 160, unique = true)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvoiceItemCategory category;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false)
    private boolean active = true;

    protected ClinicService() {}
    public ClinicService(String name, InvoiceItemCategory category, BigDecimal price, String description) {
        update(name, category, price, description);
    }
    public void update(String name, InvoiceItemCategory category, BigDecimal price, String description) {
        this.name = name.trim();
        this.category = category;
        this.unitPrice = price;
        this.description = description;
    }
    public Long serviceId() { return getId(); }
    public String getName() { return name; }
    public InvoiceItemCategory getCategory() { return category; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
}
