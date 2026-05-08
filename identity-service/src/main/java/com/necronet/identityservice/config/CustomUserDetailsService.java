package com.necronet.identityservice.config;

import com.necronet.identityservice.entity.UserCredential;
import com.necronet.identityservice.repository.UserCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserCredentialRepository repository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        Optional<UserCredential> credential = repository.findByUsername(usernameOrEmail);

        if (credential.isEmpty() && usernameOrEmail != null && usernameOrEmail.contains("@")) {
            credential = repository.findByEmail(usernameOrEmail);
        }

        return credential
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("user not found with identifier: " + usernameOrEmail));
    }
}
