import React, { useEffect, useRef } from 'react';

declare global {
  interface Window {
    TradingView: any;
  }
}

interface TradingViewChartProps {
  symbol: string;
  width?: string | number;
  height?: string | number;
}

export const TradingViewChart: React.FC<TradingViewChartProps> = ({
  symbol,
  width = "100%",
  height = 500,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!window.TradingView) {
      const script = document.createElement('script');
      script.src = 'https://s3.tradingview.com/tv.js';
      script.async = true;
      script.onload = () => createWidget();
      document.body.appendChild(script);
    } else {
      createWidget();
    }

    function createWidget() {
      if (containerRef.current && window.TradingView) {
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
    }
  }, [symbol]);

  return (
    <div
      id={`tradingview_${symbol}`}
      ref={containerRef}
      style={{ width, height }}
    />
  );
};
