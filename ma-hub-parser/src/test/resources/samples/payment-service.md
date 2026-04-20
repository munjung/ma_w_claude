# Service: payment-service

## Description
결제 처리, 환불, 결제 이력 관리 서비스

## Domain Models

### Payment
- id: Long [PK, auto]
- orderId: Long [required]
- amount: BigDecimal [required]
- method: String [required]
- status: String [required]
- paidAt: LocalDateTime

## APIs

### POST /payments
- Summary: 결제 요청
- Auth: jwt
- Request: PaymentRequest { orderId: Long, amount: BigDecimal, method: String }
- Response: PaymentResponse { id: Long, status: String, paidAt: LocalDateTime }
- Errors: 400, 404, 409

### POST /payments/{id}/refund
- Summary: 결제 환불
- Auth: jwt
- Response: PaymentResponse { id: Long, status: String }
- Errors: 404, 409

### GET /payments/{id}
- Summary: 결제 단건 조회
- Auth: jwt
- Response: PaymentResponse { id: Long, amount: BigDecimal, status: String }
- Errors: 404

## Dependencies

### order-service
- GET /orders/{id}: 주문 정보 및 금액 확인

## Events

### Published
- payment.completed: { paymentId: Long, orderId: Long, amount: BigDecimal }
- payment.refunded: { paymentId: Long, orderId: Long }

### Consumed
- (none)

## Communication
- Outbound: rest+kafka
- DB: postgresql

## Config
- port: 8084
- context-path: /payment-service
