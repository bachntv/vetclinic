package com.bach.vetclinic.service;

import com.bach.vetclinic.entity.*;
import com.bach.vetclinic.repository.MedicineRepository;
import com.bach.vetclinic.repository.PrescriptionRepository;
import com.bach.vetclinic.repository.VisitRepository;
import com.bach.vetclinic.web.ConflictException;
import com.bach.vetclinic.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class PrescriptionService {
    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository;
    private final VisitRepository visitRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository, MedicineRepository medicineRepository,
                               VisitRepository visitRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.medicineRepository = medicineRepository;
        this.visitRepository = visitRepository;
    }

    @Transactional(readOnly = true)
    public Prescription findByVisitId(Long visitId) {
        return prescriptionRepository.findWithDetailsByVisitId(visitId)
                .orElseThrow(() -> new NotFoundException("Lượt khám chưa có đơn thuốc"));
    }
    @Transactional(readOnly=true)
    public List<Prescription> history(Long petId){return prescriptionRepository.findAllByVisitPetIdOrderByCreatedAtDesc(petId);}

    public Prescription create(Long visitId, String notes, List<ItemData> items) {
        Visit visit = visitRepository.findWithDetailsByIdForUpdate(visitId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lượt khám có mã " + visitId));
        if (visit.getStatus() != VisitStatus.IN_EXAM) {
            throw new ConflictException("Chỉ có thể kê đơn khi lượt khám đang ở trạng thái khám");
        }
        if (prescriptionRepository.findWithDetailsByVisitId(visitId).isPresent()) {
            throw new ConflictException("Lượt khám này đã có đơn thuốc");
        }

        Prescription prescription = new Prescription(visit, clean(notes));
        for (ItemData data : items) {
            Medicine medicine = medicineRepository.findByIdForUpdate(data.medicineId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy thuốc có mã " + data.medicineId()));
            if (!medicine.isActive()) throw new ConflictException("Thuốc đã ngừng sử dụng: " + medicine.getName());
            BigDecimal quantity = data.quantity();
            if (medicine.getStockQuantity().compareTo(quantity) < 0) {
                throw new ConflictException("Không đủ tồn kho thuốc " + medicine.getName()
                        + "; còn " + medicine.getStockQuantity() + " " + medicine.getUnit());
            }
            medicine.deductStock(quantity);
            prescription.addItem(new PrescriptionItem(prescription, medicine, data.dosage(), data.frequency(),
                    data.durationDays(), quantity, medicine.getUnitPrice(), clean(data.instructions())));
        }
        return prescriptionRepository.save(prescription);
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record ItemData(Long medicineId, String dosage, String frequency, Integer durationDays,
                           BigDecimal quantity, String instructions) {}
}
