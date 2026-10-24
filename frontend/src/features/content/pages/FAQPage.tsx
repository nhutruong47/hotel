"use client";
import { useState, useMemo } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api, API_PATHS } from "../../../shared/api/client";
import { useTranslation } from "../../../shared/i18n/hooks";
import { ContentShell } from "../shared";

interface FaqItem {
  id: number;
  question: string;
  answer: string;
  category: string;
  helpfulCount?: number;
  notHelpfulCount?: number;
  displayOrder: number;
  isActive: boolean;
}

const FALLBACK_FAQS: FaqItem[] = [
  {
    id: 901,
    category: "BOOKING",
    displayOrder: 1,
    isActive: true,
    helpfulCount: 42,
    question: "Giờ nhận phòng (Check-in) và trả phòng (Check-out) tại Nhu Villas là mấy giờ?",
    answer:
      "Giờ nhận biệt thự tiêu chuẩn là 14:00 và trả biệt thự là 12:00 trưa. Với khách đặt trực tiếp trên website hoặc dùng mã NHUVILLAS20, Quản gia hỗ trợ nhận phòng sớm từ 11:00 hoặc trả phòng muộn tới 15:00 miễn phí (tùy tình trạng trống thực tế).",
  },
  {
    id: 902,
    category: "FACILITIES",
    displayOrder: 2,
    isActive: true,
    helpfulCount: 38,
    question: "Hồ bơi riêng tại các biệt thự có hệ thống nước ấm không?",
    answer:
      "Có. Toàn bộ 6 biệt thự tại Nhu Villas đều được trang bị hệ thống gia nhiệt hồ bơi tự động duy trì nhiệt độ nước từ 29°C – 31°C suốt 24/7, giúp Quý khách thoải mái bơi lội giữa tiết trời se lạnh của Đà Lạt.",
  },
  {
    id: 903,
    category: "SERVICES",
    displayOrder: 3,
    isActive: true,
    helpfulCount: 35,
    question: "Giá phòng đã bao gồm bữa sáng và dịch vụ Quản gia chưa?",
    answer:
      "Mọi đặt phòng tại Nhu Villas đều đã bao gồm: Bữa sáng nghệ thuật phục vụ tại biệt thự (khay nổi hồ bơi hoặc bàn tiệc sân hiên), Trà chiều thảo mộc Đà Lạt, Minibar tiêu chuẩn ngày đầu và Quản gia riêng chăm sóc 24/7.",
  },
  {
    id: 904,
    category: "TRANSPORT",
    displayOrder: 4,
    isActive: true,
    helpfulCount: 29,
    question: "Di chuyển từ Sân bay Liên Khương (DLI) về khu biệt thự mất bao lâu?",
    answer:
      "Nhu Villas cách Sân bay Liên Khương khoảng 28km (tương đương 30 phút di chuyển qua cao tốc Liên Khương – Prenn). Quý khách có thể tích chọn dịch vụ Xe riêng đón sân bay ngay ở bước Đặt phòng.",
  },
  {
    id: 905,
    category: "CANCELLATION",
    displayOrder: 5,
    isActive: true,
    helpfulCount: 31,
    question: "Chính sách hoàn hủy và đổi ngày lưu trú như thế nào?",
    answer:
      "Quý khách được miễn phí đổi ngày lưu trú hoặc hủy phòng hoàn tiền 100% khi thông báo trước ngày Check-in tối thiểu 7 ngày (đối với ngày thường) hoặc 14 ngày (đối với dịp Lễ/Tết).",
  },
];

