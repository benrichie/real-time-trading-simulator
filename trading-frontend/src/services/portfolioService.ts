// src/services/portfolioService.ts
import apiClient from '../api/client';
import { Portfolio, PortfolioSummary } from '../types';

export const portfolioService = {
  async getPortfolios(): Promise<Portfolio[]> {
    const response = await apiClient.get<Portfolio[]>('/portfolios');
    return response.data;
  },

  async getPortfolio(id: number): Promise<Portfolio> {
    const response = await apiClient.get<Portfolio>(`/portfolios/${id}`);
    return response.data;
  },

  async getPortfolioSummary(id: number): Promise<PortfolioSummary> {
    const response = await apiClient.get<PortfolioSummary>(`/portfolios/${id}/summary`);
    return response.data;
  },

  async createPortfolio(): Promise<Portfolio> {
    const response = await apiClient.post<Portfolio>('/portfolios/me');
    return response.data;
},

};