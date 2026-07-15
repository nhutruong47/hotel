# 00. PROJECT RULE: NHƯ VILLAS

> **LƯU Ý DÀNH CHO AI:** Đây là TÀI LIỆU GỐC (Source of Truth) của toàn bộ dự án. Bất kỳ AI nào trước khi nhận task và thực thi code đều MẶC ĐỊNH phải tuân thủ nghiêm ngặt 100% các quy chuẩn trong tài liệu này. Mọi quyết định đi ngược lại framework này đều bị cấm, trừ khi có sự đồng ý rõ ràng từ User.

---

## 1. TRIẾT LÝ THIẾT KẾ (DESIGN PHILOSOPHY)
- **Japanese Wabi-Sabi & Minimalism:** Tôn vinh vẻ đẹp của sự nguyên bản, tĩnh lặng, tự nhiên và khoảng trống.
- **"Less, but better":** Không bao giờ thêm các chi tiết dư thừa (viền quá đậm, bóng đổ quá gắt, màu sắc sặc sỡ).
- **Luxury Boutique:** Trải nghiệm số phải đắt tiền, mượt mà và tinh tế như các thương hiệu hạng sang (Aman Resorts, Apple). Cảm xúc của người dùng là ưu tiên tối thượng.

---

## 2. QUY CHUẨN UI (UI SPECIFICATIONS)
- **Typography:** Cấm sử dụng font mặc định của trình duyệt. Dùng `Cormorant Garamond` cho Headings (thể hiện tính cổ điển, mượt mà) và `Outfit` cho Body text (thể hiện sự hiện đại, tối giản).
- **Khoảng trắng (White-space):** "Khoảng trắng là một element". Sử dụng padding/margin cực lớn (ví dụ: `py-32`, `py-40`) để tạo không gian thở cho mắt.
- **Màu sắc (Colors):** Không dùng màu nguyên bản (pure black/white). Sử dụng hệ màu tự nhiên được định nghĩa trong CSS: Sand, Stone, Charcoal, Sage, Ink.
- **Border & Shadow:** Radius siêu nhỏ (ví dụ: `rounded-[2px]`) hoặc vuông vức. Cấm shadow gắt, nếu có chỉ sử dụng shadow cực nhẹ và blend vào nền.

---

## 3. QUY CHUẨN UX (UX SPECIFICATIONS)
- **Hành trình cảm xúc:** Trải nghiệm cuộn trang phải như một dòng chảy tự nhiên. Không làm người dùng bất ngờ theo cách tiêu cực.
- **CTA (Call to Action):** Đặt nút bấm đúng lúc, đúng chỗ. Cấm spam nút "Đặt ngay" ở mọi nơi. CTA chính thức (Primary) chỉ xuất hiện khi cảm xúc người dùng đạt đỉnh (Scene hoàng hôn, view hồ bơi).
- **Phễu chuyển đổi:** Giao diện đặt phòng (Booking Flow) phải ít bước nhất có thể. Min bạch về giá cả, không có phí ẩn (hidden fees).

---

## 4. QUY CHUẨN MOTION (MOTION SPECIFICATIONS)
- Mọi animation phải phục vụ một mục đích cụ thể: Hướng sự chú ý, tạo nhịp điệu, hoặc kể chuyện. Cấm animation bay nhảy lộn xộn, vô nghĩa.
- Sử dụng hiệu ứng: Reveal (Clip-path), Parallax (di chuyển khác tốc độ), Fade-up, Staggered Text.
- Tốc độ: Chậm rãi, mượt mà, ease chuẩn (`power3.out`, `power4.inOut`). 
- **Lưu ý:** Luôn phải tôn trọng cài đặt `prefers-reduced-motion` của hệ điều hành.

---

