package com.necronet.apiregisterms.repository;

import com.necronet.apiregisterms.entity.AuthConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthConfigRepository extends JpaRepository<AuthConfig, Long> {
    Optional<AuthConfig> findByApi_Id(Long apiId);
}