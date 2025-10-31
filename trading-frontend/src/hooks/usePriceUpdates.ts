import { useEffect, useRef } from "react";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

interface StockPrice {
  symbol: string;
  price: number;
  change: number;
  changePercent: number;
  timestamp: string;
}

export function usePriceUpdates(onPriceUpdate: (data: StockPrice) => void) {
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    const socket = new SockJS("http://localhost:8080/ws-trading");
    const client = new Client({
      webSocketFactory: () => socket as any,
      debug: (str) => console.log(str),
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe("/topic/prices", (message) => {
          const data = JSON.parse(message.body);
          onPriceUpdate(data);
        });
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [onPriceUpdate]);
}
