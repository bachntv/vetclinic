package com.bach.vetclinic.service;

import com.bach.vetclinic.entity.*;
import com.bach.vetclinic.repository.InvoiceRepository;
import com.bach.vetclinic.repository.VisitRepository;
import com.bach.vetclinic.web.ConflictException;
import com.bach.vetclinic.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final VisitRepository visitRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, VisitRepository visitRepository) {
        this.invoiceRepository = invoiceRepository;
        this.visitRepository = visitRepository;
    }

    @Transactional(readOnly = true)
    public List<Invoice> findAll() { return invoiceRepository.findAllByOrderByIssuedAtDesc(); }

    @Transactional(readOnly = true)
    public Invoice findById(Long id) {
        return invoiceRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn có mã " + id));
    }

    public Invoice create(Long visitId, BigDecimal vatRate, List<ItemData> items) {
        Visit visit = visitRepository.findWithDetailsById(visitId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lượt khám có mã " + visitId));
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            throw new ConflictException("Không thể lập hóa đơn cho lượt khám đã hủy");
        }
        Invoice invoice = new Invoice(visit, LocalDateTime.now(), normalizeVatRate(vatRate));
        invoice.replaceItems(toEntities(invoice, items));
        checkTotal(invoice);
        return invoiceRepository.save(invoice);
    }

    public Invoice updateDraftItems(Long id, BigDecimal vatRate, List<ItemData> items) {
        Invoice invoice = findById(id);
        requireDraft(invoice);
        invoice.setVatRate(normalizeVatRate(vatRate));
        invoice.replaceItems(toEntities(invoice, items));
        checkTotal(invoice);
        return invoice;
    }

    public Invoice changeStatus(Long id, InvoiceStatus nextStatus) {
        Invoice invoice = findById(id);
        if (nextStatus == InvoiceStatus.SENT) {
            requireDraft(invoice);
            if (invoice.getVisit().getStatus() != VisitStatus.COMPLETED) {
                throw new ConflictException("Chỉ có thể gửi hóa đơn sau khi hoàn thành lượt khám");
            }
            if (invoice.getItems().isEmpty()) {
                throw new ConflictException("Hóa đơn cần có ít nhất một chi tiết trước khi gửi");
            }
        } else if (nextStatus == InvoiceStatus.PAID) {
            if (invoice.getStatus() != InvoiceStatus.SENT) {
                throw new ConflictException("Chỉ có thể thanh toán hóa đơn đã gửi");
            }
        } else {
            throw new ConflictException("Không thể chuyển hóa đơn trở lại trạng thái nháp");
        }
        invoice.setStatus(nextStatus);
        return invoice;
    }

    private List<InvoiceItem> toEntities(Invoice invoice, List<ItemData> items) {
        return items.stream().map(item -> {
            BigDecimal quantity = item.quantity().setScale(2, RoundingMode.HALF_UP);
            BigDecimal unitPrice = item.unitPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            return new InvoiceItem(invoice, item.description().trim(), item.category(), quantity, unitPrice, lineTotal);
        }).toList();
    }

    private BigDecimal normalizeVatRate(BigDecimal vatRate) {
        return vatRate.setScale(4, RoundingMode.HALF_UP);
    }
    private void checkTotal(Invoice invoice) {
        if (invoice.getTotal().compareTo(new BigDecimal("9999999999.99")) > 0)
            throw new ConflictException("Tổng hóa đơn vượt giới hạn 9.999.999.999,99");
    }

    private void requireDraft(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ConflictException("Chỉ có thể sửa hóa đơn đang ở trạng thái nháp");
        }
    }

    public record ItemData(String description, InvoiceItemCategory category, BigDecimal quantity, BigDecimal unitPrice) {}
}
