package org.wespeak.conversation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.wespeak.conversation.dto.RegistrationDto;
import org.wespeak.conversation.entity.Registration;
import org.wespeak.conversation.entity.TimeSlot;
import org.wespeak.conversation.exception.RegistrationException;
import org.wespeak.conversation.exception.ResourceNotFoundException;
import org.wespeak.conversation.repository.RegistrationRepository;
import org.wespeak.conversation.repository.TimeSlotRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private TimeSlotService timeSlotService;

    @InjectMocks
    private RegistrationService registrationService;

    private TimeSlot testSlot;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        testSlot = TimeSlot.builder()
                .id("slot-1")
                .targetLanguageCode("en")
                .level(TimeSlot.Level.A2)
                .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
                .durationMinutes(30)
                .maxParticipants(8)
                .build();

        // Set default values via reflection
        ReflectionTestUtils.setField(registrationService, "maxActiveRegistrations", 3);
        ReflectionTestUtils.setField(registrationService, "cancellationDeadlineMinutes", 15);
        ReflectionTestUtils.setField(registrationService, "registrationDeadlineMinutes", 5);
    }

    @Test
    void register_shouldCreateRegistration() {
        // Given
        when(timeSlotRepository.findById("slot-1")).thenReturn(Optional.of(testSlot));
        when(registrationRepository.existsByTimeSlotIdAndUserIdAndStatus(any(), any(), any())).thenReturn(false);
        when(registrationRepository.countByUserIdAndStatus(any(), any())).thenReturn(0L);
        when(registrationRepository.countByTimeSlotIdAndStatus(any(), any())).thenReturn(0L);
        when(registrationRepository.save(any())).thenAnswer(inv -> {
            Registration reg = inv.getArgument(0);
            reg.setId("reg-1");
            return reg;
        });

        // When
        RegistrationDto result = registrationService.register("slot-1", userId);

        // Then
        assertNotNull(result);
        assertEquals("reg-1", result.getId());
        assertEquals("slot-1", result.getTimeSlotId());
        verify(registrationRepository).save(any());
    }

    @Test
    void register_shouldThrowWhenSlotNotFound() {
        // Given
        when(timeSlotRepository.findById("invalid")).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, 
                () -> registrationService.register("invalid", userId));
    }

    @Test
    void register_shouldThrowWhenAlreadyRegistered() {
        // Given
        when(timeSlotRepository.findById("slot-1")).thenReturn(Optional.of(testSlot));
        when(registrationRepository.existsByTimeSlotIdAndUserIdAndStatus(
                "slot-1", userId, Registration.Status.registered)).thenReturn(true);

        // When/Then
        RegistrationException ex = assertThrows(RegistrationException.class,
                () -> registrationService.register("slot-1", userId));
        assertEquals("ALREADY_REGISTERED", ex.getCode());
    }

    @Test
    void register_shouldThrowWhenSlotFull() {
        // Given
        when(timeSlotRepository.findById("slot-1")).thenReturn(Optional.of(testSlot));
        when(registrationRepository.existsByTimeSlotIdAndUserIdAndStatus(any(), any(), any())).thenReturn(false);
        when(registrationRepository.countByUserIdAndStatus(any(), any())).thenReturn(0L);
        when(registrationRepository.countByTimeSlotIdAndStatus(any(), any())).thenReturn(8L); // Full

        // When/Then
        RegistrationException ex = assertThrows(RegistrationException.class,
                () -> registrationService.register("slot-1", userId));
        assertEquals("SLOT_FULL", ex.getCode());
    }

    @Test
    void register_shouldThrowWhenMaxRegistrationsReached() {
        // Given
        when(timeSlotRepository.findById("slot-1")).thenReturn(Optional.of(testSlot));
        when(registrationRepository.existsByTimeSlotIdAndUserIdAndStatus(any(), any(), any())).thenReturn(false);
        when(registrationRepository.countByUserIdAndStatus(userId, Registration.Status.registered)).thenReturn(3L);

        // When/Then
        RegistrationException ex = assertThrows(RegistrationException.class,
                () -> registrationService.register("slot-1", userId));
        assertEquals("MAX_REGISTRATIONS", ex.getCode());
    }

    @Test
    void register_shouldThrowWhenRegistrationClosed() {
        // Given - slot starts in 2 minutes (past the 5 min deadline)
        TimeSlot closingSlot = TimeSlot.builder()
                .id("slot-closing")
                .startTime(Instant.now().plus(2, ChronoUnit.MINUTES))
                .maxParticipants(8)
                .build();
        when(timeSlotRepository.findById("slot-closing")).thenReturn(Optional.of(closingSlot));

        // When/Then
        RegistrationException ex = assertThrows(RegistrationException.class,
                () -> registrationService.register("slot-closing", userId));
        assertEquals("REGISTRATION_CLOSED", ex.getCode());
    }

    @Test
    void unregister_shouldCancelRegistration() {
        // Given
        Registration registration = Registration.builder()
                .id("reg-1")
                .timeSlotId("slot-1")
                .userId(userId)
                .status(Registration.Status.registered)
                .build();

        when(registrationRepository.findByTimeSlotIdAndUserId("slot-1", userId))
                .thenReturn(Optional.of(registration));
        when(timeSlotRepository.findById("slot-1")).thenReturn(Optional.of(testSlot));
        when(registrationRepository.save(any())).thenReturn(registration);

        // When
        registrationService.unregister("slot-1", userId);

        // Then
        verify(registrationRepository).save(argThat(reg -> 
                reg.getStatus() == Registration.Status.cancelled));
    }

    @Test
    void isUserRegistered_shouldReturnTrue() {
        // Given
        when(registrationRepository.existsByTimeSlotIdAndUserIdAndStatus(
                "slot-1", userId, Registration.Status.registered)).thenReturn(true);

        // When/Then
        assertTrue(registrationService.isUserRegistered("slot-1", userId));
    }
}
