package com.necronet.identityservice.service;

import com.necronet.identityservice.entity.SessionLog;
import com.necronet.identityservice.repository.SessionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionLogService {

    private final SessionLogRepository sessionLogRepository;

    @Transactional
    public SessionLog logLogin(String username, String token, String ipAddress, String userAgent) {
        SessionLog session = new SessionLog();
        session.setUsername(username);
        session.setToken(token);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setStatus("ACTIVE");
        session.setLoginTime(new Date());
        SessionLog saved = sessionLogRepository.save(session);
        log.info("Session logged: user={}, ip={}, sessionId={}", username, ipAddress, saved.getId());
        return saved;
    }

    @Transactional
    public void logLogout(String token) {
        sessionLogRepository.findByTokenAndStatus(token, "ACTIVE")
                .ifPresent(session -> {
                    session.setStatus("LOGGED_OUT");
                    session.setLogoutTime(new Date());
                    sessionLogRepository.save(session);
                    log.info("Session closed: user={}, sessionId={}", session.getUsername(), session.getId());
                });
    }

    @Transactional
    public void expireSession(String token) {
        sessionLogRepository.findByTokenAndStatus(token, "ACTIVE")
                .ifPresent(session -> {
                    session.setStatus("EXPIRED");
                    session.setLogoutTime(new Date());
                    sessionLogRepository.save(session);
                    log.info("Session expired: user={}, sessionId={}", session.getUsername(), session.getId());
                });
    }

    @Transactional(readOnly = true)
    public List<SessionLog> getSessionsByUsername(String username) {
        return sessionLogRepository.findByUsernameOrderByLoginTimeDesc(username);
    }
}
