package com.combo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComboOrder {
    private String orderId;
    private String customerName;
    private String flightNumber;
    private String hotelName;
    private double totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String flightBookingId;
    private String hotelBookingId;
    private String paymentId;
    private String errorMessage;
}
