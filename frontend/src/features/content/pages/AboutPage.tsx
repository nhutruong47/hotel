"use client";
import { useState } from "react";
import Link from "next/link";
import { useTranslation } from "../../../shared/i18n/hooks";
import { ContentShell } from "../shared";

const MATERIAL_SPECIMENS = [
  {
    id: "basalt",
    badge: "01 · HIGHLAND STONE",
    titleVi: "Đá Bazan Xẻ Thô Tây Nguyên",
    titleEn: "Hand-Cut Highland Basalt",
    originVi: "Khai thác bền vững tại Bảo Lộc & Di Linh",
    originEn: "Sustainably quarried in Bao Loc & Di Linh",
    descVi:
      "Bề mặt đá xẻ thô giữ nhiệt tự nhiên vào ban ngày và tỏa hơi ấm dịu nhẹ quanh khu vực hồ bơi vô cực vào ban đêm khi nhiệt độ Đà Lạt xuống 16°C.",
    descEn:
      "Raw-split volcanic basalt retains daytime solar warmth and provides slip-resistant tactile grounding around our heated infinity pools.",
    image: "/images/nhu-infinity-pool.jpg",
  },
  {
    id: "cedar",
    badge: "02 · TIMBER CRAFT",
    titleVi: "Gỗ Tuyết Tùng & Thông Đỏ Tái Sinh",
    titleEn: "Reclaimed Cedar & Highland Pine",
    originVi: "Xử lý thủ công chống ẩm sương mù ở độ cao 1,500m",
    originEn: "Hand-treated for 1,500m highland mist resilience",
    descVi:
      "Hệ khung dầm và trần gỗ cao 4.8 mét tỏa hương tinh dầu gỗ tự nhiên dịu nhẹ, giúp nhịp tim chậm lại ngay khi bước qua hiên biệt thự.",
    descEn:
      "4.8-meter timber ceilings release subtle phytoncide aromatics, lowering cortisol and framing panoramic pine forest views.",
    image: "/images/nhu-villa-interior.jpg",
  },
  {
    id: "thermal",
    badge: "03 · HYDRO-ENGINEERING",
    titleVi: "Hệ Thống Hồ Bơi Khoáng Ấm 30°C",
    titleEn: "30°C Mineral Hydro-Thermal Pools",
    originVi: "Lọc muối khoáng sinh học không mùi Clo",
    originEn: "Chlorine-free bio-mineral ionization",
    descVi:
      "Mỗi biệt thự sở hữu hồ bơi riêng được gia nhiệt bằng năng lượng mặt trời và bơm nhiệt biến tần, cho phép bơi lội giữa làn sương mù Đà Lạt suốt bốn mùa.",
    descEn:
      "Heated via solar-assisted heat pumps so guests can swim comfortably amidst misty highland mornings year-round.",
    image: "/images/nhu-hero-villa-4k.jpg",
  },
];

