package com.necronet.identityservice.service;

import com.necronet.identityservice.entity.SessionLog;
import com.necronet.identityservice.repository.SessionLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SessionLogServiceTest {

    @Mock
    private SessionLogRepository sessionLogRepository;

    @InjectMocks
    private SessionLogService sessionLogService;

    @Captor
    private ArgumentCaptor<SessionLog> sessionCaptor;

    @Test
    void logLogin_shouldCreateActiveSession() {
        given(sessionLogRepository.save(any(SessionLog.class))).willAnswer(inv -> inv.getArgument(0));

        SessionLog result = sessionLogService.logLogin("testuser", "token123", "192.168.1.1", "Mozilla/5.0");

        then(sessionLogRepository).should().save(sessionCaptor.capture());
        SessionLog saved = sessionCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("testuser");
        assertThat(saved.getToken()).isEqualTo("token123");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getLoginTime()).isNotNull();
        assertThat(saved.getLogoutTime()).isNull();

        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    void logLogin_withNullIpAndAgent_shouldHandleGracefully() {
        given(sessionLogRepository.save(any(SessionLog.class))).willAnswer(inv -> inv.getArgument(0));

        SessionLog result = sessionLogService.logLogin("testuser", "token123", null, null);

        assertThat(result.getIpAddress()).isNull();
        assertThat(result.getUserAgent()).isNull();
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void logLogout_shouldUpdateMatchingSession() {
        SessionLog active = new SessionLog();
        active.setId(1L);
        active.setUsername("testuser");
        active.setToken("token123");
        active.setStatus("ACTIVE");
        active.setLoginTime(new java.util.Date());

        given(sessionLogRepository.findByTokenAndStatus("token123", "ACTIVE")).willReturn(Optional.of(active));
        given(sessionLogRepository.save(any(SessionLog.class))).willAnswer(inv -> inv.getArgument(0));

        sessionLogService.logLogout("token123");

        assertThat(active.getStatus()).isEqualTo("LOGGED_OUT");
        assertThat(active.getLogoutTime()).isNotNull();
    }

    @Test
    void logLogout_whenNoActiveSession_shouldDoNothing() {
        given(sessionLogRepository.findByTokenAndStatus("nonexistent", "ACTIVE")).willReturn(Optional.empty());

        sessionLogService.logLogout("nonexistent");

        then(sessionLogRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void expireSession_shouldUpdateStatus() {
        SessionLog active = new SessionLog();
        active.setId(1L);
        active.setToken("token456");
        active.setStatus("ACTIVE");

        given(sessionLogRepository.findByTokenAndStatus("token456", "ACTIVE")).willReturn(Optional.of(active));
        given(sessionLogRepository.save(any(SessionLog.class))).willAnswer(inv -> inv.getArgument(0));

        sessionLogService.expireSession("token456");

        assertThat(active.getStatus()).isEqualTo("EXPIRED");
        assertThat(active.getLogoutTime()).isNotNull();
    }

    @Test
    void getSessionsByUsername_shouldReturnOrderedList() {
        SessionLog s1 = new SessionLog();
        s1.setId(2L);
        s1.setUsername("testuser");
        SessionLog s2 = new SessionLog();
        s2.setId(1L);
        s2.setUsername("testuser");

        given(sessionLogRepository.findByUsernameOrderByLoginTimeDesc("testuser")).willReturn(List.of(s1, s2));

        List<SessionLog> sessions = sessionLogService.getSessionsByUsername("testuser");

        assertThat(sessions).hasSize(2);
        assertThat(sessions.get(0).getId()).isEqualTo(2L);
    }

    @Test
    void getSessionsByUsername_whenNone_shouldReturnEmpty() {
        given(sessionLogRepository.findByUsernameOrderByLoginTimeDesc("nobody")).willReturn(List.of());

        List<SessionLog> sessions = sessionLogService.getSessionsByUsername("nobody");

        assertThat(sessions).isEmpty();
    }

    @Test
    void logLogout_shouldOnlyUpdateActiveSession() {
        SessionLog alreadyExpired = new SessionLog();
        alreadyExpired.setId(1L);
        alreadyExpired.setToken("token789");
        alreadyExpired.setStatus("EXPIRED");

        given(sessionLogRepository.findByTokenAndStatus("token789", "ACTIVE")).willReturn(Optional.empty());

        sessionLogService.logLogout("token789");

        then(sessionLogRepository).shouldHaveNoMoreInteractions();
    }
}
