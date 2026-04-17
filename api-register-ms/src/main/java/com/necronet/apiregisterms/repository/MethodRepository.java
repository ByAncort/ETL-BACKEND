package com.necronet.apiregisterms.repository;

import com.necronet.apiregisterms.entity.Method;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MethodRepository extends JpaRepository<Method, Long> {
    Optional<Method> findByName(String name);
}