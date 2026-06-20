package com.necronet.identityservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "session_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(name = "login_time", nullable = false)
    private Date loginTime;

    @Column(name = "logout_time")
    private Date logoutTime;

    @Column(length = 20, nullable = false)
    private String status = "ACTIVE";

    @Column(columnDefinition = "TEXT")
    private String token;

    @PrePersist
    protected void onCreate() {
        loginTime = new Date();
    }
}
