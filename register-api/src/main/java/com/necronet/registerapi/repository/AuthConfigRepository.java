package com.necronet.registerapi.repository;

import com.necronet.registerapi.entity.AuthConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthConfigRepository extends JpaRepository<AuthConfig, Long> {
}