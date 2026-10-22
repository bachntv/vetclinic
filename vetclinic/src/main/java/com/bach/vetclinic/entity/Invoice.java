package com.bach.vetclinic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "vat_rate", nullable = false, precision = 6, scale = 4)
    private BigDecimal vatRate;

    @Column(name = "vat_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal vatAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @OneToMany(mappedBy = "invoice", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    protected Invoice() {
    }

    public Invoice(Visit visit, LocalDateTime issuedAt, BigDecimal vatRate) {
        this.visit = visit;
        this.issuedAt = issuedAt;
        this.vatRate = vatRate;
        this.subtotal = BigDecimal.ZERO.setScale(2);
        this.vatAmount = BigDecimal.ZERO.setScale(2);
        this.total = BigDecimal.ZERO.setScale(2);
    }

    public Visit getVisit() { return visit; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getVatRate() { return vatRate; }
    public BigDecimal getVatAmount() { return vatAmount; }
    public BigDecimal getTotal() { return total; }
    public InvoiceStatus getStatus() { return status; }
    public List<InvoiceItem> getItems() { return List.copyOf(items); }

    public void replaceItems(List<InvoiceItem> replacementItems) {
        items.clear();
        items.addAll(replacementItems);
        recalculateTotals();
    }

    public void addItem(InvoiceItem item) { items.add(item); }

    public void setVatRate(BigDecimal vatRate) { this.vatRate = vatRate; }

    public void recalculateTotals() {
        subtotal = items.stream().map(InvoiceItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        vatAmount = subtotal.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
        total = subtotal.add(vatAmount).setScale(2, RoundingMode.HALF_UP);
    }

    public void setStatus(InvoiceStatus status) { this.status = status; }
}
