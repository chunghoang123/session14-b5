package com.combo.service;

import com.combo.dto.*;
import com.combo.model.OrderStatus;
import com.combo.saga.SagaStateManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class OrderComboService {

    private static final long TIMEOUT_SECONDS = 5;
    private static final int MAX_RETRIES = 2;

    private final FlightService flightService;
    private final HotelService hotelService;
    private final PaymentService paymentService;

    public OrderComboService(FlightService flightService, HotelService hotelService, PaymentService paymentService) {
        this.flightService = flightService;
        this.hotelService = hotelService;
        this.paymentService = paymentService;
    }

    public ComboOrderResponse processCombo(ComboOrderRequest request) {
        SagaStateManager saga = new SagaStateManager();
        String orderId = "ORD-" + System.currentTimeMillis();

        try {
            // STEP 1: Book Flight
            boolean flightForceFail = request.isFlightForceFail();
            boolean flightTimeout = request.isFlightTimeout();
            FlightRequest flightReq = new FlightRequest(request.getFlightNumber(), request.getCustomerName(), flightForceFail, flightTimeout);
            FlightResponse flightResp;
            try {
                flightResp = callWithRetry(
                    () -> flightService.bookFlight(flightReq),
                    "Flight", MAX_RETRIES);
            } catch (TimeoutException e) {
                saga.markCancelled();
                return buildResponse(orderId, saga, "Flight booking timed out after retries.");
            }

            if (!flightResp.isSuccess()) {
                saga.markCancelled();
                return buildResponse(orderId, saga, "Flight booking failed: " + flightResp.getMessage());
            }
            saga.markFlightBooked(flightResp.getFlightBookingId());

            // STEP 2: Book Hotel
            boolean hotelForceFail = request.isHotelForceFail();
            boolean hotelTimeout = request.isHotelTimeout();
            HotelRequest hotelReq = new HotelRequest(request.getHotelName(), request.getCustomerName(), hotelForceFail, hotelTimeout);
            HotelResponse hotelResp;
            try {
                hotelResp = callWithTimeout(
                    () -> hotelService.bookHotel(hotelReq),
                    "Hotel", TIMEOUT_SECONDS);
            } catch (TimeoutException e) {
                saga.markCancelling();
                compensateFlight(saga);
                saga.markCancelled();
                return buildResponse(orderId, saga, "Hotel booking timed out. Flight cancelled (refunded).");
            }

            if (!hotelResp.isSuccess()) {
                saga.markCancelling();
                compensateFlight(saga);
                saga.markCancelled();
                return buildResponse(orderId, saga, "Hotel booking failed: " + hotelResp.getMessage() + ". Flight cancelled (refunded).");
            }
            saga.markHotelBooked(hotelResp.getHotelBookingId());

            // STEP 3: Process Payment
            boolean paymentForceFail = request.isPaymentForceFail();
            boolean paymentTimeout = request.isPaymentTimeout();
            PaymentRequest paymentReq = new PaymentRequest(orderId, request.getAmount(), paymentForceFail, paymentTimeout);
            PaymentResponse paymentResp;
            try {
                paymentResp = callWithTimeout(
                    () -> paymentService.pay(paymentReq),
                    "Payment", TIMEOUT_SECONDS);
            } catch (TimeoutException e) {
                saga.markCancelling();
                compensateHotel(saga);
                compensateFlight(saga);
                saga.markCancelled();
                return buildResponse(orderId, saga, "Payment timed out. Flight and Hotel cancelled (refunded).");
            }

            if (!paymentResp.isSuccess()) {
                saga.markCancelling();
                compensateHotel(saga);
                compensateFlight(saga);
                saga.markCancelled();
                return buildResponse(orderId, saga, "Payment failed: " + paymentResp.getMessage() + ". Flight and Hotel cancelled (refunded).");
            }
            saga.markPaid(paymentResp.getPaymentId());
            saga.markCompleted();

            return buildSuccessResponse(orderId, saga);

        } catch (Exception e) {
            saga.markCancelling();
            saga.markCancelled();
            return buildResponse(orderId, saga, "Unexpected error: " + e.getMessage());
        }
    }

    private <T> T callWithRetry(java.util.function.Supplier<CompletableFuture<T>> supplier, String serviceName, int maxRetries) throws Exception {
        int attempts = 0;
        while (attempts <= maxRetries) {
            try {
                CompletableFuture<T> future = supplier.get();
                return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                attempts++;
                if (attempts > maxRetries) {
                    throw new TimeoutException(serviceName + " timed out after " + maxRetries + " retries");
                }
                try {
                    Thread.sleep((long) Math.pow(2, attempts) * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new Exception("Interrupted during retry");
                }
            } catch (Exception e) {
                throw e;
            }
        }
        throw new Exception(serviceName + " failed after " + maxRetries + " retries");
    }

    private <T> T callWithTimeout(java.util.function.Supplier<CompletableFuture<T>> supplier, String serviceName, long timeoutSeconds) throws Exception {
        CompletableFuture<T> future = supplier.get();
        return future.get(timeoutSeconds, TimeUnit.SECONDS);
    }

    private void compensateFlight(SagaStateManager saga) {
        if (saga.getFlightBookingId() != null) {
            try {
                FlightResponse cancelResp = flightService.cancelFlight(saga.getFlightBookingId()).get(5, TimeUnit.SECONDS);
                if (cancelResp.isSuccess()) {
                    String msg = saga.getErrorMessage() != null ? saga.getErrorMessage() + "; " : "";
                    saga.setErrorMessage(msg + cancelResp.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Flight compensation failed: " + e.getMessage());
            }
        }
    }

    private void compensateHotel(SagaStateManager saga) {
        if (saga.getHotelBookingId() != null) {
            try {
                HotelResponse cancelResp = hotelService.cancelHotel(saga.getHotelBookingId()).get(5, TimeUnit.SECONDS);
                if (cancelResp.isSuccess()) {
                    String msg = saga.getErrorMessage() != null ? saga.getErrorMessage() + "; " : "";
                    saga.setErrorMessage(msg + cancelResp.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Hotel compensation failed: " + e.getMessage());
            }
        }
    }

    private void compensatePayment(SagaStateManager saga) {
        if (saga.getPaymentId() != null) {
            try {
                PaymentResponse refundResp = paymentService.refund(saga.getPaymentId()).get(5, TimeUnit.SECONDS);
                if (refundResp.isSuccess()) {
                    String msg = saga.getErrorMessage() != null ? saga.getErrorMessage() + "; " : "";
                    saga.setErrorMessage(msg + refundResp.getMessage());
                }
            } catch (Exception e) {
                System.err.println("Payment compensation failed: " + e.getMessage());
            }
        }
    }

    private ComboOrderResponse buildResponse(String orderId, SagaStateManager saga, String error) {
        return new ComboOrderResponse(
            orderId, saga.getStatus().name(), error,
            saga.getFlightBookingId(), saga.getHotelBookingId(), saga.getPaymentId(), error);
    }

    private ComboOrderResponse buildSuccessResponse(String orderId, SagaStateManager saga) {
        return new ComboOrderResponse(
            orderId, saga.getStatus().name(), "Combo booking completed successfully!",
            saga.getFlightBookingId(), saga.getHotelBookingId(), saga.getPaymentId(), null);
    }
}
