package com.xome.auction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class AuctionStore {

    public static final long STARTING_PRICE = 250_000L;
    public static final long INCREMENT = 5_000L;

    private final Instant closesAt = Instant.now().plusSeconds(3600);
    private final List<Bid> bids = new ArrayList<>();
    private final Map<String, Long> maxBidsByBidder = new HashMap<>();

    public Instant closesAt() {
        return closesAt;
    }

    public List<Bid> bids() {
        return bids;
    }

    public void addBid(Bid b) {
        bids.add(b);
    }

    public boolean recordMaximum(String bidderId, long maximum) {
        // Equal maxima still need a tie-break rule; not implemented yet.
        boolean alreadyExistsForDifferentBidder = maxBidsByBidder.entrySet().stream()
                .anyMatch(entry -> !entry.getKey().equals(bidderId) && entry.getValue() == maximum);
        if (alreadyExistsForDifferentBidder) {
            return false;
        }

        maxBidsByBidder.put(bidderId, maximum);
        bids.add(new Bid(bidderId, maximum, Instant.now()));
        return true;
    }

    public String leadingBidder() {
        return maxBidsByBidder.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public long currentPrice() {
        if (maxBidsByBidder.isEmpty()) {
            return STARTING_PRICE;
        }

        List<Long> maxima = maxBidsByBidder.values().stream()
                .sorted(Comparator.reverseOrder())
                .toList();

        if (maxima.size() == 1) {
            return STARTING_PRICE;
        }

        long leaderMax = maxima.get(0);
        long secondHighest = maxima.get(1);
        long candidate = secondHighest + INCREMENT;
        return Math.min(candidate, leaderMax);
    }

    public void clear() {
        bids.clear();
        maxBidsByBidder.clear();
    }
}
