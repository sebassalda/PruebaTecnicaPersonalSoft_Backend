package com.jssv.brp_offer_management.security.config;

import com.jssv.brp_offer_management.security.entity.ModuleEntity;
import com.jssv.brp_offer_management.security.entity.RoleEntity;
import com.jssv.brp_offer_management.security.entity.UserEntity;
import com.jssv.brp_offer_management.security.entity.UserModuleEntity;
import com.jssv.brp_offer_management.security.repository.ModuleRepository;
import com.jssv.brp_offer_management.security.repository.RoleRepository;
import com.jssv.brp_offer_management.security.repository.UserModuleRepository;
import com.jssv.brp_offer_management.security.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ModuleRepository moduleRepository;
    private final UserModuleRepository userModuleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {

        String email = environment.getProperty("APP_ADMIN_EMAIL");
        String password = environment.getProperty("APP_ADMIN_PASSWORD");

        if (email == null || email.isBlank()
                || password == null || password.isBlank()) {
            return;
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        transactionTemplate.executeWithoutResult(status -> {

            if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                return;
            }

            RoleEntity adminRole = roleRepository
                    .findByNombre("ADMINISTRADOR")
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe el rol ADMINISTRADOR"
                    ));

            UserEntity admin = new UserEntity();
            admin.setNombre("Administrador inicial");
            admin.setEmail(normalizedEmail);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setActivo(true);
            admin.setRol(adminRole);

            UserEntity savedAdmin =
                    userRepository.saveAndFlush(admin);

            List<ModuleEntity> modules =
                    moduleRepository.findAll();

            for (ModuleEntity module : modules) {

                UserModuleEntity assignment =
                        new UserModuleEntity();

                assignment.setUsuarioId(savedAdmin.getId());
                assignment.setModuloId(module.getId());

                userModuleRepository.save(assignment);
            }
        });
    }
}
