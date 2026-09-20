# Hồ sơ thiết kế kiến trúc: Hệ thống đặt vé Combo "Chuyến đi trọn gói"

## 1. Phân tích vấn đề

### 1.1 Các dịch vụ tham gia

| Dịch vụ | Đối tác | API | Chức năng |
|---------|---------|-----|-----------|
| Flight Service | Đối tác A | REST API bên ngoài | Đặt vé máy bay |
| Hotel Service | Đối tác B | REST API bên ngoài | Đặt phòng khách sạn |
| Payment Service | Nội bộ | REST API nội bộ | Xử lý thanh toán |
| Order Combo Service (Orchestrator) | Nội bộ | REST API | Điều phối giao dịch combo |

### 1.2 Các bước trong luồng đặt combo

```
Bước 1: Khách hàng gửi yêu cầu đặt combo (flight + hotel + payment)
Bước 2: Order Combo Service (Orchestrator) nhận yêu cầu
Bước 3: Gọi Flight Service → Đặt vé máy bay
Bước 4: Gọi Hotel Service → Đặt phòng khách sạn
Bước 5: Gọi Payment Service → Xử lý thanh toán
Bước 6: Hoàn tất combo
```

### 1.3 Các điểm có thể thất bại

| Bước | Lỗi có thể xảy ra | Hậu quả |
|------|---------------------|---------|
| Bước 3 | Flight Service trả về lỗi hoặc timeout | Không đặt được vé |
| Bước 4 | Hotel Service trả về lỗi hoặc timeout | Không đặt được phòng |
| Bước 5 | Payment Service trả về lỗi hoặc timeout | Không thanh toán được |
| Bất kỳ | Mạng bị đứt, phản hồi chậm | Giao dịch treo |

## 2. Lựa chọn mô hình Saga: **Orchestration**

### 2.1 Lý do chọn Orchestration thay vì Choreography

| Tiêu chí | Orchestration | Choreography |
|-----------|--------------|--------------|
| Độ phức tạp mã nguồn | Trung bình (có 1 orchestrator) | Cao (mỗi service biết các service khác) |
| Kiểm soát luồng | Tập trung, dễ debug | Phân tán, khó debug |
| Xử lý lỗi tập trung | Có (tại orchestrator) | Mỗi service tự xử lý |
| Phù hợp với bài tập | ✅ Có 4 service, cần rollback có trật tự | ❌ Khó quản lý với nhiều service |
| Đối tác khác nhau | ✅ Orchestrator gọi API bên ngoài | ✅ nhưng khó điều phối |

**Kết luận:** Chọn **Orchestration** vì:
- Có nhiều service (4) thuộc các đối tác khác nhau
- Cần rollback có trật tự (ngược lại với forward)
- Cần tập trung giám sát và logging
- Dễ hình ảnh hóa luồng giao dịch trong sơ đồ kiến trúc

### 2.2 Sơ đồ kiến trúc Orchestration

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENT (Khách hàng)                         │
│                     POST /api/order/combo                     │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              ORDER COMBO SERVICE (Orchestrator)               │
│  - Nhận request, tạo order saga                             │
│  - Điều phối các bước: Flight → Hotel → Payment             │
│  - Quản lý state: PENDING → FLIGHT_BOOKED → HOTEL_BOOKED →  │
│    PAID → COMPLETED / COMPENSATING → CANCELLED              │
│  - Gọi compensate khi có lỗi                                │
└───┬──────────────┬──────────────┬────────────────────────────┘
    │              │              │
    ▼              ▼              ▼
┌────────┐  ┌──────────┐  ┌────────────┐
│ FLIGHT │  │  HOTEL   │  │ PAYMENT    │
│ Service│  │ Service  │  │ Service    │
│ (Đối   │  │ (Đối tác │  │ (Nội bộ)   │
│ tác A) │  │ B)       │  │            │
└────────┘  └──────────┘  └────────────┘
```

## 3. Cơ chế bù trừ (Compensation)

### 3.1 Luồng forward (thành công)

```
Order Created → Call Flight (Book) → Call Hotel (Book) → Call Payment (Pay) → Completed
```

### 3.2 Luồng backward (bù trừ)

```
Lỗi tại bước N → Gọi compensate cho bước N-1, N-2, ..., 1
```

| Bước bị lỗi | Compensate |
|-------------|-----------|
| Flight thất bại | Không cần (chưa booking gì) |
| Hotel thất bại | Cancel Flight (refund vé) |
| Payment thất bại | Cancel Flight + Cancel Hotel |
| Hotel timeout | Cancel Flight + Mark cancelled |

### 3.3 Sơ đồ bù trừ

```
Giai đoạn thành công:   [Flight: Book] → [Hotel: Book] → [Payment: Pay] → [COMPLETED]
Giai đoạn bù trừ:                            ← [Hotel: Cancel] ← [Flight: Cancel] ← [CANCELLED]
                                     ← [Payment: Refund] ←
