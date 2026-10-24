"use client";
import { useState, useMemo } from "react";
import Link from "next/link";
import { useTranslation } from "../../../shared/i18n/hooks";
import { ContentShell } from "../shared";

type ExhibitionFrame = {
  id: string;
  src: string;
  category: "ARCHITECTURE" | "WATER" | "INTERIOR" | "GASTRONOMY";
  titleVi: string;
  titleEn: string;
  captionVi: string;
  captionEn: string;
  cameraSpec: string;
  villaSlug: string;
  villaName: string;
  spanClass: string;
};

const EXHIBITION_FRAMES: ExhibitionFrame[] = [
  {
    id: "frame-01",
    src: "/images/nhu-hero-villa-4k.jpg",
    category: "ARCHITECTURE",
    titleVi: "Toàn Cảnh Hillside Residence Lúc Hoàng Hôn",
    titleEn: "Hillside Residence at Golden Hour",
    captionVi: "Kiến trúc gỗ tuyết tùng và kính tràn viền phản chiếu trên mặt hồ bơi vô cực ở độ cao 1,560m.",
    captionEn: "Cantilevered cedar timber and frameless glass reflecting across the 1,560m infinity pool.",
    cameraSpec: "35mm · f/2.8 · ISO 100 · 17:18 ICT",
    villaSlug: "hillside-residence-private-pool",
    villaName: "Hillside Residence (HR-03)",
    spanClass: "md:col-span-2 md:row-span-2",
  },
  {
    id: "frame-02",
    src: "/images/nhu-infinity-pool.jpg",
    category: "WATER",
    titleVi: "Hồ Bơi Vô Cực Khoáng Ấm 30°C Giữa Sương Sớm",
    titleEn: "30°C Heated Mineral Infinity Pool in Morning Mist",
    captionVi: "Làn hơi ấm bốc lên từ mặt nước hồ bơi hòa cùng sương mù Hồ Tuyền Lâm lúc 6:30 sáng.",
    captionEn: "Thermal mist rising from the infinity edge overlooking Tuyen Lam pine valley.",
    cameraSpec: "50mm · f/2.0 · ISO 160 · 06:35 ICT",
    villaSlug: "water-courtyard-villa",
    villaName: "Water Courtyard Villa (WC-05)",
    spanClass: "md:col-span-1 md:row-span-1",
  },
  {
    id: "frame-03",
    src: "/images/nhu-villa-interior.jpg",
    category: "INTERIOR",
    titleVi: "Không Gian Phòng Ngủ Master Ngập Tràn Ánh Sáng",
    titleEn: "Sun-Drenched Master Sanctuary Suite",
    captionVi: "Vải lanh hữu cơ, gỗ thông tự nhiên và tầm nhìn 180 độ thẳng ra tán thông cổ thụ.",
    captionEn: "Organic Belgian linen, warm highland timber, and 180-degree forest canopy views.",
    cameraSpec: "24mm · f/4.0 · ISO 200 · 09:15 ICT",
    villaSlug: "sanctuary-pool-villa",
    villaName: "Sanctuary Pool Villa (PV-02)",
    spanClass: "md:col-span-1 md:row-span-1",
  },
  {
    id: "frame-04",
    src: "/images/nhu-private-dining.jpg",
    category: "GASTRONOMY",
    titleVi: "Bàn Tiệc Tối Ánh Nến Bên Hồ Bơi Riêng",
    titleEn: "Candlelit Poolside Chef's Table",
    captionVi: "Thực đơn Fine Dining 6 món được chuẩn bị riêng tại sân hiên biệt thự dưới bầu trời sao Đà Lạt.",
    captionEn: "Bespoke six-course tasting menu served beside your private heated pool.",
    cameraSpec: "50mm · f/1.4 · ISO 400 · 19:45 ICT",
    villaSlug: "garden-pool-villa-da-lat",
    villaName: "Garden Pool Villa (GV-01)",
    spanClass: "md:col-span-1 md:row-span-1",
  },
  {
    id: "frame-05",
    src: "/images/nhu-garden-courtyard.jpg",
    category: "ARCHITECTURE",
    titleVi: "Khoảng Sân Thiền & Vườn Nhiệt Đới Biệt Lập",
    titleEn: "Secluded Zen Courtyard & Botanical Garden",
    captionVi: "Lối đi lát đá bazan dẫn qua khu vườn dương xỉ và thông cổ thụ tĩnh lặng tuyệt đối.",
    captionEn: "Hand-cut basalt stepping stones winding through native highland ferns and pines.",
    cameraSpec: "28mm · f/3.2 · ISO 125 · 15:40 ICT",
    villaSlug: "forest-pavilion",
    villaName: "Forest Pavilion (FP-06)",
    spanClass: "md:col-span-1 md:row-span-1",
  },
  {
    id: "frame-06",
    src: "/images/nhu-location-landscape.jpg",
    category: "WATER",
    titleVi: "Bán Đảo Thông Nguyên Sinh Ven Hồ Tuyền Lâm",
    titleEn: "Private Pine Peninsula on Tuyen Lam Lake",
    captionVi: "Mật độ xây dựng chỉ 12%, bảo tồn trọn vẹn hệ sinh thái rừng thông và mặt nước tự nhiên.",
    captionEn: "Preserving 88% untouched forest and lakefront shoreline for complete solitude.",
    cameraSpec: "70mm · f/4.0 · ISO 100 · 07:10 ICT",
    villaSlug: "lakefront-family-estate",
    villaName: "Lakefront Estate (LE-04)",
    spanClass: "md:col-span-1 md:row-span-1",
  },
];

