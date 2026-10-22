package com.bach.vetclinic.service;

import com.bach.vetclinic.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.*;

/** Reads current source rows on every request; no stale patient index or cross-patient retrieval. */
@Service
public class AssistantService {
    private final PetService pets;
    private final VisitRepository visits;
    private final PrescriptionRepository prescriptions;
    private final VaccinationRepository vaccinations;
    private final KnowledgeDocumentRepository knowledge;
    private final LocalAiGateway ai;
    public AssistantService(PetService pets, VisitRepository visits, PrescriptionRepository prescriptions,
                            VaccinationRepository vaccinations, KnowledgeDocumentRepository knowledge, LocalAiGateway ai) {
        this.pets=pets;this.visits=visits;this.prescriptions=prescriptions;
        this.vaccinations=vaccinations;this.knowledge=knowledge;this.ai=ai;
    }
    public record Source(String id,String title,String text,String href,String updatedAt) {}
    public record Answer(String mode,String answer,List<Source> sources,boolean insufficientEvidence) {}

    @PreAuthorize("hasAnyRole('ADMIN','VET')")
    @Transactional(readOnly=true)
    public Answer ask(Long petId,String question,boolean generate) {
        var pet=pets.findActiveById(petId);
        var documents=new ArrayList<Source>();
        add(documents,"pet:"+petId,"Hồ sơ "+pet.getName(),
                "Loài: "+pet.getSpecies()+". Dị ứng: "+text(pet.getAllergies())+
                ". Bệnh mãn tính: "+text(pet.getChronicConditions()),"/pets/"+petId,pet.getUpdatedAt());
        for(var v:visits.findAllByPetIdOrderByScheduledAtDesc(petId)) {
            if(v.getDiagnosis()==null && v.getSymptoms()==null && v.getTreatmentNotes()==null) continue;
            add(documents,"visit:"+v.getId(),"Lượt khám #"+v.getId()+" · "+v.getScheduledAt(),
                    "Ngày khám: "+v.getScheduledAt()+". Lý do: "+text(v.getChiefComplaint())+
                    ". Triệu chứng: "+text(v.getSymptoms())+". Chẩn đoán: "+text(v.getDiagnosis())+
                    ". Điều trị: "+text(v.getTreatmentNotes())+". Tái khám: "+text(v.getFollowUpDate()),
                    "/visits/"+v.getId(),v.getUpdatedAt());
        }
        for(var p:prescriptions.findAllByVisitPetIdOrderByCreatedAtDesc(petId)) {
            var lines=new StringBuilder("Đơn đã ghi nhận, không phải chỉ định mới. ");
            for(var i:p.getItems()) lines.append(i.getMedicine().getName()).append(": ").append(i.getDosage())
                    .append(", ").append(i.getFrequency()).append(", ").append(i.getDurationDays()).append(" ngày. ");
            lines.append(text(p.getNotes()));
            add(documents,"prescription:"+p.getId(),"Đơn thuốc #"+p.getId(),lines.toString(),"/visits/"+p.getVisit().getId(),p.getUpdatedAt());
        }
        for(var v:vaccinations.findAllByPetIdOrderByAdministeredAtDesc(petId))
            add(documents,"vaccination:"+v.getId(),"Tiêm "+v.getVaccineType(),
                    "Vaccine: "+v.getVaccineType()+". Ngày tiêm: "+v.getAdministeredAt()+". Hẹn tiếp: "+text(v.getNextDueDate())+
                    ". Ghi chú: "+text(v.getNotes()),"/pets/"+petId,v.getUpdatedAt());
        for(var d:knowledge.findAllByOrderByUpdatedAtDesc())
            add(documents,"knowledge:"+d.getId(),"Tài liệu chung: "+d.getTitle(),d.getTitle()+". "+d.getContent(),"/knowledge",d.getUpdatedAt());
        var terms=tokens(question);
        // Lexical filtering also gates semantic ranking so unrelated documents cannot become evidence.
        var candidates=documents.stream().filter(s->overlap(terms,tokens(s.title()+" "+s.text()))>0)
                .sorted(Comparator.comparingDouble((Source s)->overlap(terms,tokens(s.title()+" "+s.text()))).reversed())
                .limit(32).toList();
        if(candidates.isEmpty()) return new Answer("NO_EVIDENCE","Không tìm thấy dữ liệu phù hợp trong hồ sơ đã chọn. Hãy đổi câu hỏi hoặc bổ sung hồ sơ.",List.of(),true);
        var sources=generate?ai.rank(question,candidates):candidates.stream().limit(6).toList();
        if(generate) return new Answer("LOCAL_RAG",ai.generate(question,sources),sources,false);
        String answer=sources.stream().map(s->"["+s.id()+"] "+s.text()).collect(java.util.stream.Collectors.joining("\n\n"));
        return new Answer("EXTRACTIVE","Thông tin trích từ dữ liệu hiện có:\n\n"+answer,sources,false);
    }
    private void add(List<Source> out,String id,String title,String body,String href,Object updated) {
        // Bounded, overlapping chunks retain source IDs and make long notes searchable.
        int part=0;
        for(int start=0;start<body.length();start+=700) {
            int end=Math.min(body.length(),start+900);
            out.add(new Source(id+":"+(part++),title,body.substring(start,end),href,String.valueOf(updated)));
            if(end==body.length()) break;
        }
    }
    static String text(Object value){return value==null?"chưa ghi nhận":value.toString();}
    static Set<String> tokens(String value) {
        String normalized=Normalizer.normalize(value.toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
                .replaceAll("\\p{M}","").replace('đ','d');
        var result=new HashSet<>(Arrays.asList(normalized.split("[^a-z0-9]+")));
        result.removeAll(Set.of("","la","va","cua","cho","toi","co","khong","gi","nao","hay","the","ho","so","thu","cung"));
        return result;
    }
    static double overlap(Set<String> query,Set<String> document) {
        if(query.isEmpty()) return 0;
        return query.stream().filter(document::contains).count()/(double)query.size();
    }
}
