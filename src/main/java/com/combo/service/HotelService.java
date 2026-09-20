package com.combo.service;

import com.combo.dto.HotelRequest;
import com.combo.dto.HotelResponse;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class HotelService {

    private static final Random random = new Random();

    public CompletableFuture<HotelResponse> bookHotel(HotelRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (request.isTimeout()) {
                    Thread.sleep(15000);
                }
                if (request.isForceFail()) {
                    return new HotelResponse(false, null, "Hotel booking failed: Hotel is fully booked");
                }
                Thread.sleep(500 + random.nextInt(500));
                String bookingId = "HTL-" + System.currentTimeMillis();
                return new HotelResponse(true, bookingId, "Hotel " + request.getHotelName() + " booked successfully");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new HotelResponse(false, null, "Hotel booking interrupted");
            }
        });
    }

    public CompletableFuture<HotelResponse> cancelHotel(String hotelBookingId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(300);
                return new HotelResponse(true, null, "Hotel " + hotelBookingId + " cancelled (refunded)");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new HotelResponse(false, null, "Cancel hotel failed");
            }
        });
    }
}
