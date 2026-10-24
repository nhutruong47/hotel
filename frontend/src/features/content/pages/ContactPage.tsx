"use client";
import { useState } from "react";
import Link from "next/link";
import { api, API_PATHS } from "../../../shared/api/client";
import { useTranslation } from "../../../shared/i18n/hooks";
import { ContentShell } from "../shared";

const CONCIERGE_PRESETS = [
  {
    id: "airport",
    type: "RESERVATION",
    labelVi: "✈️ Đặt Xe Riêng Đón Sân Bay Liên Khương",
    labelEn: "✈️ Private Lien Khuong Airport Chauffeur",
    subjectVi: "Yêu cầu xe Mercedes đón tại Sân bay Liên Khương",
    subjectEn: "Private Airport Chauffeur Request (Lien Khuong DLI)",
    bodyVi:
      "Xin chào Quản gia Nhu Villas,\nTôi muốn đặt xe riêng đón tại sân bay Liên Khương (DLI). Mã chuyến bay dự kiến: [VN...], hạ cánh lúc [...]. Mong đội ngũ sắp xếp tài xế và khăn ấm đón đoàn.",
    bodyEn:
      "Dear Nhu Villas Concierge,\nI would like to arrange a private chauffeur pickup at Lien Khuong Airport (DLI). Flight number: [...], arrival time: [...].",
  },
  {
    id: "dining",
    type: "RESERVATION",
    labelVi: "🍷 Đặt Tiệc Tối Bếp Trưởng Tại Hồ Bơi",
    labelEn: "🍷 Private Poolside Chef's Table",
    subjectVi: "Đặt bàn tiệc tối riêng tư tại sân hiên biệt thự",
    subjectEn: "Private In-Villa Chef's Table Reservation",
    bodyVi:
      "Xin chào Quản gia,\nGia đình tôi muốn đặt thực đơn Fine Dining / BBQ nướng than hồng tại sân hiên biệt thự cho [...] khách vào tối ngày [...].",
    bodyEn:
      "Dear Concierge,\nWe would like to book an in-villa Chef's Table dinner by our private pool for [...] guests on [...].",
  },
  {
    id: "romance",
    type: "SPECIAL",
    labelVi: "🌹 Trang Trí Cầu Hôn / Kỷ Niệm Lãng Mạn",
    labelEn: "🌹 Bespoke Proposal & Anniversary Setup",
    subjectVi: "Thiết kế không gian kỷ niệm / cầu hôn bí mật",
    subjectEn: "Bespoke Proposal / Anniversary Curation",
    bodyVi:
      "Xin chào Quản gia,\nTôi muốn chuẩn bị bất ngờ kỷ niệm / cầu hôn với hoa hồng Đà Lạt, nến nổi trên hồ bơi và rượu Champagne vào lúc hoàng hôn (17:15).",
    bodyEn:
      "Dear Concierge,\nI would love to arrange a surprise sunset candlelit setup with Da Lat roses and Champagne around our infinity pool.",
  },
  {
    id: "buyout",
    type: "PARTNERSHIP",
    labelVi: "🏰 Thuê Nguyên Khu 6 Biệt Thự (Estate Buyout)",
    labelEn: "🏰 Full 6-Villa Estate Buyout Inquiry",
    subjectVi: "Tư vấn thuê độc quyền toàn bộ khu nghỉ dưỡng (Estate Buyout)",
    subjectEn: "Exclusive Full-Estate Buyout Inquiry",
    bodyVi:
      "Xin chào Ban Quản Lý Nhu Villas,\nChúng tôi muốn tư vấn gói thuê độc quyền toàn bộ 6 biệt thự cho kỳ nghỉ gia đình / sự kiện riêng tư khoảng [...] khách vào tháng [...].",
    bodyEn:
      "Dear Management,\nWe are inquiring about an exclusive buyout of all 6 villas for a private gathering of [...] guests.",
  },
];

