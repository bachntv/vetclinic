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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class AssistantTests {
    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired OwnerService owners;
    @Autowired PetService pets;
    @Autowired VetService vets;
    @Autowired VisitService visits;
    @Autowired KnowledgeDocumentRepository knowledge;
    @MockitoBean LocalAiGateway ai;
    final JsonMapper json=JsonMapper.builder().build();
    String vet,reception; long pet,other;
    @BeforeEach void fixture()throws Exception {
        vet=login(UserRole.VET);reception=login(UserRole.RECEPTIONIST);
        var owner=owners.create("Test","Owner",null,null,null);
        pet=pets.create(owner.getId(),"Milo","Cat",null,null,null,null,null,null,null).getId();
        other=pets.create(owner.getId(),"Other","Dog",null,null,null,null,null,null,null).getId();
        var doctor=vets.create("Test vet",null,null,null);
        var v=visits.create(pet,doctor.getId(),LocalDateTime.now().plusDays(1),null);
        visits.changeStatus(v.getId(),VisitStatus.CHECKED_IN);visits.changeStatus(v.getId(),VisitStatus.IN_EXAM);
        visits.updateMedicalRecord(v.getId(),"viêm da", "theo dõi",null,"ngứa");
        var secret=visits.create(other,doctor.getId(),LocalDateTime.now().plusDays(2),null);
        visits.changeStatus(secret.getId(),VisitStatus.CHECKED_IN);visits.changeStatus(secret.getId(),VisitStatus.IN_EXAM);
        visits.updateMedicalRecord(secret.getId(),"SECRET_OTHER_PATIENT",null,null,null);
    }
    String login(UserRole role)throws Exception {
        String email=UUID.randomUUID()+"@test.local";
        users.create("Tester",email,"Test-password-2026",role);
        var response=mvc.perform(post("/api/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email",email,"password","Test-password-2026")))).andExpect(status().isOk()).andReturn().getResponse();
        return json.readTree(response.getContentAsString()).get("accessToken").asText();
    }
    org.springframework.test.web.servlet.ResultActions ask(String token,long id,String question,boolean generate)throws Exception {
        return mvc.perform(post("/api/assistant/ask").header("Authorization","Bearer "+token).contentType("application/json")
                .content(json.writeValueAsString(Map.of("petId",id,"question",question,"generate",generate))));
    }
    @Test void retrievalHasSourceAndCannotLeakOtherPatient()throws Exception {
        ask(vet,pet,"chẩn đoán",false).andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("EXTRACTIVE"))
                .andExpect(jsonPath("$.sources[0].id").exists()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("SECRET_OTHER_PATIENT"))));
        ask(vet,pet,"SECRET_OTHER_PATIENT",false).andExpect(status().isOk()).andExpect(jsonPath("$.insufficientEvidence").value(true));
        verifyNoInteractions(ai);
    }
    @Test void receptionistAndAnonymousCannotUseClinicalAssistant()throws Exception {
        ask(reception,pet,"chẩn đoán",false).andExpect(status().isForbidden());
        mvc.perform(post("/api/assistant/ask").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        verifyNoInteractions(ai);
    }
    @Test void missingArchivedAndInvalidRequestsAreRejected()throws Exception {
        ask(vet,999999,"chẩn đoán",false).andExpect(status().isNotFound());
        ask(vet,pet," ",false).andExpect(status().isBadRequest());
        pets.archive(pet);ask(vet,pet,"chẩn đoán",false).andExpect(status().isNotFound());
    }
    @Test void currentKnowledgeIsReadAndDeletionPropagates()throws Exception {
        var d=knowledge.save(new KnowledgeDocument("Zebrafish","zebrafish protocol","SOP"));
        ask(vet,pet,"zebrafish",false).andExpect(status().isOk()).andExpect(jsonPath("$.sources[0].id").value("knowledge:"+d.getId()+":0"));
        knowledge.delete(d);
        ask(vet,pet,"zebrafish",false).andExpect(status().isOk()).andExpect(jsonPath("$.insufficientEvidence").value(true));
    }
    @Test void generationUsesRetrievedSourcesAndFailureIsExplicit()throws Exception {
        when(ai.rank(anyString(),anyList())).thenAnswer(i->i.getArgument(1));
        when(ai.generate(anyString(),anyList())).thenReturn("Kết quả kiểm thử có nguồn.");
        ask(vet,pet,"chẩn đoán",true).andExpect(status().isOk()).andExpect(jsonPath("$.mode").value("LOCAL_RAG"));
        when(ai.generate(anyString(),anyList())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Model offline"));
        ask(vet,pet,"chẩn đoán",true).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.detail").value("Model offline"));
    }
}
