package net.alex.gsr.orderbook;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OrderBookTest {

    @Test
    public void testAdds() {
        OrderBook orderBook = new OrderBook(3);
        onBuy(orderBook, 123, 100);
        onBuy(orderBook, 122, 200);
        onBuy(orderBook, 121, 300);
        onSell(orderBook, 124, 100);
        onSell(orderBook, 125, 200);
        checkPrices(orderBook.buys, 1.23, 1.22, 1.21);
        checkQuantities(orderBook.buys, 1, 2, 3);
        checkPrices(orderBook.sells, 1.24, 1.25);
        checkQuantities(orderBook.sells, 1, 2);
    }

    @Test
    public void testUpdates() {
        OrderBook orderBook = new OrderBook(3);
        onBuy(orderBook, 123, 100);
        onBuy(orderBook, 122, 200);
        onBuy(orderBook, 121, 300);
        onSell(orderBook, 124, 100);
        onSell(orderBook, 125, 200);
        onBuy(orderBook, 122, 300);
        onSell(orderBook, 124, 200);
        checkPrices(orderBook.buys, 1.23, 1.22, 1.21);
        checkQuantities(orderBook.buys, 1, 3, 3);
        checkPrices(orderBook.sells, 1.24, 1.25);
        checkQuantities(orderBook.sells, 2, 2);
    }

    @Test
    public void testDeletes() {
        OrderBook orderBook = new OrderBook(3);
        onBuy(orderBook, 123, 100);
        onBuy(orderBook, 122, 200);
        onBuy(orderBook, 121, 300);
        onSell(orderBook, 124, 100);
        onSell(orderBook, 125, 200);
        onBuy(orderBook, 122, 0);
        onSell(orderBook, 125, 0);
        checkPrices(orderBook.buys, 1.23, 1.21);
        checkQuantities(orderBook.buys, 1, 3);
        checkPrices(orderBook.sells, 1.24);
        checkQuantities(orderBook.sells, 1);
    }

    @Test
    public void testLiquitySum() {
        OrderBook orderBook = new OrderBook(3);
        onBuy(orderBook, 123, 100);
        onBuy(orderBook, 122, 200);
        onBuy(orderBook, 121, 300);
        onSell(orderBook, 124, 100);
        onSell(orderBook, 125, 200);
        Assertions.assertEquals(3, orderBook.getSide(true).getLiquidity(2));
        Assertions.assertEquals(3, orderBook.getSide(false).getLiquidity(3));
    }

    @Test
    public void testBigLiquitySum() {
        OrderBook orderBook = new OrderBook(3);
        onBuy(orderBook, 123, 1_000_000_000);
        onBuy(orderBook, 122, 1_500_000_000);
        onBuy(orderBook, 121, 2_000_000_000);
        onSell(orderBook, 124, 1_000_000_000);
        onSell(orderBook, 125, 2_000_000_000);
        Assertions.assertEquals(45_000_000, orderBook.getSide(true).getLiquidity(5));
        Assertions.assertEquals(30_000_000, orderBook.getSide(false).getLiquidity(5));
    }

    @Test
    public void testMid() {
        OrderBook orderBook = new OrderBook(3);
        onBuy(orderBook, 123, 100);
        Assertions.assertTrue(Double.isNaN(orderBook.getMid()));
        onBuy(orderBook, 122, 200);
        onBuy(orderBook, 121, 300);
        onSell(orderBook, 124, 100);
        onSell(orderBook, 125, 200);
        Assertions.assertEquals(1.235, orderBook.getMid(), 0.000001);
    }

    @Test
    public void testAverageMid() {
        OrderBook orderBook = new OrderBook(3);
        onBuy(orderBook, 123, 100);
        Assertions.assertTrue(Double.isNaN(orderBook.getAverageMid()));
        onSell(orderBook, 125, 100);
        Assertions.assertEquals(((1.23 + 1.25)) / 2, orderBook.getAverageMid(), 0.000001);
        onBuy(orderBook, 122, 200);
        Assertions.assertEquals(((1.23 + 1.25)) / 2, orderBook.getAverageMid(), 0.000001); // Top of book not changed
        onSell(orderBook, 124, 100);
        Assertions.assertEquals(((1.23 + 1.25) + (1.23 + 1.24)) / 2 / 2, orderBook.getAverageMid(), 0.000001);
        onBuy(orderBook, 123, 0);
        Assertions.assertEquals(((1.23 + 1.25) + (1.23 + 1.24) + (1.22 + 1.24)) / 3 / 2, orderBook.getAverageMid(), 0.000001);
        onSell(orderBook, 124, 0);
        Assertions.assertEquals(((1.22 + 1.25) + (1.23 + 1.24) + (1.22 + 1.24)) / 3 / 2, orderBook.getAverageMid(), 0.000001);
    }

    void checkPrices(OrderBook.Side side, double ... prices) {
        Assertions.assertEquals(prices.length, side.depth);
        for (int i = 0; i < prices.length; i++) {
            Assertions.assertEquals(prices[i], side.getPrice(i));
        }
    }

    void checkQuantities(OrderBook.Side side, double ... quantities) {
        Assertions.assertEquals(quantities.length, side.depth);
        for (int i = 0; i < quantities.length; i++) {
            Assertions.assertEquals(quantities[i], side.getQuantity(i));
        }
    }



    int time;

    void onBuy(OrderBook orderBook, int price, int quantity) {
        orderBook.onInput(input(time++, "BTC-USD", price, quantity, true));
    }

    void onSell(OrderBook orderBook, int price, int quantity) {
        orderBook.onInput(input(time++, "BTC-USD", price, quantity, false));
    }

    static OrderBook.Input input(long timestamp, String instrument, int price, int quantity, boolean isBuy) {
        OrderBook.Input input = new OrderBook.Input();
        input.timestamp = timestamp;
        input.instrument = instrument;
        input.price = price;
        input.quantity = quantity;
        input.isBuy = isBuy;
        return input;
    }
}
