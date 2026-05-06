package com.necronet.apiregisterms.repository;

import com.necronet.apiregisterms.entity.AuthCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthCredentialRepository extends JpaRepository<AuthCredential, Long> {
}
