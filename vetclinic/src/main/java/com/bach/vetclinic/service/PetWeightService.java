package com.bach.vetclinic.service;

import com.bach.vetclinic.entity.Pet;
import com.bach.vetclinic.entity.PetWeightRecord;
import com.bach.vetclinic.repository.PetRepository;
import com.bach.vetclinic.repository.PetWeightRecordRepository;
import com.bach.vetclinic.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PetWeightService {
    private final PetWeightRecordRepository weightRepository;
    private final PetRepository petRepository;

    public PetWeightService(PetWeightRecordRepository weightRepository, PetRepository petRepository) {
        this.weightRepository = weightRepository;
        this.petRepository = petRepository;
    }

    @Transactional(readOnly = true)
    public List<PetWeightRecord> findAllForPet(Long petId) {
        if (!petRepository.existsById(petId)) throw new NotFoundException("Không tìm thấy thú cưng có mã " + petId);
        return weightRepository.findAllByPetIdOrderByRecordedAtDesc(petId);
    }

    public PetWeightRecord record(Long petId, BigDecimal weightKg, LocalDateTime recordedAt, String note) {
        Pet pet = petRepository.findByIdAndArchivedFalse(petId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thú cưng đang hoạt động có mã " + petId));
        var history = weightRepository.findAllByPetIdOrderByRecordedAtDesc(petId);
        if (history.isEmpty() || !recordedAt.isBefore(history.getFirst().getRecordedAt())) pet.recordWeight(weightKg);
        return weightRepository.save(new PetWeightRecord(pet, weightKg, recordedAt, note));
    }
}
