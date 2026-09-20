package com.combo.controller;

import com.combo.dto.ComboOrderRequest;
import com.combo.dto.ComboOrderResponse;
import com.combo.service.OrderComboService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SagaSimulationRunner implements CommandLineRunner {

    private final OrderComboService orderComboService;

    public SagaSimulationRunner(OrderComboService orderComboService) {
        this.orderComboService = orderComboService;
    }

    @Override
    public void run(String... args) {
        System.out.println("=".repeat(80));
        System.out.println("  SAGA ORCHESTRATION PATTERN - COMBO BOOKING SIMULATION");
        System.out.println("=".repeat(80));
        System.out.println();

        // Scenario 1: Happy Path - All Success
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  SCENARIO 1: HAPPY PATH - Flight + Hotel + Payment all succeed          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");
        ComboOrderRequest req1 = new ComboOrderRequest("Nguyen Van A", "VN123", "Sheraton", 5000000, false, false, false, false, false);
        ComboOrderResponse res1 = orderComboService.processCombo(req1);
        printResult(res1);
        System.out.println();

        // Scenario 2: Flight Success, Hotel Failed → Rollback Flight
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  SCENARIO 2: HOTEL FAILED → Rollback Flight                             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");
        ComboOrderRequest req2 = new ComboOrderRequest("Nguyen Van B", "VN456", "Full Hotel", 5000000, false, true, false, false, false);
        ComboOrderResponse res2 = orderComboService.processCombo(req2);
        printResult(res2);
        System.out.println();

        // Scenario 3: Payment Failed → Rollback Flight + Hotel
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  SCENARIO 3: PAYMENT FAILED → Rollback Flight + Hotel                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");
        ComboOrderRequest req3 = new ComboOrderRequest("Nguyen Van C", "VN789", "Sofitel", 5000000, false, false, false, true, false);
        ComboOrderResponse res3 = orderComboService.processCombo(req3);
        printResult(res3);
        System.out.println();

        // Scenario 4: Hotel Timeout → Rollback Flight
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  SCENARIO 4: HOTEL TIMEOUT → Rollback Flight                            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");
        ComboOrderRequest req4 = new ComboOrderRequest("Nguyen Van D", "VN101", "Slow Hotel", 5000000, false, false, true, false, false);
        ComboOrderResponse res4 = orderComboService.processCombo(req4);
        printResult(res4);
        System.out.println();

        System.out.println("=".repeat(80));
        System.out.println("  ALL SCENARIOS COMPLETED!");
        System.out.println("=".repeat(80));
        System.exit(0);
    }

    private void printResult(ComboOrderResponse res) {
        System.out.println("  Order ID    : " + res.getOrderId());
        System.out.println("  Status      : " + res.getStatus());
        System.out.println("  Message     : " + res.getMessage());
        System.out.println("  Flight ID   : " + res.getFlightBookingId());
        System.out.println("  Hotel ID    : " + res.getHotelBookingId());
        System.out.println("  Payment ID  : " + res.getPaymentId());
        if (res.getErrorMessage() != null && !res.getErrorMessage().isEmpty()) {
            System.out.println("  Error       : " + res.getErrorMessage());
        }
        System.out.println("-".repeat(80));
    }
}
