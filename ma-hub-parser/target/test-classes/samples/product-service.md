# Service: product-service

## Description
상품 등록, 조회, 재고 관리 서비스

## Domain Models

### Product
- id: Long [PK, auto]
- name: String [required, max=200]
- price: BigDecimal [required]
- stock: Integer [required, min=0]
- createdAt: LocalDateTime [auto]

## APIs

### POST /products
- Summary: 상품 등록
- Auth: api-key
- Request: CreateProductRequest { name: String, price: BigDecimal, stock: Integer }
- Response: ProductResponse { id: Long, name: String, price: BigDecimal, stock: Integer }
- Errors: 400

### GET /products/{id}
- Summary: 상품 단건 조회
- Auth: none
- Response: ProductResponse { id: Long, name: String, price: BigDecimal, stock: Integer }
- Errors: 404

### PUT /products/{id}/stock
- Summary: 재고 수량 변경
- Auth: api-key
- Request: StockUpdateRequest { quantity: Integer }
- Response: ProductResponse { id: Long, stock: Integer }
- Errors: 404, 400

## Dependencies

## Events

### Published
- product.stock.updated: { productId: Long, newStock: Integer }

### Consumed
- order.created: 주문 생성 시 재고 차감

## Communication
- Outbound: rest
- DB: postgresql

## Config
- port: 8083
- context-path: /product-service
