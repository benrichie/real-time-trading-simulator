# Real-Time Trading Simulator

Full-stack trading platform demonstrating Spring Boot architecture, real-time systems, and production deployment.

## Live Demo

**Frontend:** https://trading-platform-frontend-wg73.onrender.com  
**Backend:** https://trading-platform-api-5b23.onrender.com

⚠️**Cold Start:** Free tier hosting requires 30-60s wake-up time. Open backend link first, then refresh frontend.


## Features

### Trading Operations
- Real-time stock and cryptocurrency quotes via Finnhub API
- Market and limit order execution with position tracking
- Portfolio management with profit/loss tracking
- Integrated TradingView charts for technical analysis

### Security & Authentication
- JWT-based authentication with Spring Security
- BCrypt password hashing
- Automatic portfolio creation with configurable balance

### Real-Time Updates
- WebSocket integration (STOMP/SockJS) for live price broadcasts
- Scheduled background tasks for price monitoring (60s intervals)
- Automatic position valuation updates

---

## Tech Stack

**Backend:** Java, Spring Boot, Spring Security, JWT, JPA/Hibernate, PostgreSQL, WebSocket/STOMP, Finnhub API  
**Frontend:** React, TypeScript, Axios, TradingView Widget, SockJS  
**Infrastructure:** Docker, Render (automated deployment)

---

## Architecture

- RESTful API with layered architecture (Controller → Service → Repository)
- Transaction management for database operations
- Scheduled async tasks for price updates (60s intervals)
- WebSocket broadcasting via STOMP for live price updates to all connected clients
- Database staleness check (30s) before fetching from external API
- Cleanup task on startup removes price data older than 14 days

---

## Known Limitations

**Infrastructure:**
- Free tier: 30-60s cold starts, limited resources
- Price updates: 60s polling intervals (API rate limits)

**API Constraints:**
- Finnhub free tier: 60 calls/minute
- Stock prices may be delayed up to 15 minutes
- TradingView chart data may differ from execution prices

---

## Roadmap

- Order book visualization
- Transaction history and analytics
- Long/Short positions
- Stop-loss/Take-profit orders
- Custom real-time charting
- Comprehensive test coverage
- Circuit breaker pattern
- Dark mode
- Fractional shares

---

## Learning Focus

This project demonstrates:
- Enterprise Spring Boot architecture
- Real-time bidirectional communication
- Security best practices (JWT, BCrypt, CORS)
- Production deployment and Docker containerization
- Third-party API integration
- Database design and ORM
- Async/scheduled processing

---

## Deployment

Fully automated via GitHub integration with Render:
- Frontend: Static site with auto-builds
- Backend: Dockerized Spring Boot app
- Database: Managed PostgreSQL
- Config: Environment variables for secrets

---

**Built as an ongoing learning project to understand full-stack development and enterprise application patterns.**
