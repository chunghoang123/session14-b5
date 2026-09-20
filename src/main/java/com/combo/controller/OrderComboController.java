package com.combo.controller;

import com.combo.dto.ComboOrderRequest;
import com.combo.dto.ComboOrderResponse;
import com.combo.service.OrderComboService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
public class OrderComboController {

    private final OrderComboService orderComboService;

    public OrderComboController(OrderComboService orderComboService) {
        this.orderComboService = orderComboService;
    }

    @PostMapping("/combo")
    public ResponseEntity<ComboOrderResponse> placeComboOrder(@RequestBody ComboOrderRequest request) {
        ComboOrderResponse response = orderComboService.processCombo(request);
        return ResponseEntity.ok(response);
    }
}
