package hpang.quickfix.executor;


/**
 * Trivial market data provider interface to allow plug-ins for
 * alternative market data sources.
 */
public interface MarketDataProvider {

    double getBid(String symbol);

    double getAsk(String symbol);
}
