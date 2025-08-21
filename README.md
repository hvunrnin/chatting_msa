# 프로젝트 아키텍처 개요

## 채팅 메시지 플로우 (Outbox 기반 시퀀스)

```mermaid
sequenceDiagram
    participant User as 사용자
    participant N as Nginx
    participant P as chat-producer
    participant PO as Producer Outbox
    participant PP as Outbox Publisher
    participant K as Kafka
    participant C as chat-consumer
    participant CO as Consumer Outbox
    participant CP as Consumer Outbox Publisher
    participant MDB as MongoDB

    User->>N: WebSocket 연결/메시지 전송
    N->>P: WS 프록시
    P->>PO: 트랜잭션 내 도메인 검증/처리 + Outbox INSERT
    PP->>PO: PENDING 이벤트 폴링
    PP->>K: chat-messages publish (key = eventId)
    Note over PP: 성공 시 Outbox= SENT

    K->>C: chat-messages consume
    C->>MDB: 메시지 저장 (roomId 업데이트/삽입)
    C->>CO: Outbox INSERT (send-msg ack)
    CP->>CO: PENDING ack 폴링
    CP->>K: send-msg publish (ack)
    Note over CP: 성공 시 Outbox= SENT

    K->>P: send-msg consume
    P->>User: WS로 전송 완료 알림
```

## 병합 이벤트 플로우

```mermaid
flowchart TD
    A["병합 트랜잭션<br/>(상태/검증/DB 반영)"] --> B["Producer Outbox INSERT<br/>(eventType=MERGE_*)"]
    B --> C[Outbox Publisher]
    C --> D[Kafka topic: merge-events]
    D --> E["Producer 내부 Consumer<br/>(or 별도 서비스)"]
    E --> F["단계별 처리<br/>ROOMS_LOCKED → MSGS → USERS"]
    F --> G["추가 Outbox INSERT<br/>다음 단계 이벤트"]
    G --> C
```

## Outbox 설계 메모
- Outbox 키: `eventId(UUID)`를 Kafka key로 사용해 멱등 보장
- 컬럼 예시: id, aggregateType, aggregateId, eventType, payload(JSON), status(PENDING|SENT|FAILED), createdAt, sentAt, version
- 퍼블리셔: 페이지네이션(예: 1000개) + 백오프 재시도
- 트랜잭션 경계: 도메인 변경과 Outbox INSERT는 동일 트랜잭션
- Consumer 측 ack 또한 Outbox로 구성(선택)하여 재시도/멱등 강화 

<br>
<br>

# 채팅방 병합 Saga 패턴 이벤트 흐름

## 전체 병합 프로세스 흐름

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant Controller as RoomController
    participant Service as ChatRoomMergeService
    participant Producer as MergeEventProducer
    participant Kafka as Kafka Topic
    participant Consumer as MergeEventConsumer
    participant UserSvc as UserMigrationService
    participant MsgSvc as MessageMigrationService
    participant DB as MySQL/MongoDB

    Client->>Controller: POST /api/rooms/merge
    Controller->>Service: initiateMerge(mergeRequest)
    Service->>DB: MergeStatus 생성 (INITIATED)
    Service->>Producer: publishMergeInitiatedEvent()
    Producer->>Kafka: MERGE_INITIATED 이벤트 발행
    
    Note over Kafka: 이벤트 소비 시작
    Kafka->>Consumer: consumeMergeEvent(MERGE_INITIATED)
    Consumer->>Service: handleRoomsLocked()
    Service->>DB: 소스/타겟 방 상태를 MERGING으로 변경
    Service->>DB: MergeStatus 업데이트 (ROOMS_LOCKED)
    Service->>Producer: publishRoomsLockedEvent()
    Producer->>Kafka: ROOMS_LOCKED 이벤트 발행
    
    Kafka->>Consumer: consumeMergeEvent(ROOMS_LOCKED)
    Consumer->>Service: handleMessagesMigrate()
    Service->>MsgSvc: migrateMessages()
    MsgSvc->>DB: MessageMigrationLog 기록
    MsgSvc->>DB: MongoDB 메시지 roomId 업데이트
    Service->>DB: MergeStatus 업데이트 (MESSAGES_MIGRATED)
    Service->>Producer: publishMessagesMigratedEvent()
    Producer->>Kafka: MESSAGES_MIGRATED 이벤트 발행
    
    Kafka->>Consumer: consumeMergeEvent(MESSAGES_MIGRATED)
    Consumer->>Service: handleUsersMigrate()
    Service->>UserSvc: migrateUsers()
    UserSvc->>DB: UserMigrationLog 기록
    UserSvc->>DB: RoomUser 테이블 업데이트
    Service->>DB: MergeStatus 업데이트 (USERS_MIGRATED)
    Service->>Producer: publishUsersMigratedEvent()
    Producer->>Kafka: USERS_MIGRATED 이벤트 발행
    
    Kafka->>Consumer: consumeMergeEvent(USERS_MIGRATED)
    Consumer->>Service: handleMergeCompleted()
    Service->>DB: 소스 방을 ARCHIVED로 변경
    Service->>DB: MergeStatus 업데이트 (COMPLETED)
    Service->>Producer: publishMergeCompletedEvent()
    Producer->>Kafka: MERGE_COMPLETED 이벤트 발행
    
    Note over Service: 병합 완료!
