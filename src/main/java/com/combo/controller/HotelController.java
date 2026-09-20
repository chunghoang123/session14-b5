package com.combo.controller;

import com.combo.dto.*;
import com.combo.service.HotelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hotel")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping("/book")
    public ResponseEntity<HotelResponse> bookHotel(@RequestBody HotelRequest request) {
        return ResponseEntity.ok(hotelService.bookHotel(request).join());
    }

    @PostMapping("/cancel")
    public ResponseEntity<HotelResponse> cancelHotel(@RequestBody HotelRequest request) {
        return ResponseEntity.ok(hotelService.cancelHotel("HTL-test").join());
    }
}
