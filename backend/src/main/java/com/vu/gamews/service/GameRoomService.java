package com.vu.gamews.service;

import com.vu.gamews.model.GameAction;
import com.vu.gamews.model.GameState;
import com.vu.gamews.model.PlayerState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameRoomService {

    private static final int DEFAULT_HP = 100;
    private static final int ATTACK_DAMAGE = 10;
    private static final int ATTACK_RANGE = 5; // Manhattan distance

    private final Map<String, Map<String, PlayerState>> rooms = new ConcurrentHashMap<>();

    public GameState handleAction(GameAction action) {
        String roomId = action.getRoomId();
        String playerId = action.getPlayerId();

        rooms.putIfAbsent(roomId, new ConcurrentHashMap<>());
        Map<String, PlayerState> roomPlayers = rooms.get(roomId);

        String normalizedType = action.getType().toUpperCase();
        String lastEvent;

        switch (normalizedType) {
            case "JOIN" -> {
                PlayerState existing = roomPlayers.get(playerId);
                if (existing != null) {
                    lastEvent = "JOIN ignored. Player ID already exists in room: " + playerId;
                } else {
                    roomPlayers.put(
                            playerId,
                            new PlayerState(
                                    playerId,
                                    action.getPlayerName() != null ? action.getPlayerName() : playerId,
                                    action.getX(),
                                    action.getY(),
                                    DEFAULT_HP
                            )
                    );
                    lastEvent = "Player " + playerId + " joined room " + roomId;
                }
            }

            case "MOVE" -> {
                PlayerState player = roomPlayers.get(playerId);

                if (player == null) {
                    lastEvent = "MOVE ignored. Player not found: " + playerId;
                } else if (isDead(player)) {
                    lastEvent = "MOVE ignored. Dead player cannot move: " + playerId;
                } else {
                    player.setX(action.getX());
                    player.setY(action.getY());
                    lastEvent = "Player " + playerId + " moved to (" + action.getX() + "," + action.getY() + ")";
                }
            }

            case "ATTACK" -> {
                PlayerState attacker = roomPlayers.get(playerId);
                PlayerState target = roomPlayers.get(action.getTargetPlayerId());

                if (attacker == null) {
                    lastEvent = "ATTACK ignored. Attacker not found: " + playerId;
                } else if (target == null) {
                    lastEvent = "ATTACK ignored. Target not found: " + action.getTargetPlayerId();
                } else if (isDead(attacker)) {
                    lastEvent = "ATTACK ignored. Dead player cannot attack: " + playerId;
                } else if (isDead(target)) {
                    lastEvent = "ATTACK ignored. Target already dead: " + action.getTargetPlayerId();
                } else if (playerId.equals(action.getTargetPlayerId())) {
                    lastEvent = "ATTACK ignored. Player cannot attack themselves.";
                } else if (isOutOfRange(attacker, target)) {
                    lastEvent = "ATTACK ignored. Target out of range.";
                } else {
                    target.setHp(Math.max(0, target.getHp() - ATTACK_DAMAGE));
                    if (target.getHp() == 0) {
                        lastEvent = "Player " + playerId + " attacked " + target.getPlayerId()
                                + " and killed them.";
                    } else {
                        lastEvent = "Player " + playerId + " attacked " + target.getPlayerId()
                                + ", target HP now " + target.getHp();
                    }
                }
            }

            case "LEAVE" -> {
                if (roomPlayers.remove(playerId) != null) {
                    lastEvent = "Player " + playerId + " left room " + roomId;
                } else {
                    lastEvent = "LEAVE ignored. Player not found: " + playerId;
                }

                if (roomPlayers.isEmpty()) {
                    rooms.remove(roomId);
                    return new GameState(roomId, new ArrayList<>(), lastEvent + ". Room is now empty.");
                }
            }

            default -> lastEvent = "Unknown action type: " + action.getType();
        }

        return new GameState(roomId, new ArrayList<>(roomPlayers.values()), lastEvent);
    }

    public void removePlayer(String roomId, String playerId) {
        Map<String, PlayerState> roomPlayers = rooms.get(roomId);
        if (roomPlayers != null) {
            roomPlayers.remove(playerId);
            if (roomPlayers.isEmpty()) {
                rooms.remove(roomId);
            }
        }
    }

    public GameState getRoomState(String roomId) {
        Map<String, PlayerState> roomPlayers = rooms.getOrDefault(roomId, new ConcurrentHashMap<>());
        return new GameState(roomId, new ArrayList<>(roomPlayers.values()), "Snapshot");
    }

    private boolean isDead(PlayerState player) {
        return player.getHp() <= 0;
    }

    private boolean isOutOfRange(PlayerState attacker, PlayerState target) {
        int dx = Math.abs(attacker.getX() - target.getX());
        int dy = Math.abs(attacker.getY() - target.getY());
        int distance = dx + dy;
        return distance > ATTACK_RANGE;
    }
}