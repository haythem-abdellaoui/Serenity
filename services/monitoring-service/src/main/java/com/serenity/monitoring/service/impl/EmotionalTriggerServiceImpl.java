package com.serenity.monitoring.service.impl;

import com.serenity.monitoring.dto.EmotionalTriggerRequest;
import com.serenity.monitoring.dto.EmotionalTriggerResponse;
import com.serenity.monitoring.entity.EmotionalTrigger;
import com.serenity.monitoring.entity.MoodEntry;
import com.serenity.monitoring.exception.ResourceNotFoundException;
import com.serenity.monitoring.mapper.EmotionalTriggerMapper;
import com.serenity.monitoring.repository.EmotionalTriggerRepository;
import com.serenity.monitoring.repository.MoodEntryRepository;
import com.serenity.monitoring.security.userdetails.CustomUserDetails;
import com.serenity.monitoring.service.EmotionalTriggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EmotionalTriggerServiceImpl implements EmotionalTriggerService {

    private final EmotionalTriggerRepository emotionalTriggerRepository;
    private final MoodEntryRepository moodEntryRepository;
    private final EmotionalTriggerMapper emotionalTriggerMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public EmotionalTriggerResponse createTrigger(Long pathMoodEntryId, EmotionalTriggerRequest request) {
        AuthUser authUser = extractCurrentUser();
        ensureRole(authUser, "DOCTOR");

        if (!pathMoodEntryId.equals(request.getMoodEntryId())) {
            throw new IllegalStateException("Path moodEntryId and body moodEntryId must match");
        }

        MoodEntry moodEntry = moodEntryRepository.findById(pathMoodEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("MoodEntry not found"));

        if (!moodEntry.getDoctorId().equals(authUser.userId())) {
            throw new AccessDeniedException("You are not assigned to this patient");
        }

        EmotionalTrigger trigger = emotionalTriggerMapper.toEntity(request);
        trigger.setMoodEntry(moodEntry);
        trigger.setDoctorId(authUser.userId());
        trigger.setRecordedAt(LocalDateTime.now());

        EmotionalTrigger saved = emotionalTriggerRepository.save(trigger);
        return emotionalTriggerMapper.toResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<EmotionalTriggerResponse> getTriggersByMoodEntryId(Long moodEntryId) {
        AuthUser authUser = extractCurrentUser();
        MoodEntry moodEntry = moodEntryRepository.findById(moodEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("MoodEntry not found"));

        if ("PATIENT".equals(authUser.role()) && !moodEntry.getPatientId().equals(authUser.userId())) {
            throw new AccessDeniedException("You do not own this mood entry");
        }
        if ("DOCTOR".equals(authUser.role()) && !moodEntry.getDoctorId().equals(authUser.userId())) {
            throw new AccessDeniedException("You are not assigned to this patient");
        }
        if (!"PATIENT".equals(authUser.role()) && !"DOCTOR".equals(authUser.role())) {
            throw new AccessDeniedException("Only doctor or patient can view triggers");
        }

        return emotionalTriggerRepository.findByMoodEntryId(moodEntryId).stream()
                .map(emotionalTriggerMapper::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public EmotionalTriggerResponse getTriggerById(Long id) {
        AuthUser authUser = extractCurrentUser();
        EmotionalTrigger trigger = emotionalTriggerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmotionalTrigger not found"));

        MoodEntry moodEntry = trigger.getMoodEntry();
        if ("PATIENT".equals(authUser.role()) && !moodEntry.getPatientId().equals(authUser.userId())) {
            throw new AccessDeniedException("You do not own this mood entry");
        }
        if ("DOCTOR".equals(authUser.role()) && !moodEntry.getDoctorId().equals(authUser.userId())) {
            throw new AccessDeniedException("You are not assigned to this patient");
        }
        if (!"PATIENT".equals(authUser.role()) && !"DOCTOR".equals(authUser.role())) {
            throw new AccessDeniedException("Only doctor or patient can view this trigger");
        }

        return emotionalTriggerMapper.toResponse(trigger);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EmotionalTriggerResponse updateTrigger(Long id, EmotionalTriggerRequest request) {
        AuthUser authUser = extractCurrentUser();
        ensureRole(authUser, "DOCTOR");

        EmotionalTrigger trigger = emotionalTriggerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmotionalTrigger not found"));

        if (!trigger.getDoctorId().equals(authUser.userId())) {
            throw new AccessDeniedException("You cannot update another doctor's trigger");
        }

        // Keep immutable ownership fields and parent relation.
        trigger.setTriggerType(request.getTriggerType());
        trigger.setDescription(request.getDescription());
        trigger.setIntensity(request.getIntensity());

        EmotionalTrigger updated = emotionalTriggerRepository.save(trigger);
        return emotionalTriggerMapper.toResponse(updated);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteTrigger(Long id) {
        AuthUser authUser = extractCurrentUser();
        ensureRole(authUser, "DOCTOR");

        EmotionalTrigger trigger = emotionalTriggerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmotionalTrigger not found"));

        if (!trigger.getDoctorId().equals(authUser.userId())) {
            throw new AccessDeniedException("You cannot delete another doctor's trigger");
        }

        emotionalTriggerRepository.delete(trigger);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<EmotionalTriggerResponse> getTriggersByDoctorId(Long doctorId) {
        AuthUser authUser = extractCurrentUser();
        ensureRole(authUser, "DOCTOR");

        if (!doctorId.equals(authUser.userId())) {
            throw new AccessDeniedException("You can only read your own triggers");
        }

        return emotionalTriggerRepository.findByDoctorId(doctorId).stream()
                .map(emotionalTriggerMapper::toResponse)
                .toList();
    }

    private void ensureRole(AuthUser user, String role) {
        if (!role.equals(user.role())) {
            throw new AccessDeniedException("Access denied for role: " + user.role());
        }
    }

    private AuthUser extractCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new AccessDeniedException("Missing authenticated user");
        }

        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("UNKNOWN");

        return new AuthUser(userDetails.getId(), role);
    }

    private record AuthUser(Long userId, String role) {
    }
}

