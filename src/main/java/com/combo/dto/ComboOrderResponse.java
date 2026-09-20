package com.combo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComboOrderResponse {
    private String orderId;
    private String status;
    private String message;
    private String flightBookingId;
    private String hotelBookingId;
    private String paymentId;
    private String errorMessage;
}
