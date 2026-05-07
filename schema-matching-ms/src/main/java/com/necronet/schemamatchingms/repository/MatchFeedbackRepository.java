package com.necronet.schemamatchingms.repository;

import com.necronet.schemamatchingms.entity.MatchFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MatchFeedbackRepository extends JpaRepository<MatchFeedback, Long> {
    List<MatchFeedback> findByMatchId(Long matchId);
}