## 5. QUY CHUẨN CODING (CODING CONVENTION)
- Sử dụng **TypeScript** 100%. Không dùng Javascript thường. `any` type bị cấm trừ khi cực kỳ bất khả kháng.
- Clean Code: Một file component không nên vượt quá 150 dòng. Nếu vượt quá, phải break down thành các sub-components.
- DRY (Don't Repeat Yourself) nhưng phải ưu tiên tính dễ đọc (Readability).
- Xóa toàn bộ `console.log` và code rác trước khi bàn giao.

---

## 6. QUY CHUẨN REACT (REACT CONVENTION)
- Function Components + Hooks là tiêu chuẩn duy nhất. Cấm sử dụng Class Components.
- Khai báo component sử dụng keyword `export default function ComponentName()`.
- Tối ưu hóa render bằng việc quản lý dependency array của `useEffect`, `useMemo`, và `useCallback` chặt chẽ.
- Không lạm dụng Global State (Zustand/Redux) cho các trạng thái chỉ tồn tại cục bộ ở một trang.

---

## 7. QUY CHUẨN TAILWIND (TAILWIND CONVENTION)
- Sử dụng **Tailwind CSS v4**.
- Định nghĩa Theme/Colors bằng CSS variables (`var(--color-brand-...)`) trong thẻ `@theme` của `index.css`.
- Tránh viết chuỗi class quá dài. Nếu một element có quá nhiều class, cân nhắc nhóm lại hoặc tách component để code dễ đọc hơn.
- Luôn sử dụng Tailwind utility cho mọi CSS. Cấm viết CSS thuần ngoài `index.css` trừ khi xử lý animation cực kỳ phức tạp.

---

## 8. QUY CHUẨN GSAP (GSAP CONVENTION)
- Sử dụng Hook `useGSAP` (nếu cần) hoặc dọn dẹp sạch sẽ trong `useEffect` (sử dụng `gsap.context()` để `revert()`). Việc quên dọn dẹp (cleanup) memory leak là lỗi nghiêm trọng.
- Khi tích hợp ScrollTrigger, luôn chỉ định rõ ràng `trigger`, `start`, và `toggleActions`.
- Tách các logic GSAP phức tạp ra thành các Custom Hooks (ví dụ: `useReveal`, `useParallax`).

---

## 9. QUY CHUẨN SEO (SEO CONVENTION)
- HTML Semantic bắt buộc: Sử dụng đúng thẻ `<main>`, `<article>`, `<section>`, `<nav>`, `<aside>`. 
- Hierarchy: Chỉ duy nhất một thẻ `<h1>` trên mỗi trang. `<h2>`, `<h3>` phải tuân theo thứ tự logic.
- Luôn phải có: Meta tags (Title, Description, Canonical), Open Graph (Facebook), Twitter Cards.
- Cấu trúc dữ liệu: Triển khai Schema Markup (JSON-LD) như Hotel, FAQ, Breadcrumb.
- Thuộc tính `alt` của hình ảnh không được để trống, phải mô tả chi tiết nội dung ảnh.

---

## 10. QUY CHUẨN ACCESSIBILITY (A11Y CONVENTION)
- Cấm phụ thuộc hoàn toàn vào chuột (Mouse). Mọi hành động đều phải thực hiện được bằng Bàn phím (Keyboard Navigation).
- Focus Ring: Bắt buộc có thuộc tính outline cho trạng thái focus (`focus-visible:outline-2`).
- Screen Reader: Cung cấp `aria-label`, `aria-controls`, `aria-expanded` cho các element không có text rõ ràng (như Hamburger menu, Icon button).
- Bắt buộc có link "Skip to main content" ẩn cho screen readers.

---

## 11. QUY CHUẨN PERFORMANCE (PERFORMANCE CONVENTION)
- Điểm Lighthouse Performance mục tiêu: ≥ 95.
- Hình ảnh: 100% sử dụng responsive `srcSet` và `sizes`.
- Hero image (Ảnh đầu tiên hiển thị): Sử dụng `loading="eager"`, `fetchPriority="high"` và thẻ `<link rel="preload">`.
- Tất cả các ảnh bên dưới màn hình đầu tiên (Below the fold) bắt buộc dùng `loading="lazy"`, `decoding="async"`.
- Tránh Layout Shift (CLS) bằng cách set sẵn `aspect-ratio` hoặc cấu trúc thẻ chứa ảnh đúng chuẩn.

---

## 12. QUY CHUẨN RESPONSIVE (RESPONSIVE CONVENTION)
- Cấm tư duy "Chỉ thu nhỏ Desktop cho Mobile".
- Thiết kế Mobile First, sau đó sử dụng breakpoint (`sm:`, `md:`, `lg:`, `xl:`) để nâng cấp giao diện.
- Trải nghiệm Scroll Storytelling trên Desktop có thể là cuộn ngang (Horizontal), nhưng trên Mobile bắt buộc phải chuyển sang cuộn dọc xếp chồng (Vertical stacked) bằng đơn vị `svh`.

---

## 13. QUY CHUẨN FOLDER STRUCTURE (FOLDER STRUCTURE CONVENTION)
Dự án áp dụng Feature-based Architecture. Không thay đổi cấu trúc sau:
```
frontend/
├── public/                 # Static assets (favicon, robots, sitemap)
├── src/
│   ├── assets/             # Hình ảnh cục bộ, font, svg
│   ├── components/         # Reusable UI components (Navbar, Footer, Button)
│   ├── hooks/              # Custom React Hooks (useSmoothScroll, useGSAP)
│   ├── sections/           # Các khối nội dung lớn của trang (HeroSection, VillaSection)
│   ├── utils/              # Các hàm helper logic
│   ├── types/              # Khai báo TypeScript Interfaces/Types
│   ├── App.tsx             # Assembly file (Lắp ráp layout)
│   ├── App.css             # Base reset (Hạn chế tối đa)
│   └── index.css           # Tailwind + Design System variables
└── vite.config.ts          # Cấu hình build
```

---

## 14. NAMING CONVENTION
- **Files/Components:** PascalCase (VD: `HeroSection.tsx`, `Navbar.tsx`).
- **Hooks:** camelCase, luôn bắt đầu bằng chữ 'use' (VD: `useSmoothScroll.ts`).
- **Utils/Functions:** camelCase (VD: `formatPrice.ts`).
- **CSS Variables:** Kebab-case (VD: `--color-brand-charcoal`).
- **Types/Interfaces:** PascalCase (VD: `VillaProps`, `BookingData`).

---

## 15. COMPONENT CONVENTION
- Tách rời giao diện (UI) và logic (Business Logic) khi cần thiết.
- Props interface phải được định nghĩa rõ ràng ngay trên đầu file component.
- Hạn chế inline-styles, trừ trường hợp dynamic values. Tất cả style còn lại phải là class của Tailwind.

---

## 16. GIT CONVENTION
- Commit message phải có nghĩa và tuân theo Conventional Commits:
  - `feat: [Mô tả tính năng]`
  - `fix: [Mô tả lỗi đã sửa]`
  - `ui: [Mô tả cập nhật giao diện]`
  - `refactor: [Mô tả tái cấu trúc code]`
- Không commit file rác, `.env` file hoặc các thư mục không cần thiết.

---

## 17. QUY TRÌNH LÀM VIỆC VỚI AI
- Đọc kỹ tài liệu yêu cầu (Prompt/Instructions) từ người dùng.
- Khi được yêu cầu làm một chức năng mới, AI phải tự đối chiếu với 16 quy chuẩn trên để đánh giá sự phù hợp.
- Nếu phát hiện yêu cầu làm giảm Performance, vi phạm Design System, hay hỏng UX, AI có nghĩa vụ cảnh báo Người dùng trước khi thực thi.
- Không tự ý nghĩ ra giao diện theo cảm tính. Mọi thiết kế phải dựa trên triết lý Japanese Minimalist Luxury.
- Chỉ thực hiện ĐÚNG và ĐỦ yêu cầu được giao, không làm thừa việc (trừ khi liên quan trực tiếp đến việc vá lỗi do mình gây ra).
