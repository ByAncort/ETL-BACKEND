package com.necronet.registerapi.repository;

import com.necronet.registerapi.entity.EndpointConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EndpointConfigRepository extends JpaRepository<EndpointConfig, Long> {
}