package com.combo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlightRequest {
    private String flightNumber;
    private String customerName;
    private boolean forceFail;
    private boolean timeout;
}
