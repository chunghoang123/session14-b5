package com.combo.saga;

import com.combo.model.OrderStatus;

public class SagaStateManager {

    private OrderStatus status;
    private String flightBookingId;
    private String hotelBookingId;
    private String paymentId;
    private String errorMessage;

    public SagaStateManager() {
        this.status = OrderStatus.CREATED;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getFlightBookingId() {
        return flightBookingId;
    }

    public void setFlightBookingId(String flightBookingId) {
        this.flightBookingId = flightBookingId;
    }

    public String getHotelBookingId() {
        return hotelBookingId;
    }

    public void setHotelBookingId(String hotelBookingId) {
        this.hotelBookingId = hotelBookingId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isCompleted() {
        return status == OrderStatus.COMPLETED;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public void markFlightBooked(String flightBookingId) {
        this.flightBookingId = flightBookingId;
        this.status = OrderStatus.FLIGHT_BOOKED;
    }

    public void markHotelBooked(String hotelBookingId) {
        this.hotelBookingId = hotelBookingId;
        this.status = OrderStatus.HOTEL_BOOKED;
    }

    public void markPaid(String paymentId) {
        this.paymentId = paymentId;
        this.status = OrderStatus.PAID;
    }

    public void markCompleted() {
        this.status = OrderStatus.COMPLETED;
    }

    public void markCancelling() {
        this.status = OrderStatus.CANCELLING;
    }

    public void markCancelled() {
        this.status = OrderStatus.CANCELLED;
    }
}
