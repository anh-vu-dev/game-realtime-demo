package com.vu.gamews.controller;

import com.vu.gamews.model.GameAction;
import com.vu.gamews.model.GameState;
import com.vu.gamews.service.GameRoomService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class GameController {

    private final GameRoomService gameRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    // sessionId -> roomId
    private final Map<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    // sessionId -> playerId
    private final Map<String, String> sessionPlayerMap = new ConcurrentHashMap<>();

    public GameController(GameRoomService gameRoomService,
                          SimpMessagingTemplate messagingTemplate) {
        this.gameRoomService = gameRoomService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game.action")
    public void handleGameAction(@Payload GameAction action, Message<?> message) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        String sessionId = accessor.getSessionId();

        if (sessionId == null) {
            return;
        }

        String actionType = action.getType() == null ? "" : action.getType().toUpperCase();

        if ("JOIN".equals(actionType)) {
            handleJoin(action, sessionId);
            return;
        }

        handleNonJoinAction(action, sessionId);
    }

    private void handleJoin(GameAction action, String sessionId) {
        String roomId = action.getRoomId();
        String requestedPlayerId = action.getPlayerId();

        if (roomId == null || roomId.isBlank() || requestedPlayerId == null || requestedPlayerId.isBlank()) {
            return;
        }

        //session already joined, keep original identity
        String existingPlayerId = sessionPlayerMap.get(sessionId);
        String finalPlayerId = existingPlayerId != null ? existingPlayerId : requestedPlayerId;

        sessionRoomMap.put(sessionId, roomId);
        sessionPlayerMap.put(sessionId, finalPlayerId);

        action.setPlayerId(finalPlayerId);

        GameState updatedState = gameRoomService.handleAction(action);
        messagingTemplate.convertAndSend("/topic/room." + roomId, updatedState);
    }

    private void handleNonJoinAction(GameAction action, String sessionId) {
        String boundRoomId = sessionRoomMap.get(sessionId);
        String boundPlayerId = sessionPlayerMap.get(sessionId);

        if (boundRoomId == null || boundPlayerId == null) {
            return;
        }

        // Hard override client-controlled identity/room
        action.setRoomId(boundRoomId);
        action.setPlayerId(boundPlayerId);

        GameState updatedState = gameRoomService.handleAction(action);
        messagingTemplate.convertAndSend("/topic/room." + boundRoomId, updatedState);

        if ("LEAVE".equalsIgnoreCase(action.getType())) {
            sessionRoomMap.remove(sessionId);
            sessionPlayerMap.remove(sessionId);
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String roomId = sessionRoomMap.remove(sessionId);
        String playerId = sessionPlayerMap.remove(sessionId);

        if (roomId != null && playerId != null) {
            gameRoomService.removePlayer(roomId, playerId);
            GameState updatedState = gameRoomService.getRoomState(roomId);
            updatedState.setLastEvent("Player " + playerId + " disconnected from room " + roomId);
            messagingTemplate.convertAndSend("/topic/room." + roomId, updatedState);
        }
    }
}