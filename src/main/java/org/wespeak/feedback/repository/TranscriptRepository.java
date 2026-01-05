package org.wespeak.feedback.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.wespeak.feedback.entity.Transcript;

@Repository
public interface TranscriptRepository extends MongoRepository<Transcript, String> {
  List<Transcript> findBySessionId(String sessionId);

  Optional<Transcript> findByRecordingId(String recordingId);

  List<Transcript> findByParticipantId(String participantId);
}
