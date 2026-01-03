package org.wespeak.conversation.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.wespeak.conversation.entity.Participant;
import org.wespeak.conversation.repository.ParticipantRepository;
import org.wespeak.conversation.repository.SessionRepository;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for WebRTC signaling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ParticipantRepository participantRepository;
    private final SessionRepository sessionRepository;

    // Map sessionId -> (userId -> WebSocketSession)
    private final Map<String, Map<String, WebSocketSession>> sessionConnections = new ConcurrentHashMap<>();

    // Map WebSocketSession id -> (sessionId, userId)
    private final Map<String, ConnectionInfo> connectionInfoMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.debug("WebSocket connection established: {}", session.getId());
        // Connection will be registered when client sends 'join' message
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String type = json.get("type").asText();

            switch (type) {
                case "join" -> handleJoin(session, json);
                case "offer" -> handleSignalingMessage(session, json, "offer");
                case "answer" -> handleSignalingMessage(session, json, "answer");
                case "ice-candidate" -> handleSignalingMessage(session, json, "ice-candidate");
                case "media-state" -> handleMediaState(session, json);
                default -> log.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendError(session, "Invalid message format");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        ConnectionInfo info = connectionInfoMap.remove(session.getId());
        if (info != null) {
            Map<String, WebSocketSession> sessionSockets = sessionConnections.get(info.sessionId);
            if (sessionSockets != null) {
                sessionSockets.remove(info.userId);
                
                // Notify other participants
                broadcastToSession(info.sessionId, info.userId, Map.of(
                    "type", "participant-left",
                    "userId", info.userId
                ));
            }

            // Update participant status in database
            updateParticipantDisconnected(info.sessionId, info.userId);
        }
        log.debug("WebSocket connection closed: {}", session.getId());
    }

    private void handleJoin(WebSocketSession session, JsonNode json) throws IOException {
        String sessionId = json.get("sessionId").asText();
        String userId = json.get("userId").asText();

        // Verify participant is in this session
        if (!participantRepository.existsBySessionIdAndUserId(sessionId, userId)) {
            sendError(session, "Not a participant of this session");
            session.close();
            return;
        }

        // Store connection info
        connectionInfoMap.put(session.getId(), new ConnectionInfo(sessionId, userId));
        sessionConnections.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(userId, session);

        // Get current participants
        Map<String, WebSocketSession> sessionSockets = sessionConnections.get(sessionId);
        var participants = sessionSockets.keySet().stream()
                .filter(id -> !id.equals(userId))
                .map(id -> participantRepository.findBySessionIdAndUserId(sessionId, id).orElse(null))
                .filter(p -> p != null)
                .map(p -> Map.of(
                    "userId", p.getUserId(),
                    "displayName", p.getDisplayName() != null ? p.getDisplayName() : "",
                    "cameraEnabled", p.getCameraEnabled(),
                    "micEnabled", p.getMicEnabled()
                ))
                .toList();

        // Send existing participants to the new joiner
        sendMessage(session, Map.of(
            "type", "room-state",
            "participants", participants
        ));

        // Notify others that this participant joined
        Participant participant = participantRepository.findBySessionIdAndUserId(sessionId, userId).orElse(null);
        if (participant != null) {
            broadcastToSession(sessionId, userId, Map.of(
                "type", "participant-joined",
                "userId", userId,
                "displayName", participant.getDisplayName() != null ? participant.getDisplayName() : "",
                "cameraEnabled", participant.getCameraEnabled(),
                "micEnabled", participant.getMicEnabled()
            ));
        }

        log.info("User {} joined signaling for session {}", userId, sessionId);
    }

    private void handleSignalingMessage(WebSocketSession session, JsonNode json, String type) throws IOException {
        ConnectionInfo info = connectionInfoMap.get(session.getId());
        if (info == null) {
            sendError(session, "Not joined to a session");
            return;
        }

        String targetUserId = json.get("targetUserId").asText();
        Map<String, WebSocketSession> sessionSockets = sessionConnections.get(info.sessionId);
        
        if (sessionSockets != null) {
            WebSocketSession targetSession = sessionSockets.get(targetUserId);
            if (targetSession != null && targetSession.isOpen()) {
                Map<String, Object> outMessage = Map.of(
                    "type", type,
                    "fromUserId", info.userId,
                    "data", json.get("data")
                );
                sendMessage(targetSession, outMessage);
            }
        }
    }

    private void handleMediaState(WebSocketSession session, JsonNode json) throws IOException {
        ConnectionInfo info = connectionInfoMap.get(session.getId());
        if (info == null) {
            sendError(session, "Not joined to a session");
            return;
        }

        boolean cameraEnabled = json.get("cameraEnabled").asBoolean();
        boolean micEnabled = json.get("micEnabled").asBoolean();

        // Update database
        participantRepository.findBySessionIdAndUserId(info.sessionId, info.userId)
                .ifPresent(p -> {
                    p.setCameraEnabled(cameraEnabled);
                    p.setMicEnabled(micEnabled);
                    participantRepository.save(p);
                });

        // Broadcast to others
        broadcastToSession(info.sessionId, info.userId, Map.of(
            "type", "media-state-changed",
            "userId", info.userId,
            "cameraEnabled", cameraEnabled,
            "micEnabled", micEnabled
        ));
    }

    private void broadcastToSession(String sessionId, String excludeUserId, Map<String, Object> message) {
        Map<String, WebSocketSession> sessionSockets = sessionConnections.get(sessionId);
        if (sessionSockets == null) return;

        sessionSockets.forEach((userId, ws) -> {
            if (!userId.equals(excludeUserId) && ws.isOpen()) {
                try {
                    sendMessage(ws, message);
                } catch (IOException e) {
                    log.error("Error broadcasting to user {}", userId, e);
                }
            }
        });
    }

    public void broadcastSessionEnded(String sessionId) {
        Map<String, WebSocketSession> sessionSockets = sessionConnections.get(sessionId);
        if (sessionSockets == null) return;

        Map<String, Object> message = Map.of("type", "session-ended");
        sessionSockets.forEach((userId, ws) -> {
            try {
                if (ws.isOpen()) {
                    sendMessage(ws, message);
                    ws.close();
                }
            } catch (IOException e) {
                log.error("Error closing WebSocket for user {}", userId, e);
            }
        });

        sessionConnections.remove(sessionId);
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> message) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        }
    }

    private void sendError(WebSocketSession session, String error) throws IOException {
        sendMessage(session, Map.of(
            "type", "error",
            "message", error
        ));
    }

    private void updateParticipantDisconnected(String sessionId, String userId) {
        participantRepository.findBySessionIdAndUserId(sessionId, userId)
                .ifPresent(p -> {
                    p.setStatus(Participant.Status.disconnected);
                    p.setLeftAt(java.time.Instant.now());
                    participantRepository.save(p);
                });
    }

    private record ConnectionInfo(String sessionId, String userId) {}
}
