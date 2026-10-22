package com.bach.vetclinic.service;

import com.bach.vetclinic.entity.Owner;
import com.bach.vetclinic.repository.OwnerRepository;
import com.bach.vetclinic.repository.PetRepository;
import com.bach.vetclinic.web.ConflictException;
import com.bach.vetclinic.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OwnerService {
    @Transactional(readOnly = true)
    public List<Owner> findIncludingArchived() { return ownerRepository.findAll(); }
    public Owner activate(Long id) {
        Owner owner = ownerRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy chủ nuôi"));
        owner.activate(); return owner;
    }
    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;

    public OwnerService(OwnerRepository ownerRepository, PetRepository petRepository) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
    }

    @Transactional(readOnly = true)
    public List<Owner> findAllActive() {
        return ownerRepository.findAllByArchivedFalseOrderByLastNameAscFirstNameAsc();
    }

    @Transactional(readOnly = true)
    public Owner findActiveById(Long id) {
        return ownerRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ nuôi có mã " + id));
    }

    public Owner create(String firstName, String lastName, String phone, String email, String address) {
        return ownerRepository.save(new Owner(firstName, lastName, phone, email, address));
    }

    public Owner update(Long id, String firstName, String lastName, String phone, String email, String address) {
        Owner owner = findActiveById(id);
        owner.updateContactInfo(firstName, lastName, phone, email, address);
        return owner;
    }

    public void archive(Long id) {
        Owner owner = findActiveById(id);
        if (petRepository.existsByOwnerIdAndArchivedFalse(id)) {
            throw new ConflictException("Không thể lưu trữ chủ nuôi khi còn thú cưng đang hoạt động");
        }
        owner.archive();
    }

}
