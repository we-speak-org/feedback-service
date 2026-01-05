package org.wespeak.feedback.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.wespeak.feedback.entity.Feedback;

@Repository
public interface FeedbackRepository extends MongoRepository<Feedback, String> {
  Page<Feedback> findByUserId(String userId, Pageable pageable);

  Page<Feedback> findByUserIdAndTargetLanguageCode(
      String userId, String languageCode, Pageable pageable);

  Optional<Feedback> findBySessionIdAndUserId(String sessionId, String userId);

  List<Feedback> findTop5ByUserIdAndTargetLanguageCodeOrderByCreatedAtDesc(
      String userId, String languageCode);
}
