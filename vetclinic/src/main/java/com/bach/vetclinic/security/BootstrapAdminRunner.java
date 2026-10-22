package com.bach.vetclinic.security;

import com.bach.vetclinic.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BootstrapAdminRunner {

    @Bean
    ApplicationRunner createConfiguredAdmin(
            UserService userService,
            @Value("${app.bootstrap-admin.full-name:}") String fullName,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password) {
        return arguments -> {
            boolean configured = !email.isBlank() || !password.isBlank() || !fullName.isBlank();
            if (!configured) return;
            if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
                throw new IllegalStateException("Cần cấu hình đầy đủ BOOTSTRAP_ADMIN_FULL_NAME, BOOTSTRAP_ADMIN_EMAIL và BOOTSTRAP_ADMIN_PASSWORD");
            }
            if (password.length() < 12) {
                throw new IllegalStateException("Mật khẩu tài khoản quản trị ban đầu cần ít nhất 12 ký tự");
            }
            userService.createBootstrapAdmin(fullName, email, password);
        };
    }
}