```

## 롤백 처리 흐름

```mermaid
sequenceDiagram
    participant Service as ChatRoomMergeService
    participant UserSvc as UserMigrationService
    participant MsgSvc as MessageMigrationService
    participant DB as MySQL/MongoDB

    Note over Service: 병합 실패 시 롤백 시작
    
    alt USERS_MIGRATED 단계에서 실패
        Service->>Service: performRollback(USERS_MIGRATED)
        Service->>Service: rollbackUserMigration()
        Service->>DB: UserMigrationLog 페이징 조회
        loop 각 사용자별 롤백
            Service->>DB: wasMemberInTo 확인
            alt 새로 추가된 사용자
                Service->>DB: 타겟 방에서 사용자 제거
            else 기존 사용자
                Service->>DB: 역할을 이전 상태로 복원
            end
            alt 소스 방에 있었던 사용자
                Service->>DB: 소스 방에 사용자 복원
            end
            Service->>DB: UserMigrationLog를 ROLLED_BACK로 변경
        end
        Service->>Service: rollbackMessageMigration()
        Service->>DB: MessageMigrationLog 페이징 조회
        loop 각 메시지별 롤백
            Service->>DB: wasInTarget 확인
            alt 실제 이동된 메시지
                Service->>DB: roomId를 sourceRoomId로 되돌림
            end
            Service->>DB: MessageMigrationLog를 ROLLED_BACK로 변경
        end
        Service->>Service: unlockRooms()
        Service->>DB: 소스/타겟 방을 ACTIVE로 변경
    end
    
    alt MESSAGES_MIGRATED 단계에서 실패
        Service->>Service: performRollback(MESSAGES_MIGRATED)
        Service->>Service: rollbackMessageMigration()
        Service->>Service: unlockRooms()
    end
    
    alt ROOMS_LOCKED 단계에서 실패
        Service->>Service: performRollback(ROOMS_LOCKED)
        Service->>Service: unlockRooms()
    end
    
    Note over Service: 롤백 완료 - 시스템 상태 복원됨
```

## 이벤트 타입별 처리

```mermaid
flowchart TD
    A[MERGE_INITIATED] --> B[방 잠금 처리]
    B --> C[ROOMS_LOCKED]
    C --> D[메시지 마이그레이션]
    D --> E[MESSAGES_MIGRATED]
    E --> F[사용자 마이그레이션]
    F --> G[USERS_MIGRATED]
    G --> H[병합 완료]
    H --> I[MERGE_COMPLETED]
    
    B --> J[실패 시 롤백]
    D --> K[실패 시 롤백]
    F --> L[실패 시 롤백]
    
    J --> M[방 잠금 해제]
    K --> N[메시지 롤백]
    N --> M
    L --> O[사용자 롤백]
    O --> N
    M --> P[시스템 복원 완료]
