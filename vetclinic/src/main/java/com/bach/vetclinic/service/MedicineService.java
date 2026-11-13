package com.bach.vetclinic.service;

import com.bach.vetclinic.entity.Medicine;
import com.bach.vetclinic.repository.MedicineRepository;
import com.bach.vetclinic.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class MedicineService {
    private final MedicineRepository medicineRepository;

    public MedicineService(MedicineRepository medicineRepository) { this.medicineRepository = medicineRepository; }

    @Transactional(readOnly = true)
    public List<Medicine> findAll() { return medicineRepository.findAllByOrderByNameAsc(); }

    @Transactional(readOnly = true)
    public Medicine findById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thuốc có mã " + id));
    }

    public Medicine create(String name, String activeIngredient, String form, String unit,
                           BigDecimal stockQuantity, BigDecimal reorderLevel, BigDecimal unitPrice) {
        validateStock(unit, stockQuantity, reorderLevel);
        return medicineRepository.save(new Medicine(name, activeIngredient, form, unit,
                stockQuantity, reorderLevel, unitPrice));
    }

    public Medicine update(Long id, String name, String activeIngredient, String form, String unit,
                           BigDecimal stockQuantity, BigDecimal reorderLevel, BigDecimal unitPrice) {
        Medicine medicine = medicineRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Không tìm thấy thuốc"));
        if (!medicine.getUnit().equalsIgnoreCase(unit.strip()) ||
                stockQuantity.compareTo(medicine.getStockQuantity()) != 0 ||
                reorderLevel.compareTo(medicine.getReorderLevel()) != 0) {
            validateStock(unit, stockQuantity, reorderLevel);
        }
        medicine.updateDetails(name, activeIngredient, form, unit, stockQuantity, reorderLevel, unitPrice);
        return medicine;
    }

    private void validateStock(String unit, BigDecimal stockQuantity, BigDecimal reorderLevel) {
        if (Medicine.isWholeUnit(unit) &&
                (stockQuantity.stripTrailingZeros().scale() > 0 || reorderLevel.stripTrailingZeros().scale() > 0)) {
            throw new com.bach.vetclinic.web.ConflictException("Tồn kho và ngưỡng cảnh báo phải là số nguyên theo đơn vị " + unit);
        }
    }

    public Medicine setActive(Long id, boolean active) {
        Medicine medicine = findById(id);
        medicine.setActive(active);
        return medicine;
    }
    public Medicine create(String name,String activeIngredient,String form,String unit,BigDecimal stockQuantity,BigDecimal reorderLevel,BigDecimal unitPrice,String description){
        var medicine=create(name,activeIngredient,form,unit,stockQuantity,reorderLevel,unitPrice);medicine.setDescription(description);return medicine;
    }
    public Medicine update(Long id,String name,String activeIngredient,String form,String unit,BigDecimal stockQuantity,BigDecimal reorderLevel,BigDecimal unitPrice,String description){
        var medicine=update(id,name,activeIngredient,form,unit,stockQuantity,reorderLevel,unitPrice);medicine.setDescription(description);return medicine;
    }
}
