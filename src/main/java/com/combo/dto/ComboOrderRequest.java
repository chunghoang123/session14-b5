package com.combo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComboOrderRequest {
    private String customerName;
    private String flightNumber;
    private String hotelName;
    private double amount;
    private boolean flightForceFail;
    private boolean hotelForceFail;
    private boolean hotelTimeout;
    private boolean paymentForceFail;
    private boolean paymentTimeout;
}
