package com.bach.vetclinic.service;

import com.bach.vetclinic.entity.User;
import com.bach.vetclinic.entity.UserRole;
import com.bach.vetclinic.repository.UserRepository;
import com.bach.vetclinic.web.ConflictException;
import com.bach.vetclinic.web.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.bach.vetclinic.repository.VetRepository vets;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, com.bach.vetclinic.repository.VetRepository vets) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.vets = vets;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll().stream().sorted((a, b) -> a.getId().compareTo(b.getId())).toList();
    }

    public User create(String fullName, String email, String rawPassword, UserRole role) {
        return create(fullName,email,rawPassword,role,null);
    }
    public User create(String fullName, String email, String rawPassword, UserRole role, Long vetId) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email này đã được sử dụng cho tài khoản khác");
        }
        User user=new User(fullName.trim(), normalizedEmail,passwordEncoder.encode(rawPassword),role);
        user.updateProfile(fullName.trim(),normalizedEmail,role,resolveVet(role,vetId,null,fullName));
        return userRepository.save(user);
    }

    public User update(Long id,String fullName,String email,UserRole role,Long vetId){
        var accounts=userRepository.lockAccounts();
        User user=findOrThrow(id);
        protectAdmin(accounts,user,role,user.isEnabled());
        String normalized=normalizeEmail(email);
        userRepository.findByEmailIgnoreCase(normalized).filter(other->!other.getId().equals(id)).ifPresent(other->{throw new ConflictException("Email đã được sử dụng");});
        var vet=resolveVet(role,vetId,user,fullName);
        user.updateProfile(fullName.trim(),normalized,role,vet);
        if(vet!=null)vet.updateProfile(fullName.trim(),vet.getSpecialty(),vet.getLicenseNo(),vet.getWorkHours());
        return user;
    }

    private com.bach.vetclinic.entity.Vet resolveVet(UserRole role,Long vetId,User current,String fullName){
        if(role!=UserRole.VET){if(vetId!=null)throw new ConflictException("Chỉ tài khoản bác sĩ mới được liên kết hồ sơ bác sĩ");return null;}
        if(vetId==null&&current!=null&&current.getVet()!=null)vetId=current.getVet().getId();
        if(vetId==null)return vets.save(new com.bach.vetclinic.entity.Vet(fullName.trim(),null,null,null));
        var vet=vets.lockById(vetId).orElseThrow(()->new NotFoundException("Không tìm thấy hồ sơ bác sĩ"));
        if(!vet.isActive())throw new ConflictException("Hồ sơ bác sĩ đã ngừng hoạt động");
        userRepository.findByVetId(vetId).filter(u->current==null||!u.getId().equals(current.getId())).ifPresent(u->{throw new ConflictException("Hồ sơ bác sĩ đã gắn với tài khoản khác");});
        return vet;
    }

    private void protectAdmin(List<User> accounts,User user,UserRole nextRole,boolean enabled){
        if(user.getRole()==UserRole.ADMIN&&user.isEnabled()&&(nextRole!=UserRole.ADMIN||!enabled)
                &&accounts.stream().filter(u->u.isEnabled()&&u.getRole()==UserRole.ADMIN).count()<=1)
            throw new ConflictException("Phải giữ ít nhất một quản trị viên đang hoạt động");
    }

    public User setEnabled(Long id, boolean enabled) {
        var accounts=userRepository.lockAccounts();
        User user = findOrThrow(id);
        protectAdmin(accounts,user,user.getRole(),enabled);
        if(enabled&&user.getVet()!=null&&!user.getVet().isActive())throw new ConflictException("Cần kích hoạt hồ sơ bác sĩ trước khi mở tài khoản");
        user.setEnabled(enabled);
        return user;
    }

    public User resetPassword(Long id, String rawPassword) {
        User user = findOrThrow(id);
        user.changePasswordHash(passwordEncoder.encode(rawPassword));
        return user;
    }

    public User createBootstrapAdmin(String fullName, String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmailIgnoreCase(normalizedEmail).orElseGet(() ->
                userRepository.save(new User(fullName.trim(), normalizedEmail,
                        passwordEncoder.encode(rawPassword), UserRole.ADMIN)));
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản có mã " + id));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
