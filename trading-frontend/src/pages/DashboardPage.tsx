import React, { useEffect, useState } from 'react';
import { portfolioService } from '../services/portfolioService';
import { positionService } from '../services/positionService';
import { tradingService } from '../services/TradingService';
import { PortfolioSummary, PositionSummary, Quote } from '../types';
import { usePriceUpdates } from '../hooks/usePriceUpdates';
import { TradingViewChart } from '../components/TradingViewChart';
import { X } from 'lucide-react';
import './Dashboard.css';

interface QuoteModalProps {
  isOpen: boolean;
  onClose: () => void;
  quote: Quote | null;
  onConfirm: () => void;
  orderType: 'BUY' | 'SELL';
  loading: boolean;
}

const QuoteModal: React.FC<QuoteModalProps> = ({ isOpen, onClose, quote, onConfirm, orderType, loading }) => {
  if (!isOpen || !quote) return null;

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <button className="modal-close" onClick={onClose}>
          <X size={20} />
        </button>

        <h3>Order Preview</h3>

        <div className="quote-details">
          <div className="quote-row">
            <span className="quote-label">Symbol</span>
            <span className="quote-value">{quote.stockSymbol}</span>
          </div>
          <div className="quote-row">
            <span className="quote-label">Company</span>
            <span className="quote-value">{quote.companyName}</span>
          </div>
          <div className="quote-row">
            <span className="quote-label">Current Price</span>
            <span className="quote-value">{formatCurrency(quote.currentPrice)}</span>
          </div>
          <div className="quote-row quote-total">
            <span className="quote-label">Total Value</span>
            <span className="quote-value">{formatCurrency(quote.totalValue)}</span>
          </div>
        </div>

        <button
          className={`btn-confirm ${orderType === 'SELL' ? 'btn-sell' : 'btn-buy'}`}
          onClick={onConfirm}
          disabled={loading}
        >
          {loading ? 'Processing...' : `Confirm ${orderType}`}
        </button>
      </div>
    </div>
  );
};

