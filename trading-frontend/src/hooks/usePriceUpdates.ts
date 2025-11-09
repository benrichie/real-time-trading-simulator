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
    // Use environment variable for API URL
    const apiUrl = process.env.REACT_APP_API_URL || "http://localhost:8080";

    // SockJS uses http/https and upgrades to WebSocket automatically
    const socketUrl = `${apiUrl}/ws-trading`;

    console.log("Connecting to WebSocket:", socketUrl);

    const socket = new SockJS(socketUrl);
    const client = new Client({
      webSocketFactory: () => socket as any,
      debug: (str) => console.log(str),
      reconnectDelay: 5000,
      onConnect: () => {
        console.log("WebSocket connected");
        client.subscribe("/topic/prices", (message) => {
          const data = JSON.parse(message.body);
          onPriceUpdate(data);
        });
      },
      onStompError: (frame) => {
        console.error("STOMP error:", frame);
      },
      onWebSocketError: (event) => {
        console.error("WebSocket error:", event);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
    };
  }, [onPriceUpdate]);
}