```

## 4. Xử lý timeout, retry, và compensate

### 4.1 Timeout

- Mỗi lần gọi service bên ngoài có **timeout = 5 giây**
- Nếu không nhận phản hồi trong 5s → coi là thất bại → kích hoạt rollback
- Sử dụng `CompletableFuture.get(timeout, TimeUnit.SECONDS)` trong Java

### 4.2 Retry

- Số lần retry tối đa: **2 lần**
- Backoff strategy: **exponential backoff** (1s, 2s)
- Retry áp dụng cho các lỗi mạng, 5xx server errors
- **KHÔNG retry** cho lỗi 4xx (client error - lỗi nghiệp vụ)

### 4.3 Fallback & Circuit Breaker

- Nếu service liên tục thất bại → Circuit Breaker mở → chuyển sang trạng thái `UNAVAILABLE`
- Khi circuit breaker mở → trả về lỗi ngay lập tức, không chờ timeout

### 4.4 Bảng xử lý timeout

```
┌────────────────┬──────────────┬───────────────────────┐
│ Tình huống      │ Timeout      │ Xử lý                 │
├────────────────┼──────────────┼───────────────────────┤
│ Flight chậm     │ 5s           │ Retry 2x → Nếu vẫn    │
│                │              │ thất bại → cancel     │
│                │              │ toàn bộ               │
├────────────────┼──────────────┼───────────────────────┤
│ Hotel chậm      │ 5s           │ Retry 2x → Nếu vẫn    │
│                │              │ thất bại → cancel     │
│                │              │ Flight + Payment      │
├────────────────┼──────────────┼───────────────────────┤
│ Payment chậm    │ 5s           │ Retry 2x → Nếu vẫn    │
│                │              │ thất bại → cancel     │
│                │              │ Flight + Hotel        │
└────────────────┴──────────────┴───────────────────────┘
```

## 5. State Machine của Saga

```
                    ┌──────────────┐
                    │   CREATED    │
                    └──────┬───────┘
                           │ Call Flight
                           ▼
                  ┌────────────────┐
                  │ FLIGHT_BOOKED  │
                  └──────┬─────────┘
                         │ Call Hotel
                         ▼
                ┌─────────────────┐
                │ HOTEL_BOOKED    │
                └──────┬──────────┘
                       │ Call Payment
                       ▼
              ┌──────────────────┐
              │ PAYMENT_PAID     │
              └──────┬───────────┘
                     │
                     ▼
              ┌──────────────┐
              │  COMPLETED   │
              └──────────────┘

── Các trạng thái bù trừ (rollback) ──

FLIGHT_BOOKED → Lỗi Hotel → CANCELLING → CANCELLED (cancel flight)
HOTEL_BOOKED → Lỗi Payment → CANCELLING → CANCELLED (cancel hotel + flight)
PAYMENT_PAID → Lỗi hệ thống → CANCELLING → CANCELLED (refund payment + cancel hotel + flight)
```

## 6. Dữ liệu mô hình

### 6.1 ComboOrder

```json
{
  "orderId": "ORD-2025-001",
  "customerName": "Nguyễn Văn A",
  "flightNumber": "VN123",
  "hotelName": "Sheraton Hotel",
  "totalAmount": 5000000,
  "status": "PENDING|FLIGHT_BOOKED|HOTEL_BOOKED|PAID|COMPLETED|CANCELLED",
  "createdAt": "2025-01-15T10:00:00",
  "updatedAt": "2025-01-15T10:05:00"
}
```

## 7. Kết luận

Hệ thống sử dụng **Saga Orchestration pattern** để quản lý giao dịch phân tán giữa các dịch vụ của đối tác khác nhau. Orchestrator đóng vai trò trung tâm điều phối, đảm bảo tính nhất quán cuối cùng (eventual consistency) thông qua cơ chế bù trừ có trật tự. Timeout, retry, và circuit breaker được áp dụng để đảm bảo hệ thống hoạt động ổn định ngay cả khi có service phản hồi chậm hoặc thất bại.