export const DashboardPage: React.FC = () => {
  const [summary, setSummary] = useState<PortfolioSummary | null>(null);
  const [positions, setPositions] = useState<PositionSummary[]>([]);
  const [portfolioId, setPortfolioId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Trading form state
  const [symbol, setSymbol] = useState('AAPL');
  const [quantity, setQuantity] = useState('1');
  const [orderType, setOrderType] = useState<'BUY' | 'SELL'>('BUY');
  const [priceType, setPriceType] = useState<'MARKET' | 'LIMIT'>('MARKET');
  const [limitPrice, setLimitPrice] = useState('');
  const [quote, setQuote] = useState<Quote | null>(null);
  const [showQuoteModal, setShowQuoteModal] = useState(false);
  const [tradeLoading, setTradeLoading] = useState(false);

  useEffect(() => {
    loadDashboardData();
  }, []);

  usePriceUpdates((updatedPrice) => {
    setPositions((prevPositions) =>
      prevPositions.map((pos) =>
        pos.stockSymbol === updatedPrice.symbol
          ? {
              ...pos,
              currentPrice: updatedPrice.price,
              currentMarketValue: pos.quantity * updatedPrice.price,
              unrealizedPnL: pos.quantity * (updatedPrice.price - pos.averagePrice),
              percentageReturn: ((updatedPrice.price - pos.averagePrice) / pos.averagePrice) * 100,
            }
          : pos
      )
    );
  });

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

  const handleGetQuote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!symbol || !quantity) return;

    setError('');
    setQuote(null);
    setTradeLoading(true);

    try {
      const quoteData = await tradingService.getQuote(
        symbol.toUpperCase(),
        parseInt(quantity),
        orderType
      );
      setQuote(quoteData);
      setShowQuoteModal(true);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to get quote');
    } finally {
      setTradeLoading(false);
    }
  };

  const handleTrade = async () => {
    if (!portfolioId) {
      setError('No portfolio found');
      return;
    }

    setError('');
    setSuccess('');
    setTradeLoading(true);

    try {
      const request = {
        portfolioId,
        stockSymbol: symbol.toUpperCase(),
        quantity: parseInt(quantity),
        priceType,
        limitPrice: priceType === 'LIMIT' ? parseFloat(limitPrice) : undefined,
      };

      const response =
        orderType === 'BUY'
          ? await tradingService.buyStock(request)
          : await tradingService.sellStock(request);

      setSuccess(response.message);
      setShowQuoteModal(false);
      setQuote(null);
      setQuantity('1');

      setTimeout(() => {
        loadDashboardData();
        setSuccess('');
      }, 2000);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Trade failed');
    } finally {
      setTradeLoading(false);
    }
  };

  const formatCurrency = (value: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);

  const formatPercent = (value: number) =>
    `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`;

  if (loading) {
    return (
      <div className="dashboard">
        <div className="loading">Loading dashboard...</div>
      </div>
    );
  }

  return (
    <div className="dashboard">
      {error && <div className="error-message">{error}</div>}
      {success && <div className="success-message">{success}</div>}

      {/* Top Section - Chart + Trading Form */}
      <div className="top-section">
        {/* Chart - Left Side */}
        <div className="chart-container">
          <TradingViewChart symbol={symbol} height={500} />
        </div>

        {/* Trading Form - Right Side */}
        <div className="trade-form">
          <h2>Place Order</h2>

          <div className="trade-form-content">
            <div className="order-type-toggle">
              <button
                type="button"
                className={`toggle-btn ${orderType === 'BUY' ? 'active buy' : ''}`}
                onClick={() => setOrderType('BUY')}
              >
                Buy
              </button>
              <button
                type="button"
                className={`toggle-btn ${orderType === 'SELL' ? 'active sell' : ''}`}
                onClick={() => setOrderType('SELL')}
              >
                Sell
              </button>
            </div>

            <div className="form-group">
              <label htmlFor="symbol">Symbol</label>
              <input
                id="symbol"
                type="text"
                value={symbol}
                onChange={(e) => setSymbol(e.target.value.toUpperCase())}
                placeholder="AAPL"
                required
                disabled={tradeLoading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="quantity">Quantity</label>
              <input
                id="quantity"
                type="number"
                min="1"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                required
                disabled={tradeLoading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="priceType">Order Type</label>
              <select
                id="priceType"
                value={priceType}
                onChange={(e) => setPriceType(e.target.value as 'MARKET' | 'LIMIT')}
                disabled={tradeLoading}
              >
                <option value="MARKET">Market</option>
                <option value="LIMIT">Limit</option>
              </select>
            </div>

            {priceType === 'LIMIT' && (
              <div className="form-group">
                <label htmlFor="limitPrice">Limit Price</label>
                <input
                  id="limitPrice"
                  type="number"
                  step="0.01"
                  min="0"
                  value={limitPrice}
                  onChange={(e) => setLimitPrice(e.target.value)}
                  placeholder="0.00"
                  required
                  disabled={tradeLoading}
                />
              </div>
            )}

            <button
              type="button"
              className="btn-primary"
              disabled={tradeLoading}
              onClick={handleGetQuote}
            >
              {tradeLoading ? 'Loading...' : 'Get Quote'}
            </button>
          </div>
        </div>
      </div>

      {/* Bottom Section - Positions + Summary */}
      <div className="bottom-section">
        {/* Positions Table */}
        <div className="positions-section">
          <h2>Open Positions</h2>
          {positions.length === 0 ? (
            <div className="empty-state">
              <p>No positions yet. Start trading to build your portfolio!</p>
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
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Summary Cards */}
        {summary && (
          <div className="summary-cards">
            <div className="card">
              <div className="card-label">TOTAL VALUE</div>
              <div className="card-value">{formatCurrency(summary.totalPortfolioValue)}</div>
            </div>
            <div className="card">
              <div className="card-label">CASH</div>
              <div className="card-value">{formatCurrency(summary.cashBalance)}</div>
            </div>
            <div className="card">
              <div className="card-label">POSITIONS</div>
              <div className="card-value">{formatCurrency(summary.totalPositionsValue)}</div>
            </div>
            <div className="card">
              <div className="card-label">UNREALIZED P/L</div>
              <div className={`card-value ${summary.unrealizedPnL >= 0 ? 'positive' : 'negative'}`}>
                {formatCurrency(summary.unrealizedPnL)}
              </div>
            </div>
            <div className="card">
              <div className="card-label">RETURN</div>
              <div className={`card-value ${summary.percentageReturn >= 0 ? 'positive' : 'negative'}`}>
                {formatPercent(summary.percentageReturn)}
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Quote Modal */}
      <QuoteModal
        isOpen={showQuoteModal}
        onClose={() => setShowQuoteModal(false)}
        quote={quote}
        onConfirm={handleTrade}
        orderType={orderType}
        loading={tradeLoading}
      />
    </div>
  );
};