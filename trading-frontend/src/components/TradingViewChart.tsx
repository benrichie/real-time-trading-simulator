import React, { useEffect, useRef } from 'react';

declare global {
  interface Window {
    TradingView: any;
  }
}

interface TradingViewChartProps {
  symbol: string;
}

export const TradingViewChart: React.FC<TradingViewChartProps> = ({ symbol }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const scriptRef = useRef<HTMLScriptElement | null>(null);

  useEffect(() => {
    let isMounted = true;

    const loadTradingView = () => {
      if (!isMounted || !containerRef.current) return;

      containerRef.current.innerHTML = '';

      if (window.TradingView) {
        new window.TradingView.widget({
          autosize: true,
          symbol: symbol.toUpperCase(),
          interval: '1',
          timezone: 'Etc/UTC',
          theme: 'light',
          style: '1',
          locale: 'en',
          toolbar_bg: '#f1f3f6',
          enable_publishing: false,
          allow_symbol_change: true,
          container_id: containerRef.current.id,
        });
      }
    };

    if (!window.TradingView) {
      const script = document.createElement('script');
      script.src = 'https://s3.tradingview.com/tv.js';
      script.async = true;
      script.onload = loadTradingView;
      document.body.appendChild(script);
      scriptRef.current = script;
    } else {
      loadTradingView();
    }

    return () => {
      isMounted = false;
      if (containerRef.current) containerRef.current.innerHTML = '';
      if (scriptRef.current) {
        scriptRef.current.onload = null;
      }
    };
  }, [symbol]);

  return (
    <div
      id={`tradingview_${symbol}`}
      ref={containerRef}
      style={{ width: '100%', height: '100%' }}
    />
  );
};
