package rtp.example.rtp;

public class RealTimeStockDataService {

    /*
    * imports stock data from api
    * communicates live stock price with relevant classes
    *   which classes are relevant?
    *       ->update the stock price so maybe stock service
    *       -> determine exectution price change current price to get live price
    *
    *   methods
    *       getcurrentstockprice - results are cached for 30 seconds
    *                               - if the price is <30 seconds old, returns it
    *                               - otherwise calls fetchandupdate
    *       fetchandupdatestockprice - @transactional
    *                                - call api to fetch price
    *                                -validate response
    *                                   - create stockPrice entity and save, update stock entity current price
    *
     *      tracksymbols, stocktracksymbols, getactivesymbols
     *
     *      scheludedupdates
     *
     *      cleanupoldpricedata
     *
     *      dto classes to map json fields from api to java
     *
    *
    * */
}
