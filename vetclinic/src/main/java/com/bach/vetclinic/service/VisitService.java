package com.bach.vetclinic.service;

import com.bach.vetclinic.entity.Pet;
import com.bach.vetclinic.entity.Vet;
import com.bach.vetclinic.entity.Visit;
import com.bach.vetclinic.entity.VisitStatus;
import com.bach.vetclinic.repository.PetRepository;
import com.bach.vetclinic.repository.VisitRepository;
import com.bach.vetclinic.web.ConflictException;
import com.bach.vetclinic.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class VisitService {
    private final VisitRepository visitRepository;
    private final PetRepository petRepository;
    private final VetService vetService;
    private final com.bach.vetclinic.repository.VetRepository vets;

    public VisitService(VisitRepository visitRepository, PetRepository petRepository, VetService vetService, com.bach.vetclinic.repository.VetRepository vets) {
        this.visitRepository = visitRepository;
        this.petRepository = petRepository;
        this.vetService = vetService;
        this.vets = vets;
    }

    @Transactional(readOnly = true)
    public List<Visit> findAllAppointments() { return visitRepository.findAllByOrderByScheduledAtAsc(); }

    @Transactional(readOnly = true)
    public Visit findById(Long id) {
        return visitRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lịch hẹn có mã " + id));
    }

    @Transactional(readOnly = true)
    public List<Visit> findAllVisits() {
        return visitRepository.findAllByOrderByScheduledAtAsc();
    }

    @Transactional(readOnly = true)
    public List<Visit> findAllForPet(Long petId) {
        ensurePetExists(petId);
        return visitRepository.findAllByPetIdOrderByScheduledAtDesc(petId);
    }

    @Transactional(readOnly = true)
    public List<Visit> findMedicalRecordsForPet(Long petId) {
        ensurePetExists(petId);
        return visitRepository.findAllByPetIdAndStatusInOrderByScheduledAtDesc(
                petId, List.of(VisitStatus.IN_EXAM, VisitStatus.COMPLETED));
    }

    public Visit updateMedicalRecord(Long id, String diagnosis, String treatmentNotes,
                                     java.time.LocalDate followUpDate) {
        Visit visit = findById(id);
        if (visit.getStatus() != VisitStatus.IN_EXAM && visit.getStatus() != VisitStatus.COMPLETED) {
            throw new ConflictException("Chỉ có thể cập nhật hồ sơ khi lượt khám đang hoặc đã khám");
        }
        visit.updateMedicalRecord(diagnosis, treatmentNotes, followUpDate);
        return visit;
    }
    public Visit updateMedicalRecord(Long id,String diagnosis,String treatmentNotes,java.time.LocalDate followUpDate,String symptoms){
        var visit=updateMedicalRecord(id,diagnosis,treatmentNotes,followUpDate);visit.setSymptoms(symptoms);return visit;
    }

    public Visit createEncounter(Long petId,Long vetId,LocalDateTime at,String chiefComplaint){
        at=at==null?LocalDateTime.now():at;
        ensureSlot(vetId,at,-1L,true);
        Visit visit=new Visit(findActivePet(petId),vetService.findActiveById(vetId),at,chiefComplaint);
        visit.setStatus(VisitStatus.CHECKED_IN);return visitRepository.save(visit);
    }

    @Transactional(readOnly=true)
    public List<Visit> filter(List<Visit> records,java.time.LocalDate from,java.time.LocalDate to,String query){
        if(from!=null&&to!=null&&from.isAfter(to))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        String q=query==null?"":query.trim().toLowerCase(java.util.Locale.ROOT);
        return records.stream().filter(v->from==null||!v.getScheduledAt().toLocalDate().isBefore(from)).filter(v->to==null||!v.getScheduledAt().toLocalDate().isAfter(to))
                .filter(v->q.isEmpty()||java.util.stream.Stream.of(v.getPet().getName(),v.getChiefComplaint(),v.getSymptoms(),v.getDiagnosis(),v.getTreatmentNotes()).filter(java.util.Objects::nonNull).anyMatch(t->t.toLowerCase(java.util.Locale.ROOT).contains(q))).toList();
    }

    public Visit create(Long petId, Long vetId, LocalDateTime scheduledAt, String chiefComplaint) {
        ensureSlot(vetId, scheduledAt, -1L);
        Pet pet = findActivePet(petId);
        Vet vet = vetService.findActiveById(vetId);
        return visitRepository.save(new Visit(pet, vet, scheduledAt, chiefComplaint));
    }

    public Visit reschedule(Long id, Long petId, Long vetId, LocalDateTime scheduledAt, String chiefComplaint) {
        ensureSlot(vetId, scheduledAt, id);
        Visit visit = findById(id);
        requireStatus(visit, VisitStatus.SCHEDULED, "Chỉ có thể sửa lịch hẹn đang chờ");
        visit.reschedule(findActivePet(petId), vetService.findActiveById(vetId), scheduledAt, chiefComplaint);
        return visit;
    }

    public Visit changeStatus(Long id, VisitStatus nextStatus) {
        Visit visit = findById(id);
        VisitStatus current = visit.getStatus();
        boolean allowed = switch (current) {
            case SCHEDULED -> nextStatus == VisitStatus.CHECKED_IN || nextStatus == VisitStatus.CANCELLED;
            case CHECKED_IN -> nextStatus == VisitStatus.IN_EXAM || nextStatus == VisitStatus.CANCELLED;
            case IN_EXAM -> nextStatus == VisitStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!allowed) throw new ConflictException("Không thể chuyển trạng thái từ " + current + " sang " + nextStatus);
        if (nextStatus == VisitStatus.COMPLETED
                && (visit.getDiagnosis() == null || visit.getDiagnosis().isBlank())
                && (visit.getTreatmentNotes() == null || visit.getTreatmentNotes().isBlank())) {
            throw new ConflictException("Cần ghi chẩn đoán hoặc thông tin điều trị trước khi hoàn thành lượt khám");
        }
        visit.setStatus(nextStatus);
        return visit;
    }

    private Pet findActivePet(Long id) {
        return petRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thú cưng đang hoạt động có mã " + id));
    }

    private void ensureSlot(Long vetId, LocalDateTime at, Long excluded) {
        ensureSlot(vetId,at,excluded,false);
    }
    private void ensureSlot(Long vetId, LocalDateTime at, Long excluded,boolean walkIn) {
        if (at.isBefore(walkIn?java.time.LocalDate.now().atStartOfDay():LocalDateTime.now())) throw new ConflictException("Thời gian khám không hợp lệ hoặc đã qua");
        vets.lockById(vetId).orElseThrow(() -> new NotFoundException("Không tìm thấy bác sĩ"));
        if (visitRepository.existsByVetIdAndIdNotAndStatusNotAndScheduledAtGreaterThanAndScheduledAtLessThan(
                vetId, excluded, VisitStatus.CANCELLED, at.minusMinutes(15), at.plusMinutes(15)))
            throw new ConflictException("Bác sĩ đã có lịch trong khoảng 15 phút này");
    }

    public Visit followUp(Long id, LocalDateTime at) {
        Visit original = visitRepository.findWithDetailsByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Không tìm thấy lượt khám"));
        requireStatus(original, VisitStatus.COMPLETED, "Chỉ tạo tái khám từ lượt khám đã hoàn thành");
        if (original.getFollowUpDate() == null || !at.toLocalDate().equals(original.getFollowUpDate()))
            throw new ConflictException("Thời gian tái khám phải đúng ngày hẹn trong hồ sơ");
        return create(original.getPet().getId(), original.getVet().getId(), at, "Tái khám từ lượt #" + id);
    }

    private void requireStatus(Visit visit, VisitStatus expected, String message) {
        if (visit.getStatus() != expected) throw new ConflictException(message);
    }

    private void ensurePetExists(Long id) {
        if (!petRepository.existsById(id)) throw new NotFoundException("Không tìm thấy thú cưng có mã " + id);
    }
}
