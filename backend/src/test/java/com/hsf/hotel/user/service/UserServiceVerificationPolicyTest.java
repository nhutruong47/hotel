package com.hsf.hotel.user.service;

import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.notification.service.NotificationProducer;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** R-12: no silent email-verification bypass when the email provider is disabled. */
@ExtendWith(MockitoExtension.class)
class UserServiceVerificationPolicyTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private NotificationProducer notificationProducer;

    private UserService service;
    private User unverified;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoder, notificationProducer);
        ReflectionTestUtils.setField(service, "emailEnabled", false);
        unverified = new User();
        unverified.setId(5);
        unverified.setUsername("guest");
        unverified.setPassword("hash");
        unverified.setEmailVerified(false);
    }

    @Test
    void loginOfUnverifiedAccountIsRefusedByDefault() {
        when(userRepository.findByUsername("guest")).thenReturn(Optional.of(unverified));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

        assertThat(service.login("guest", "pw")).isEmpty();
        assertThat(unverified.getEmailVerified()).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void registrationIsUnavailableWithoutEmailProviderByDefault() {
        assertThatThrownBy(() -> service.registerUser("newguest", "Str0ng!Pass", "new@example.test", "New"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(e -> ((BusinessRuleException) e).getCode()).isEqualTo("REGISTRATION_UNAVAILABLE");
        verify(userRepository, never()).save(any());
    }

    @Test
    void explicitOptInAutoVerifiesOnLogin() {
        ReflectionTestUtils.setField(service, "autoVerifyWithoutEmail", true);
        when(userRepository.findByUsername("guest")).thenReturn(Optional.of(unverified));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

        assertThat(service.login("guest", "pw")).isPresent();
        assertThat(unverified.getEmailVerified()).isTrue();
    }
}
