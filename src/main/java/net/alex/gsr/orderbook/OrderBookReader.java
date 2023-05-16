package net.alex.gsr.orderbook;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class OrderBookReader {

    // Assuming timestamp and instrument are fixed length
    private static final String SHORTEST_UPDATE = "t=1638848595|i=BTC-USD|p=0.01|q=0.00|s=b";
    private static final String LONGEST_UPDATE = "t=1638848595|i=BTC-USD|p=999.99|q=10737418.23|s=b";
    private static final int MIN_BYTES = SHORTEST_UPDATE.length();
    private static final int MAX_BYTES = LONGEST_UPDATE.length();
    private static final int EARLIEST_LAST_EQUALS_INDEX = SHORTEST_UPDATE.lastIndexOf("=");

    OrderBook.Input input = new OrderBook.Input();
    byte[] incomingBytes = new byte[100];
    int incomingBytesStart;
    int incomingBytesEnd;

    public void parse(InputStream inputStream, Consumer<OrderBook.Input> priceConsumer) throws IOException {
        int available;
        while ((available = inputStream.available()) > 0) {
            incomingBytesEnd += inputStream.read(incomingBytes, incomingBytesEnd, Math.min(available, MAX_BYTES - incomingBytesEnd));
            if (incomingBytesEnd >= MIN_BYTES) {
                if (endFound(incomingBytes, incomingBytesEnd)) {
                    int numBytesRead = readPrice(incomingBytes, input);
                    priceConsumer.accept(input);
                    System.arraycopy(incomingBytes, numBytesRead, incomingBytes, 0, incomingBytesEnd - numBytesRead);
                    incomingBytesEnd -= numBytesRead;
                }
            }
        }
    }

    private final IntParseResult result = new IntParseResult();
    private final Map<StringBuilder, String> stringMap = new HashMap<>();
    private final StringBuilder sb = new StringBuilder();

    String getString(StringBuilder sb) {
        String string = stringMap.get(sb);
        if (string == null) {
            string = sb.toString();
            stringMap.put(new StringBuilder(sb), string);
        }
        return string;
    }

    // returns number of bytes read for price
    int readPrice(byte[] bytes, OrderBook.Input input) {
        input.timestamp = 0;
        for (int i = 2; i < 12; i++) { // Timestamp is 10 bytes
            input.timestamp = input.timestamp * 10 + asciiToInt(bytes[i]);
        }
        sb.setLength(0);
        for (int i = 15; i < 22; i++) {
            sb.append((char)bytes[i]);
        }
        input.instrument = getString(sb);
        read2dpDoubleAsLong(bytes, 25, result);
        input.price = result.result;
        read2dpDoubleAsLong(bytes, result.nextByteIndex + 3, result);
        input.quantity = result.result;
        input.isBuy = bytes[result.nextByteIndex + 3] == 'b';
        return result.nextByteIndex + 4;
    }

    static class IntParseResult {
        int result;
        int nextByteIndex;
    }

    static void read2dpDoubleAsLong(byte[] bytes, int start, IntParseResult result) {
        result.result = 0;
        for (int i = start; i < start + 10; i++) {
            if (bytes[i] == '.') {
                result.result = result.result * 100 + 10 * asciiToInt(bytes[i + 1]) + asciiToInt(bytes[i + 2]);
                result.nextByteIndex = i + 3;
                return;
            } else {
                result.result = result.result * 10 + asciiToInt(bytes[i]);
            }
        }
        throw new IllegalArgumentException("Failed to parse bytes: " + new String(bytes));
    }

    private static int asciiToInt(byte ascii) {
        return ascii - '0';
    }

    static boolean endFound(byte[] bytes, int endIndex) {
        for (int i = EARLIEST_LAST_EQUALS_INDEX; i < endIndex; i++) {
            if (bytes[i] == '=') {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        Map<String, OrderBook> orderBooks = new HashMap<>();
//        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(new File("prices.txt")));
        String newLineStrippedString = new String(OrderBookReader.class.getResourceAsStream("/prices.txt").readAllBytes()).replaceAll("(\\r|\\n)", "");
        byte[] newLineStrippedBytes = newLineStrippedString.getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(newLineStrippedBytes);
        new OrderBookReader().parse(inputStream, input -> {
            OrderBook orderBook = orderBooks.computeIfAbsent(input.instrument, i -> new OrderBook(3));
            orderBook.onInput(input);
            System.out.println("OrderBook [" + input.instrument + "] Best Buy [" + orderBook.buys.getPrice(0) + "] Best Sell [" + orderBook.sells.getPrice(0) + "] Mid [" + orderBook.getMid() + "] Avg Mid [" + orderBook.getAverageMid() + "] Buys Liq [" + orderBook.buys.getLiquidity(100) + "] Sells Liq [" + orderBook.sells.getLiquidity(100) + "].");
        });
    }
}
