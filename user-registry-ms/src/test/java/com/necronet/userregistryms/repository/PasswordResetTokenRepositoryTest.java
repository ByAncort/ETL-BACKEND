package com.necronet.userregistryms.repository;

import com.necronet.userregistryms.entity.PasswordResetToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PasswordResetTokenRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    private PasswordResetToken persistedToken;
    private PasswordResetToken secondToken;

    @BeforeEach
    void setUp() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("abc-123-def");
        token.setUserId(1L);
        token.setExpiryDate(LocalDateTime.now().plusHours(1));
        token.setUsed(false);
        persistedToken = em.persistAndFlush(token);

        PasswordResetToken token2 = new PasswordResetToken();
        token2.setToken("xyz-789-uvw");
        token2.setUserId(1L);
        token2.setExpiryDate(LocalDateTime.now().plusHours(1));
        token2.setUsed(false);
        secondToken = em.persistAndFlush(token2);

        em.clear();
    }

    @Test
    void findByToken_shouldReturnToken() {
        Optional<PasswordResetToken> found = tokenRepository.findByToken("abc-123-def");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(persistedToken.getId());
        assertThat(found.get().getUserId()).isEqualTo(1L);
        assertThat(found.get().isUsed()).isFalse();
        assertThat(found.get().isExpired()).isFalse();
    }

    @Test
    void findByToken_shouldReturnEmpty_whenNotFound() {
        Optional<PasswordResetToken> found = tokenRepository.findByToken("non-existent-token");

        assertThat(found).isEmpty();
    }

    @Test
    void findByToken_shouldReturnEmpty_whenTokenIsNull() {
        Optional<PasswordResetToken> found = tokenRepository.findByToken(null);

        assertThat(found).isEmpty();
    }

    @Test
    void deleteByUserId_shouldDeleteAllTokensForUser() {
        tokenRepository.deleteByUserId(1L);
        em.flush();
        em.clear();

        Optional<PasswordResetToken> found1 = tokenRepository.findByToken("abc-123-def");
        Optional<PasswordResetToken> found2 = tokenRepository.findByToken("xyz-789-uvw");

        assertThat(found1).isEmpty();
        assertThat(found2).isEmpty();
    }

    @Test
    void deleteByUserId_shouldNotAffectOtherUsers() {
        PasswordResetToken otherToken = new PasswordResetToken();
        otherToken.setToken("other-user-token");
        otherToken.setUserId(2L);
        otherToken.setExpiryDate(LocalDateTime.now().plusHours(1));
        otherToken.setUsed(false);
        em.persistAndFlush(otherToken);
        em.clear();

        tokenRepository.deleteByUserId(1L);
        em.flush();
        em.clear();

        Optional<PasswordResetToken> found = tokenRepository.findByToken("other-user-token");
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(2L);
    }

    @Test
    void deleteByUserId_shouldDoNothing_whenNoTokens() {
        tokenRepository.deleteByUserId(999L);
        em.flush();
        em.clear();

        Optional<PasswordResetToken> found = tokenRepository.findByToken("abc-123-def");
        assertThat(found).isPresent();
    }

    @Test
    void token_shouldBeExpired_whenExpiryDatePassed() {
        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setToken("expired-token");
        expiredToken.setUserId(3L);
        expiredToken.setExpiryDate(LocalDateTime.now().minusHours(2));
        expiredToken.setUsed(false);
        em.persistAndFlush(expiredToken);
        em.clear();

        Optional<PasswordResetToken> found = tokenRepository.findByToken("expired-token");
        assertThat(found).isPresent();
        assertThat(found.get().isExpired()).isTrue();
    }

    @Test
    void token_shouldNotBeExpired_whenExpiryDateInFuture() {
        Optional<PasswordResetToken> found = tokenRepository.findByToken("abc-123-def");
        assertThat(found).isPresent();
        assertThat(found.get().isExpired()).isFalse();
    }
}
