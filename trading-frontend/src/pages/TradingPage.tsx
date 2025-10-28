// src/pages/TradingPage.tsx
import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { portfolioService } from '../services/portfolioService';
import { tradingService } from '../services/TradingService';
import { Quote } from '../types';
import './Trading.css';

export const TradingPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const [portfolioId, setPortfolioId] = useState<number | null>(null);
  const [symbol, setSymbol] = useState(searchParams.get('symbol') || '');
  const [quantity, setQuantity] = useState('1');
  const [orderType, setOrderType] = useState<'BUY' | 'SELL'>(
    (searchParams.get('action')?.toUpperCase() as 'BUY' | 'SELL') || 'BUY'
  );
  const [priceType, setPriceType] = useState<'MARKET' | 'LIMIT'>('MARKET');
  const [limitPrice, setLimitPrice] = useState('');

  const [quote, setQuote] = useState<Quote | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

useEffect(() => {
  const loadPortfolio = async () => {
    try {
      const portfolio = await portfolioService.getMyPortfolio();
      if (portfolio) {
        setPortfolioId(portfolio.id);
      } else {
        setError('No portfolio found. Please create one first.');
      }
    } catch (err) {
      console.error('Failed to load portfolio', err);
      setError('Failed to load portfolio.');
    }
  };
  loadPortfolio();
}, []);

  const handleGetQuote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!symbol || !quantity) return;

    setError('');
    setQuote(null);
    setLoading(true);

    try {
      const quoteData = await tradingService.getQuote(
        symbol.toUpperCase(),
        parseInt(quantity),
        orderType
      );
      setQuote(quoteData);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to get quote');
    } finally {
      setLoading(false);
    }
  };

  const handleTrade = async () => {
    if (!portfolioId) {
      setError('No portfolio found');
      return;
    }

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const request = {
        portfolioId,
        stockSymbol: symbol.toUpperCase(),
        quantity: parseInt(quantity),
        priceType,
        limitPrice: priceType === 'LIMIT' ? parseFloat(limitPrice) : undefined,
      };

      const response = orderType === 'BUY'
        ? await tradingService.buyStock(request)
        : await tradingService.sellStock(request);

      setSuccess(response.message);
      setQuote(null);
      setSymbol('');
      setQuantity('1');

      // Redirect to dashboard after 2 seconds
      setTimeout(() => {
        navigate('/dashboard');
      }, 2000);

    } catch (err: any) {
      setError(err.response?.data?.message || 'Trade failed');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(value);
  };

  return (
    <div className="trading-page">
      <div className="trading-header">
        <h1>Trade Stocks</h1>
        <button className="btn-secondary" onClick={() => navigate('/dashboard')}>
          Back to Dashboard
        </button>
      </div>

      {error && <div className="error-message">{error}</div>}
      {success && <div className="success-message">{success}</div>}

      <div className="trading-container">
        {/* Trade Form */}
        <div className="trade-form-card">
          <h2>Place Order</h2>

          <form onSubmit={handleGetQuote}>
            {/* Order Type Toggle */}
            <div className="order-type-toggle">
              <button
                type="button"
                className={`toggle-btn ${orderType === 'BUY' ? 'active' : ''}`}
                onClick={() => setOrderType('BUY')}
              >
                Buy
              </button>
              <button
                type="button"
                className={`toggle-btn ${orderType === 'SELL' ? 'active' : ''}`}
                onClick={() => setOrderType('SELL')}
              >
                Sell
              </button>
            </div>

            {/* Stock Symbol */}
            <div className="form-group">
              <label htmlFor="symbol">Stock Symbol</label>
              <input
                id="symbol"
                type="text"
                value={symbol}
                onChange={(e) => setSymbol(e.target.value.toUpperCase())}
                placeholder="AAPL, TSLA, MSFT..."
                required
                disabled={loading}
              />
            </div>

            {/* Quantity */}
            <div className="form-group">
              <label htmlFor="quantity">Quantity</label>
              <input
                id="quantity"
                type="number"
                min="1"
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                required
                disabled={loading}
              />
            </div>

            {/* Price Type */}
            <div className="form-group">
              <label htmlFor="priceType">Order Type</label>
              <select
                id="priceType"
                value={priceType}
                onChange={(e) => setPriceType(e.target.value as 'MARKET' | 'LIMIT')}
                disabled={loading}
              >
                <option value="MARKET">Market Order</option>
                <option value="LIMIT">Limit Order</option>
              </select>
            </div>

            {/* Limit Price */}
            {priceType === 'LIMIT' && (
              <div className="form-group">
                <label htmlFor="limitPrice">Limit Price ($)</label>
                <input
                  id="limitPrice"
                  type="number"
                  step="0.01"
                  min="0"
                  value={limitPrice}
                  onChange={(e) => setLimitPrice(e.target.value)}
                  placeholder="0.00"
                  required
                  disabled={loading}
                />
              </div>
            )}

            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? 'Loading...' : 'Get Quote'}
            </button>
          </form>
        </div>

        {/* Quote Display */}
        {quote && (
          <div className="quote-card">
            <h2>Order Preview</h2>

            <div className="quote-details">
              <div className="quote-row">
                <span className="quote-label">Symbol:</span>
                <span className="quote-value">{quote.stockSymbol}</span>
              </div>
              <div className="quote-row">
                <span className="quote-label">Company:</span>
                <span className="quote-value">{quote.companyName}</span>
              </div>
              <div className="quote-row">
                <span className="quote-label">Current Price:</span>
                <span className="quote-value">{formatCurrency(quote.currentPrice)}</span>
              </div>
              <div className="quote-row">
                <span className="quote-label">Quantity:</span>
                <span className="quote-value">{quote.quantity}</span>
              </div>
              <div className="quote-row">
                <span className="quote-label">Order Type:</span>
                <span className="quote-value">{quote.orderType}</span>
              </div>
              <div className="quote-row total">
                <span className="quote-label">Total Value:</span>
                <span className="quote-value">{formatCurrency(quote.totalValue)}</span>
              </div>
            </div>

            <button
              className={`btn-primary ${orderType === 'SELL' ? 'btn-sell' : ''}`}
              onClick={handleTrade}
              disabled={loading}
            >
              {loading ? 'Processing...' : `Confirm ${orderType}`}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};