package com.necronet.userregistryms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class UserStatusObserverService {

    private final Map<String, SseEmitter> observers = new ConcurrentHashMap<>();

    public SseEmitter addObserver(String username) {
        // Set timeout to 1 hour (or 0 for infinite)
        SseEmitter emitter = new SseEmitter(3600000L);
        observers.put(username, emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE Emitter completed for user: {}", username);
            observers.remove(username, emitter);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE Emitter timed out for user: {}", username);
            emitter.complete();
            observers.remove(username, emitter);
        });
        emitter.onError((e) -> {
            log.debug("SSE Emitter error for user: {}", username, e);
            observers.remove(username, emitter);
        });

        // Send an initial event to keep connection alive
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected"));
            log.info("SSE Connection established for user: {}", username);
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void notifyDeactivation(String username) {
        SseEmitter emitter = observers.get(username);
        if (emitter != null) {
            try {
                log.info("Notifying user {} about deactivation", username);
                emitter.send(SseEmitter.event().name("DEACTIVATED").data("Your account has been deactivated"));
                emitter.complete();
            } catch (IOException e) {
                log.error("Failed to send deactivation event to user: {}", username, e);
                emitter.completeWithError(e);
            } finally {
                observers.remove(username);
            }
        } else {
            log.debug("No active SSE connection found for user: {}", username);
        }
    }
}
