package com.serenity.monitoring.service.impl;

import com.serenity.monitoring.dto.CrisisAlertPayload;
import com.serenity.monitoring.dto.WeeklyDoctorDigestPayload;
import com.serenity.monitoring.service.CrisisAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CrisisAlertServiceImpl implements CrisisAlertService {

    // One active SSE channel per doctor session.
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(Long doctorId) {
        SseEmitter emitter = new SseEmitter(0L);
        SseEmitter previous = emitters.put(doctorId, emitter);
        if (previous != null) {
            previous.complete();
        }
        log.info("SSE subscribed for doctorId={} (active emitters={})", doctorId, emitters.keySet());

        emitter.onCompletion(() -> {
            emitters.computeIfPresent(doctorId, (id, current) -> current == emitter ? null : current);
            log.info("SSE completed for doctorId={} (active emitters={})", doctorId, emitters.keySet());
        });
        emitter.onTimeout(() -> {
            emitters.computeIfPresent(doctorId, (id, current) -> current == emitter ? null : current);
            emitter.complete();
            log.info("SSE timeout for doctorId={} (active emitters={})", doctorId, emitters.keySet());
        });
        emitter.onError((ex) -> {
            emitters.computeIfPresent(doctorId, (id, current) -> current == emitter ? null : current);
            log.warn("SSE error for doctorId={} -> {}", doctorId, ex.getMessage());
        });

        try {
            emitter.send(SseEmitter.event().name("connected").data("SSE connection established"));
        } catch (IOException ex) {
            emitters.remove(doctorId);
            emitter.completeWithError(ex);
        }

        return emitter;
    }

    @Override
    public void sendCrisisAlert(CrisisAlertPayload payload) {
        if (payload == null || payload.getDoctorId() == null) {
            return;
        }

        SseEmitter emitter = emitters.get(payload.getDoctorId());
        if (emitter == null) {
            log.warn("No active SSE emitter for doctorId={} when sending crisis alert (active emitters={})",
                    payload.getDoctorId(), emitters.keySet());
            return;
        }

        try {
            emitter.send(SseEmitter.event().name("crisis-alert").data(payload, MediaType.APPLICATION_JSON));
            // Also emit as unnamed message for EventSource.onmessage fallback compatibility.
            emitter.send(payload, MediaType.APPLICATION_JSON);
            log.info("Crisis alert sent to doctorId={} for patientId={} moodLevel={}",
                    payload.getDoctorId(), payload.getPatientId(), payload.getMoodLevel());
        } catch (IOException ex) {
            emitters.computeIfPresent(payload.getDoctorId(), (id, current) -> current == emitter ? null : current);
            emitter.completeWithError(ex);
            log.warn("SSE emitter removed for doctorId={} after send failure: {}",
                    payload.getDoctorId(), ex.getMessage());
        }
    }

    @Override
    public void sendWeeklyDigestNotification(WeeklyDoctorDigestPayload payload) {
        if (payload == null || payload.getDoctorId() == null) {
            return;
        }

        SseEmitter emitter = emitters.get(payload.getDoctorId());
        if (emitter == null) {
            log.info("No active SSE emitter for doctorId={} during weekly digest push", payload.getDoctorId());
            return;
        }

        try {
            emitter.send(SseEmitter.event().name("doctor-weekly-digest").data(payload, MediaType.APPLICATION_JSON));
            emitter.send(payload, MediaType.APPLICATION_JSON);
            log.info("Weekly digest sent to doctorId={} for week {}..{}",
                    payload.getDoctorId(), payload.getWeekStartDate(), payload.getWeekEndDate());
        } catch (IOException ex) {
            emitters.computeIfPresent(payload.getDoctorId(), (id, current) -> current == emitter ? null : current);
            emitter.completeWithError(ex);
            log.warn("SSE emitter removed for doctorId={} after weekly digest send failure: {}",
                    payload.getDoctorId(), ex.getMessage());
        }
    }
}