```

## 데이터베이스 상태 변화

```mermaid
stateDiagram-v2
    [*] --> ACTIVE: 방 생성
    ACTIVE --> MERGING: 병합 시작
    MERGING --> ACTIVE: 롤백 시
    MERGING --> ARCHIVED: 병합 완료 (소스 방)
    ACTIVE --> ARCHIVED: 병합 완료 (소스 방)
    
    note right of MERGING
        병합 중인 상태
        - 메시지 이동 불가
        - 사용자 이동 불가
        - 롤백 가능
    end note
    
    note right of ARCHIVED
        병합 완료된 소스 방
        - 읽기 전용
        - 복구 불가
    end note
```

## 멱등성 보장 메커니즘

```mermaid
flowchart LR
    A[이벤트 수신] --> B{로그 존재?}
    B -->|Yes| C{이미 처리됨?}
    B -->|No| D[로그 생성]
    C -->|Yes| E[스킵]
    C -->|No| F[처리 진행]
    D --> F
    F --> G[로그 상태 업데이트]
    E --> H[처리 완료]
    G --> H
```

## 페이징 처리 흐름

```mermaid
flowchart TD
    A[마이그레이션 시작] --> B[페이지 0부터 시작]
    B --> C[1000개씩 조회]
    C --> D{데이터 있음?}
    D -->|No| E[마이그레이션 완료]
    D -->|Yes| F[배치 처리]
    F --> G[로그 기록]
    G --> H[실제 마이그레이션]
    H --> I[페이지 증가]
    I --> C
```

<br>
<br>

# 전체 서비스 토폴로지

```mermaid
flowchart LR

  subgraph Client["Client (Browser)"]
    U[사용자]
    WS[WebSocket/STOMP]
  end

  subgraph Nginx["Nginx (Load Balancer)"]
  end

  subgraph Producer["chat-producer (Spring Boot)"]
    P_WS[WebSocket Endpoints]
    P_API[REST APIs]
    P_REDIS[(Redis)]
    P_MYSQL[(MySQL<br/>Rooms, Users, MergeStatus, Outbox)]
    P_MONGO_REPO[(Mongo Repository)]

    P_OUTBOX[(Outbox Event Table)]
    P_OBPUB["Outbox Publisher\n(Scheduler/Batch)"]

    P_KC["Kafka Consumers\nsend-msg(ack), merge-events"]
  end

  subgraph Kafka[Kafka]
    K_CHAT[topic: chat-messages]
    K_ACK[topic: send-msg]
    K_MERGE[topic: merge-events]
  end

  subgraph Consumer["chat-consumer (Spring Boot)"]
    S_KC["Kafka Consumer\nchat-messages"]
    S_MONGO_WRITE[Mongo Writer]

    S_OUTBOX[(Outbox Event Table)]
    S_OBPUB["Outbox Publisher\n(Scheduler/Batch)"]
  end

  subgraph Datastores[Datastores]
    MONGO[(MongoDB\nchat_messages_ind)]
    MYSQL[(MySQL RDBMS)]
  end

  %% Client ingress
  U --> WS --> Nginx --> Producer

  %% Producer internal state
  Producer -->|세션/캐시| P_REDIS
  Producer -->|JPA| P_MYSQL
  P_MYSQL --- MYSQL
  P_MONGO_REPO --- MONGO

  %% Producer Outbox write (chat message & merge events)
  Producer -->|도메인 변경 + Outbox INSERT| P_OUTBOX
  P_OBPUB -->|PENDING 이벤트 폴링| P_OUTBOX
  P_OBPUB -->|publish chat| K_CHAT
  P_OBPUB -->|publish merge| K_MERGE

  %% Consumer: chat-messages consume -> Mongo 저장 -> ack Outbox
  K_CHAT --> S_KC
  S_KC --> S_MONGO_WRITE --> MONGO
  S_KC -->|ack Outbox INSERT| S_OUTBOX
  S_OBPUB -->|PENDING ack 폴링| S_OUTBOX
  S_OBPUB -->|publish ack| K_ACK

  %% ack 소비 후 사용자 통지
  K_ACK --> Producer
  Producer --> P_KC
  P_KC -->|WS push| P_WS --> WS --> U

  %% Merge saga (관리 상태 업데이트)
  Producer -->|상태 업데이트| MYSQL
  Producer -->|메시지/사용자 이동| MONGO

  %% Admin/REST
  U -->|HTTP| Nginx --> P_API
```
