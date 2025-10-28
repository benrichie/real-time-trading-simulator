// src/services/tradingService.ts
import apiClient from '../api/client';
import { Quote, TradeRequest, TradeResponse } from '../types';

export const tradingService = {
  async getQuote(
    stockSymbol: string,
    quantity: number,
    orderType: 'BUY' | 'SELL'
  ): Promise<Quote> {
    const response = await apiClient.get<Quote>('/trading/quote', {
      params: { stockSymbol, quantity, orderType },
    });
    return response.data;
  },

  async buyStock(request: TradeRequest): Promise<TradeResponse> {
    const response = await apiClient.post<TradeResponse>('/trading/buy', request);
    return response.data;
  },

  async sellStock(request: TradeRequest): Promise<TradeResponse> {
    const response = await apiClient.post<TradeResponse>('/trading/sell', request);
    return response.data;
  },

  async sellAllShares(
    portfolioId: number,
    stockSymbol: string,
    priceType: 'MARKET' | 'LIMIT' = 'MARKET',
    limitPrice?: number
  ): Promise<TradeResponse> {
    const response = await apiClient.post<TradeResponse>('/trading/sell-all', {
      portfolioId,
      stockSymbol,
      priceType,
      limitPrice,
    });
    return response.data;
  },
};
