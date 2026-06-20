package com.necronet.identityservice.repository;

import com.necronet.identityservice.entity.SessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionLogRepository extends JpaRepository<SessionLog, Long> {
    List<SessionLog> findByUsernameOrderByLoginTimeDesc(String username);
    Optional<SessionLog> findByTokenAndStatus(String token, String status);
    List<SessionLog> findByStatus(String status);
}
