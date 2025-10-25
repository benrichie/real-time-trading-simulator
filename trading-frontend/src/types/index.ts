export interface User {
  id: number;
  username: string;
  email: string;
  initialBalance: number;
}

export interface AuthResponse {
  token: string;
  userId: number;
  username: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  initialBalance: number;
}

export interface Portfolio {
  id: number;
  userId: number;
  cashBalance: number;
  totalValue: number;
  initialCapital: number;
}

export interface PortfolioSummary {
  portfolioId: number;
  cashBalance: number;
  totalPositionsValue: number;
  totalPortfolioValue: number;
  totalCostBasis: number;
  unrealizedPnL: number;
  percentageReturn: number;
}

export interface Position {
  id: number;
  portfolioId: number;
  stockSymbol: string;
  quantity: number;
  averagePrice: number;
  currentValue: number;
}

export interface PositionSummary {
  positionId: number;
  stockSymbol: string;
  quantity: number;
  averagePrice: number;
  currentPrice: number;
  costBasis: number;
  currentMarketValue: number;
  unrealizedPnL: number;
  percentageReturn: number;
}

export interface Order {
  id: number;
  portfolioId: number;
  stockSymbol: string;
  orderType: 'BUY' | 'SELL';
  priceType: 'MARKET' | 'LIMIT';
  quantity: number;
  limitPrice?: number;
  status: 'PENDING' | 'FILLED' | 'CANCELLED';
  filledPrice?: number;
  createdAt: string;
  filledAt?: string;
}

export interface Transaction {
  id: number;
  orderId: number;
  portfolioId: number;
  stockSymbol: string;
  transactionType: 'BUY' | 'SELL';
  quantity: number;
  price: number;
  createdAt: string;
}

export interface Quote {
  stockSymbol: string;
  companyName: string;
  currentPrice: number;
  quantity: number;
  orderType: 'BUY' | 'SELL';
  totalValue: number;
  lastUpdated: string;
}

export interface TradeRequest {
  portfolioId: number;
  stockSymbol: string;
  quantity: number;
  priceType: 'MARKET' | 'LIMIT';
  limitPrice?: number;
}

export interface TradeResponse {
  success: boolean;
  message: string;
  order: Order;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}