package net.alex.gsr.orderbook;

import java.util.HashMap;
import java.util.Map;

public class OrderBook {

    private final int lengthOfHistory;
    private final int[] bestBuys, bestSells; // Circular buffers
    private int nextHistoryIndex;
    private int lastBests;

    public OrderBook(int lengthOfHistory) {
        this.lengthOfHistory = lengthOfHistory;
        bestBuys = new int[lengthOfHistory];
        bestSells = new int[lengthOfHistory];
    }


    public static class Input {
        long timestamp;
        String instrument;
        int price;
        int quantity;
        boolean isBuy;
    }

    final Side buys = new BuySide(), sells = new SellSide();

    public Side getSide(boolean isBuy) {
        return isBuy ? buys : sells;
    }

    public double getMid() {
        return buys.depth > 0 && sells.depth > 0 ? ((double)(buys.prices[0] + sells.prices[0])) / 200 : Double.NaN;
    }

    public double getAverageMid() {
        if (nextHistoryIndex > 0) {
            long total = 0;
            int numBests = Math.min(nextHistoryIndex, lengthOfHistory);
            for (int i = 0; i < numBests; i++) {
                total += bestBuys[i] + bestSells[i];
            }
            return ((double)total) / numBests / 200;
        }
        return Double.NaN;
    }

    void onInput(Input input) {
        (input.isBuy ? buys : sells).onInput(input);
        if (buys.depth > 0 && sells.depth > 0) {
            int currentBests = buys.prices[0] + sells.prices[0];
            if (currentBests != lastBests) {
                int moddedNextMidIndex = nextHistoryIndex++ % lengthOfHistory;
                bestBuys[moddedNextMidIndex] = buys.prices[0];
                bestSells[moddedNextMidIndex] = sells.prices[0];
                lastBests = currentBests;
            }
        }

    }

    static class BuySide extends Side {
        @Override
        int findIndex(Input input) {
            for (int i = 0; i < depth; i++) {
                if (input.price >= prices[i]) {
                    return i;
                }
            }
            return depth;
        }
    }

    static class SellSide extends Side {
        @Override
        int findIndex(Input input) {
            for (int i = 0; i < depth; i++) {
                if (input.price <= prices[i]) {
                    return i;
                }
            }
            return depth;
        }
    }

    static abstract class Side {
        boolean isBid;
        // Could probably just use longs as will presumably be running on a 64 bit machine
        int[] prices = new int[100];
        int[] quantities = new int[100];
        int depth;

        abstract int findIndex(Input input);

        void onInput(Input input) {
            int index = findIndex(input);
            if (index < depth) {
                if (prices[index] == input.price) {
                    if (input.quantity > 0) { // Update price
                        quantities[index] = input.quantity;
                    } else { // Remove price and shift everything up
                        System.arraycopy(prices, index + 1, prices, index, depth - index);
                        System.arraycopy(quantities, index + 1, quantities, index, depth - index);
                        depth--;
                    }
                } else { // Shift everything down and insert price
                    System.arraycopy(prices, index, prices, index + 1, depth - index);
                    System.arraycopy(quantities, index, quantities, index + 1, depth - index);
                    prices[index] = input.price;
                    quantities[index] = input.quantity;
                    depth++;
                }
            } else { // Stick on the end
                prices[depth] = input.price;
                quantities[depth] = input.quantity;
                depth++;
            }
        }

        // Assume this won't be called as often as we get price updates so calculated on demand
        public double getLiquidity(int levels) {
            long liquidity = 0;
            for (int i = 0; i < Math.min(levels, depth); i++) {
                liquidity += quantities[i];
            }
            return toDouble(liquidity);
        }

        public int getDepth() {
            return depth;
        }

        public double getPrice(int depth) {
            return depth < this.depth ? toDouble(prices[depth]) : Double.NaN;
        }

        public double getQuantity(int depth) {
            return depth < this.depth ? toDouble(quantities[depth]) : Double.NaN;
        }

    }

    private static double toDouble(long l) {
        return ((double)l) / 100;
    }

}
