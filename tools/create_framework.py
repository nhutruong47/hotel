import os

framework_dir = r"d:\d\1\hotelNew\AI_PROJECT_FRAMEWORK"

if not os.path.exists(framework_dir):
    os.makedirs(framework_dir)

files = {
    "00_PROJECT_RULE.md": """# 00. PROJECT RULE
- Bắt buộc tuân thủ UX/UI guidelines trước khi code.
- Không tự ý thay đổi Design System (Luxury, Minimal, Japanese).
- Mọi quyết định phải dựa trên UX, SEO, Performance, Accessibility.
- Nếu có mâu thuẫn giữa yêu cầu mới và Framework, phải báo cáo trước khi thực hiện.
""",
    "01_UX_RESEARCH.md": """# 01. UX RESEARCH
- Persona A: The Relaxation Seeker (Tìm kiếm bình yên, riêng tư)
- Persona B: The Aesthetic Traveler (Yêu cái đẹp, Wabi-Sabi)
- Journey: Awareness -> Exploration -> Consideration -> Action -> Booking -> Post-Booking
- Pain points: Đông đúc, ồn ào, hình ảnh không chân thực.
""",
    "02_INFORMATION_ARCHITECTURE.md": """# 02. INFORMATION ARCHITECTURE
- Cấu trúc: Home -> Villa Collection -> Villa Detail -> Experience -> Booking Flow
- Mục tiêu mỗi trang phải rõ ràng, không gộp quá nhiều tính năng vào một màn hình.
""",
    "03_WIREFRAME.md": """# 03. WIREFRAME
- Focus: Layout, Khoảng trắng, CTA, Navigation.
- Không màu sắc, không ảnh ở bước này (sử dụng placeholder).
""",
    "04_UI_SYSTEM.md": """# 04. UI SYSTEM
- Vibe: Luxury, Boutique, Minimal, Japanese, Warm, Natural, Calm
- Typography: Cormorant Garamond (Heading), Outfit (Body).
- Màu sắc: Sand (#f4f1eb), Stone (#e5e0d8), Charcoal (#232323), Sage (#9bafa0).
""",
    "05_MOTION_STORYBOARD.md": """# 05. MOTION STORYBOARD
- Scroll Storytelling: Hoạt động như một thước phim.
- Các scene chuyển tiếp mượt mà: Hero -> Story -> Walkthrough (Horizontal Scroll) -> Villas -> CTA.
""",
    "06_MOTION_SPEC.md": """# 06. MOTION SPEC
- Thư viện: GSAP + Lenis.
- Hiệu ứng: Parallax, Clip-path reveal, Staggered text, Fade up.
- Luôn tôn trọng `prefers-reduced-motion`. Không dùng hiệu ứng gây chóng mặt.
""",
    "07_FRONTEND_ARCHITECTURE.md": """# 07. FRONTEND ARCHITECTURE
- Tech Stack: React, Vite, TypeScript, Tailwind CSS v4.
- Feature-based structure.
- State management đơn giản, hạn chế over-engineering.
""",
    "08_COMPONENT_LIBRARY.md": """# 08. COMPONENT LIBRARY
- Navbar: Auto-hide, mobile overlay.
- Footer: Semantic, responsive grid.
- Tuân thủ DRY (Don't Repeat Yourself) nhưng không hi sinh tính dễ đọc.
""",
    "09_SEO_GUIDE.md": """# 09. SEO GUIDE
- Bắt buộc: Meta tags, Canonical, Open Graph, Twitter Cards.
- Structured Data: Hotel Schema, FAQ Schema, Breadcrumb.
- Semantic HTML (main, section, article, nav).
""",
    "10_PERFORMANCE_GUIDE.md": """# 10. PERFORMANCE GUIDE
- Ảnh: Responsive srcSet, webp/avif, lazy load (trừ Hero image dùng eager + preload).
- Font: display=swap.
- CSS/JS: Minified, tree-shaken by Vite.
""",
    "11_ACCESSIBILITY_GUIDE.md": """# 11. ACCESSIBILITY GUIDE
- Bắt buộc có focus-visible cho outline.
- Skip-to-content link.
- Contrast AA+. ARIA labels cho các nút/icon không có text.
""",
    "12_TESTING_GUIDE.md": """# 12. TESTING GUIDE
- TypeScript: strict type checking.
- Browser test: Chrome, Safari, Firefox.
- Responsive test: Mobile, Tablet, Desktop.
""",
    "13_DEPLOY_GUIDE.md": """# 13. DEPLOY GUIDE
- Build command: `vite build`
- Đảm bảo các file sitemap, robots.txt có sẵn trong public folder.
"""
}

for filename, content in files.items():
    filepath = os.path.join(framework_dir, filename)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)

print("Đã tạo thành công thư mục AI_PROJECT_FRAMEWORK và 14 files.")
