REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_recon_summary;

SELECT DISTINCT
    t.instrument_id,
        t.trade_date,
            SUM(t.price * t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date)
                    / NULLIF(SUM(t.quantity) OVER (PARTITION BY t.instrument_id, t.trade_date), 0)
                                AS vwap
                                FROM trades t
                                WHERE t.deleted_at IS NULL
                                  AND t.asset_class = 'EQUITY'
                                  ORDER BY t.trade_date DESC, t.instrument_id;