package org.wespeak.conversation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.wespeak.conversation.dto.*;
import org.wespeak.conversation.entity.Registration;
import org.wespeak.conversation.entity.TimeSlot;
import org.wespeak.conversation.exception.ResourceNotFoundException;
import org.wespeak.conversation.repository.RegistrationRepository;
import org.wespeak.conversation.repository.TimeSlotRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final RegistrationRepository registrationRepository;

    @Value("${app.conversation.max-participants:8}")
    private int defaultMaxParticipants;

    /**
     * List time slots with filters.
     */
    public TimeSlotsResponse findTimeSlots(
            String language,
            TimeSlot.Level level,
            Instant fromDate,
            Instant toDate,
            int page,
            int size) {

        // Default date range: now to 7 days ahead
        if (fromDate == null) fromDate = Instant.now();
        if (toDate == null) toDate = fromDate.plus(7, ChronoUnit.DAYS);

        List<TimeSlot> slots;
        if (language != null && level != null) {
            slots = timeSlotRepository.findByTargetLanguageCodeAndLevelAndIsActiveAndStartTimeBetweenOrderByStartTime(
                    language, level, true, fromDate, toDate);
        } else if (language != null) {
            slots = timeSlotRepository.findByTargetLanguageCodeAndIsActiveAndStartTimeBetweenOrderByStartTime(
                    language, true, fromDate, toDate);
        } else {
            Page<TimeSlot> pageResult = timeSlotRepository.findByIsActiveAndStartTimeBetweenOrderByStartTime(
                    true, fromDate, toDate, PageRequest.of(page, size));
            slots = pageResult.getContent();
        }

        List<TimeSlotDto> dtos = slots.stream()
                .map(this::toTimeSlotDto)
                .collect(Collectors.toList());

        return TimeSlotsResponse.builder()
                .timeslots(dtos)
                .total((long) dtos.size())
                .hasMore(false)
                .build();
    }

    /**
     * Get a time slot by ID.
     */
    public TimeSlotDto findById(String id) {
        TimeSlot slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.timeSlotNotFound(id));
        return toTimeSlotDto(slot);
    }

    /**
     * Create a new time slot.
     */
    public TimeSlotDto create(CreateTimeSlotRequest request) {
        TimeSlot slot = TimeSlot.builder()
                .targetLanguageCode(request.getTargetLanguageCode())
                .level(request.getLevel())
                .startTime(request.getStartTime())
                .durationMinutes(request.getDurationMinutes())
                .maxParticipants(request.getMaxParticipants() != null ? request.getMaxParticipants() : defaultMaxParticipants)
                .recurrence(request.getRecurrence() != null ? request.getRecurrence() : TimeSlot.Recurrence.once)
                .isActive(true)
                .build();

        slot = timeSlotRepository.save(slot);
        log.info("Created time slot: {} for {} {} at {}", slot.getId(), slot.getTargetLanguageCode(), 
                slot.getLevel(), slot.getStartTime());
        return toTimeSlotDto(slot);
    }

    /**
     * Update a time slot.
     */
    public TimeSlotDto update(String id, CreateTimeSlotRequest request) {
        TimeSlot slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.timeSlotNotFound(id));

        // Don't allow updates to past time slots
        if (slot.getStartTime().isBefore(Instant.now())) {
            throw new IllegalStateException("Cannot update a past time slot");
        }

        if (request.getTargetLanguageCode() != null) slot.setTargetLanguageCode(request.getTargetLanguageCode());
        if (request.getLevel() != null) slot.setLevel(request.getLevel());
        if (request.getStartTime() != null) slot.setStartTime(request.getStartTime());
        if (request.getDurationMinutes() != null) slot.setDurationMinutes(request.getDurationMinutes());
        if (request.getMaxParticipants() != null) slot.setMaxParticipants(request.getMaxParticipants());
        if (request.getRecurrence() != null) slot.setRecurrence(request.getRecurrence());

        slot = timeSlotRepository.save(slot);
        log.info("Updated time slot: {}", slot.getId());
        return toTimeSlotDto(slot);
    }

    /**
     * Delete (deactivate) a time slot.
     */
    public void delete(String id) {
        TimeSlot slot = timeSlotRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.timeSlotNotFound(id));

        slot.setIsActive(false);
        timeSlotRepository.save(slot);
        log.info("Deactivated time slot: {}", id);
    }

    /**
     * Scheduled job to generate recurring time slots.
     * Runs daily at midnight.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void generateRecurringSlots() {
        log.info("Starting recurring time slot generation...");
        
        Instant now = Instant.now();
        Instant endDate = now.plus(7, ChronoUnit.DAYS);

        // Find all recurring slots
        List<TimeSlot> recurringSlots = timeSlotRepository.findAll().stream()
                .filter(s -> s.getIsActive() && s.getRecurrence() != TimeSlot.Recurrence.once)
                .collect(Collectors.toList());

        int generated = 0;
        for (TimeSlot template : recurringSlots) {
            generated += generateSlotsFromTemplate(template, now, endDate);
        }

        log.info("Generated {} recurring time slots", generated);
    }

    private int generateSlotsFromTemplate(TimeSlot template, Instant from, Instant to) {
        int count = 0;
        Instant nextTime = template.getStartTime();

        while (nextTime.isBefore(to)) {
            if (nextTime.isAfter(from)) {
                // Check if slot already exists
                boolean exists = timeSlotRepository.existsByTargetLanguageCodeAndLevelAndStartTime(
                        template.getTargetLanguageCode(), template.getLevel(), nextTime);

                if (!exists) {
                    TimeSlot newSlot = TimeSlot.builder()
                            .targetLanguageCode(template.getTargetLanguageCode())
                            .level(template.getLevel())
                            .startTime(nextTime)
                            .durationMinutes(template.getDurationMinutes())
                            .maxParticipants(template.getMaxParticipants())
                            .minParticipants(template.getMinParticipants())
                            .recurrence(TimeSlot.Recurrence.once)
                            .isActive(true)
                            .build();
                    timeSlotRepository.save(newSlot);
                    count++;
                }
            }

            // Calculate next occurrence
            nextTime = switch (template.getRecurrence()) {
                case daily -> nextTime.plus(1, ChronoUnit.DAYS);
                case weekly -> nextTime.plus(7, ChronoUnit.DAYS);
                default -> to; // Stop loop
            };
        }

        return count;
    }

    private TimeSlotDto toTimeSlotDto(TimeSlot slot) {
        long registeredCount = registrationRepository.countByTimeSlotIdAndStatus(
                slot.getId(), Registration.Status.registered);
        int available = slot.getMaxParticipants() - (int) registeredCount;
        boolean isAvailable = available > 0 && slot.getStartTime().isAfter(Instant.now());

        return TimeSlotDto.builder()
                .id(slot.getId())
                .targetLanguageCode(slot.getTargetLanguageCode())
                .level(slot.getLevel())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .durationMinutes(slot.getDurationMinutes())
                .maxParticipants(slot.getMaxParticipants())
                .registeredCount((int) registeredCount)
                .availableSpots(Math.max(0, available))
                .isAvailable(isAvailable)
                .build();
    }
}
