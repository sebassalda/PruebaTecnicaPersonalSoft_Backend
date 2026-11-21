package com.jssv.brp_offer_management.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordConfigTest {

    private final PasswordEncoder passwordEncoder =
            new PasswordConfig().passwordEncoder();

    @Test
    void shouldEncodePasswordUsingBCrypt() {

        String rawPassword = "SecurePassword123!";

        String encodedPassword =
                passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encodedPassword.startsWith("$2"));
        assertTrue(passwordEncoder.matches(
                rawPassword,
                encodedPassword
        ));
    }

    @Test
    void shouldRejectIncorrectPassword() {

        String encodedPassword =
                passwordEncoder.encode("SecurePassword123!");

        assertFalse(passwordEncoder.matches(
                "WrongPassword123!",
                encodedPassword
        ));
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {

        String rawPassword = "SecurePassword123!";

        String firstHash = passwordEncoder.encode(rawPassword);
        String secondHash = passwordEncoder.encode(rawPassword);

        assertNotEquals(firstHash, secondHash);

        assertTrue(passwordEncoder.matches(
                rawPassword,
                firstHash
        ));

        assertTrue(passwordEncoder.matches(
                rawPassword,
                secondHash
        ));
    }
}