export const GalleryPage = () => {
  const { lang } = useTranslation();
  const isVi = lang === "vi";

  const [activeCategory, setActiveCategory] = useState<string>("ALL");
  const [lightboxFrame, setLightboxFrame] = useState<ExhibitionFrame | null>(null);

  const filteredFrames = useMemo(() => {
    if (activeCategory === "ALL") return EXHIBITION_FRAMES;
    return EXHIBITION_FRAMES.filter((f) => f.category === activeCategory);
  }, [activeCategory]);

  return (
    <ContentShell
      eyebrow={isVi ? "Triển Lãm Thị Giác & Kiến Trúc" : "Visual Monograph & Lookbook"}
      title={isVi ? "Ánh Sáng, Mặt Nước & Chất Cảm." : "Light, water, texture."}
      copy={
        isVi
          ? "Chạm vào từng khung hình để thưởng lãm chi tiết kiến trúc, thông số nhiếp ảnh và khám phá trực tiếp căn biệt thự xuất hiện trong tác phẩm."
          : "Click any plate in our architectural lookbook to inspect full-resolution details and explore the featured residence."
      }
    >
      {/* Exhibition Filter Bar */}
      <div className="mb-10 flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-wrap gap-2">
          {[
            { id: "ALL", label: isVi ? "✦ Toàn Bộ Triển Lãm (06)" : "✦ Complete Monograph (06)" },
            { id: "ARCHITECTURE", label: isVi ? "🏛️ Kiến Trúc & Đồi Thông" : "🏛️ Architecture" },
            { id: "WATER", label: isVi ? "🌊 Hồ Vô Cực & Mặt Hồ" : "🌊 Water & Mist" },
            { id: "INTERIOR", label: isVi ? "🛏️ Nội Thất Thủ Công" : "🛏️ Interiors" },
            { id: "GASTRONOMY", label: isVi ? "🍷 Ẩm Thực Biệt Thự" : "🍷 Gastronomy" },
          ].map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveCategory(tab.id)}
              className={`rounded-full px-5 py-2.5 text-xs font-semibold uppercase tracking-wider transition ${
                activeCategory === tab.id
                  ? "bg-brand-forest text-white shadow-sm"
                  : "bg-brand-paper text-brand-charcoal hover:bg-brand-white"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Asymmetric Editorial Bento Lookbook */}
      <div className="grid gap-6 md:grid-cols-3">
        {filteredFrames.map((frame, index) => (
          <div
            key={frame.id}
            onClick={() => setLightboxFrame(frame)}
            className={`group relative cursor-pointer overflow-hidden rounded-[2rem] bg-brand-forest-deep shadow-[0_22px_70px_rgba(32,52,43,0.12)] ${
              activeCategory === "ALL" ? frame.spanClass : ""
            }`}
          >
            <img
              src={frame.src}
              alt={isVi ? frame.titleVi : frame.titleEn}
              className="h-full min-h-[280px] w-full object-cover transition duration-700 group-hover:scale-105"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent opacity-85 transition group-hover:opacity-95" />

            <div className="absolute left-4 top-4 flex items-center gap-2">
              <span className="rounded-full bg-black/55 px-3 py-1 font-mono text-[0.65rem] font-bold text-amber-300 backdrop-blur-md">
                PLATE 0{index + 1}
              </span>
              <span className="rounded-full bg-white/15 px-3 py-1 text-[0.62rem] font-semibold uppercase tracking-widest text-white backdrop-blur-md">
                {frame.category}
              </span>
            </div>

            <div className="absolute bottom-5 left-5 right-5 text-white">
              <p className="font-mono text-[0.65rem] text-brand-paper/75">{frame.cameraSpec}</p>
              <h2 className="mt-1 font-serif text-2xl text-white sm:text-3xl">
                {isVi ? frame.titleVi : frame.titleEn}
              </h2>
              <p className="mt-1.5 line-clamp-2 text-xs leading-relaxed text-brand-paper/80">
                {isVi ? frame.captionVi : frame.captionEn}
              </p>
              <div className="mt-3 flex items-center justify-between border-t border-white/15 pt-3 text-xs">
                <span className="font-semibold text-amber-300">🏡 {frame.villaName}</span>
                <span className="uppercase tracking-widest text-white/90 group-hover:underline">
                  {isVi ? "Phóng to & Đặt phòng →" : "Inspect Plate →"}
                </span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Full-Screen Curated Lightbox Modal */}
      {lightboxFrame && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 p-4 backdrop-blur-md sm:p-8"
          onClick={() => setLightboxFrame(null)}
        >
          <div
            className="relative max-h-[90vh] w-full max-w-5xl overflow-hidden rounded-[2.2rem] bg-brand-forest-deep text-white shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              onClick={() => setLightboxFrame(null)}
              className="absolute right-4 top-4 z-10 flex h-10 w-10 items-center justify-center rounded-full bg-black/60 text-sm text-white hover:bg-black"
            >
              ✕
            </button>

            <div className="grid lg:grid-cols-[1.35fr_0.65fr]">
              <div className="bg-black">
                <img
                  src={lightboxFrame.src}
                  alt={isVi ? lightboxFrame.titleVi : lightboxFrame.titleEn}
                  className="max-h-[72vh] w-full object-cover"
                />
              </div>
              <div className="flex flex-col justify-between p-6 sm:p-8">
                <div>
                  <span className="font-mono text-xs uppercase tracking-widest text-amber-300">
                    {lightboxFrame.cameraSpec}
                  </span>
                  <h3 className="mt-3 font-serif text-3xl text-white">
                    {isVi ? lightboxFrame.titleVi : lightboxFrame.titleEn}
                  </h3>
                  <p className="mt-4 text-sm leading-7 text-brand-paper/80">
                    {isVi ? lightboxFrame.captionVi : lightboxFrame.captionEn}
                  </p>
                </div>

                <div className="mt-8 space-y-3 border-t border-white/15 pt-5">
                  <p className="text-xs uppercase tracking-widest text-brand-sage-light">
                    {isVi ? "Biệt Thự Trong Tác Phẩm" : "Featured Sanctuary"}
                  </p>
                  <p className="font-serif text-2xl text-amber-300">{lightboxFrame.villaName}</p>
                  <Link
                    href={`/villas/${lightboxFrame.villaSlug}`}
                    className="mt-3 inline-flex min-h-12 w-full items-center justify-center rounded-full bg-brand-paper px-6 text-xs font-bold uppercase tracking-[0.15em] text-brand-forest-deep transition hover:bg-white"
                  >
                    {isVi ? "Khám Phá & Đặt Biệt Thự Này →" : "Explore & Book This Villa →"}
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </ContentShell>
  );
};
