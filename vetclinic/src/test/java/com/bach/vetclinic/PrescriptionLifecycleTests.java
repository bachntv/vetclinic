package com.bach.vetclinic;
import com.bach.vetclinic.entity.*;
import com.bach.vetclinic.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class PrescriptionLifecycleTests {
 @Autowired InvoiceService invoices;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
 @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;
 @Autowired MockMvc mvc;@Autowired UserService users;@Autowired OwnerService owners;@Autowired PetService pets;@Autowired VetService vets;@Autowired VisitService visits;@Autowired MedicineService medicines;@Autowired JdbcTemplate db;
 final JsonMapper json=JsonMapper.builder().build();String admin,vet,reception;long visit,medicine,rx;
 String login(UserRole role)throws Exception{String email=UUID.randomUUID()+"@qa.local";users.create("Lifecycle tester",email,"Lifecycle-password-2026",role);return json.readTree(mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email",email,"password","Lifecycle-password-2026")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("accessToken").asText();}
 Object lines(int qty){return List.of(Map.of("medicineId",medicine,"dosage","Test dosage","frequency","Test frequency","durationDays",2,"quantity",qty));}
 @BeforeEach void fixture()throws Exception{
  admin=login(UserRole.ADMIN);vet=login(UserRole.VET);reception=login(UserRole.RECEPTIONIST);
  var o=owners.create("Lifecycle","Test",null,null,null);var p=pets.create(o.getId(),"Lifecycle pet","Cat",null,null,null,null,null,null,null);var v=vets.create("Lifecycle vet",null,null,null);
  visit=visits.create(p.getId(),v.getId(),LocalDateTime.now().plusDays(3),null).getId();visits.changeStatus(visit,VisitStatus.CHECKED_IN);visits.changeStatus(visit,VisitStatus.IN_EXAM);visits.updateMedicalRecord(visit,"Test diagnosis",null,null);
  medicine=medicines.create("Lifecycle medicine",null,"Tablet","tablet",new BigDecimal("10"),BigDecimal.ZERO,BigDecimal.ONE).getId();
  var response=mvc.perform(post("/api/prescriptions").header("Authorization","Bearer "+vet).contentType("application/json").content(json.writeValueAsString(Map.of("visitId",visit,"items",lines(2))))).andExpect(status().isCreated()).andReturn().getResponse();rx=json.readTree(response.getContentAsString()).get("id").asLong();
 }
 org.springframework.test.web.servlet.ResultActions replace(String token,long revision,int qty)throws Exception{return mvc.perform(put("/api/prescriptions/by-visit/"+visit).header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(Map.of("revision",revision,"reason","Correction","items",lines(qty)))));}
 org.springframework.test.web.servlet.ResultActions cancel(String token,long revision)throws Exception{return mvc.perform(post("/api/prescriptions/by-visit/"+visit+"/cancel").header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(Map.of("revision",revision,"reason","Cancelled before dispensing"))));}
 BigDecimal stock(){return db.queryForObject("select stock_quantity from medicines where id=?",BigDecimal.class,medicine);}
 long draftInvoice(){
  visits.changeStatus(visit,VisitStatus.COMPLETED);
  return invoices.create(visit,BigDecimal.ZERO,List.of(new InvoiceService.ItemData("Consultation",InvoiceItemCategory.CONSULTATION,BigDecimal.ONE,new BigDecimal("123.45")))).getId();
 }
 @Test void paymentDateIsRecordedOnceAndRevenueUsesCollectionMonth()throws Exception{
  long id=draftInvoice();
  db.update("update invoices set issued_at=? where id=?",LocalDateTime.of(2001,1,1,10,0),id);
  assertNull(invoices.findById(id).getPaidAt());
  invoices.changeStatus(id,InvoiceStatus.SENT);
  assertNull(invoices.findById(id).getPaidAt());
  var before=LocalDateTime.now().minusSeconds(1);
  invoices.changeStatus(id,InvoiceStatus.PAID);
  var paidAt=invoices.findById(id).getPaidAt();
  assertNotNull(paidAt);assertFalse(paidAt.isBefore(before));
  assertThrows(com.bach.vetclinic.web.ConflictException.class,()->invoices.changeStatus(id,InvoiceStatus.PAID));
  assertEquals(paidAt,invoices.findById(id).getPaidAt());
  // A unique historical collection month keeps this assertion independent of other fixtures.
  db.update("update invoices set paid_at=? where id=?",LocalDateTime.of(2002,2,3,10,0),id);
  mvc.perform(get("/api/dashboard/summary").header("Authorization","Bearer "+admin)).andExpect(status().isOk())
   .andExpect(jsonPath("$.revenueByMonth['2002-02-01']").value(123.45))
   .andExpect(jsonPath("$.revenueByMonth['2001-01-01']").doesNotExist());
 }
 @Test void legacyPaidInvoicesRemainInTotalWithoutInventingCollectionDate()throws Exception{
  long id=draftInvoice();
  db.update("update invoices set status='PAID', paid_at=null where id=?",id);
  mvc.perform(get("/api/invoices/"+id).header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.paidAt").isEmpty());
  var summary=json.readTree(mvc.perform(get("/api/dashboard/summary").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
  assertTrue(summary.get("revenueWithoutPaymentDate").decimalValue().compareTo(new BigDecimal("123.45"))>=0);
  assertTrue(summary.get("paidRevenue").decimalValue().compareTo(summary.get("revenueWithoutPaymentDate").decimalValue())>=0);
 }
 @Test void staleDraftCannotOverwriteConcurrentlyFinalizedInvoice(){
  long id=draftInvoice();
  var tx=new org.springframework.transaction.support.TransactionTemplate(transactionManager);
  tx.executeWithoutResult(status->{
   assertEquals(InvoiceStatus.DRAFT,entityManager.find(Invoice.class,id).getStatus());
   try(var executor=java.util.concurrent.Executors.newSingleThreadExecutor()){
    executor.submit(()->invoices.changeStatus(id,InvoiceStatus.SENT)).get(10,java.util.concurrent.TimeUnit.SECONDS);
   }catch(Exception e){throw new RuntimeException(e);}
   assertThrows(com.bach.vetclinic.web.ConflictException.class,()->invoices.updateDraftItems(id,BigDecimal.ZERO,List.of(new InvoiceService.ItemData("Stale edit",InvoiceItemCategory.OTHER,BigDecimal.ONE,BigDecimal.ONE))));
   status.setRollbackOnly();
  });
  assertEquals(InvoiceStatus.SENT,invoices.findById(id).getStatus());
  assertEquals(0,new BigDecimal("123.45").compareTo(invoices.findById(id).getTotal()));
 }
 @Test void replaceCancelAndReissuePreserveStockAndHistory()throws Exception{
  replace(vet,0,5).andExpect(status().isOk()).andExpect(jsonPath("$.revision").value(1));assertEquals(0,stock().compareTo(new BigDecimal("5")));
  cancel(vet,1).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));assertEquals(0,stock().compareTo(BigDecimal.TEN));
  cancel(vet,2).andExpect(status().isConflict());assertEquals(0,stock().compareTo(BigDecimal.TEN));
  replace(vet,2,3).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));assertEquals(0,stock().compareTo(new BigDecimal("7")));
  assertEquals(4,db.queryForObject("select count(*) from audit_events where entity_type='PRESCRIPTION' and entity_id=?",Integer.class,rx));
 }
 @Test void failedEditRollsBackRestorationAndDoesNotLogSuccess()throws Exception{
  replace(vet,0,99).andExpect(status().isConflict());assertEquals(0,stock().compareTo(new BigDecimal("8")));
  assertEquals(1,db.queryForObject("select count(*) from audit_events where entity_type='PRESCRIPTION' and entity_id=?",Integer.class,rx));
 }
 @Test void staleRevisionCannotRestoreOrDeductTwice()throws Exception{
  replace(vet,0,3).andExpect(status().isOk());replace(vet,0,6).andExpect(status().isConflict());cancel(vet,0).andExpect(status().isConflict());assertEquals(0,stock().compareTo(new BigDecimal("7")));
 }
 @Test void permissionsAreEnforcedOnWritesAndAudit()throws Exception{
  replace(reception,0,3).andExpect(status().isForbidden());cancel(reception,0).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/audit").header("Authorization","Bearer "+vet)).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/audit").header("Authorization","Bearer "+admin).param("entityType","PRESCRIPTION").param("entityId",String.valueOf(rx))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.content[0].actor").value(org.hamcrest.Matchers.endsWith("@qa.local")));
 }
 @Test void finalizedInvoicePreventsStockChangingPrescription()throws Exception{
  visits.changeStatus(visit,VisitStatus.COMPLETED);
  var response=mvc.perform(post("/api/invoices").header("Authorization","Bearer "+admin).contentType("application/json").content(json.writeValueAsString(Map.of("visitId",visit,"vatRate",0,"items",List.of(Map.of("description","Consultation","category","CONSULTATION","quantity",1,"unitPrice",100)))))).andExpect(status().isCreated()).andReturn().getResponse();
  long invoice=json.readTree(response.getContentAsString()).get("id").asLong();
  for(String state:List.of("SENT","PAID"))mvc.perform(patch("/api/invoices/"+invoice+"/status").header("Authorization","Bearer "+admin).contentType("application/json").content(json.writeValueAsString(Map.of("status",state)))).andExpect(status().isOk());
  cancel(vet,0).andExpect(status().isConflict());assertEquals(0,stock().compareTo(new BigDecimal("8")));
  assertEquals(1,db.queryForObject("select count(*) from audit_events where action='PAYMENT_RECORDED' and entity_id=?",Integer.class,invoice));
 }
}
