package com.necronet.integrationms.repository;

import com.necronet.integrationms.entity.Integration;
import com.necronet.integrationms.entity.IntegrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntegrationRepository extends JpaRepository<Integration, Long> {
    List<Integration> findByStatus(IntegrationStatus status);
    List<Integration> findByStatusNot(IntegrationStatus status);
}
