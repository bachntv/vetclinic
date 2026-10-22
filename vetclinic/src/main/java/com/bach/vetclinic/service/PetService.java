package com.bach.vetclinic.service;

import com.bach.vetclinic.entity.Owner;
import com.bach.vetclinic.entity.Pet;
import com.bach.vetclinic.repository.OwnerRepository;
import com.bach.vetclinic.repository.PetRepository;
import com.bach.vetclinic.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class PetService {
    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;

    public PetService(PetRepository petRepository, OwnerRepository ownerRepository) {
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
    }

    @Transactional(readOnly = true)
    public List<Pet> findAllActive() {
        return petRepository.findAllByArchivedFalseOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Pet> findAllActiveByOwner(Long ownerId) {
        findActiveOwner(ownerId);
        return petRepository.findAllByOwnerIdAndArchivedFalseOrderByNameAsc(ownerId);
    }

    @Transactional(readOnly = true)
    public Pet findActiveById(Long id) {
        return petRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thú cưng có mã " + id));
    }

    public Pet create(Long ownerId, String name, String species, String breed, String speciesNote,
                      LocalDate birthDate, String sex, BigDecimal weightKg, String allergies, String chronicConditions) {
        Owner owner = findActiveOwner(ownerId);
        Pet pet = new Pet(owner, name, species);
        pet.update(owner, name, species, breed, speciesNote, birthDate, sex, weightKg, allergies, chronicConditions);
        return petRepository.save(pet);
    }

    public Pet update(Long id, Long ownerId, String name, String species, String breed, String speciesNote,
                      LocalDate birthDate, String sex, BigDecimal weightKg, String allergies, String chronicConditions) {
        Pet pet = findActiveById(id);
        Owner owner = findActiveOwner(ownerId);
        pet.update(owner, name, species, breed, speciesNote, birthDate, sex, weightKg, allergies, chronicConditions);
        return pet;
    }

    public void archive(Long id) {
        findActiveById(id).archive();
    }
    @Transactional(readOnly = true)
    public List<Pet> findIncludingArchived() { return petRepository.findAll(); }
    public Pet activate(Long id) {
        Pet pet = petRepository.findWithOwnerById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy thú cưng"));
        findActiveOwner(pet.getOwner().getId()); pet.activate(); return pet;
    }

    private Owner findActiveOwner(Long ownerId) {
        return ownerRepository.findByIdAndArchivedFalse(ownerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chủ nuôi đang hoạt động có mã " + ownerId));
    }
}
