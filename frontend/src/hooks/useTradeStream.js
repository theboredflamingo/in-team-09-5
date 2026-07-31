// TICKET-ADV116 — useTradeStream() — SSE subscription returning live trades.
import { useEffect, useState } from 'react';

const MAX_TRADES = 200;

export function useTradeStream(url = '/api/v1/trades/stream') {
  const [trades, setTrades] = useState([]);
  const [isConnected, setConnected] = useState(false);

  useEffect(() => {
    const source = new EventSource(url);

    source.onopen = () => setConnected(true);

    source.addEventListener('connected', () => setConnected(true));

    source.onmessage = (event) => {
      try {
        const trade = JSON.parse(event.data);
        setTrades((prev) => [trade, ...prev].slice(0, MAX_TRADES));
      } catch {
        // ignore malformed payloads
      }
    };

    source.onerror = () => {
      if (source.readyState === EventSource.CLOSED) {
        setConnected(false);
      }
    };

    return () => {
      source.close();
      setConnected(false);
    };
  }, [url]);

  return { trades, isConnected };
}
