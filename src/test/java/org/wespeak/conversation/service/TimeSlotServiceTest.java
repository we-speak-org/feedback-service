package org.wespeak.conversation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wespeak.conversation.dto.CreateTimeSlotRequest;
import org.wespeak.conversation.dto.TimeSlotDto;
import org.wespeak.conversation.dto.TimeSlotsResponse;
import org.wespeak.conversation.entity.TimeSlot;
import org.wespeak.conversation.exception.ResourceNotFoundException;
import org.wespeak.conversation.repository.RegistrationRepository;
import org.wespeak.conversation.repository.TimeSlotRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private TimeSlotService timeSlotService;

    private TimeSlot testSlot;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        testSlot = TimeSlot.builder()
                .id("slot-1")
                .targetLanguageCode("en")
                .level(TimeSlot.Level.A2)
                .startTime(now.plus(1, ChronoUnit.HOURS))
                .durationMinutes(30)
                .maxParticipants(8)
                .minParticipants(2)
                .isActive(true)
                .build();
    }

    @Test
    void findTimeSlots_shouldReturnSlots() {
        // Given
        when(timeSlotRepository.findByTargetLanguageCodeAndIsActiveAndStartTimeBetweenOrderByStartTime(
                eq("en"), eq(true), any(), any()))
                .thenReturn(List.of(testSlot));
        when(registrationRepository.countByTimeSlotIdAndStatus(any(), any()))
                .thenReturn(2L);

        // When
        TimeSlotsResponse response = timeSlotService.findTimeSlots("en", null, null, null, 0, 20);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getTimeslots().size());
        assertEquals("slot-1", response.getTimeslots().get(0).getId());
        assertEquals(2, response.getTimeslots().get(0).getRegisteredCount());
        assertEquals(6, response.getTimeslots().get(0).getAvailableSpots());
    }

    @Test
    void findById_shouldReturnSlot() {
        // Given
        when(timeSlotRepository.findById("slot-1")).thenReturn(Optional.of(testSlot));
        when(registrationRepository.countByTimeSlotIdAndStatus(any(), any())).thenReturn(0L);

        // When
        TimeSlotDto result = timeSlotService.findById("slot-1");

        // Then
        assertNotNull(result);
        assertEquals("slot-1", result.getId());
        assertEquals("en", result.getTargetLanguageCode());
        assertEquals(TimeSlot.Level.A2, result.getLevel());
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        // Given
        when(timeSlotRepository.findById("invalid")).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, () -> timeSlotService.findById("invalid"));
    }

    @Test
    void create_shouldCreateSlot() {
        // Given
        CreateTimeSlotRequest request = CreateTimeSlotRequest.builder()
                .targetLanguageCode("fr")
                .level(TimeSlot.Level.B1)
                .startTime(now.plus(2, ChronoUnit.HOURS))
                .durationMinutes(45)
                .maxParticipants(6)
                .build();

        TimeSlot savedSlot = TimeSlot.builder()
                .id("new-slot")
                .targetLanguageCode("fr")
                .level(TimeSlot.Level.B1)
                .startTime(request.getStartTime())
                .durationMinutes(45)
                .maxParticipants(6)
                .isActive(true)
                .build();

        when(timeSlotRepository.save(any())).thenReturn(savedSlot);
        when(registrationRepository.countByTimeSlotIdAndStatus(any(), any())).thenReturn(0L);

        // When
        TimeSlotDto result = timeSlotService.create(request);

        // Then
        assertNotNull(result);
        assertEquals("new-slot", result.getId());
        assertEquals("fr", result.getTargetLanguageCode());
        assertEquals(TimeSlot.Level.B1, result.getLevel());
        verify(timeSlotRepository).save(any());
    }

    @Test
    void delete_shouldDeactivateSlot() {
        // Given
        when(timeSlotRepository.findById("slot-1")).thenReturn(Optional.of(testSlot));
        when(timeSlotRepository.save(any())).thenReturn(testSlot);

        // When
        timeSlotService.delete("slot-1");

        // Then
        verify(timeSlotRepository).save(argThat(slot -> !slot.getIsActive()));
    }
}
