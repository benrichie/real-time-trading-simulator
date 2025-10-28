// src/services/positionService.ts
import apiClient from '../api/client';
import { Position, PositionSummary, Order, Transaction } from '../types';

export const positionService = {
  async getPositions(portfolioId: number): Promise<Position[]> {
    const response = await apiClient.get<Position[]>(`/positions/portfolio/${portfolioId}`);
    return response.data;
  },

  async getPositionSummaries(portfolioId: number): Promise<PositionSummary[]> {
    const response = await apiClient.get<PositionSummary[]>(
      `/positions/portfolio/${portfolioId}/summaries`
    );
    return response.data;
  },

  async getTopPerformers(portfolioId: number, limit: number = 5): Promise<PositionSummary[]> {
    const response = await apiClient.get<PositionSummary[]>(
      `/positions/portfolio/${portfolioId}/top-performers`,
      { params: { limit } }
    );
    return response.data;
  },

  async getWorstPerformers(portfolioId: number, limit: number = 5): Promise<PositionSummary[]> {
    const response = await apiClient.get<PositionSummary[]>(
      `/positions/portfolio/${portfolioId}/worst-performers`,
      { params: { limit } }
    );
    return response.data;
  },
};

export const orderService = {
  async getOrders(portfolioId: number): Promise<Order[]> {
    const response = await apiClient.get<Order[]>(`/orders/portfolio/${portfolioId}`);
    return response.data;
  },

  async getPendingOrders(): Promise<Order[]> {
    const response = await apiClient.get<Order[]>('/orders/pending');
    return response.data;
  },
};

export const transactionService = {
  async getTransactions(portfolioId: number): Promise<Transaction[]> {
    const response = await apiClient.get<Transaction[]>(
      `/transactions/portfolio/${portfolioId}`
    );
    return response.data;
  },
};