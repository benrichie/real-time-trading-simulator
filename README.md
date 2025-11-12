# Real-Time Trading Simulator

Full-stack trading platform demonstrating Spring Boot architecture, real-time systems, and production deployment.

**Live Demo:** https://trading-platform-frontend-wg73.onrender.com  
**Backend API:** https://trading-platform-api-5b23.onrender.com

**⚠️** Free tier hosting requires 30-60s wake-up time. Open backend link first, then refresh frontend.

---

## Features

- Real-time stock/crypto quotes via Finnhub API with WebSocket broadcasting (STOMP/SockJS)
- Market and limit order execution with position tracking and P&L calculations
- JWT authentication with Spring Security and BCrypt password hashing
- Scheduled background tasks for price monitoring (60s intervals)
- Integrated TradingView charts for technical analysis

**Tech Stack:** Java, Spring Boot, Spring Security, JWT, JPA/Hibernate, PostgreSQL, WebSocket/STOMP, React, TypeScript, Docker, Render

---

## Architecture

- RESTful API with layered architecture (Controller → Service → Repository)
- WebSocket broadcasting via STOMP for live price updates to all connected clients
- Scheduled async tasks for price updates (60s intervals)
- Database staleness check (30s) before fetching from external API
- Cleanup task on startup removes price data older than 14 days
- Transaction management for database operations

---

## Known Limitations

- Free tier: 30-60s cold starts, 60s price update intervals
- Finnhub free tier: 60 calls/minute, prices may be delayed up to 15 minutes

---

## Roadmap

Order book visualization • Transaction history • Long/Short positions • Stop-loss/Take-profit • Custom charting • Test coverage • Circuit breaker • Dark mode • Fractional shares

---

## Learning Focus

**Spring Boot Architecture** - Layered design separating controllers, services, and repositories  
**Real-time Communication** - WebSocket server push without polling  
**Security** - JWT stateless auth, BCrypt encryption, CORS configuration  
**Deployment** - Dockerized app on Render with managed PostgreSQL  
**API Integration** - Finnhub integration with error handling and rate limits  
**Database/ORM** - JPA/Hibernate entity modeling  
**Async Processing** - Background tasks without blocking requests
