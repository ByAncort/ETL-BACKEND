package com.necronet.apiregisterms.repository;

import com.necronet.apiregisterms.entity.Header;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HeaderRepository extends JpaRepository<Header, Long> {
    Optional<Header> findByValue(String key);
}