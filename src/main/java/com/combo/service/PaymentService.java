package com.combo.service;

import com.combo.dto.PaymentRequest;
import com.combo.dto.PaymentResponse;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentService {

    private static final Random random = new Random();

    public CompletableFuture<PaymentResponse> pay(PaymentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (request.isTimeout()) {
                    Thread.sleep(15000);
                }
                if (request.isForceFail()) {
                    return new PaymentResponse(false, null, "Payment failed: Insufficient funds");
                }
                Thread.sleep(300 + random.nextInt(300));
                String paymentId = "PAY-" + System.currentTimeMillis();
                return new PaymentResponse(true, paymentId, "Payment of $" + request.getAmount() + " processed successfully");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new PaymentResponse(false, null, "Payment interrupted");
            }
        });
    }

    public CompletableFuture<PaymentResponse> refund(String paymentId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(300);
                return new PaymentResponse(true, null, "Payment " + paymentId + " refunded successfully");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new PaymentResponse(false, null, "Refund failed");
            }
        });
    }
}
