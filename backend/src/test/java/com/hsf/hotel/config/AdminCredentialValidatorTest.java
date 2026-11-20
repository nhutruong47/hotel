package com.hsf.hotel.config;

import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** R-11: the guessable admin/admin123 account may only ever be created for an explicit "dev" profile. */
@ExtendWith(MockitoExtension.class)
class AdminCredentialValidatorTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private AdminCredentialValidator validator(String user, String pass, String profile) {
        return new AdminCredentialValidator(userRepository, passwordEncoder, user, pass, "ops@example.test", profile);
    }

    @Test
    void defaultProfileWithoutCredentialsCreatesNoAdmin() {
        validator("", "", "default").run();
        verify(userRepository, never()).save(any());
    }

    @Test
    void localPostgresProfileWithoutCredentialsCreatesNoAdmin() {
        validator("", "", "postgres").run();
        verify(userRepository, never()).save(any());
    }

    @Test
    void explicitDevProfileMayUseInsecureDefault() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        validator("", "", "dev").run();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("admin");
    }

    @Test
    void configuredCredentialsAreUsedOutsideDev() {
        when(userRepository.findByUsername("owner")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("S3cure!Passphrase")).thenReturn("hashed");

        validator("owner", "S3cure!Passphrase", "postgres").run();

        verify(passwordEncoder).encode("S3cure!Passphrase");
        verify(userRepository).save(any());
    }

    @Test
    void prodWithoutCredentialsFailsFast() {
        assertThatThrownBy(() -> validator("", "", "prod").run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Admin username not configured");
    }

    @Test
    void prodRejectsWeakPassword() {
        assertThatThrownBy(() -> validator("owner", "admin123", "prod").run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("too weak");
    }
}
