package com.hsf.hotel.profile.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsf.hotel.common.dto.PreferencesDTO;
import com.hsf.hotel.config.ErrorCodes;
import com.hsf.hotel.exception.BusinessRuleException;
import com.hsf.hotel.notification.service.NotificationProducer;
import com.hsf.hotel.profile.dto.ProfileDTO;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceConsistencyTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private NotificationProducer notificationProducer;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(
                userRepository, passwordEncoder, notificationProducer, objectMapper);
    }

    @Test
    void profileRejectsDuplicateEmailIgnoringCase() {
        User current = user(1, "alice", "alice@example.com");
        User owner = user(2, "bob", "bob@example.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(current));
        when(userRepository.findByEmailIgnoreCase("bob@example.com")).thenReturn(Optional.of(owner));

        ProfileDTO update = new ProfileDTO();
        update.setFullName("Alice");
        update.setEmail("  BOB@EXAMPLE.COM ");

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> profileService.updateProfile("alice", update, null));

        assertEquals(ErrorCodes.EMAIL_TAKEN, exception.getCode());
    }

    @Test
    void partialPreferenceUpdatePreservesUnspecifiedFieldsUnderLock() throws Exception {
        User current = user(1, "alice", "alice@example.com");
        current.setPreferencesJson("{\"theme\":\"dark\",\"emailBooking\":true}");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(current));
        when(userRepository.findByIdForUpdate(1)).thenReturn(Optional.of(current));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PreferencesDTO patch = new PreferencesDTO();
        patch.setEmailMarketing(true);

        PreferencesDTO result = profileService.updatePreferences("alice", patch);
        PreferencesDTO persisted = objectMapper.readValue(current.getPreferencesJson(), PreferencesDTO.class);

        assertEquals("dark", result.getTheme());
        assertTrue(result.getEmailBooking());
        assertTrue(result.getEmailMarketing());
        assertEquals("dark", persisted.getTheme());
        assertTrue(persisted.getEmailBooking());
        assertTrue(persisted.getEmailMarketing());
    }

    @Test
    void invalidPreferenceIsRejectedAtServiceBoundary() {
        PreferencesDTO patch = new PreferencesDTO();
        patch.setTheme("sepia");

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> profileService.updatePreferences("alice", patch));

        assertEquals(ErrorCodes.INVALID_PREFERENCES, exception.getCode());
    }

    private static User user(int id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("encoded");
        return user;
    }
}
