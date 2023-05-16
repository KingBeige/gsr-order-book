package net.alex.gsr.orderbook;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OrderBookReaderTest {
    @Test
    void testRead2dpDoubleAsInt() {
        OrderBookReader.IntParseResult result = new OrderBookReader.IntParseResult();
        OrderBookReader.read2dpDoubleAsLong("t=1638848595|i=BTC-USD|p=0.01|q=0.00|s=b".getBytes(), 25, result);
        Assertions.assertEquals(1, result.result);
        Assertions.assertEquals(29, result.nextByteIndex);
        OrderBookReader.read2dpDoubleAsLong("t=1638848595|i=BTC-USD|p=0.01|q=0.00|s=b".getBytes(), 32, result);
        Assertions.assertEquals(0, result.result);
        Assertions.assertEquals(36, result.nextByteIndex);
        OrderBookReader.read2dpDoubleAsLong("t=1638848595|i=BTC-USD|p=999.99|q=10737418.23|s=b".getBytes(), 25, result);
        Assertions.assertEquals(99999, result.result);
        Assertions.assertEquals(31, result.nextByteIndex);
        OrderBookReader.read2dpDoubleAsLong("t=1638848595|i=BTC-USD|p=999.99|q=10737418.23|s=b".getBytes(), 34, result);
        Assertions.assertEquals(1073741823, result.result);
        Assertions.assertEquals(45, result.nextByteIndex);
    }

    @Test
    void testReadPrice() {
        OrderBookReader orderBook = new OrderBookReader();
        OrderBook.Input input = new OrderBook.Input();
        orderBook.readPrice("t=1638848595|i=BTC-USD|p=0.01|q=0.00|s=b".getBytes(), input);
        Assertions.assertEquals(1638848595, input.timestamp);
        Assertions.assertEquals("BTC-USD", input.instrument.toString());
        Assertions.assertEquals(1, input.price);
        Assertions.assertEquals(0, input.quantity);
        Assertions.assertEquals(true, input.isBuy);
        orderBook.readPrice("t=1638848595|i=BTC-USD|p=999.99|q=10737418.23|s=s".getBytes(), input);
        Assertions.assertEquals(1638848595, input.timestamp);
        Assertions.assertEquals("BTC-USD", input.instrument.toString());
        Assertions.assertEquals(99999, input.price);
        Assertions.assertEquals(1073741823, input.quantity);
        Assertions.assertEquals(false, input.isBuy);
    }
}