package org.wespeak.conversation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.wespeak.conversation.dto.RegistrationDto;
import org.wespeak.conversation.dto.RegistrationsResponse;
import org.wespeak.conversation.dto.TimeSlotDto;
import org.wespeak.conversation.entity.Registration;
import org.wespeak.conversation.entity.TimeSlot;
import org.wespeak.conversation.exception.RegistrationException;
import org.wespeak.conversation.exception.ResourceNotFoundException;
import org.wespeak.conversation.repository.RegistrationRepository;
import org.wespeak.conversation.repository.TimeSlotRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final TimeSlotService timeSlotService;

    @Value("${app.conversation.max-active-registrations:3}")
    private int maxActiveRegistrations;

    @Value("${app.conversation.cancellation-deadline-minutes:15}")
    private int cancellationDeadlineMinutes;

    @Value("${app.conversation.registration-deadline-minutes:5}")
    private int registrationDeadlineMinutes;

    /**
     * Register a user for a time slot.
     */
    public RegistrationDto register(String timeSlotId, String userId) {
        TimeSlot slot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> ResourceNotFoundException.timeSlotNotFound(timeSlotId));

        // Check registration deadline
        Instant deadline = slot.getStartTime().minus(registrationDeadlineMinutes, ChronoUnit.MINUTES);
        if (Instant.now().isAfter(deadline)) {
            throw RegistrationException.registrationClosed();
        }

        // Check if already registered
        if (registrationRepository.existsByTimeSlotIdAndUserIdAndStatus(timeSlotId, userId, Registration.Status.registered)) {
            throw RegistrationException.alreadyRegistered();
        }

        // Check max active registrations
        long activeCount = registrationRepository.countByUserIdAndStatus(userId, Registration.Status.registered);
        if (activeCount >= maxActiveRegistrations) {
            throw RegistrationException.maxRegistrations(maxActiveRegistrations);
        }

        // Check slot capacity
        long registeredCount = registrationRepository.countByTimeSlotIdAndStatus(timeSlotId, Registration.Status.registered);
        if (registeredCount >= slot.getMaxParticipants()) {
            throw RegistrationException.slotFull();
        }

        // Create registration
        Registration registration = Registration.builder()
                .timeSlotId(timeSlotId)
                .userId(userId)
                .status(Registration.Status.registered)
                .build();

        registration = registrationRepository.save(registration);
        log.info("User {} registered for time slot {}", userId, timeSlotId);

        return toRegistrationDto(registration);
    }

    /**
     * Cancel a registration.
     */
    public void unregister(String timeSlotId, String userId) {
        Registration registration = registrationRepository.findByTimeSlotIdAndUserId(timeSlotId, userId)
                .orElseThrow(RegistrationException::notRegistered);

        if (registration.getStatus() != Registration.Status.registered) {
            throw RegistrationException.notRegistered();
        }

        TimeSlot slot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> ResourceNotFoundException.timeSlotNotFound(timeSlotId));

        // Check cancellation deadline
        Instant deadline = slot.getStartTime().minus(cancellationDeadlineMinutes, ChronoUnit.MINUTES);
        if (Instant.now().isAfter(deadline)) {
            throw RegistrationException.cancellationDeadlinePassed();
        }

        registration.setStatus(Registration.Status.cancelled);
        registration.setCancelledAt(Instant.now());
        registrationRepository.save(registration);

        log.info("User {} cancelled registration for time slot {}", userId, timeSlotId);
    }

    /**
     * Get registrations for a user.
     */
    public RegistrationsResponse getUserRegistrations(String userId) {
        List<Registration> registrations = registrationRepository.findByUserIdOrderByRegisteredAtDesc(userId);

        List<RegistrationDto> dtos = registrations.stream()
                .filter(r -> r.getStatus() == Registration.Status.registered)
                .map(this::toRegistrationDto)
                .collect(Collectors.toList());

        return RegistrationsResponse.builder()
                .registrations(dtos)
                .build();
    }

    /**
     * Check if user is registered for a time slot.
     */
    public boolean isUserRegistered(String timeSlotId, String userId) {
        return registrationRepository.existsByTimeSlotIdAndUserIdAndStatus(
                timeSlotId, userId, Registration.Status.registered);
    }

    /**
     * Get all registered users for a time slot.
     */
    public List<Registration> getRegisteredUsers(String timeSlotId) {
        return registrationRepository.findByTimeSlotIdAndStatus(timeSlotId, Registration.Status.registered);
    }

    /**
     * Mark registration as attended.
     */
    public void markAttended(String timeSlotId, String userId) {
        registrationRepository.findByTimeSlotIdAndUserId(timeSlotId, userId)
                .ifPresent(reg -> {
                    reg.setStatus(Registration.Status.attended);
                    registrationRepository.save(reg);
                });
    }

    /**
     * Mark registration as no-show.
     */
    public void markNoShow(String timeSlotId, String userId) {
        registrationRepository.findByTimeSlotIdAndUserId(timeSlotId, userId)
                .ifPresent(reg -> {
                    if (reg.getStatus() == Registration.Status.registered) {
                        reg.setStatus(Registration.Status.noshow);
                        registrationRepository.save(reg);
                    }
                });
    }

    private RegistrationDto toRegistrationDto(Registration registration) {
        TimeSlotDto timeSlotDto = null;
        try {
            timeSlotDto = timeSlotService.findById(registration.getTimeSlotId());
        } catch (Exception e) {
            log.warn("Could not fetch time slot for registration {}", registration.getId());
        }

        return RegistrationDto.builder()
                .id(registration.getId())
                .timeSlotId(registration.getTimeSlotId())
                .timeSlot(timeSlotDto)
                .status(registration.getStatus())
                .registeredAt(registration.getRegisteredAt())
                .build();
    }
}
