package com.combo.controller;

import com.combo.dto.*;
import com.combo.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> pay(@RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.pay(request).join());
    }

    @PostMapping("/refund")
    public ResponseEntity<PaymentResponse> refund(@RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.refund("PAY-test").join());
    }
}
