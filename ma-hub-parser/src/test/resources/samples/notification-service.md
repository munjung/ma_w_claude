# Service: notification-service

## Description
이메일, SMS, 푸시 알림 발송 서비스

## Domain Models

### Notification
- id: Long [PK, auto]
- userId: Long [required]
- type: String [required]
- message: String [required]
- sentAt: LocalDateTime [auto]
- status: String [required]

## APIs

### POST /notifications
- Summary: 알림 직접 발송
- Auth: api-key
- Request: NotificationRequest { userId: Long, type: String, message: String }
- Response: NotificationResponse { id: Long, status: String }
- Errors: 400

### GET /notifications/{userId}
- Summary: 사용자 알림 목록 조회
- Auth: jwt
- Response: NotificationListResponse { notifications: List }

## Dependencies

### user-service
- GET /users/{id}: 수신자 이메일/정보 조회

## Events

### Published
- (none)

### Consumed
- user.created: 가입 환영 이메일 발송
- order.created: 주문 확인 알림 발송
- payment.completed: 결제 완료 알림 발송
- order.cancelled: 주문 취소 알림 발송

## Communication
- Outbound: kafka
- DB: postgresql

## Config
- port: 8085
- context-path: /notification-service