export const ContactPage = () => {
  const { lang } = useTranslation();
  const isVi = lang === "vi";

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [subject, setSubject] = useState("");
  const [message, setMessage] = useState("");
  const [type, setType] = useState("RESERVATION");
  const [success, setSuccess] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const applyPreset = (preset: (typeof CONCIERGE_PRESETS)[number]) => {
    setType(preset.type);
    setSubject(isVi ? preset.subjectVi : preset.subjectEn);
    setMessage(isVi ? preset.bodyVi : preset.bodyEn);
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setSuccess(null);

    if (name.trim().length < 2 || !email.includes("@") || message.trim().length < 5) {
      setErrorMsg(
        isVi
          ? "Vui lòng điền đầy đủ Họ tên, Email hợp lệ và Nội dung yêu cầu."
          : "Please fill in all required fields correctly."
      );
      return;
    }

    setSubmitting(true);
    try {
      const result = await api.post<{ success: boolean; message: string; ticketId: string }>(
        API_PATHS.contact,
        { name, email, phone, subject, message, type }
      );
      setSuccess(
        `${result.message || (isVi ? "Yêu cầu đã được chuyển tới Trưởng Quản Gia" : "Request received")} (Mã phiếu: ${result.ticketId || "NV-VIP"})`
      );
      setName("");
      setEmail("");
      setPhone("");
      setSubject("");
      setMessage("");
    } catch {
      setErrorMsg(
        isVi
          ? "Không thể gửi yêu cầu lúc này. Vui lòng thử lại hoặc gọi hotline Quản gia."
          : "Unable to send message right now. Please try again."
      );
    } finally {
      setSubmitting(false);
    }
  };

  const contactTypes = [
    { value: "RESERVATION", label: isVi ? "Đặt phòng & Quản gia riêng" : "Reservation & Butler" },
    { value: "GENERAL", label: isVi ? "Tư vấn chung" : "General Inquiry" },
    { value: "SUPPORT", label: isVi ? "Hỗ trợ kỳ nghỉ" : "Guest Support" },
    { value: "FEEDBACK", label: isVi ? "Góp ý & Phản hồi" : "Guest Feedback" },
    { value: "PARTNERSHIP", label: isVi ? "Hợp tác & Sự kiện riêng" : "Partnership & Events" },
    { value: "EMERGENCY", label: isVi ? "Hỗ trợ khẩn cấp 24/7" : "24/7 Urgent Assistance" },
  ];

  return (
    <ContentShell
      eyebrow={isVi ? "Tổng Đài Quản Gia & Đặt Phòng" : "Private Concierge Desk"}
      title={isVi ? "Trò Chuyện Cùng Quản Gia." : "Speak with the stay team."}
      copy={
        isVi
          ? "Từ việc sắp xếp xe riêng đón tại sân bay Liên Khương, chuẩn bị bàn tiệc tối bên hồ bơi đến thiết kế kỳ nghỉ kỷ niệm — đội ngũ Quản gia Nhu Villas luôn sẵn sàng 24/7."
          : "For private airport transfers, bespoke dining, special celebrations, or full-estate buyouts, our Head Butler desk responds within 15 minutes."
      }
    >
      <div className="grid gap-8 lg:grid-cols-[0.82fr_1.18fr]">
        {/* Left: Direct Sanctuary Channels */}
        <div className="space-y-6">
          <div className="rounded-[2rem] bg-brand-forest-deep p-7 text-white shadow-[0_24px_70px_rgba(12,28,22,0.2)]">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-brand-sage-light">
              🛎️ {isVi ? "Đường Dây Nóng Trưởng Quản Gia" : "Head Butler Direct Line"}
            </p>
            <h2 className="mt-2 font-serif text-3xl text-white">
              {isVi ? "Hỗ Trợ Cá Nhân Hóa 24/7" : "24/7 Private Concierge"}
            </h2>
            <div className="mt-5 space-y-3 text-sm text-brand-paper/85">
              <p>📍 Phân khu Bán đảo Thông, Hồ Tuyền Lâm, Phường 4, Đà Lạt</p>
              <p>📞 Hotline VIP: (0263) 388-9999</p>
              <p>✉️ Email: concierge@nhuvillas.com</p>
            </div>
          </div>

          <div className="rounded-[2rem] bg-brand-paper p-7">
            <h3 className="font-serif text-2xl text-brand-charcoal">
              {isVi ? "Khung Giờ Tiếp Đón & Vận Hành" : "Sanctuary Operating Hours"}
            </h3>
            <dl className="mt-4 space-y-2.5 text-sm text-brand-ink/70">
              <div className="flex justify-between">
                <dt>{isVi ? "Tư vấn Đặt phòng & Sự kiện" : "Reservations Desk"}</dt>
                <dd className="font-medium text-brand-charcoal">07:00 – 22:00</dd>
              </div>
              <div className="flex justify-between">
                <dt>{isVi ? "Đưa đón Sân bay Liên Khương" : "Airport Chauffeur"}</dt>
                <dd className="font-medium text-brand-charcoal">Theo giờ bay thực tế</dd>
              </div>
              <div className="flex justify-between border-t border-brand-stone/50 pt-2.5">
                <dt className="font-semibold text-brand-forest">
                  {isVi ? "Quản gia Nội khu & An ninh" : "In-Villa Butler & Security"}
                </dt>
                <dd className="font-semibold text-brand-forest">24/7 Trực chiến</dd>
              </div>
            </dl>
          </div>

          <div className="rounded-[2rem] bg-brand-paper p-7">
            <h3 className="font-serif text-2xl text-brand-charcoal">
              {isVi ? "Cần Trợ Lý AI Tư Vấn Ngay?" : "Need Instant Villa Matching?"}
            </h3>
            <p className="mt-2 text-sm leading-relaxed text-brand-ink/65">
              {isVi
                ? "Bấm vào biểu tượng Trợ lý AI ở góc phải bên dưới màn hình hoặc tra cứu Cẩm nang Câu hỏi thường gặp."
                : "Use our live AI Concierge widget at the bottom-right corner or browse our pre-arrival FAQ."}
            </p>
            <Link
              href="/faq"
              className="mt-4 inline-flex rounded-full border border-brand-forest px-5 py-2 text-xs font-semibold uppercase tracking-wider text-brand-forest transition hover:bg-brand-forest hover:text-white"
            >
              {isVi ? "Xem Câu Hỏi Thường Gặp →" : "Explore FAQ →"}
            </Link>
          </div>
        </div>

        {/* Right: Interactive Concierge Request Composer */}
        <form
          onSubmit={submit}
          className="space-y-6 rounded-[2.2rem] bg-brand-paper p-7 shadow-[0_24px_80px_rgba(32,52,43,0.08)] sm:p-9"
        >
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-brand-sage">
              ✨ {isVi ? "Mẫu Yêu Cầu Nhanh 1-Chạm" : "1-Click Concierge Request Templates"}
            </p>
            <div className="mt-3 flex flex-wrap gap-2">
              {CONCIERGE_PRESETS.map((preset) => (
                <button
                  key={preset.id}
                  type="button"
                  onClick={() => applyPreset(preset)}
                  className="rounded-full border border-brand-stone bg-brand-white px-3.5 py-2 text-xs font-semibold text-brand-charcoal transition hover:border-brand-forest hover:bg-brand-forest hover:text-white"
                >
                  {isVi ? preset.labelVi : preset.labelEn}
                </button>
              ))}
            </div>
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <label className="grid gap-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-brand-charcoal">
                {isVi ? "Họ và Tên Quý Khách" : "Full Name"} <span className="text-brand-coral">*</span>
              </span>
              <input
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={isVi ? "Nguyễn Văn A" : "Your full name"}
                className="h-13 rounded-full border border-brand-stone bg-brand-white px-5 text-sm outline-none focus:border-brand-forest"
              />
            </label>
            <label className="grid gap-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-brand-charcoal">
                Email <span className="text-brand-coral">*</span>
              </span>
              <input
                required
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="vip@domain.com"
                className="h-13 rounded-full border border-brand-stone bg-brand-white px-5 text-sm outline-none focus:border-brand-forest"
              />
            </label>
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <label className="grid gap-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-brand-charcoal">
                {isVi ? "Số Điện Thoại / WhatsApp / Zalo" : "Phone / WhatsApp"}
              </span>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="090..."
                className="h-13 rounded-full border border-brand-stone bg-brand-white px-5 text-sm outline-none focus:border-brand-forest"
              />
            </label>
            <label className="grid gap-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-brand-charcoal">
                {isVi ? "Chuyên Mục Yêu Cầu" : "Inquiry Category"}
              </span>
              <select
                value={type}
                onChange={(e) => setType(e.target.value)}
                className="h-13 rounded-full border border-brand-stone bg-brand-white px-5 text-sm outline-none focus:border-brand-forest"
              >
                {contactTypes.map((item) => (
                  <option key={item.value} value={item.value}>
                    {item.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <label className="grid gap-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-brand-charcoal">
              {isVi ? "Tiêu Đề Yêu Cầu" : "Subject"} <span className="text-brand-coral">*</span>
            </span>
            <input
              required
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              placeholder={isVi ? "Ví dụ: Đặt xe đón sân bay / Tiệc tối kỷ niệm" : "Subject of your request"}
              className="h-13 rounded-full border border-brand-stone bg-brand-white px-5 text-sm outline-none focus:border-brand-forest"
            />
          </label>

          <label className="grid gap-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-brand-charcoal">
              {isVi ? "Chi Tiết Mong Muốn Của Quý Khách" : "Bespoke Notes & Preferences"}{" "}
              <span className="text-brand-coral">*</span>
            </span>
            <textarea
              required
              minLength={5}
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder={
                isVi
                  ? "Chia sẻ thời gian dự kiến, số lượng khách hoặc bất kỳ yêu cầu cá nhân hóa nào..."
                  : "Share your preferred dates, guest count, or bespoke arrangements..."
              }
              className="min-h-36 rounded-[1.5rem] border border-brand-stone bg-brand-white p-5 text-sm outline-none focus:border-brand-forest"
            />
          </label>

          {errorMsg && (
            <p className="rounded-2xl bg-brand-coral/10 px-5 py-3 text-sm text-brand-coral">{errorMsg}</p>
          )}
          {success && (
            <p className="rounded-2xl bg-emerald-900/10 px-5 py-3 text-sm font-semibold text-brand-forest">
              ✓ {success}
            </p>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="min-h-13 w-full rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.16em] text-brand-white transition hover:bg-brand-forest-deep disabled:opacity-50"
          >
            {submitting
              ? isVi
                ? "Đang chuyển tới Quản gia..."
                : "Sending to Concierge..."
              : isVi
              ? "Gửi Yêu Cầu Tới Trưởng Quản Gia →"
              : "Dispatch to Head Butler →"}
          </button>
        </form>
      </div>
    </ContentShell>
  );
};

