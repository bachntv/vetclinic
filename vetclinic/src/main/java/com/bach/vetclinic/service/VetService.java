package com.bach.vetclinic.service;

import com.bach.vetclinic.entity.Vet;
import com.bach.vetclinic.repository.VetRepository;
import com.bach.vetclinic.web.ConflictException;
import com.bach.vetclinic.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VetService {
    @Transactional(readOnly = true)
    public List<Vet> findAll() { return vetRepository.findAll(); }
    public Vet activate(Long id) {
        Vet vet = vetRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy bác sĩ"));
        vet.setActive(true); return vet;
    }
    private final VetRepository vetRepository;
    private final com.bach.vetclinic.repository.UserRepository users;

    public VetService(VetRepository vetRepository,com.bach.vetclinic.repository.UserRepository users) { this.vetRepository = vetRepository; this.users=users; }

    @Transactional(readOnly = true)
    public List<Vet> findActiveVets() { return vetRepository.findAllByActiveTrueOrderByNameAsc(); }

    @Transactional(readOnly = true)
    public Vet findActiveById(Long id) {
        return vetRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bác sĩ đang hoạt động có mã " + id));
    }

    public Vet create(String name, String specialty, String licenseNo, String workHours) {
        ensureLicenseUnique(licenseNo, null);
        return vetRepository.save(new Vet(name, specialty, licenseNo, workHours));
    }

    public Vet update(Long id, String name, String specialty, String licenseNo, String workHours) {
        Vet vet = findActiveById(id);
        ensureLicenseUnique(licenseNo, id);
        vet.updateProfile(name, specialty, licenseNo, workHours);
        users.findByVetId(id).ifPresent(u->u.updateFullName(name));
        return vet;
    }

    public void deactivate(Long id) { findActiveById(id).setActive(false); users.findByVetId(id).ifPresent(u->u.setEnabled(false)); }

    private void ensureLicenseUnique(String licenseNo, Long currentId) {
        if (licenseNo == null || licenseNo.isBlank()) return;
        vetRepository.findByLicenseNoIgnoreCase(licenseNo.trim())
                .filter(vet -> currentId == null || !vet.getId().equals(currentId))
                .ifPresent(vet -> {
                    throw new ConflictException("Số giấy phép hành nghề đã được sử dụng");
                });
    }
}
