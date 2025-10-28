import apiClient from '../api/client';
import { Portfolio, PortfolioSummary } from '../types';

export const portfolioService = {
  getMyPortfolio: async (): Promise<Portfolio> => {
    const response = await apiClient.get<Portfolio>('/portfolios/me');
    return response.data;
  },

  getPortfolio: async (id: number): Promise<Portfolio> => {
    const response = await apiClient.get<Portfolio>(`/portfolios/${id}`);
    return response.data;
  },

  getPortfolioSummary: async (id: number): Promise<PortfolioSummary> => {
    const response = await apiClient.get<PortfolioSummary>(`/portfolios/${id}/summary`);
    return response.data;
  },

  createPortfolio: async (): Promise<Portfolio> => {
    const response = await apiClient.post<Portfolio>('/portfolios/me');
    return response.data;
  },
};
