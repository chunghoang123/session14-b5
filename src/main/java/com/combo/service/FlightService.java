package com.combo.service;

import com.combo.dto.FlightRequest;
import com.combo.dto.FlightResponse;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class FlightService {

    private static final Random random = new Random();

    public CompletableFuture<FlightResponse> bookFlight(FlightRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (request.isTimeout()) {
                    Thread.sleep(15000);
                }
                if (request.isForceFail()) {
                    return new FlightResponse(false, null, "Flight booking failed: No seats available");
                }
                Thread.sleep(500 + random.nextInt(500));
                String bookingId = "FLT-" + System.currentTimeMillis();
                return new FlightResponse(true, bookingId, "Flight " + request.getFlightNumber() + " booked successfully");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new FlightResponse(false, null, "Flight booking interrupted");
            }
        });
    }

    public CompletableFuture<FlightResponse> cancelFlight(String flightBookingId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(300);
                return new FlightResponse(true, null, "Flight " + flightBookingId + " cancelled (refunded)");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new FlightResponse(false, null, "Cancel flight failed");
            }
        });
    }
}
