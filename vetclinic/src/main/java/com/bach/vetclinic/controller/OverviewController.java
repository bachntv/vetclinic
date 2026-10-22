package com.bach.vetclinic.controller;

import com.bach.vetclinic.entity.*;
import com.bach.vetclinic.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Transactional(readOnly = true)
public class OverviewController {
    private final OwnerRepository owners;
    private final PetRepository pets;
    private final VisitRepository visits;
    private final InvoiceRepository invoices;
    private final VaccinationRepository vaccinations;
    private final MedicineRepository medicines;
    public OverviewController(OwnerRepository owners, PetRepository pets, VisitRepository visits, InvoiceRepository invoices,
                              VaccinationRepository vaccinations, MedicineRepository medicines) {
        this.owners=owners; this.pets=pets; this.visits=visits; this.invoices=invoices; this.vaccinations=vaccinations; this.medicines=medicines;
    }
    @GetMapping("/dashboard/summary")
    public Map<String,Object> summary() {
        LocalDate today = LocalDate.now();
        var allVisits=visits.findAllByOrderByScheduledAtAsc(); var allInvoices=invoices.findAllByOrderByIssuedAtDesc();
        var allVaccines=vaccinations.findAllByOrderByAdministeredAtDesc();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("ownerCount",owners.findAllByArchivedFalseOrderByLastNameAscFirstNameAsc().size());
        result.put("petCount",pets.findAllByArchivedFalseOrderByNameAsc().size());
        var recentPets=allVisits.stream().filter(v->v.getStatus()!=VisitStatus.CANCELLED && v.getScheduledAt().isAfter(LocalDateTime.now().minusYears(2))).map(v->v.getPet().getId()).collect(Collectors.toSet());
        result.put("inactivePatientCount",pets.findAllByArchivedFalseOrderByNameAsc().stream().filter(p->!recentPets.contains(p.getId())).count());
        result.put("todaySchedule",allVisits.stream().filter(v->v.getScheduledAt().toLocalDate().equals(today) && v.getStatus()!=VisitStatus.CANCELLED).map(AppointmentController.AppointmentResponse::from).toList());
        result.put("paidRevenue",allInvoices.stream().filter(i->i.getStatus()==InvoiceStatus.PAID).map(Invoice::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add));
        result.put("unpaidCount",allInvoices.stream().filter(i->i.getStatus()==InvoiceStatus.SENT).count());
        result.put("revenueByMonth",allInvoices.stream().filter(i->i.getStatus()==InvoiceStatus.PAID).collect(Collectors.groupingBy(i->i.getIssuedAt().toLocalDate().withDayOfMonth(1).toString(),TreeMap::new,Collectors.reducing(BigDecimal.ZERO,Invoice::getTotal,BigDecimal::add))));
        result.put("vetWorkload",allVisits.stream().filter(v->v.getStatus()!=VisitStatus.CANCELLED).collect(Collectors.groupingBy(v->v.getVet().getName()+" (#"+v.getVet().getId()+")",Collectors.counting())));
        result.put("appointmentTrend",allVisits.stream().filter(v->v.getStatus()!=VisitStatus.CANCELLED).collect(Collectors.groupingBy(v->v.getScheduledAt().toLocalDate().withDayOfMonth(1).toString(),TreeMap::new,Collectors.counting())));
        result.put("vaccinationAlerts",allVaccines.stream().filter(v->v.getNextDueDate()!=null && !v.getNextDueDate().isAfter(today.plusDays(30))).map(VaccinationController.VaccinationResponse::from).toList());
        result.put("followUps",allVisits.stream().filter(v->v.getFollowUpDate()!=null && !v.getFollowUpDate().isBefore(today) && !v.getFollowUpDate().isAfter(today.plusDays(30))).map(VisitController.VisitResponse::from).toList());
        result.put("lowStock",medicines.findAllByOrderByNameAsc().stream().filter(m->m.isActive() && m.getStockQuantity().compareTo(m.getReorderLevel())<=0).map(MedicineController.MedicineResponse::from).toList());
        return result;
    }
    @GetMapping("/owners/page")
    public Map<String,Object> ownerPage(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="false") boolean includeArchived,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){
        var data=owners.search(q.trim(),includeArchived,paging(page,size));
        return Map.of("content",data.getContent().stream().map(OwnerController.OwnerResponse::from).toList(),"total",data.getTotalElements(),"pages",data.getTotalPages(),"page",page);
    }
    @GetMapping("/pets/page")
    public Map<String,Object> petPage(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="false") boolean includeArchived,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){
        var data=pets.search(q.trim(),includeArchived,paging(page,size));
        return Map.of("content",data.getContent().stream().map(PetController.PetResponse::from).toList(),"total",data.getTotalElements(),"pages",data.getTotalPages(),"page",page);
    }
    private org.springframework.data.domain.Pageable paging(int page,int size){
        if(page<0||size<1||size>100)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Trang phải >= 0; kích thước từ 1 đến 100");
        return org.springframework.data.domain.PageRequest.of(page,size,org.springframework.data.domain.Sort.by("id").descending());
    }
    @GetMapping("/vets/{id}/performance")
    public Map<String,Object> vetPerformance(@PathVariable Long id){
        var doctorVisits=visits.findAllByOrderByScheduledAtAsc().stream().filter(v->v.getVet().getId().equals(id)).toList();
        return Map.of("visitCount",doctorVisits.size(),"completed",doctorVisits.stream().filter(v->v.getStatus()==VisitStatus.COMPLETED).count(),"paidRevenue",invoices.findAllByOrderByIssuedAtDesc().stream().filter(i->i.getVisit().getVet().getId().equals(id)&&i.getStatus()==InvoiceStatus.PAID).map(Invoice::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add));
    }
    @GetMapping("/owners/{id}/invoices")
    public List<InvoiceController.InvoiceResponse> ownerInvoices(@PathVariable Long id){
        if(!owners.existsById(id))throw new com.bach.vetclinic.web.NotFoundException("Không tìm thấy chủ nuôi");
        return invoices.findAllByOrderByIssuedAtDesc().stream().filter(i->i.getVisit().getPet().getOwner().getId().equals(id)).map(InvoiceController.InvoiceResponse::from).toList();
    }
    @GetMapping("/pets/{id}/invoices")
    public List<InvoiceController.InvoiceResponse> petInvoices(@PathVariable Long id){
        if(!pets.existsById(id))throw new com.bach.vetclinic.web.NotFoundException("Không tìm thấy thú cưng");
        return invoices.findAllByOrderByIssuedAtDesc().stream().filter(i->i.getVisit().getPet().getId().equals(id)).map(InvoiceController.InvoiceResponse::from).toList();
    }
    @GetMapping("/notifications")
    public Map<String,Object> notifications(){
        var data=summary();
        return Map.of("vaccinations",data.get("vaccinationAlerts"),"followUps",data.get("followUps"),"lowStock",data.get("lowStock"),"appointments",visits.findAllByOrderByScheduledAtAsc().stream().filter(v->v.getStatus()==VisitStatus.SCHEDULED && v.getScheduledAt().isAfter(LocalDateTime.now()) && v.getScheduledAt().isBefore(LocalDateTime.now().plusDays(7))).map(AppointmentController.AppointmentResponse::from).toList());
    }
    @GetMapping("/search")
    public Map<String,Object> search(@RequestParam(defaultValue="") String q) {
        String term=q.trim().toLowerCase(Locale.ROOT);
        if(term.length()<2) return Map.of("owners",List.of(),"pets",List.of(),"visits",List.of());
        return Map.of("owners",owners.findAllByArchivedFalseOrderByLastNameAscFirstNameAsc().stream().filter(o->matches(term,o.getFirstName(),o.getLastName(),o.getPhone(),o.getEmail())).limit(50).map(OwnerController.OwnerResponse::from).toList(),
                "pets",pets.findAllByArchivedFalseOrderByNameAsc().stream().filter(p->matches(term,p.getName(),p.getSpecies(),p.getBreed())).limit(50).map(PetController.PetResponse::from).toList(),
                "visits",visits.findAllByOrderByScheduledAtAsc().stream().filter(v->matches(term,v.getPet().getName(),v.getDiagnosis(),v.getChiefComplaint())).limit(50).map(VisitController.VisitResponse::from).toList());
    }
    private boolean matches(String term,String... texts) { return Arrays.stream(texts).filter(Objects::nonNull).anyMatch(t->t.toLowerCase(Locale.ROOT).contains(term)); }
}
