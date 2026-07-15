# 07. FRONTEND ARCHITECTURE FRAMEWORK

> **TÀI LIỆU TIÊU CHUẨN:** Đây là bộ khung kiến trúc (Architecture Framework) quy định cấu trúc và cách luân chuyển luồng dữ liệu (Data Flow) trong các dự án Frontend. Cấu trúc này đặt tính tái sử dụng, phân tách trách nhiệm (Separation of Concerns) và khả năng mở rộng (Scalability) lên hàng đầu.

---

## 1. ARCHITECTURE OVERVIEW (TỔNG QUAN KIẾN TRÚC)
Dự án áp dụng mô hình **Feature-Based Architecture** (Kiến trúc hướng tính năng) kết hợp với **Domain-Driven Design (DDD) cơ bản**. Thay vì chia thư mục theo loại file (tất cả components vào một chỗ, tất cả hooks vào một chỗ), chúng ta nhóm các file liên quan đến cùng một "Tính năng" hoặc "Nghiệp vụ" vào chung một thư mục.

## 2. FOLDER STRUCTURE (CẤU TRÚC THƯ MỤC CỐT LÕI)
Mọi dự án Frontend phải tuân thủ nghiêm ngặt cấu trúc rễ (Root Structure) sau:

```
src/
├── app/            # App Layer: Global Setup, Providers, Main Router, Global Styles
├── domains/        # Domain Layer: Logic nghiệp vụ cốt lõi không dính dáng đến UI
├── features/       # Feature Layer: Nhóm các Component, Logic theo tính năng cụ thể
├── shared/         # Shared Layer: UI Components dùng chung, Utils, Global Hooks
├── assets/         # Tài nguyên tĩnh (Hình ảnh, Icon, Fonts)
├── env/            # Định nghĩa types và schema validation cho Environment Variables
└── main.tsx        # Entry point duy nhất của React
```

---

## 3. APP LAYER (TẦNG ỨNG DỤNG)
- **Nhiệm vụ:** Điểm khởi tạo và quản lý toàn bộ vòng đời ứng dụng.
- **Thành phần:**
  - `App.tsx`: Nơi gom các Global Providers (Redux/Zustand, ThemeContext, QueryClient, RouterProvider).
  - `router/`: Cấu hình routing tổng (chứa danh sách các Route).
  - Không chứa UI Component hay Business logic tại đây.

## 4. DOMAIN LAYER (TẦNG NGHIỆP VỤ)
- **Nhiệm vụ:** Nơi chứa các quy tắc nghiệp vụ cốt lõi (Business Rules), Entities, Types. Hoàn toàn "mù" về React (không chứa `.tsx`, không chứa CSS).
- **Thành phần:**
  - `models/`: Định nghĩa các Interface/Type (VD: `User.ts`, `Booking.ts`).
  - `repositories/`: Các class hoặc functions giao tiếp trực tiếp với Backend (API Layer).
  - `services/`: Nơi xử lý logic tinh vi trước khi đẩy data ra UI (Format data, Validate).

## 5. FEATURE LAYER (TẦNG TÍNH NĂNG)
- **Nhiệm vụ:** Chứa mọi thứ liên quan đến một chức năng độc lập (Ví dụ: `Auth`, `Booking`, `RoomList`).
- Một Feature folder phải đóng gói (Encapsulation): Nó chứa components, hooks, stores, api, types riêng của nó.
- **Quy tắc Vàng:** Một Feature KHÔNG ĐƯỢC PHÉP import trực tiếp từ một Feature khác để tránh vòng lặp (Circular Dependency). Nếu cần chia sẻ, đưa logic đó xuống Shared Layer.

## 6. SHARED LAYER (TẦNG DÙNG CHUNG)
- **Nhiệm vụ:** Nơi chứa những gì được dùng đi dùng lại trên toàn hệ thống.
- **Thành phần:**
  - `components/`: Nút bấm (Button), Card, Modal, Form Elements (Dumb/Presentational Components).
  - `hooks/`: Custom Hooks dùng chung (Ví dụ: `useWindowSize`, `useClickOutside`).
  - `utils/`: Hàm tiện ích (Date formatter, Currency formatter).
  - `layouts/`: Header, Footer, Sidebar.

