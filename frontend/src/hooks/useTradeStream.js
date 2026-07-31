// TICKET-ADV116 — useTradeStream() — SSE subscription returning live trades.
import { useEffect, useState } from 'react';

const MAX_TRADES = 200;

export function useTradeStream(url = '/api/v1/trades/stream') {
  const [trades, setTrades] = useState([]);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    const source = new EventSource(url);

    source.onopen = () => setIsConnected(true);

    source.addEventListener('connected', () => setIsConnected(true));

    source.onmessage = (event) => {
      try {
        const trade = JSON.parse(event.data);
        setTrades((prev) => [trade, ...prev].slice(0, MAX_TRADES));
      } catch {
        // ignore non-JSON payloads
      }
    };

    source.onerror = () => {
      if (source.readyState === EventSource.CLOSED) {
        setIsConnected(false);
      }
    };

    return () => {
      source.close();
      setIsConnected(false);
    };
  }, [url]);

  return { trades, isConnected };
}