export const AboutPage = () => {
  const { lang } = useTranslation();
  const isVi = lang === "vi";
  const [activeMaterial, setActiveMaterial] = useState(MATERIAL_SPECIMENS[0]);

  const timeline = [
    {
      year: "2018",
      titleVi: "Khởi Nguyên Trên Đồi Thông",
      titleEn: "Sanctuary Conception",
      eventVi: "Chọn vùng bán đảo biệt lập ven Hồ Tuyền Lâm với mật độ xây dựng chỉ 12%.",
      eventEn: "Selected a secluded peninsula on Tuyen Lam Lake with a strict 12% building footprint.",
    },
    {
      year: "2020",
      titleVi: "Khai Trương Bộ Sưu Tập Đầu Tiên",
      titleEn: "First Villa Unveiling",
      eventVi: "Ra mắt Garden Pool Villa (GV-01) và Sanctuary Pool Villa (PV-02) với hồ bơi nước ấm riêng.",
      eventEn: "Debuted Garden Pool Villa and Sanctuary Pool Villa with private heated pools.",
    },
    {
      year: "2023",
      titleVi: "Hoàn Thiện Tuyệt Tác Đỉnh Đồi & Mặt Hồ",
      titleEn: "Estate Masterpiece Expansion",
      eventVi: "Khánh thành Hillside Residence (HR-03) và Lakefront Estate (LE-04) cùng dịch vụ Quản gia 24/7.",
      eventEn: "Opened Hillside Residence and Lakefront Estate with dedicated 24/7 Butler service.",
    },
    {
      year: "2026",
      titleVi: "Biểu Tượng Nghỉ Dưỡng Độc Bản Đà Lạt",
      titleEn: "Avant-Garde Highland Sanctuary",
      eventVi: "Đạt chuẩn 6 biệt thự nghệ thuật cùng hệ thống AI Concierge & Đặt phòng trực quan thế hệ mới.",
      eventEn: "Crowned Top Boutique Villa Sanctuary with 6 architectural residences and AI Concierge.",
    },
  ];

  const awards = [
    { year: "2023", title: "Best Highland Boutique Villa", org: "Travel + Leisure Southeast Asia" },
    { year: "2024", title: "Green Architecture Distinction", org: "Sustainable Luxury Awards" },
    { year: "2025", title: "Top 10 Private Pool Retreats", org: "Condé Nast Traveler" },
    { year: "2026", title: "9.9/10 Guest Satisfaction", org: "Direct Sanctuary Index" },
  ];

  return (
    <ContentShell
      eyebrow={isVi ? "Câu Chuyện & Triết Lý Kiến Trúc" : "Architecture & Ethos"}
      title={isVi ? "Nghệ Thuật Nghỉ Dưỡng Tĩnh Lặng." : "Private hospitality, shaped slowly."}
      copy={
        isVi
          ? "Nhu Villas được kiến tạo như một ốc đảo ẩn mình giữa rừng thông nguyên sinh và mặt nước Hồ Tuyền Lâm — nơi kiến trúc nhường lời cho thiên nhiên."
          : "Nhu Villas is a direct sanctuary experience built around calm architecture, pine forest solitude, and bespoke butler service."
      }
    >
      {/* 1. Architectural Manifesto Numbers */}
      <section className="mb-16 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
        {[
          { num: "12%", labelVi: "Mật độ xây dựng toàn khu", labelEn: "Estate Building Footprint" },
          { num: "1,520m", labelVi: "Độ cao trung bình trên mực nước biển", labelEn: "Highland Elevation" },
          { num: "06", labelVi: "Biệt thự độc bản có hồ bơi ấm riêng", labelEn: "Private Heated Pool Villas" },
          { num: "1 : 1", labelVi: "Tỷ lệ Quản gia chăm sóc từng căn", labelEn: "Dedicated Villa Butler Ratio" },
        ].map((stat) => (
          <div
            key={stat.num}
            className="rounded-[2rem] bg-brand-paper p-7 shadow-[0_18px_60px_rgba(32,52,43,0.06)]"
          >
            <p className="font-serif text-5xl text-brand-forest">{stat.num}</p>
            <p className="mt-3 text-xs font-semibold uppercase tracking-[0.16em] text-brand-ink/65">
              {isVi ? stat.labelVi : stat.labelEn}
            </p>
          </div>
        ))}
      </section>

      {/* 2. Interactive Materiality & Craft Lab */}
      <section className="mb-16 rounded-[2.4rem] bg-brand-forest-deep p-6 text-white shadow-[0_28px_90px_rgba(12,28,22,0.24)] sm:p-10">
        <div className="flex flex-wrap items-end justify-between gap-4 border-b border-white/15 pb-6">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage-light">
              🏛️ {isVi ? "Phòng Thí Nghiệm Vật Liệu Kiến Trúc" : "Architectural Materiality Lab"}
            </p>
            <h2 className="mt-2 font-serif text-3xl text-white sm:text-5xl">
              {isVi ? "Đá, Gỗ, Nước & Ánh Sáng Cao Nguyên" : "Tactile Elements of the Sanctuary"}
            </h2>
          </div>
          <div className="flex flex-wrap gap-2">
            {MATERIAL_SPECIMENS.map((m) => (
              <button
                key={m.id}
                type="button"
                onClick={() => setActiveMaterial(m)}
                className={`rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-wider transition ${
                  activeMaterial.id === m.id
                    ? "bg-amber-300 text-brand-forest-deep"
                    : "bg-white/10 text-white hover:bg-white/20"
                }`}
              >
                {m.badge.split("·")[1]}
              </button>
            ))}
          </div>
        </div>

        <div className="mt-8 grid items-center gap-8 lg:grid-cols-[1.05fr_0.95fr]">
          <div className="relative aspect-[16/10] overflow-hidden rounded-[1.75rem]">
            <img
              src={activeMaterial.image}
              alt={isVi ? activeMaterial.titleVi : activeMaterial.titleEn}
              className="h-full w-full object-cover transition duration-700"
            />
            <span className="absolute left-4 top-4 rounded-full bg-black/60 px-3.5 py-1.5 text-[0.65rem] font-bold uppercase tracking-widest text-amber-300 backdrop-blur-md">
              {activeMaterial.badge}
            </span>
          </div>

          <div className="space-y-5">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-amber-300">
              {isVi ? activeMaterial.originVi : activeMaterial.originEn}
            </p>
            <h3 className="font-serif text-3xl text-white sm:text-4xl">
              {isVi ? activeMaterial.titleVi : activeMaterial.titleEn}
            </h3>
            <p className="text-base leading-8 text-brand-paper/80">
              {isVi ? activeMaterial.descVi : activeMaterial.descEn}
            </p>
            <div className="pt-2">
              <Link
                href="/villas"
                className="inline-flex rounded-full bg-brand-paper px-6 py-3.5 text-xs font-semibold uppercase tracking-[0.16em] text-brand-forest-deep transition hover:bg-white"
              >
                {isVi ? "Khám Phá Kiến Trúc Từng Căn →" : "Explore the Residences →"}
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* 3. Journey Timeline */}
      <section className="mb-16">
        <h2 className="mb-8 font-serif text-4xl text-brand-charcoal">
          {isVi ? "Hành Trình Kiến Tạo" : "Our Journey"}
        </h2>
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
          {timeline.map((item) => (
            <div
              key={item.year}
              className="flex flex-col justify-between rounded-[2rem] bg-brand-paper p-7 shadow-[0_18px_60px_rgba(32,52,43,0.06)]"
            >
              <div>
                <span className="inline-block rounded-full bg-brand-forest px-3.5 py-1 text-xs font-bold text-white">
                  {item.year}
                </span>
                <h3 className="mt-4 font-serif text-2xl text-brand-charcoal">
                  {isVi ? item.titleVi : item.titleEn}
                </h3>
                <p className="mt-2 text-sm leading-relaxed text-brand-ink/68">
                  {isVi ? item.eventVi : item.eventEn}
                </p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* 4. Recognition */}
      <section className="mb-16">
        <h2 className="mb-8 font-serif text-4xl text-brand-charcoal">
          {isVi ? "Giải Thưởng & Ghi Nhận Quốc Tế" : "Global Recognition"}
        </h2>
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
          {awards.map((award) => (
            <div
              key={`${award.year}-${award.title}`}
              className="rounded-[1.75rem] border border-brand-stone/60 bg-brand-paper p-6 text-center"
            >
              <div className="text-3xl">🏆</div>
              <p className="mt-3 font-serif text-2xl text-brand-charcoal">{award.title}</p>
              <p className="mt-1 text-xs text-brand-ink/60">{award.org}</p>
              <p className="mt-2 text-xs font-bold uppercase tracking-widest text-brand-forest">
                {award.year}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* 5. Location & Coordinates */}
      <section className="rounded-[2.2rem] bg-brand-paper p-8 shadow-[0_22px_70px_rgba(32,52,43,0.08)]">
        <div className="grid items-center gap-8 lg:grid-cols-2">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-brand-sage">
              📍 11.9038° N, 108.4368° E · TUYỀN LÂM LAKE
            </p>
            <h2 className="mt-2 font-serif text-4xl text-brand-charcoal">
              {isVi ? "Vị Trí Biệt Lập Giữa Đồi Thông Đà Lạt" : "Secluded Highland Coordinates"}
            </h2>
            <p className="mt-4 text-base leading-8 text-brand-ink/74">
              {isVi
                ? "Tọa lạc trên bán đảo thông riêng tư tại Khu du lịch Quốc gia Hồ Tuyền Lâm, Nhu Villas vừa đủ tách biệt để giữ trọn sự tĩnh lặng tuyệt đối, vừa kết nối thuận tiện tới trung tâm Đà Lạt và sân bay Liên Khương."
                : "Nestled on a private pine peninsula overlooking Tuyen Lam Lake, offering effortless access to Lien Khuong Airport and central Da Lat while preserving complete serenity."}
            </p>
            <dl className="mt-6 grid gap-4 sm:grid-cols-3">
              <div className="rounded-2xl bg-brand-sand p-4">
                <dt className="font-serif text-2xl text-brand-forest">30 phút</dt>
                <dd className="mt-1 text-xs uppercase tracking-wider text-brand-ink/60">
                  {isVi ? "Sân bay Liên Khương" : "Lien Khuong Airport"}
                </dd>
              </div>
              <div className="rounded-2xl bg-brand-sand p-4">
                <dt className="font-serif text-2xl text-brand-forest">12 phút</dt>
                <dd className="mt-1 text-xs uppercase tracking-wider text-brand-ink/60">
                  {isVi ? "Trung tâm Đà Lạt" : "Central Da Lat"}
                </dd>
              </div>
              <div className="rounded-2xl bg-brand-sand p-4">
                <dt className="font-serif text-2xl text-brand-forest">5 phút</dt>
                <dd className="mt-1 text-xs uppercase tracking-wider text-brand-ink/60">
                  {isVi ? "Thiền Viện Trúc Lâm" : "Truc Lam Zen Monastery"}
                </dd>
              </div>
            </dl>
          </div>
          <div className="overflow-hidden rounded-[1.75rem]">
            <img
              src="/images/nhu-location-landscape.jpg"
              alt="Tuyen Lam Lake Location"
              className="h-full w-full object-cover"
            />
          </div>
        </div>
      </section>
    </ContentShell>
  );
};
