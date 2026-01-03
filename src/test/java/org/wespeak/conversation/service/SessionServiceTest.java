package org.wespeak.conversation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.wespeak.conversation.dto.JoinSessionRequest;
import org.wespeak.conversation.dto.MediaStateRequest;
import org.wespeak.conversation.dto.ParticipantDto;
import org.wespeak.conversation.dto.SessionDto;
import org.wespeak.conversation.entity.Participant;
import org.wespeak.conversation.entity.Session;
import org.wespeak.conversation.entity.TimeSlot;
import org.wespeak.conversation.exception.SessionException;
import org.wespeak.conversation.repository.ParticipantRepository;
import org.wespeak.conversation.repository.SessionRepository;
import org.wespeak.conversation.repository.TimeSlotRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private RegistrationService registrationService;

    @Mock
    private ConversationEventPublisher eventPublisher;

    @InjectMocks
    private SessionService sessionService;

    private TimeSlot testSlot;
    private Session testSession;
    private String userId;

    @BeforeEach
    void setUp() {
        userId = "user-123";
        
        testSlot = TimeSlot.builder()
                .id("slot-1")
                .targetLanguageCode("en")
                .level(TimeSlot.Level.A2)
                .startTime(Instant.now()) // Starting now
                .durationMinutes(30)
                .maxParticipants(8)
                .build();

        testSession = Session.builder()
                .id("session-1")
                .timeSlotId("slot-1")
                .targetLanguageCode("en")
                .level(TimeSlot.Level.A2)
                .status(Session.Status.waiting)
                .build();

        ReflectionTestUtils.setField(sessionService, "gracePeriodMinutes", 5);
        ReflectionTestUtils.setField(sessionService, "maxParticipants", 8);
        ReflectionTestUtils.setField(sessionService, "minParticipants", 2);
    }

    @Test
    void joinSession_shouldCreateParticipant() {
        // Given
        JoinSessionRequest request = JoinSessionRequest.builder()
                .timeSlotId("slot-1")
                .recordingConsent(true)
                .displayName("Test User")
                .build();

        when(registrationService.isUserRegistered("slot-1", userId)).thenReturn(true);
        when(participantRepository.findByUserIdAndStatusNot(userId, Participant.Status.disconnected))
                .thenReturn(Optional.empty());
        when(sessionRepository.findByTimeSlotIdAndStatusIn(eq("slot-1"), any(), any()))
                .thenReturn(Optional.of(testSession));
        when(timeSlotRepository.findById("slot-1")).thenReturn(Optional.of(testSlot));
        when(participantRepository.countBySessionIdAndStatus(any(), eq(Participant.Status.connected)))
                .thenReturn(0L);
        when(participantRepository.findBySessionIdAndUserId("session-1", userId))
                .thenReturn(Optional.empty());
        when(participantRepository.save(any())).thenAnswer(inv -> {
            Participant p = inv.getArgument(0);
            p.setId("participant-1");
            return p;
        });
        when(sessionRepository.save(any())).thenReturn(testSession);
        when(participantRepository.findBySessionIdAndStatusNot(any(), any())).thenReturn(List.of());

        // When
        SessionDto result = sessionService.joinSession(userId, request);

        // Then
        assertNotNull(result);
        assertEquals("session-1", result.getId());
        verify(participantRepository).save(any());
        verify(registrationService).markAttended("slot-1", userId);
    }

    @Test
    void joinSession_shouldThrowWhenNotRegistered() {
        // Given
        JoinSessionRequest request = JoinSessionRequest.builder()
                .timeSlotId("slot-1")
                .build();
        when(registrationService.isUserRegistered("slot-1", userId)).thenReturn(false);

        // When/Then
        SessionException ex = assertThrows(SessionException.class,
                () -> sessionService.joinSession(userId, request));
        assertEquals("NOT_REGISTERED", ex.getCode());
    }

    @Test
    void joinSession_shouldThrowWhenAlreadyInSession() {
        // Given
        JoinSessionRequest request = JoinSessionRequest.builder()
                .timeSlotId("slot-1")
                .build();
        Participant existingParticipant = Participant.builder()
                .id("existing")
                .sessionId("other-session")
                .status(Participant.Status.connected)
                .build();

        when(registrationService.isUserRegistered("slot-1", userId)).thenReturn(true);
        when(participantRepository.findByUserIdAndStatusNot(userId, Participant.Status.disconnected))
                .thenReturn(Optional.of(existingParticipant));

        // When/Then
        SessionException ex = assertThrows(SessionException.class,
                () -> sessionService.joinSession(userId, request));
        assertEquals("ALREADY_IN_SESSION", ex.getCode());
    }

    @Test
    void updateMediaState_shouldUpdateParticipant() {
        // Given
        Participant participant = Participant.builder()
                .id("participant-1")
                .sessionId("session-1")
                .userId(userId)
                .status(Participant.Status.connected)
                .cameraEnabled(true)
                .micEnabled(true)
                .build();

        MediaStateRequest request = MediaStateRequest.builder()
                .cameraEnabled(false)
                .micEnabled(true)
                .build();

        when(participantRepository.findByUserIdAndStatusNot(userId, Participant.Status.disconnected))
                .thenReturn(Optional.of(participant));
        when(participantRepository.save(any())).thenReturn(participant);

        // When
        ParticipantDto result = sessionService.updateMediaState(userId, request);

        // Then
        assertNotNull(result);
        verify(participantRepository).save(argThat(p -> !p.getCameraEnabled() && p.getMicEnabled()));
    }

    @Test
    void leaveSession_shouldDisconnectParticipant() {
        // Given
        Participant participant = Participant.builder()
                .id("participant-1")
                .sessionId("session-1")
                .userId(userId)
                .status(Participant.Status.connected)
                .build();

        when(participantRepository.findByUserIdAndStatusNot(userId, Participant.Status.disconnected))
                .thenReturn(Optional.of(participant));
        when(participantRepository.save(any())).thenReturn(participant);
        when(sessionRepository.findById("session-1")).thenReturn(Optional.of(testSession));
        when(participantRepository.countBySessionIdAndStatus(any(), eq(Participant.Status.connected)))
                .thenReturn(1L);

        // When
        sessionService.leaveSession(userId);

        // Then
        verify(participantRepository).save(argThat(p -> 
                p.getStatus() == Participant.Status.disconnected && p.getLeftAt() != null));
    }

    @Test
    void leaveSession_shouldThrowWhenNoActiveSession() {
        // Given
        when(participantRepository.findByUserIdAndStatusNot(userId, Participant.Status.disconnected))
                .thenReturn(Optional.empty());

        // When/Then
        SessionException ex = assertThrows(SessionException.class,
                () -> sessionService.leaveSession(userId));
        assertEquals("NO_ACTIVE_SESSION", ex.getCode());
    }
}
