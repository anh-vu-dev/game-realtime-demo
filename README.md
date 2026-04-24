# Realtime Game Backend Demo (Spring Boot + WebSocket)

# Overview

This project is a realtime multiplayer game backend prototype built using Spring Boot and WebSocket (STOMP).

It demonstrates how to handle:

- Realtime player actions
- Room-based state synchronization
- Server-authoritative game logic
- Event-driven communication

# Architecture

The system is separated into two layers:

1. Transport Layer (WebSocket / STOMP)
- Client connects via WebSocket
- Subscribes to /topic/room.{roomId}
- Receives realtime updates from server

2. Domain Layer (Game Logic)
- Players must JOIN a room before acting
- Server processes all actions (MOVE, ATTACK, LEAVE)
- Server broadcasts updated game state

# Realtime Flow
- Client sends action: /app/game.action → Server processes logic →  Server broadcasts: /topic/room.{roomId} → All subscribed clients receive updated state

# Features
- Realtime multiplayer room system
- WebSocket-based event broadcasting
- Server-authoritative game logic
- Player session binding (prevents spoofing)
- Spectator mode (subscribe without join)
- Join / Move / Attack / Leave lifecycle

# Game Rules (Simplified)
- Each player starts with 100 HP
- Attack damage: 10 HP
- Attack range: Manhattan distance ≤ 5
- Distance formula: |x1 - x2| + |y1 - y2|
- Dead players (HP = 0) cannot move or attack
- Players cannot attack themselves
- Server validates all actions

# Tech Stack
- Java (Spring Boot)
- WebSocket (STOMP)
- SockJS
- ConcurrentHashMap (in-memory state)

# How to Run
1. Backend
mvn clean package
mvn spring-boot:run

2. Client

Open:
test-client.html

# Demo
Backend: https://game-realtime-demo.onrender.com/

WebSocket endpoint:
wss://game-realtime-demo.onrender.com/ws-game

Use `client/test-client.html` to interact with the system.

Note: The demo is hosted on Render free tier. 
The service may go to sleep after inactivity and can take ~30–60 seconds to wake up on first request.

# Future Improvements
- Authentication (Spring Security + Principal)
- Redis for distributed state
- Matchmaking system
- Persistent storage
- Anti-cheat validation
- Scalable message broker (RabbitMQ / Kafka)

# Key Learnings
- Separation of transport vs domain logic
- Session-based identity binding for WebSocket
- Realtime event broadcasting with STOMP
- Designing server-authoritative systems

# Author
Anh Vu (Backend Developer)