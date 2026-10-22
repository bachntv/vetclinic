package com.bach.vetclinic.service;
import com.bach.vetclinic.entity.*;
import com.bach.vetclinic.repository.*;
import com.bach.vetclinic.web.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service @Transactional
public class PrescriptionService {
 private final PrescriptionRepository prescriptions; private final MedicineRepository medicines;private final VisitRepository visits;private final InvoiceRepository invoices;private final AuditService audit;
 public PrescriptionService(PrescriptionRepository prescriptions,MedicineRepository medicines,VisitRepository visits,InvoiceRepository invoices,AuditService audit){this.prescriptions=prescriptions;this.medicines=medicines;this.visits=visits;this.invoices=invoices;this.audit=audit;}
 @Transactional(readOnly=true) public Prescription findByVisitId(Long id){return prescriptions.findWithDetailsByVisitId(id).orElseThrow(()->new NotFoundException("Lượt khám chưa có đơn thuốc"));}
 @Transactional(readOnly=true) public List<Prescription> history(Long petId){return prescriptions.findAllByVisitPetIdOrderByCreatedAtDesc(petId);}
 public Prescription create(Long visitId,String notes,List<ItemData> items){
  var visit=lockVisit(visitId);requireExam(visit);requireUnbilled(visitId);
  if(prescriptions.findWithDetailsByVisitId(visitId).isPresent())throw new ConflictException("Lượt khám đã có đơn thuốc. Mở đơn hiện có để sửa hoặc kê lại.");
  var p=new Prescription(visit,clean(notes));var locked=lockMedicines(items,Map.of());
  var lines=build(p,items,locked,Map.of());lines.forEach(p::addItem);
  prescriptions.saveAndFlush(p);audit.record("PRESCRIPTION_CREATED","PRESCRIPTION",p.getId(),null,null,snapshot(p));return p;
 }
 public Prescription replace(Long visitId,long revision,String reason,String notes,List<ItemData> items){
  var visit=lockVisit(visitId);requireExam(visit);requireUnbilled(visitId);
  var p=findByVisitId(visitId);requireRevision(p,revision);var before=snapshot(p);
  var old=quantities(p);var locked=lockMedicines(items,old);
  old.forEach((id,quantity)->locked.get(id).restoreStock(quantity));
  var lines=build(p,items,locked,old);p.replace(clean(notes),lines);prescriptions.flush();
  audit.record("PRESCRIPTION_UPDATED","PRESCRIPTION",p.getId(),reason.trim(),before,snapshot(p));return p;
 }
 public Prescription cancel(Long visitId,long revision,String reason){
  var visit=lockVisit(visitId);if(visit.getStatus()==VisitStatus.CANCELLED)throw new ConflictException("Lượt khám đã hủy");
  requireUnbilled(visitId);var p=findByVisitId(visitId);requireRevision(p,revision);
  if(!p.getStatus().equals("ACTIVE"))throw new ConflictException("Đơn đã hủy; tồn kho đã được hoàn trước đó.");
  var before=snapshot(p);var old=quantities(p);var locked=lockMedicines(List.of(),old);
  old.forEach((id,quantity)->{var medicine=locked.get(id);medicine.restoreStock(quantity);checkStockLimit(medicine);});
  p.cancel(reason.trim());audit.record("PRESCRIPTION_CANCELLED","PRESCRIPTION",p.getId(),reason.trim(),before,snapshot(p));return p;
 }
 private Visit lockVisit(Long id){return visits.findWithDetailsByIdForUpdate(id).orElseThrow(()->new NotFoundException("Không tìm thấy lượt khám"));}
 private void requireExam(Visit v){if(v.getStatus()!=VisitStatus.IN_EXAM)throw new ConflictException("Chỉ kê hoặc sửa đơn khi lượt khám đang khám.");}
 private void requireUnbilled(Long visitId){if(invoices.existsByVisitIdAndStatusIn(visitId,List.of(InvoiceStatus.SENT,InvoiceStatus.PAID)))throw new ConflictException("Lượt khám có hóa đơn đã chốt hoặc thanh toán; không sửa/hủy đơn để tránh lệch tiền và tồn kho.");}
 private void requireRevision(Prescription p,long revision){if(p.getRevision()!=revision)throw new ConflictException("Đơn đã thay đổi bởi người khác. Làm mới và kiểm tra lại trước khi lưu.");}
 private Map<Long,BigDecimal> quantities(Prescription p){var result=new TreeMap<Long,BigDecimal>();if(p.getStatus().equals("ACTIVE"))for(var i:p.getItems())result.merge(i.getMedicine().getId(),i.getQuantity(),BigDecimal::add);return result;}
 private Map<Long,Medicine> lockMedicines(List<ItemData> items,Map<Long,BigDecimal> old){
  var ids=new TreeSet<Long>(old.keySet());items.forEach(i->ids.add(i.medicineId()));var result=new HashMap<Long,Medicine>();
  for(Long id:ids)result.put(id,medicines.findByIdForUpdate(id).orElseThrow(()->new NotFoundException("Không tìm thấy thuốc #"+id)));return result;
 }
 private List<PrescriptionItem> build(Prescription p,List<ItemData> items,Map<Long,Medicine> locked,Map<Long,BigDecimal> old){
  var quantities=new HashMap<Long,BigDecimal>();for(var i:items)quantities.merge(i.medicineId(),i.quantity(),BigDecimal::add);
  for(var e:quantities.entrySet()){
   var m=locked.get(e.getKey());if(!m.isActive()&&e.getValue().compareTo(old.getOrDefault(e.getKey(),BigDecimal.ZERO))>0)throw new ConflictException("Thuốc đã ngừng sử dụng: "+m.getName());
   if(e.getValue().signum()<=0||m.getStockQuantity().compareTo(e.getValue())<0)throw new ConflictException("Không đủ tồn kho thuốc "+m.getName()+"; còn "+m.getStockQuantity()+" "+m.getUnit());
   m.deductStock(e.getValue());checkStockLimit(m);
  }
  locked.values().forEach(this::checkStockLimit);
  return items.stream().map(i->new PrescriptionItem(p,locked.get(i.medicineId()),i.dosage(),i.frequency(),i.durationDays(),i.quantity(),locked.get(i.medicineId()).getUnitPrice(),clean(i.instructions()))).toList();
 }
 private void checkStockLimit(Medicine m){if(m.getStockQuantity().compareTo(new BigDecimal("9999999999.99"))>0)throw new ConflictException("Tồn kho sau hoàn vượt giới hạn: "+m.getName());}
 private Object snapshot(Prescription p){return new Snapshot(p.getStatus(),p.getRevision(),p.getNotes(),p.getCancelReason(),p.getItems().stream().map(i->new Line(i.getMedicine().getId(),i.getDosage(),i.getFrequency(),i.getDurationDays(),i.getQuantity(),i.getUnitPrice(),i.getInstructions())).toList());}
 private record Snapshot(String status,long revision,String notes,String cancelReason,List<Line> items){}
 private record Line(Long medicineId,String dosage,String frequency,Integer days,BigDecimal quantity,BigDecimal unitPrice,String instructions){}
 private String clean(String v){return v==null||v.isBlank()?null:v.trim();}
 public record ItemData(Long medicineId,String dosage,String frequency,Integer durationDays,BigDecimal quantity,String instructions){}
}
