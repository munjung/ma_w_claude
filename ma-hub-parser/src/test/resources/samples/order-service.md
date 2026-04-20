# Service: order-service

## Description
주문 생성, 조회, 상태 관리 서비스

## Domain Models

### Order
- id: Long [PK, auto]
- userId: Long [required]
- status: String [required]
- totalAmount: BigDecimal [required]
- createdAt: LocalDateTime [auto]

### OrderItem
- id: Long [PK, auto]
- orderId: Long [required]
- productId: Long [required]
- quantity: Integer [required]
- unitPrice: BigDecimal [required]

## APIs

### POST /orders
- Summary: 주문 생성
- Auth: jwt
- Request: CreateOrderRequest { userId: Long, items: List }
- Response: OrderResponse { id: Long, status: String, totalAmount: BigDecimal }
- Errors: 400, 404

### GET /orders/{id}
- Summary: 주문 단건 조회
- Auth: jwt
- Response: OrderResponse { id: Long, status: String, totalAmount: BigDecimal }
- Errors: 404

### GET /orders
- Summary: 사용자 주문 목록 조회
- Auth: jwt
- Response: OrderListResponse { orders: List }

### PATCH /orders/{id}/cancel
- Summary: 주문 취소
- Auth: jwt
- Response: OrderResponse { id: Long, status: String }
- Errors: 404, 409

## Dependencies

### user-service
- GET /users/{id}: 주문자 존재 여부 및 정보 조회

### product-service
- GET /products/{id}: 상품 재고 및 가격 조회

## Events

### Published
- order.created: { orderId: Long, userId: Long, totalAmount: BigDecimal }
- order.cancelled: { orderId: Long, userId: Long }

### Consumed
- payment.completed: 결제 완료 시 주문 상태 변경

## Communication
- Outbound: rest+kafka
- DB: postgresql

## Config
- port: 8082
- context-path: /order-service
