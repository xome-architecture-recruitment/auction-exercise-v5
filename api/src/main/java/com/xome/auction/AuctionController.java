package com.xome.auction;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auction")
@CrossOrigin(origins = "http://localhost:3000") // pre-configured so you don't fight CORS
public class AuctionController {

    private static final Logger log = LoggerFactory.getLogger(AuctionController.class);

    private final AuctionStore store;

    public AuctionController(AuctionStore store) {
        this.store = store;
    }

    /** Health check — already working. */
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }

    @PostMapping("/bids")
    public ResponseEntity<Map<String, Object>> placeBid(@RequestBody BidRequest request) {
        if (request == null || request.bidderId() == null || request.bidderId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "accepted", false,
                    "reason", "Bidder ID is required",
                    "status", currentStatus()));
        }

        log.info("received bid request for bidder {} with maximum {}", request.bidderId(), request.amount());

        if (request.amount() <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "accepted", false,
                    "reason", "Bid amount must be positive",
                    "status", currentStatus()));
        }

        if (!store.recordMaximum(request.bidderId(), request.amount())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "accepted", false,
                    "reason", "Another bidder already has that maximum",
                    "status", currentStatus()));
        }

        return ResponseEntity.ok(Map.of(
                "accepted", true,
                "reason", "Accepted",
                "status", currentStatus()));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(currentStatus());
    }

    private Map<String, Object> currentStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("currentPrice", store.currentPrice());
        status.put("leadingBidder", store.leadingBidder());
        status.put("reserveMet", false);
        status.put("closesAt", store.closesAt().toString());
        return status;
    }
}
