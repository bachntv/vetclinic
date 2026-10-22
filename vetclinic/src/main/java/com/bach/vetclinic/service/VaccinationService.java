package com.bach.vetclinic.service;

import com.bach.vetclinic.entity.Pet;
import com.bach.vetclinic.entity.Vaccination;
import com.bach.vetclinic.repository.PetRepository;
import com.bach.vetclinic.repository.VaccinationRepository;
import com.bach.vetclinic.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class VaccinationService {
    private final VaccinationRepository vaccinationRepository;
    private final PetRepository petRepository;
    private final com.bach.vetclinic.repository.VaccineTypeRepository vaccineTypes;

    public VaccinationService(VaccinationRepository vaccinationRepository, PetRepository petRepository,com.bach.vetclinic.repository.VaccineTypeRepository vaccineTypes) {
        this.vaccinationRepository = vaccinationRepository;
        this.petRepository = petRepository;
        this.vaccineTypes=vaccineTypes;
    }

    @Transactional(readOnly = true)
    public List<Vaccination> findAll(Long petId) {
        return petId == null ? vaccinationRepository.findAllByOrderByAdministeredAtDesc()
                : vaccinationRepository.findAllByPetIdOrderByAdministeredAtDesc(petId);
    }

    @Transactional(readOnly = true)
    public Vaccination findById(Long id) {
        return vaccinationRepository.findWithPetById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bản ghi tiêm chủng có mã " + id));
    }

    public Vaccination create(Long petId, String vaccineType, LocalDateTime administeredAt,
                              String lotNumber, LocalDate nextDueDate, String administeredBy) {
        return create(petId,vaccineType,administeredAt,lotNumber,nextDueDate,administeredBy,null,null);
    }
    public Vaccination create(Long petId, String vaccineType, LocalDateTime administeredAt,
                              String lotNumber, LocalDate nextDueDate, String administeredBy,Long vaccineId,String notes) {
        var catalog=resolve(vaccineId,null);vaccineType=label(catalog,vaccineType);
        checkDueDate(administeredAt, nextDueDate);
        Pet pet = findActivePet(petId);
        Vaccination vaccination = new Vaccination(pet, vaccineType, administeredAt);
        vaccination.update(pet, vaccineType, administeredAt, lotNumber, nextDueDate, administeredBy);
        vaccination.setCatalog(catalog,notes);
        return vaccinationRepository.save(vaccination);
    }

    public Vaccination update(Long id, Long petId, String vaccineType, LocalDateTime administeredAt,
                              String lotNumber, LocalDate nextDueDate, String administeredBy) {
        return update(id,petId,vaccineType,administeredAt,lotNumber,nextDueDate,administeredBy,null,null);
    }
    public Vaccination update(Long id, Long petId, String vaccineType, LocalDateTime administeredAt,
                              String lotNumber, LocalDate nextDueDate, String administeredBy,Long vaccineId,String notes) {
        checkDueDate(administeredAt, nextDueDate);
        Vaccination vaccination = findById(id);
        var catalog=resolve(vaccineId,vaccination);
        vaccineType=catalog!=null&&vaccination.getVaccine()!=null&&catalog.getId().equals(vaccination.getVaccine().getId())?vaccination.getVaccineType():label(catalog,vaccineType);
        vaccination.update(findActivePet(petId), vaccineType, administeredAt, lotNumber, nextDueDate, administeredBy);
        vaccination.setCatalog(catalog,notes);
        return vaccination;
    }

    public void delete(Long id) { vaccinationRepository.delete(findById(id)); }

    private Pet findActivePet(Long id) {
        return petRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thú cưng đang hoạt động có mã " + id));
    }
    private void checkDueDate(LocalDateTime administeredAt, LocalDate nextDueDate) {
        if (nextDueDate != null && !nextDueDate.isAfter(administeredAt.toLocalDate()))
            throw new com.bach.vetclinic.web.ConflictException("Ngày tiêm tiếp theo phải sau ngày tiêm");
    }
    private com.bach.vetclinic.entity.VaccineType resolve(Long id,Vaccination current){
        if(id==null)return current==null?null:current.getVaccine();
        var v=vaccineTypes.findById(id).orElseThrow(()->new NotFoundException("Không tìm thấy vaccine trong danh mục"));
        if(!v.isActive()&&(current==null||current.getVaccine()==null||!current.getVaccine().getId().equals(id)))
            throw new com.bach.vetclinic.web.ConflictException("Vaccine đã ngừng sử dụng");
        return v;
    }
    private String label(com.bach.vetclinic.entity.VaccineType catalog,String legacy){
        if(catalog!=null)return catalog.getName();
        if(legacy==null||legacy.isBlank())throw new com.bach.vetclinic.web.ConflictException("Cần chọn vaccine trong danh mục");
        return legacy.trim();
    }
}
