package com.hsf.hotel.user.service;

import com.hsf.hotel.notification.service.NotificationProducer;
import com.hsf.hotel.user.model.User;
import com.hsf.hotel.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceEmailTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private NotificationProducer notificationProducer;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, notificationProducer);
    }

    @Test
    void registrationPersistsCanonicalEmail() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password1")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10);
            return user;
        });

        User registered = userService.registerUser(
                "alice", "password1", "  Alice@Example.COM  ", "Alice");

        assertEquals("alice@example.com", registered.getEmail());
        verify(userRepository).findByEmailIgnoreCase("alice@example.com");
        verify(notificationProducer).sendEmailNotification(any());
    }

    @Test
    void resendUsesCanonicalLookupAndDoesNotRevealMissingAccount() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com"))
                .thenReturn(Optional.empty());

        userService.resendVerificationEmail(" Missing@Example.COM ");

        verify(userRepository).findByEmailIgnoreCase("missing@example.com");
        verify(userRepository, never()).save(any());
        verify(notificationProducer, never()).sendEmailNotification(any());
    }
}
