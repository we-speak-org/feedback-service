package org.wespeak.feedback.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.wespeak.feedback.entity.UserFeedbackStats;

@Repository
public interface UserFeedbackStatsRepository extends MongoRepository<UserFeedbackStats, String> {
  Optional<UserFeedbackStats> findByUserIdAndTargetLanguageCode(String userId, String languageCode);
}
