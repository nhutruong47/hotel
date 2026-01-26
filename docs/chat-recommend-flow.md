# 📊 Luồng Chat AI Recommend - Hotel Booking System

## 📌 Tổng Quan

Tính năng **AI Recommend** cho phép người dùng chat với AI để nhận gợi ý phòng phù hợp. Hệ thống sử dụng **Ollama** (model `qwen2.5:7b`) làm AI engine, với fallback logic khi AI không khả dụng.

---

## 🔄 Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    participant U as 👤 User
    participant B as 🌐 Browser
    participant AC as 🎮 AiController
    participant OS as 🤖 OllamaService
    participant RR as 📦 RoomRepository
    participant CMR as 💬 ChatMessageRepository
    participant DB as 🗄️ Database
    participant AI as 🧠 Ollama AI<br/>(localhost:11434)

    rect rgb(230, 245, 255)
        Note over U,DB: BƯỚC 1: MỞ TRANG CHAT
        U->>B: Click "Gợi ý phòng AI"
        B->>AC: GET /ai-recommend
        AC->>CMR: findTop20ByUserOrderByCreatedAtDesc(user)
        CMR->>DB: SELECT * FROM ChatMessages WHERE user_id=?
        DB-->>CMR: List<ChatMessage>
        CMR-->>AC: Lịch sử chat (20 tin gần nhất)
        AC-->>B: ai-recommend.html + chatHistory
        B-->>U: Hiển thị trang chat
    end

    rect rgb(255, 250, 230)
        Note over U,AI: BƯỚC 2: GỬI TIN NHẮN
        U->>B: Nhập: "Tôi cần phòng cho cặp đôi, view đẹp"
        B->>AC: POST /ai-recommend<br/>{"message": "..."}
        AC->>OS: getAiRecommendation(userMessage)
        
        Note over OS,RR: Lấy danh sách phòng trống
        OS->>RR: findByIsAvailableTrue()
        RR->>DB: SELECT * FROM Rooms WHERE is_available=1
        DB-->>RR: List<Room>
        RR-->>OS: availableRooms
        
        Note over OS: Build prompt với context
        OS->>OS: roomsContext = "Phòng 101 (Deluxe)..."
        OS->>OS: prompt = "Bạn là trợ lý AI... PHÒNG TRỐNG: {context}"
    end

    rect rgb(230, 255, 230)
        Note over OS,AI: BƯỚC 3: GỌI OLLAMA AI
        OS->>AI: POST /api/generate<br/>model: "qwen2.5:7b"<br/>prompt: "..."<br/>stream: false
        
        alt ✅ Ollama khả dụng
            AI-->>OS: {"response": "Dựa trên yêu cầu của bạn..."}
        else ❌ Ollama không khả dụng
            OS->>OS: getFallbackRecommendation()
            Note over OS: Keyword matching:<br/>- "cặp đôi" → DELUXE, DOUBLE<br/>- "sang trọng" → VIP, SUITE<br/>- "tiết kiệm" → Sort by price
            OS-->>OS: Fallback response
        end
    end

    rect rgb(255, 230, 245)
        Note over AC,DB: BƯỚC 4: LƯU VÀ TRẢ VỀ
        OS-->>AC: aiResponse string
        AC->>CMR: save(new ChatMessage(user, userMessage, aiResponse))
        CMR->>DB: INSERT INTO ChatMessages...
        AC-->>B: {"response": "Gợi ý phòng..."}
        B-->>U: Hiển thị response
    end

    rect rgb(245, 245, 245)
        Note over U,DB: BƯỚC 5: XÓA LỊCH SỬ (TÙY CHỌN)
        U->>B: Click "Xóa lịch sử"
        B->>AC: POST /ai-recommend/clear
        AC->>CMR: deleteByUser(user)
        CMR->>DB: DELETE FROM ChatMessages WHERE user_id=?
        AC-->>B: {"success": "Đã xóa"}
    end
