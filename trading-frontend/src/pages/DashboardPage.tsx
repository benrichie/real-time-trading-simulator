// src/pages/DashboardPage.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { portfolioService } from '../services/portfolioService';
import { positionService } from '../services/positionService';
import { PortfolioSummary, PositionSummary } from '../types';
import './Dashboard.css';

export const DashboardPage: React.FC = () => {
  const [summary, setSummary] = useState<PortfolioSummary | null>(null);
  const [positions, setPositions] = useState<PositionSummary[]>([]);
  const [portfolioId, setPortfolioId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      setError('');

      const portfolio = await portfolioService.getMyPortfolio();
      if (!portfolio) {
        setError('No portfolio found. Please contact support.');
        return;
      }

      setPortfolioId(portfolio.id);

      const summaryData = await portfolioService.getPortfolioSummary(portfolio.id);
      setSummary(summaryData);

      const positionsData = await positionService.getPositionSummaries(portfolio.id);
      setPositions(positionsData);
    } catch (err: any) {
      console.error('Failed to load dashboard', err);
      setError('Failed to load dashboard data. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="dashboard">
        <div className="loading">Loading dashboard...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard">
        <div className="error-message">{error}</div>
      </div>
    );
  }

  if (!summary) {
    return null;
  }

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(value);

  const formatPercent = (value: number) =>
    `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`;

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h1>Portfolio Dashboard</h1>
        <button className="btn-primary" onClick={() => navigate('/trade')}>
          Trade Stocks
        </button>
      </div>

      {/* Summary Cards */}
      <div className="summary-cards">
        <div className="card">
          <div className="card-label">Total Value</div>
          <div className="card-value">{formatCurrency(summary.totalPortfolioValue)}</div>
        </div>

        <div className="card">
          <div className="card-label">Cash Balance</div>
          <div className="card-value">{formatCurrency(summary.cashBalance)}</div>
        </div>

        <div className="card">
          <div className="card-label">Positions Value</div>
          <div className="card-value">{formatCurrency(summary.totalPositionsValue)}</div>
        </div>

        <div className="card">
          <div className="card-label">Unrealized P/L</div>
          <div
            className={`card-value ${
              summary.unrealizedPnL >= 0 ? 'positive' : 'negative'
            }`}
          >
            {formatCurrency(summary.unrealizedPnL)}
          </div>
        </div>

        <div className="card">
          <div className="card-label">Total Return</div>
          <div
            className={`card-value ${
              summary.percentageReturn >= 0 ? 'positive' : 'negative'
            }`}
          >
            {formatPercent(summary.percentageReturn)}
          </div>
        </div>
      </div>

      {/* Positions Section */}
      <div className="positions-section">
        <h2>Your Positions</h2>
        {positions.length === 0 ? (
          <div className="empty-state">
            <p>No positions yet. Start trading to build your portfolio!</p>
            <button className="btn-primary" onClick={() => navigate('/trade')}>
              Start Trading
            </button>
          </div>
        ) : (
          <div className="positions-table">
            <table>
              <thead>
                <tr>
                  <th>Symbol</th>
                  <th>Quantity</th>
                  <th>Avg Price</th>
                  <th>Current Price</th>
                  <th>Market Value</th>
                  <th>Cost Basis</th>
                  <th>P/L</th>
                  <th>Return %</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {positions.map((position) => (
                  <tr key={position.positionId}>
                    <td className="symbol">{position.stockSymbol}</td>
                    <td>{position.quantity}</td>
                    <td>{formatCurrency(position.averagePrice)}</td>
                    <td>{formatCurrency(position.currentPrice)}</td>
                    <td>{formatCurrency(position.currentMarketValue)}</td>
                    <td>{formatCurrency(position.costBasis)}</td>
                    <td className={position.unrealizedPnL >= 0 ? 'positive' : 'negative'}>
                      {formatCurrency(position.unrealizedPnL)}
                    </td>
                    <td className={position.percentageReturn >= 0 ? 'positive' : 'negative'}>
                      {formatPercent(position.percentageReturn)}
                    </td>
                    <td>
                      <button
                        className="btn-small"
                        onClick={() => navigate(`/trade?symbol=${position.stockSymbol}&action=sell`)}
                      >
                        Sell
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
