## Real-Time Trading Simulator

https://trading-platform-frontend-wg73.onrender.com

If the demo appears blank or requests fail, open the backend link first to wake it, then refresh the frontend. Scheduled background updates (e.g., price and order checks) pause while the backend sleeps.
> https://trading-platform-api-5b23.onrender.com/

An ongoing full-stack trading simulator that began as a Spring Boot learning project. As it slowly developed from backend logic to live deployment, I learned many core concepts and technologies. While currently in early stages, I chose a trading simulator specifically for its potential as a long-term learning platform with room for continuous iteration and expansion.

### Tech Stack
**Frontend:** React, TypeScript, Axios, TradingView Widget  
**Backend:** Java, Spring Boot, PostgreSQL, JWT Authentication, Finnhub API  
**Infrastructure:** Docker, Render (backend, frontend, and database hosting)

### Features

- User registration and JWT-based authentication
- Automatic portfolio creation with configurable initial balance
- Real-time stock and cryptocurrency quotes with simulated order execution
- Portfolio summary with profit/loss tracking and position management
- Integrated TradingView charts for technical analysis
- Support for market and limit orders across multiple asset classes

### Deployment

The application is deployed entirely on Render's infrastructure:

- **Frontend**: Static site deployment with automated builds
- **Backend**: Dockerized Spring Boot application
- **Database**: Managed PostgreSQL instance

All sensitive configuration, including API keys and JWT secrets, is managed through environment variables. Deployment is fully automated through GitHub integration.

### Known Limitations

**Infrastructure Constraints:**
- Free-tier services sleep after 15 minutes of inactivity, resulting in **30-60 second cold start times**
- **Limited CPU and memory allocation impacts performance**
- Database storage restrictions on the free tier

**API and Data:**
- Finnhub free tier limits API calls to 60 per minute
- Stock prices may be delayed up to 15 minutes on the free plan
- Chart data from TradingView may differ from execution prices

**Real-Time Features:**
- Price updates use polling intervals (every 60 seconds) rather than WebSocket streaming
- Update frequency is limited to avoid potential service suspension from excessive API calls
- Connection stability may vary due to hosting limitations

### Future developments:
- Orderbook
- Transaction History
- Long/Short
- Stop loss/Take Profit
- Comprehensive Testing
- Circuit breaker
- Performance testing
- Custom live graph?
- Dark mode
