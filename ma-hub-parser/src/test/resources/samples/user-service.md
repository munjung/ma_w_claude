# Service: user-service

## Description
사용자 계정 생성, 인증, 프로필 관리 서비스

## Domain Models

### User
- id: Long [PK, auto]
- username: String [required, unique, max=50]
- email: String [required, unique]
- passwordHash: String [required]
- createdAt: LocalDateTime [auto]

## APIs

### POST /users
- Summary: 신규 사용자 등록
- Auth: none
- Request: CreateUserRequest { username: String, email: String, password: String }
- Response: UserResponse { id: Long, username: String, email: String }
- Errors: 409

### GET /users/{id}
- Summary: 사용자 단건 조회
- Auth: jwt
- Response: UserResponse { id: Long, username: String, email: String }
- Errors: 404

### PUT /users/{id}
- Summary: 사용자 정보 수정
- Auth: jwt
- Request: UpdateUserRequest { username: String, email: String }
- Response: UserResponse { id: Long, username: String, email: String }
- Errors: 404

## Dependencies

## Events

### Published
- user.created: { userId: Long, email: String, createdAt: String }

### Consumed
- (none)

## Communication
- Outbound: rest
- DB: postgresql

## Config
- port: 8081
- context-path: /user-service
