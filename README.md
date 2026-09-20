# Combo Booking Saga - Hệ thống đặt vé Combo "Chuyến đi trọn gói"

## Tổng quan

Dự án mô phỏng hệ thống đặt vé combo (vé máy bay + phòng khách sạn) sử dụng **Saga Orchestration Pattern** để quản lý giao dịch phân tán.

## Cấu trúc dự án

```
combo-booking-saga/
├── src/main/java/com/combo/
│   ├── ComboBookingSagaApplication.java    # Main Spring Boot Application
│   ├── config/                             # Cấu hình
│   │   └── SagaConfig.java
│   ├── model/                              # Model classes
│   │   ├── ComboOrder.java
│   │   └ enum/
│   │       └── OrderStatus.java
│   ├── dto/                                # DTO classes
│   │   ├── FlightRequest.java
│   │   ├── FlightResponse.java
│   │   ├── HotelRequest.java
│   │   ├── HotelResponse.java
│   │   ├── PaymentRequest.java
│   │   ├── PaymentResponse.java
│   │   ├── ComboOrderRequest.java
│   │   └── ComboOrderResponse.java
│   ├── service/                            # Service classes
│   │   ├── FlightService.java
│   │   ├── HotelService.java
│   │   ├── PaymentService.java
│   │   └── OrderComboService.java          # Saga Orchestrator
│   ├── controller/                         # REST Controllers
│   │   ├── FlightController.java
│   │   ├── HotelController.java
│   │   ├── PaymentController.java
│   │   └── OrderComboController.java
│   └── saga/                               # Saga state management
│       └── SagaStateManager.java
├── pom.xml                                 # Maven configuration
├── DESIGN.md                               # Architecture design document
└── README.md
```

## Chạy project

```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run
```

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| POST | `/api/order/combo` | Đặt combo mới |
| POST | `/api/flight/book` | Đặt vé máy bay |
| POST | `/api/hotel/book` | Đặt phòng khách sạn |
| POST | `/api/payment/pay` | Xử lý thanh toán |
| POST | `/api/flight/cancel` | Hủy vé máy bay |
| POST | `/api/hotel/cancel` | Hủy phòng khách sạn |
| POST | `/api/payment/refund` | Hoàn tiền |
| GET | `/api/order/{orderId}` | Lấy trạng thái đơn hàng |

## Kịch bản test

### 1. Thành công cả hai (Happy Path)
```bash
curl -X POST http://localhost:8080/api/order/combo \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Nguyễn Văn A","flightNumber":"VN123","hotelName":"Sheraton","amount":5000000}'
```

### 2. Flight thành công, Hotel thất bại → Rollback Flight
```bash
# Gọi API hotel với forceFail=true
curl -X POST http://localhost:8080/api/hotel/book \
  -H "Content-Type: application/json" \
  -d '{"hotelName":"Full Hotel","forceFail":true}'
```

### 3. Payment thất bại → Rollback cả hai
```bash
# Gọi API payment với forceFail=true
curl -X POST http://localhost:8080/api/payment/pay \
  -H "Content-Type: application/json" \
  -d '{"orderId":"ORD-001","amount":5000000,"forceFail":true}'
```

### 4. Timeout khi gọi Hotel
```bash
# Gọi API hotel với timeout=true
curl -X POST http://localhost:8080/api/hotel/book \
  -H "Content-Type: application/json" \
  -d '{"hotelName":"Slow Hotel","timeout":true}'
```

## Công nghệ

- **Spring Boot 3.2** - REST API framework
- **Java 17** - Ngôn ngữ lập trình
- **Maven** - Dependency management
- **Lombok** - Reduce boilerplate code
- **Jackson** - JSON serialization

## Mô hình Saga

- **Pattern**: Orchestration
- **Orchestrator**: `OrderComboService`
- **Compensation**: Gọi ngược lại các service đã thành công
- **Timeout**: 5 giây cho mỗi call
- **Retry**: 2 lần với exponential backoff
# session14-b5