export const FAQPage = () => {
  const { lang } = useTranslation();
  const isVi = lang === "vi";

  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState<string>("all");
  const [openFaqId, setOpenFaqId] = useState<number | null>(901);

  const { data: apiFaqs = [], isLoading } = useQuery({
    queryKey: ["faqs"],
    queryFn: () => api.get<FaqItem[]>(API_PATHS.faqs.all),
    staleTime: 60_000,
  });

  const faqs = useMemo(() => {
    if (apiFaqs.length === 0) return FALLBACK_FAQS;
    return [...apiFaqs, ...FALLBACK_FAQS];
  }, [apiFaqs]);

  const categoryLabels: Record<string, string> = {
    BOOKING: isVi ? "Đặt phòng" : "Booking",
    PAYMENT: isVi ? "Thanh toán" : "Payment",
    CANCELLATION: isVi ? "Hoàn & Đổi lịch" : "Cancellation",
    SERVICES: isVi ? "Dịch vụ Quản gia" : "Butler Services",
    FACILITIES: isVi ? "Hồ bơi & Tiện ích" : "Heated Pool & Amenities",
    TRANSPORT: isVi ? "Đưa đón Sân bay" : "Airport Transfer",
    POLICIES: isVi ? "Nội quy lưu trú" : "Sanctuary Policies",
  };

  const filteredFaqs = useMemo(() => {
    return searchQuery
      ? faqs.filter(
          (f: FaqItem) =>
            f.question.toLowerCase().includes(searchQuery.toLowerCase()) ||
            f.answer.toLowerCase().includes(searchQuery.toLowerCase())
        )
      : selectedCategory === "all"
      ? faqs
      : faqs.filter((f: FaqItem) => f.category === selectedCategory);
  }, [faqs, searchQuery, selectedCategory]);

  const markHelpful = async (id: number, helpful: boolean) => {
    try {
      await api.post(`${API_PATHS.faqs.helpful(id)}?helpful=${helpful}`);
    } catch {
      // ignore on fallback items
    }
  };

  return (
    <ContentShell
      eyebrow={isVi ? "Cẩm Nang Trước Kỳ Nghỉ" : "Pre-Arrival Dossier & FAQ"}
      title={isVi ? "Mọi Điều Cần Biết Trước Khi Đến." : "Answers before arrival."}
      copy={
        isVi
          ? "Chính sách minh bạch, hướng dẫn thời tiết Đà Lạt và thông tin chi tiết về dịch vụ Quản gia giúp Quý khách hoàn toàn an tâm trước chuyến đi."
          : "Transparent stay policies, highland climate guidance, and butler service details for an effortless arrival."
      }
    >
      {/* Pre-Arrival Quick Blueprint Cards */}
      <div className="mb-12 grid gap-5 md:grid-cols-3">
        <div className="rounded-[1.75rem] bg-brand-forest-deep p-6 text-white">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-brand-sage-light">
            🕒 {isVi ? "Khung Giờ Nhận & Trả Biệt Thự" : "Check-In & Check-Out"}
          </p>
          <p className="mt-3 font-serif text-2xl text-white">14:00 Check-in · 12:00 Check-out</p>
          <p className="mt-2 text-xs leading-relaxed text-brand-paper/75">
            {isVi
              ? "Hỗ trợ nhận phòng sớm miễn phí khi đặt trực tiếp kèm mã ưu đãi thành viên."
              : "Complimentary early check-in subject to villa availability for direct bookings."}
          </p>
        </div>

        <div className="rounded-[1.75rem] bg-brand-paper p-6">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-brand-sage">
            🌡️ {isVi ? "Khí Hậu Đồi Thông & Hồ Bơi" : "Highland Micro-Climate"}
          </p>
          <p className="mt-3 font-serif text-2xl text-brand-charcoal">
            {isVi ? "Không khí 16–23°C · Hồ bơi 30°C" : "Air 16–23°C · Heated Pool 30°C"}
          </p>
          <p className="mt-2 text-xs leading-relaxed text-brand-ink/65">
            {isVi
              ? "Biệt thự có sẵn áo choàng tắm lông cừu ấm, lò sưởi và hồ bơi khoáng gia nhiệt 24/7."
              : "Plush bathrobes, fireplace warmth, and 24/7 heated mineral pools included."}
          </p>
        </div>

        <div className="rounded-[1.75rem] bg-brand-paper p-6">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-brand-sage">
            🛡️ {isVi ? "Cam Kết Đặt Phòng Trực Tiếp" : "Direct Booking Guarantee"}
          </p>
          <p className="mt-3 font-serif text-2xl text-brand-charcoal">
            {isVi ? "Đổi lịch linh hoạt · Giá tốt nhất" : "Flexible Dates · Best Tariff"}
          </p>
          <p className="mt-2 text-xs leading-relaxed text-brand-ink/65">
            {isVi
              ? "Miễn phí đổi ngày lưu trú trước 7 ngày và tặng kèm trà chiều tại biệt thự."
              : "Free date modification up to 7 days prior to arrival plus complimentary high tea."}
          </p>
        </div>
      </div>

      {/* Search */}
      <div className="mb-8">
        <div className="relative">
          <input
            type="search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder={
              isVi
                ? "Tìm câu hỏi (ví dụ: hồ bơi nước ấm, sân bay, bữa sáng, hoàn tiền)..."
                : "Search questions (e.g. heated pool, airport, breakfast)..."
            }
            className="h-14 w-full rounded-full border border-brand-stone bg-brand-paper px-6 pl-14 text-sm focus:border-brand-forest focus:outline-none"
          />
          <span className="absolute left-5 top-1/2 -translate-y-1/2 text-brand-ink/40">🔍</span>
        </div>
      </div>

      {/* Category Pills */}
      <div className="mb-8 flex flex-wrap gap-2">
        <button
          onClick={() => setSelectedCategory("all")}
          className={`rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-wider transition ${
            selectedCategory === "all"
              ? "bg-brand-forest text-brand-white"
              : "bg-brand-paper text-brand-ink/62 hover:bg-brand-forest/10"
          }`}
        >
          {isVi ? "Tất cả" : "All"}
        </button>
        {Object.entries(categoryLabels).map(([key, label]) => (
          <button
            key={key}
            onClick={() => setSelectedCategory(key)}
            className={`rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-wider transition ${
              selectedCategory === key
                ? "bg-brand-forest text-brand-white"
                : "bg-brand-paper text-brand-ink/62 hover:bg-brand-forest/10"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {/* FAQ List */}
      {isLoading ? (
        <div className="space-y-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="rounded-[1.5rem] bg-brand-paper p-6">
              <div className="h-6 w-3/4 animate-pulse rounded-full bg-brand-stone" />
              <div className="mt-3 h-4 w-full animate-pulse rounded-full bg-brand-stone" />
            </div>
          ))}
        </div>
      ) : filteredFaqs.length > 0 ? (
        <div className="space-y-4">
          {filteredFaqs.map((faq: FaqItem) => (
            <details
              key={faq.id}
              open={openFaqId === faq.id}
              onToggle={() => setOpenFaqId(openFaqId === faq.id ? null : faq.id)}
              className="group rounded-[1.5rem] bg-brand-paper shadow-[0_12px_40px_rgba(32,52,43,0.05)]"
            >
              <summary className="flex cursor-pointer items-center justify-between p-6 font-serif text-2xl text-brand-charcoal marker:content-['']">
                <span>{faq.question}</span>
                <span className="ml-4 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-sand text-sm text-brand-forest">
                  {openFaqId === faq.id ? "−" : "+"}
                </span>
              </summary>
              <div className="border-t border-brand-stone/40 px-6 pb-6 pt-4">
                <p className="text-sm leading-7 text-brand-ink/75 sm:text-base">{faq.answer}</p>
                <div className="mt-4 flex items-center gap-4">
                  <span className="text-xs text-brand-ink/50">
                    {isVi ? "Câu trả lời này có hữu ích?" : "Was this helpful?"}
                  </span>
                  <button
                    type="button"
                    onClick={() => markHelpful(faq.id, true)}
                    className="flex items-center gap-1 rounded-full bg-brand-sage/20 px-3 py-1 text-xs text-brand-forest hover:bg-brand-sage/30"
                  >
                    👍 {faq.helpfulCount || 12}
                  </button>
                </div>
              </div>
            </details>
          ))}
        </div>
      ) : (
        <div className="rounded-[2rem] bg-brand-paper p-16 text-center">
          <h3 className="text-3xl text-brand-charcoal">
            {isVi ? "Không tìm thấy câu hỏi phù hợp" : "No matching questions found"}
          </h3>
        </div>
      )}

      {/* Contact CTA */}
      <div className="mt-12 rounded-[2rem] bg-brand-forest-deep p-8 text-center text-brand-white">
        <h3 className="font-serif text-3xl text-brand-white">
          {isVi ? "Quý Khách Có Yêu Cầu Đặc Biệt Khác?" : "Still have bespoke questions?"}
        </h3>
        <p className="mt-2 text-brand-paper/75">
          {isVi
            ? "Đội ngũ Quản gia Nhu Villas sẵn sàng tư vấn trực tiếp cho lịch trình riêng của bạn."
            : "Our Head Butler team is delighted to tailor every detail of your stay."}
        </p>
        <Link
          href="/contact"
          className="mt-6 inline-flex min-h-12 items-center justify-center rounded-full bg-brand-white px-8 py-4 text-xs font-semibold uppercase tracking-[0.16em] text-brand-forest-deep transition hover:bg-brand-paper"
        >
          {isVi ? "Trò Chuyện Cùng Quản Gia →" : "Contact Our Butler Team →"}
        </Link>
      </div>
    </ContentShell>
  );
};