---

## CÁC QUY TRÌNH & CHIẾN LƯỢC KỸ THUẬT

### 7. STATE MANAGEMENT STRATEGY (QUẢN LÝ TRẠNG THÁI)
- **Server State (Dữ liệu từ API):** BẮT BUỘC sử dụng các thư viện như `React Query` hoặc `SWR` để quản lý caching, background fetching, deduplication. Cấm dùng `useEffect` + `useState` để tự fetch data.
- **Client Global State:** Sử dụng thư viện nhẹ nhàng như `Zustand` hoặc `Jotai` (Tránh Redux trừ khi dự án quá khổng lồ). Chỉ dùng cho: Theme, User Auth Session, Giỏ hàng.
- **Client Local State:** Dùng `useState` hoặc `useReducer` cho các trạng thái đóng/mở UI, input form. Không đẩy Local State lên Global.

### 8. API LAYER & DATA FETCHING
- Mọi lời gọi API phải được tập trung quản lý. Sử dụng `Axios` instance hoặc cấu hình `fetch` chuẩn hóa có sẵn Interceptors (để gắn Token, bắt lỗi 401/403 tự động).
- **Tuyệt đối không:** Gọi API trực tiếp bên trong UI Component bằng chuỗi URL cứng (Hard-coded URL).
- Luôn định nghĩa Request / Response Interfaces rõ ràng.

### 9. ROUTING STRATEGY
- Dùng thư viện Router tiêu chuẩn (VD: React Router v6+).
- Nên cấu hình Data Loader (nếu hỗ trợ) để fetch dữ liệu trước khi render trang, giảm thiểu loading spinners nhấp nháy.
- Bảo vệ các Route yêu cầu đăng nhập bằng `ProtectedRoute` component.

### 10. CODE SPLITTING & LAZY LOADING
- Không bundle toàn bộ code JS vào một file khổng lồ.
- Áp dụng `React.lazy()` và `Suspense` ở cấp độ Route (Route-level Code Splitting). Mọi Route cấp cao đều phải được tải bất đồng bộ.
- Components/Thư viện nặng (VD: Map, Chart) cũng phải được lazy load nếu không xuất hiện ngay lập tức.

### 11. ERROR BOUNDARY
- Phải có ít nhất 1 Global Error Boundary bọc toàn bộ App để bắt các lỗi JS chưa xử lý (Cấm tình trạng trắng màn hình).
- Nên có Error Boundary chia nhỏ ở từng Feature/Section để khi một component crash, các component khác vẫn hoạt động.

### 12. ENVIRONMENT VARIABLES (.ENV)
- Không push file `.env` lên Git. 
- Mọi biến môi trường (`VITE_API_URL`, `REACT_APP_KEY`) phải có Prefix chuẩn của builder.
- Phải tạo một file schema validation (ví dụ dùng `Zod`) để validate sự tồn tại và định dạng của các biến môi trường ngay lúc khởi động app.

### 13. BUILD STRATEGY
- Bật cấu hình Tree-Shaking của Bundler (Vite/Webpack) để loại bỏ code thừa.
- Bundle Analysis: Thường xuyên kiểm tra kích thước Bundle. Cảnh báo/Chặn merge nếu một package rác làm tăng dung lượng quá giới hạn (VD: > 500KB JS).

---

## 14. FRONTEND CHECKLIST
Trước khi tạo PR (Pull Request) / Đẩy code:
- [ ] Code có vi phạm quy tắc "Feature không được import chéo Feature khác" không?
- [ ] Các logic gọi API đã được đẩy qua React Query / SWR thay vì `useEffect` chưa?
- [ ] Có Component nào đang vượt quá độ phức tạp (Chứa quá 3 trách nhiệm hoặc quá 200 dòng code)?
- [ ] Tình trạng Loading/Error của Data Fetching đã được xử lý triệt để trong UI chưa?
- [ ] Tất cả Component lớn (đặc biệt là Route Pages) đã được phân tách bằng Lazy Loading chưa?
