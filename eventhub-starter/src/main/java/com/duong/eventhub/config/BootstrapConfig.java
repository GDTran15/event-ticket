package com.duong.eventhub.config;

import com.duong.eventhub.user.AppUser;
import com.duong.eventhub.user.AppUserRepository;
import com.duong.eventhub.user.Role;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean CommandLineRunner adminBootstrap(AppUserRepository users, PasswordEncoder passwords,
        @Value("${app.bootstrap.admin-email:}") String email,
        @Value("${app.bootstrap.admin-password:}") String password) {
        return args -> {
            if (email.isBlank() && password.isBlank()) return;
            if (email.isBlank() || password.length() < 12) throw new IllegalStateException("Set both admin email and password (12+ chars), or neither");
            String normalized = email.trim().toLowerCase(Locale.ROOT);
            if (users.existsByEmail(normalized)) return;
            AppUser admin = new AppUser();
            admin.setEmail(normalized); admin.setDisplayName("Administrator");
            admin.setEnabled(true); admin.setPasswordHash(passwords.encode(password));
            admin.getRoles().add(Role.ADMIN);
            users.save(admin);
        };
    }
}