```

---

## 🏗️ Class Diagram

```mermaid
classDiagram
    class AiController {
        -OllamaService ollamaService
        -ChatMessageRepository chatMessageRepository
        +aiRecommendPage(session, model) String
        +getAiRecommendation(request, session) Map
        +clearHistory(session) Map
    }

    class OllamaService {
        -WebClient webClient
        -RoomRepository roomRepository
        +getAiRecommendation(userRequest) String
        -getFallbackRecommendation(rooms, request) String
    }

    class ChatMessage {
        -Integer id
        -User user
        -String userMessage
        -String aiResponse
        -LocalDateTime createdAt
    }

    class Room {
        -Integer id
        -String roomNumber
        -RoomType roomType
        -BigDecimal pricePerNight
        -String description
        -Boolean isAvailable
    }

    class ChatMessageRepository {
        <<interface>>
        +findTop20ByUserOrderByCreatedAtDesc(user) List
        +deleteByUser(user) void
    }

    class RoomRepository {
        <<interface>>
        +findByIsAvailableTrue() List
    }

    AiController --> OllamaService : uses
    AiController --> ChatMessageRepository : uses
    OllamaService --> RoomRepository : uses
    ChatMessageRepository ..> ChatMessage : manages
    RoomRepository ..> Room : manages
    ChatMessage --> User : belongs to
```

---

## 📈 Flowchart

```mermaid
flowchart TD
    A[👤 User mở /ai-recommend] --> B{Đã đăng nhập?}
    B -->|Không| C[Redirect /login]
    B -->|Có| D[Load lịch sử chat<br/>20 tin gần nhất]
    D --> E[Hiển thị trang chat]
    
    E --> F[User nhập tin nhắn]
    F --> G[POST /ai-recommend]
    
    G --> H[Lấy danh sách phòng trống<br/>RoomRepository.findByIsAvailableTrue]
    
    H --> I{Có phòng trống?}
    I -->|Không| J[Return: Không có phòng trống]
    I -->|Có| K[Build prompt với context phòng]
    
    K --> L[Gọi Ollama API<br/>model: qwen2.5:7b]
    
    L --> M{Ollama response?}
    M -->|✅ Thành công| N[Lấy AI response]
    M -->|❌ Lỗi timeout/connection| O[getFallbackRecommendation]
    
    O --> P{Keyword matching}
    P -->|VIP/sang trọng| Q[Filter VIP, SUITE]
    P -->|Gia đình/4 người| R[Filter FAMILY, DELUXE]
    P -->|Cặp đôi| S[Filter DOUBLE, DELUXE]
    P -->|Tiết kiệm/rẻ| T[Sort by price ASC]
    P -->|Khác| U[Return first 2 rooms]
    
    Q & R & S & T & U --> V[Build fallback response]
    
    N & V --> W[Lưu ChatMessage vào DB]
    W --> X[Return JSON response]
    X --> Y[Hiển thị tin nhắn AI]
    Y --> E

    style A fill:#e1f5fe
    style C fill:#ffcdd2
    style J fill:#fff3e0
    style N fill:#c8e6c9
    style V fill:#fff9c4
```

---

## 📂 Các Class Liên Quan

| Class | File Path | Vai trò |
|-------|-----------|---------|
| `AiController` | `controller/AiController.java` | Xử lý HTTP request `/ai-recommend` |
| `OllamaService` | `service/OllamaService.java` | Gọi Ollama API, build prompt |
| `ChatMessage` | `model/ChatMessage.java` | Entity lưu tin nhắn |
| `ChatMessageRepository` | `repository/ChatMessageRepository.java` | CRUD tin nhắn |
| `Room` | `model/Room.java` | Entity phòng |
| `RoomRepository` | `repository/RoomRepository.java` | Query phòng trống |

---

## ⚙️ Cấu Hình

| Config | Giá trị | Mô tả |
|--------|---------|-------|
| Ollama URL | `http://localhost:11434` | Địa chỉ Ollama server |
| Model | `qwen2.5:7b` | Model AI sử dụng |
| Timeout | 120 seconds | Thời gian chờ response |
| Max history | 20 messages | Số tin nhắn lịch sử hiển thị |

---

## 🔀 Fallback Logic

Khi Ollama không khả dụng, hệ thống sử dụng **keyword matching**:

```java
if (request.contains("vip") || request.contains("sang trọng"))
    → Filter VIP, SUITE rooms
    
if (request.contains("gia đình") || request.contains("4 người"))
    → Filter FAMILY, DELUXE rooms
    
if (request.contains("cặp đôi") || request.contains("lãng mạn"))
    → Filter DOUBLE, DELUXE rooms
    
if (request.contains("rẻ") || request.contains("tiết kiệm"))
    → Sort by price ascending
    
default → Return first 2 available rooms
```
