package com.necronet.apiregisterms.repository;

import com.necronet.apiregisterms.entity.Apis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApisRepository extends JpaRepository<Apis, Long> {
    List<Apis> findByUrlContaining(String url);
    List<Apis> findByMethod_Name(String methodName);
}