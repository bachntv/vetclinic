package com.bach.vetclinic.controller;

import com.bach.vetclinic.entity.User;
import com.bach.vetclinic.entity.UserRole;
import com.bach.vetclinic.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.findAll().stream().map(UserResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(userService.create(request.fullName(), request.email(), request.password(), request.role(), request.vetId()));
    }
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id,@Valid @RequestBody UpdateUserRequest request,org.springframework.security.core.Authentication auth){
        if(userService.findAll().stream().anyMatch(u->u.getId().equals(id)&&u.getEmail().equalsIgnoreCase(auth.getName()))&&request.role()!=com.bach.vetclinic.entity.UserRole.ADMIN)
            throw new com.bach.vetclinic.web.ConflictException("Không thể tự bỏ quyền quản trị viên");
        return UserResponse.from(userService.update(id,request.fullName(),request.email(),request.role(),request.vetId()));
    }
    public record UpdateUserRequest(@NotBlank @Size(max=150) String fullName,@NotBlank @Email @Size(max=255) String email,
                                     @NotNull UserRole role,@jakarta.validation.constraints.Positive Long vetId){}

    @PatchMapping("/{id}/enabled")
    public UserResponse setEnabled(@PathVariable Long id, @Valid @RequestBody EnabledRequest request, org.springframework.security.core.Authentication authentication) {
        if (!request.enabled() && userService.findAll().stream().anyMatch(u -> u.getId().equals(id) && u.getEmail().equalsIgnoreCase(authentication.getName())))
            throw new com.bach.vetclinic.web.ConflictException("Không thể tự khóa tài khoản đang đăng nhập");
        return UserResponse.from(userService.setEnabled(id, request.enabled()));
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.password());
    }

    public record CreateUserRequest(@NotBlank @Size(max = 150) String fullName,
                                    @NotBlank @Email @Size(max = 255) String email,
                                    @NotBlank @Size(min = 12, max = 100) String password,
                                    @NotNull UserRole role,@jakarta.validation.constraints.Positive Long vetId) {}

    public record EnabledRequest(@NotNull Boolean enabled) {}
    public record ResetPasswordRequest(@NotBlank @Size(min = 12, max = 100) String password) {}

    public record UserResponse(Long id, String fullName, String email, UserRole role, boolean enabled,
                               LocalDateTime createdAt, LocalDateTime updatedAt,Long vetId,String vetName) {
        static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                    user.isEnabled(), user.getCreatedAt(), user.getUpdatedAt(),user.getVet()==null?null:user.getVet().getId(),user.getVet()==null?null:user.getVet().getName());
        }
    }
}
