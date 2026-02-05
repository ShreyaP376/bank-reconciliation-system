package com.bank.reconciliation.config;

import com.bank.reconciliation.entity.User;
import com.bank.reconciliation.entity.UserRole;
import com.bank.reconciliation.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(new User("admin@test.com", passwordEncoder.encode("admin"), UserRole.ADMIN));
            userRepository.save(new User("editor@test.com", passwordEncoder.encode("editor"), UserRole.EDITOR));
            userRepository.save(new User("viewer@test.com", passwordEncoder.encode("viewer"), UserRole.VIEWER));
        }
    }
}
