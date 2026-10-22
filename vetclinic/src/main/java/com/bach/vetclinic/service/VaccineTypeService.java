package com.bach.vetclinic.service;
import com.bach.vetclinic.entity.VaccineType;
import com.bach.vetclinic.repository.VaccineTypeRepository;
import com.bach.vetclinic.web.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @Transactional
public class VaccineTypeService {
    private final VaccineTypeRepository repository;
    public VaccineTypeService(VaccineTypeRepository repository){this.repository=repository;}
    @Transactional(readOnly=true) public List<VaccineType> list(){return repository.findAllByOrderByNameAsc();}
    @Transactional(readOnly=true) public VaccineType get(Long id){return repository.findById(id).orElseThrow(()->new NotFoundException("Không tìm thấy vaccine"));}
    public VaccineType create(String name,String manufacturer,String description){return repository.save(new VaccineType(name.trim(),manufacturer,description));}
    public VaccineType update(Long id,String name,String manufacturer,String description){var v=get(id);v.update(name.trim(),manufacturer,description);return v;}
    public VaccineType active(Long id,boolean active){var v=get(id);v.setActive(active);return v;}
}
