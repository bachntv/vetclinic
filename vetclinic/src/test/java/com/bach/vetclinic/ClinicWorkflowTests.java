package com.bach.vetclinic;

import com.bach.vetclinic.entity.*;
import com.bach.vetclinic.repository.*;
import com.bach.vetclinic.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:workflow-test;DB_CLOSE_DELAY=-1", "app.bootstrap-admin.email=", "app.bootstrap-admin.password=", "app.bootstrap-admin.full-name="})
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ClinicWorkflowTests {
    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired OwnerService owners;
    @Autowired PetService pets;
    @Autowired VetService vets;
    @Autowired VisitService visits;
    @Autowired MedicineService medicines;
    @Autowired PetWeightService weights;
    @Autowired MedicineRepository medicineRepository;
    @Autowired PetRepository petRepository;
    final JsonMapper json=JsonMapper.builder().build();
    String admin,vet,reception;
    final String password="Testing-password-2026";
    @BeforeEach void accounts() throws Exception {
        String unique=UUID.randomUUID().toString().substring(0,8);
        admin=account("admin"+unique,UserRole.ADMIN);vet=account("vet"+unique,UserRole.VET);reception=account("reception"+unique,UserRole.RECEPTIONIST);
    }
    String account(String name,UserRole role)throws Exception{
        users.create(name,name+"@test.local",password,role);
        String body=mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email",name+"@test.local","password",password)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }
    long postId(String path,String token,Object body)throws Exception{
        String result=mvc.perform(post(path).header("Authorization","Bearer "+token).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(result).get("id").asLong();
    }
    long[] fixture(){
        var o=owners.create("An","Nguyen",null,null,null);
        var p=pets.create(o.getId(),"Milo","Cat",null,null,null,null,new BigDecimal("4.00"),null,null);
        var v=vets.create("Dr Test",null,null,null);
        return new long[]{o.getId(),p.getId(),v.getId()};
    }
    void change(long id,String state)throws Exception{mvc.perform(put("/api/appointments/"+id+"/status").header("Authorization","Bearer "+reception).contentType("application/json").content(json.writeValueAsString(Map.of("status",state)))).andExpect(status().isOk());}
    @Test void receptionToExamPrescriptionAndPayment()throws Exception{
        long owner=postId("/api/owners",reception,Map.of("firstName","An","lastName","Nguyen"));
        long pet=postId("/api/pets",reception,Map.of("ownerId",owner,"name","Milo","species","Cat"));
        long doctor=postId("/api/vets",admin,Map.of("name","Dr One"));
        long visit=postId("/api/appointments",reception,Map.of("petId",pet,"vetId",doctor,"scheduledAt",LocalDateTime.now().plusDays(1).toString()));
        change(visit,"CHECKED_IN");change(visit,"IN_EXAM");
        mvc.perform(put("/api/visits/"+visit).header("Authorization","Bearer "+reception).contentType("application/json").content("{\"diagnosis\":\"test\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/visits/"+visit).header("Authorization","Bearer "+vet).contentType("application/json").content("{\"diagnosis\":\"Routine check\",\"treatmentNotes\":\"Observation\"}")).andExpect(status().isOk());
        var medicine=medicines.create("Medicine",null,"Tablet","tablet",new BigDecimal("10"),new BigDecimal("2"),new BigDecimal("100"));
        postId("/api/prescriptions",vet,Map.of("visitId",visit,"items",List.of(Map.of("medicineId",medicine.getId(),"dosage","one tablet","frequency","daily","durationDays",2,"quantity",2))));
        assertEquals(0,medicineRepository.findById(medicine.getId()).orElseThrow().getStockQuantity().compareTo(new BigDecimal("8")));
        mvc.perform(get("/api/prescriptions/by-pet/"+pet).header("Authorization","Bearer "+vet)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].prescribedAt").exists()).andExpect(jsonPath("$[0].items[0].medicineName").value("Medicine"));
        mvc.perform(get("/api/prescriptions/by-pet/"+pet).header("Authorization","Bearer "+reception)).andExpect(status().isForbidden());
        change(visit,"COMPLETED");
        long invoice=postId("/api/invoices",reception,Map.of("visitId",visit,"vatRate",0.1,"items",List.of(Map.of("description","Consultation","category","CONSULTATION","quantity",1,"unitPrice",100000))));
        mvc.perform(get("/api/invoices/"+invoice).header("Authorization","Bearer "+reception)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(110000));
        for(String state:List.of("SENT","PAID"))mvc.perform(patch("/api/invoices/"+invoice+"/status").header("Authorization","Bearer "+reception).contentType("application/json").content(json.writeValueAsString(Map.of("status",state)))).andExpect(status().isOk());
        mvc.perform(get("/api/dashboard/summary").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.paidRevenue").value(110000));
    }
    @Test void roleBoundariesAndBookingCollision()throws Exception{
        var ids=fixture();var at=LocalDateTime.now().plusDays(2);
        visits.create(ids[1],ids[2],at,null);
        mvc.perform(post("/api/appointments").header("Authorization","Bearer "+reception).contentType("application/json").content(json.writeValueAsString(Map.of("petId",ids[1],"vetId",ids[2],"scheduledAt",at.plusMinutes(10).toString())))).andExpect(status().isConflict());
        mvc.perform(post("/api/owners").header("Authorization","Bearer "+vet).contentType("application/json").content("{\"firstName\":\"A\",\"lastName\":\"B\"}")).andExpect(status().isCreated());
        mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+reception)).andExpect(status().isForbidden());
    }
    @Test void disableAccountInvalidatesIssuedToken()throws Exception{
        var user=users.findAll().stream().filter(u->u.getRole()==UserRole.VET).findFirst().orElseThrow();users.setEnabled(user.getId(),false);
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+vet)).andExpect(status().isUnauthorized());
    }
    @Test void passwordResetInvalidatesIssuedToken()throws Exception{
        var user=users.findAll().stream().filter(u->u.getRole()==UserRole.VET).findFirst().orElseThrow();users.resetPassword(user.getId(),"Changed-password-2026");
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+vet)).andExpect(status().isUnauthorized());
    }
    @Test void historicalWeightDoesNotOverwriteLatest(){
        var ids=fixture();weights.record(ids[1],new BigDecimal("5.00"),LocalDateTime.now(),null);weights.record(ids[1],new BigDecimal("3.00"),LocalDateTime.now().minusDays(10),null);
        assertEquals(0,petRepository.findById(ids[1]).orElseThrow().getWeightKg().compareTo(new BigDecimal("5.00")));
    }
    @Test void supportPrivacyAndKnowledgePermissions()throws Exception{
        postId("/api/support-requests",vet,Map.of("subject","Private request","description","Only my request"));
        mvc.perform(get("/api/support-requests").header("Authorization","Bearer "+reception)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/support-requests").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post("/api/knowledge").header("Authorization","Bearer "+vet).contentType("application/json").content("{\"title\":\"Guide\",\"content\":\"Notes\"}")).andExpect(status().isForbidden());
        postId("/api/knowledge",admin,Map.of("title","Guide","content","Internal guide","source","Clinic SOP v1"));
    }
    @Test void archivedPetCanBeReactivated()throws Exception{
        var ids=fixture();pets.archive(ids[1]);
        mvc.perform(get("/api/pets/"+ids[1]).header("Authorization","Bearer "+admin)).andExpect(status().isNotFound());
        mvc.perform(post("/api/pets/"+ids[1]+"/activate").header("Authorization","Bearer "+reception)).andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(false));
    }
    @Test void pagedSearchAndNotificationsUseActualRecords()throws Exception{
        var ids=fixture();
        mvc.perform(get("/api/pets/page?q=Milo&page=0&size=1").header("Authorization","Bearer "+reception)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.content[0].id").value(ids[1]));
        mvc.perform(get("/api/owners/page?size=101").header("Authorization","Bearer "+admin)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/notifications").header("Authorization","Bearer "+vet)).andExpect(status().isOk()).andExpect(jsonPath("$.appointments").isArray());
    }
    @Test void cannotDisableOwnAdminAccount()throws Exception{
        var user=users.findAll().stream().filter(u->u.getRole()==UserRole.ADMIN).findFirst().orElseThrow();
        mvc.perform(patch("/api/admin/users/"+user.getId()+"/enabled").header("Authorization","Bearer "+admin).contentType("application/json").content("{\"enabled\":false}")).andExpect(status().isConflict());
    }
    @Test void clinicalWarningsReferenceRecordedAllergies()throws Exception{
        var ids=fixture();pets.update(ids[1],ids[0],"Milo","Cat",null,null,null,null,null,"Penicillin",null);
        var visit=visits.create(ids[1],ids[2],LocalDateTime.now().plusDays(1),null);
        visits.changeStatus(visit.getId(),VisitStatus.CHECKED_IN);visits.changeStatus(visit.getId(),VisitStatus.IN_EXAM);
        mvc.perform(put("/api/visits/"+visit.getId()).header("Authorization","Bearer "+vet).contentType("application/json").content("{\"treatmentNotes\":\"Review penicillin exposure\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.warnings.length()").value(1));
    }
    @Test void vetCanManageOwnerPetAndAppointmentAccordingToApprovedUseCases()throws Exception{
        long owner=postId("/api/owners",vet,Map.of("firstName","Doctor","lastName","Client"));
        long pet=postId("/api/pets",vet,Map.of("ownerId",owner,"name","Report pet","species","Dog"));
        var doctor=vets.create("Approved doctor",null,null,null);
        long visit=postId("/api/appointments",vet,Map.of("petId",pet,"vetId",doctor.getId(),"scheduledAt",LocalDateTime.now().plusDays(4).toString()));
        mvc.perform(put("/api/owners/"+owner).header("Authorization","Bearer "+vet).contentType("application/json").content("{\"firstName\":\"Updated\",\"lastName\":\"Client\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/pets/"+pet).header("Authorization","Bearer "+vet).contentType("application/json").content(json.writeValueAsString(Map.of("ownerId",owner,"name","Updated pet","species","Dog")))).andExpect(status().isOk());
        mvc.perform(put("/api/appointments/"+visit).header("Authorization","Bearer "+vet).contentType("application/json").content(json.writeValueAsString(Map.of("petId",pet,"vetId",doctor.getId(),"scheduledAt",LocalDateTime.now().plusDays(5).toString())))).andExpect(status().isOk());
        mvc.perform(delete("/api/appointments/"+visit).header("Authorization","Bearer "+vet)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/pets/"+pet).header("Authorization","Bearer "+vet)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/owners/"+owner).header("Authorization","Bearer "+vet)).andExpect(status().isNoContent());
    }
    @Test void walkInSymptomsAndInclusiveDateSearch()throws Exception{
        var ids=fixture();long visit=postId("/api/visits",vet,Map.of("petId",ids[1],"vetId",ids[2],"chiefComplaint","Walk-in"));
        mvc.perform(get("/api/visits/"+visit).header("Authorization","Bearer "+vet)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CHECKED_IN"));
        change(visit,"IN_EXAM");
        mvc.perform(put("/api/visits/"+visit).header("Authorization","Bearer "+vet).contentType("application/json").content("{\"symptoms\":\"fever and coughing\",\"diagnosis\":\"Observation\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.symptoms").value("fever and coughing"));
        String date=java.time.LocalDate.now().toString();
        mvc.perform(get("/api/pets/"+ids[1]+"/medical-records").param("from",date).param("to",date).param("q","coughing").header("Authorization","Bearer "+vet)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/pets/"+ids[1]+"/medical-records").param("from",java.time.LocalDate.now().plusDays(1).toString()).param("to",date).header("Authorization","Bearer "+vet)).andExpect(status().isBadRequest());
    }
    @Test void logoutRevokesOnlySubmittedToken()throws Exception{
        var user=users.findAll().stream().filter(u->u.getRole()==UserRole.VET).findFirst().orElseThrow();
        String result=mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email",user.getEmail(),"password",password)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second=json.readTree(result).get("accessToken").asText();
        mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+vet)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+vet)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+second)).andExpect(status().isOk());
    }
    @Test void adminCreatesLinkedDoctorAndUpdatesAccount()throws Exception{
        long id=postId("/api/admin/users",admin,Map.of("fullName","New doctor","email","new-doctor@test.local","password",password,"role","VET"));
        var user=users.findAll().stream().filter(u->u.getId().equals(id)).findFirst().orElseThrow();assertNotNull(user.getVet());
        long vetId=user.getVet().getId();
        mvc.perform(put("/api/admin/users/"+id).header("Authorization","Bearer "+admin).contentType("application/json").content(json.writeValueAsString(Map.of("fullName","Updated doctor","email","updated-doctor@test.local","role","VET","vetId",vetId)))).andExpect(status().isOk()).andExpect(jsonPath("$.vetId").value(vetId)).andExpect(jsonPath("$.vetName").value("Updated doctor"));
        mvc.perform(post("/api/admin/users").header("Authorization","Bearer "+admin).contentType("application/json").content(json.writeValueAsString(Map.of("fullName","Duplicate link","email","duplicate-doctor@test.local","password",password,"role","VET","vetId",vetId)))).andExpect(status().isConflict());
    }
    @Test void vaccineCatalogPreservesHistoryAndEnforcesPermissions()throws Exception{
        var ids=fixture();long vaccine=postId("/api/vaccine-types",admin,Map.of("name","Clinic vaccine","description","Catalog note"));
        long record=postId("/api/vaccinations",vet,Map.of("petId",ids[1],"vaccineId",vaccine,"administeredAt",LocalDateTime.now().toString(),"notes","Patient note"));
        mvc.perform(put("/api/vaccine-types/"+vaccine).header("Authorization","Bearer "+admin).contentType("application/json").content("{\"name\":\"Renamed catalog entry\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/vaccinations/"+record).header("Authorization","Bearer "+vet)).andExpect(status().isOk()).andExpect(jsonPath("$.vaccineType").value("Clinic vaccine")).andExpect(jsonPath("$.notes").value("Patient note"));
        mvc.perform(put("/api/vaccinations/"+record).header("Authorization","Bearer "+vet).contentType("application/json").content(json.writeValueAsString(Map.of("petId",ids[1],"vaccineId",vaccine,"administeredAt",LocalDateTime.now().toString(),"notes","Updated note")))).andExpect(status().isOk()).andExpect(jsonPath("$.vaccineType").value("Clinic vaccine"));
        mvc.perform(post("/api/vaccine-types").header("Authorization","Bearer "+vet).contentType("application/json").content("{\"name\":\"Forbidden\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/vaccine-types/"+vaccine).header("Authorization","Bearer "+admin)).andExpect(status().isNoContent());
        mvc.perform(post("/api/vaccinations").header("Authorization","Bearer "+vet).contentType("application/json").content(json.writeValueAsString(Map.of("petId",ids[1],"vaccineId",vaccine,"administeredAt",LocalDateTime.now().toString())))).andExpect(status().isConflict());
    }
    @Test void linkedDoctorLifecycleAndRoleChangeInvalidateAccess()throws Exception{
        var user=users.findAll().stream().filter(u->u.getRole()==UserRole.VET).findFirst().orElseThrow();
        long doctor=user.getVet().getId();
        vets.deactivate(doctor);
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+vet)).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/admin/users/"+user.getId()+"/enabled").header("Authorization","Bearer "+admin).contentType("application/json").content("{\"enabled\":true}")).andExpect(status().isConflict());
        vets.activate(doctor);users.setEnabled(user.getId(),true);
        users.update(user.getId(),"Reception doctor",user.getEmail(),UserRole.RECEPTIONIST,null);
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+vet)).andExpect(status().isUnauthorized());
        assertNull(user.getVet());
    }
}
