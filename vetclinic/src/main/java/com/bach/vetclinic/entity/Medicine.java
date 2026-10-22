package com.bach.vetclinic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** A medicine sold or prescribed by the clinic. */
@Entity
@Table(name = "medicines")
public class Medicine extends BaseEntity {

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 160)
    private String activeIngredient;

    @Column(nullable = false, length = 80)
    private String form;

    @Column(nullable = false, length = 40)
    private String unit;

    @Column(name = "stock_quantity", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockQuantity;

    @Column(name = "reorder_level", nullable = false, precision = 12, scale = 2)
    private BigDecimal reorderLevel;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private boolean active = true;
    @Column(columnDefinition="text") private String description;
    public String getDescription(){return description;}
    public void setDescription(String description){this.description=description;}

    protected Medicine() {}

    public Medicine(String name, String activeIngredient, String form, String unit,
                    BigDecimal stockQuantity, BigDecimal reorderLevel, BigDecimal unitPrice) {
        updateDetails(name, activeIngredient, form, unit, stockQuantity, reorderLevel, unitPrice);
    }

    public void updateDetails(String name, String activeIngredient, String form, String unit,
                              BigDecimal stockQuantity, BigDecimal reorderLevel, BigDecimal unitPrice) {
        this.name = name;
        this.activeIngredient = activeIngredient;
        this.form = form;
        this.unit = unit;
        this.stockQuantity = stockQuantity;
        this.reorderLevel = reorderLevel;
        this.unitPrice = unitPrice;
    }

    public void deductStock(BigDecimal quantity) { stockQuantity = stockQuantity.subtract(quantity); }
    public void restoreStock(BigDecimal quantity) { stockQuantity = stockQuantity.add(quantity); }
    public void setActive(boolean active) { this.active = active; }
    public Long getId() { return super.getId(); }
    public String getName() { return name; }
    public String getActiveIngredient() { return activeIngredient; }
    public String getForm() { return form; }
    public String getUnit() { return unit; }
    public BigDecimal getStockQuantity() { return stockQuantity; }
    public BigDecimal getReorderLevel() { return reorderLevel; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public boolean isActive() { return active; }
}
