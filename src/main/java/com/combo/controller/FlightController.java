package com.combo.controller;

import com.combo.dto.*;
import com.combo.service.FlightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flight")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @PostMapping("/book")
    public ResponseEntity<FlightResponse> bookFlight(@RequestBody FlightRequest request) {
        return ResponseEntity.ok(flightService.bookFlight(request).join());
    }

    @PostMapping("/cancel")
    public ResponseEntity<FlightResponse> cancelFlight(@RequestBody FlightRequest request) {
        return ResponseEntity.ok(flightService.cancelFlight("FLT-test").join());
    }
}
