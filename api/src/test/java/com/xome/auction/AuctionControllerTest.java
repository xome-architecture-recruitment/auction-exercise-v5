package com.xome.auction;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuctionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuctionStore store;

    @BeforeEach
    void resetAuction() {
        store.clear();
    }

    @Test
    void pingReturnsOk() throws Exception {
        mockMvc.perform(get("/api/auction/ping"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void aliceLeadsAtStartingPrice() throws Exception {
        mockMvc.perform(post("/api/auction/bids")
                       .contentType(APPLICATION_JSON)
                       .content("{\"bidderId\":\"Alice\",\"amount\":300000,\"requestId\":\"1\"}"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.accepted").value(true))
               .andExpect(jsonPath("$.status.currentPrice").value(250000))
               .andExpect(jsonPath("$.status.leadingBidder").value("Alice"));
    }

    @Test
    void bobDoesNotDislodgeAlice() throws Exception {
        placeBid("Alice", 300000);
        placeBid("Bob", 280000);

        mockMvc.perform(get("/api/auction/status"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.currentPrice").value(285000))
               .andExpect(jsonPath("$.leadingBidder").value("Alice"));
    }

    @Test
    void carlaBecomesLeaderAtFiveThousandAboveAlice() throws Exception {
        placeBid("Alice", 300000);
        placeBid("Bob", 280000);
        placeBid("Carla", 310000);

        mockMvc.perform(get("/api/auction/status"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.currentPrice").value(305000))
               .andExpect(jsonPath("$.leadingBidder").value("Carla"));
    }

    @Test
    void equalMaximumFromDifferentBidderIsRejectedWithoutChangingState() throws Exception {
        placeBid("Alice", 300000);

        mockMvc.perform(post("/api/auction/bids")
                       .contentType(APPLICATION_JSON)
                       .content("{\"bidderId\":\"Bob\",\"amount\":300000,\"requestId\":\"bob-300000\"}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.accepted").value(false))
               .andExpect(jsonPath("$.status.leadingBidder").value("Alice"))
               .andExpect(jsonPath("$.status.currentPrice").value(250000));

        mockMvc.perform(get("/api/auction/status"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.leadingBidder").value("Alice"))
               .andExpect(jsonPath("$.currentPrice").value(250000));
    }

    @Test
    void currentPriceCannotExceedLeadingMaximum() throws Exception {
        placeBid("Alice", 300000);
        placeBid("Bob", 310000);

        mockMvc.perform(get("/api/auction/status"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.leadingBidder").value("Bob"))
               .andExpect(jsonPath("$.currentPrice").value(305000));
    }

    private void placeBid(String bidderId, long amount) throws Exception {
        mockMvc.perform(post("/api/auction/bids")
                       .contentType(APPLICATION_JSON)
                       .content("{\"bidderId\":\"" + bidderId + "\",\"amount\":" + amount + ",\"requestId\":\"" + bidderId + amount + "\"}"));
    }
}
