package com.vu.gamews.model;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private String roomId;
    private List<PlayerState> players = new ArrayList<>();
    private String lastEvent;

    public GameState() {
    }

    public GameState(String roomId, List<PlayerState> players, String lastEvent) {
        this.roomId = roomId;
        this.players = players;
        this.lastEvent = lastEvent;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public List<PlayerState> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerState> players) {
        this.players = players;
    }

    public String getLastEvent() {
        return lastEvent;
    }

    public void setLastEvent(String lastEvent) {
        this.lastEvent = lastEvent;
    }
}