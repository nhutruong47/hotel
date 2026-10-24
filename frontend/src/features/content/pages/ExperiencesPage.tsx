"use client";
import { useState, useMemo } from "react";
import Link from "next/link";
import { useTranslation } from "../../../shared/i18n/hooks";
import { ContentShell } from "../shared";

type RitualItem = {
  id: string;
  timeSlot: string;
  category: "DAWN" | "WELLNESS" | "EXPLORE" | "EVENING";
  titleVi: string;
  titleEn: string;
  durationVi: string;
  durationEn: string;
  privilegeVi: string;
  privilegeEn: string;
  descVi: string;
  descEn: string;
  image: string;
  recommendedVillaSlug: string;
  recommendedVillaName: string;
};

const CURATED_RITUALS: RitualItem[] = [
  {
    id: "kayak-mist",
    timeSlot: "06:00 – 07:30",
    category: "DAWN",
    titleVi: "Chèo Thuyền Gỗ Đón Sương Sớm Hồ Tuyền Lâm",
    titleEn: "Dawn Mist Wooden Kayak on Tuyen Lam Lake",
    durationVi: "90 phút · Có hướng dẫn viên riêng",
    durationEn: "90 mins · Private guide included",
    privilegeVi: "Miễn phí cho khách lưu trú",
    privilegeEn: "Complimentary for Villa Guests",
    descVi:
      "Lướt nhẹ trên mặt hồ phẳng lặng như gương khi làn sương mù còn phủ kín các bán đảo thông. Kết thúc hành trình bằng tách cà phê Arabica Cầu Đất nóng hổi tại bến thuyền riêng.",
    descEn:
      "Glide across mirror-still waters as morning mist rises through the pine peninsula, concluding with hot Cau Dat Arabica coffee on our private jetty.",
    image: "/images/nhu-location-landscape.jpg",
    recommendedVillaSlug: "lakefront-family-estate",
    recommendedVillaName: "Lakefront Estate (LE-04)",
  },
  {
    id: "shinrin-yoku",
    timeSlot: "08:30 – 10:30",
    category: "WELLNESS",
    titleVi: "Thiền Hành Tắm Rừng Thông & Chuông Xoay Himalaya",
    titleEn: "Shinrin-Yoku Forest Bathing & Sound Healing",
    durationVi: "120 phút · Chuyên gia trị liệu riêng",
    durationEn: "120 mins · Private sound healer",
    privilegeVi: "Bao gồm trà thảo mộc & tinh dầu",
    privilegeEn: "Includes herbal infusion & aromatics",
    descVi:
      "Đi bộ chân trần trên thảm lá thông cổ thụ ở độ cao 1,550m, hít thở hợp chất Phytoncide tự nhiên và thư giãn sâu cùng tần số rung động của chuông xoay Himalaya bên suối.",
    descEn:
      "Guided sensory forest immersion beneath century-old pines at 1,550m elevation, paired with Tibetan singing bowl resonance.",
    image: "/images/nhu-garden-courtyard.jpg",
    recommendedVillaSlug: "forest-pavilion",
    recommendedVillaName: "Forest Pavilion (FP-06)",
  },
  {
    id: "thermal-hydro",
    timeSlot: "14:00 – 16:00",
    category: "WELLNESS",
    titleVi: "Thủy Trị Liệu Khoáng Nóng 30°C & Spa Tại Biệt Thự",
    titleEn: "In-Villa 30°C Hydro-Thermal & Hot Stone Spa",
    durationVi: "120 phút · Ngay tại phòng Spa biệt thự",
    durationEn: "120 mins · In-villa private treatment",
    privilegeVi: "Ưu đãi 20% khi đặt trước",
    privilegeEn: "20% Privilege when pre-booked",
    descVi:
      "Kết hợp ngâm hồ bơi vô cực gia nhiệt 30°C với liệu trình massage đá bazan núi lửa nóng và tinh dầu thông đỏ ngay trong không gian riêng tư của biệt thự.",
    descEn:
      "Alternate between your 30°C heated infinity pool and a volcanic basalt hot-stone massage performed in the privacy of your residence.",
    image: "/images/nhu-infinity-pool.jpg",
    recommendedVillaSlug: "water-courtyard-villa",
    recommendedVillaName: "Water Courtyard Villa (WC-05)",
  },
  {
    id: "cau-dat-tea",
    timeSlot: "15:30 – 17:30",
    category: "EXPLORE",
    titleVi: "Thưởng Ngoạn Đồi Trà Cổ Thụ Cầu Đất Bằng Xe Cổ",
    titleEn: "Vintage Chauffeur Tour to Cau Dat Ancient Tea Hills",
    durationVi: "150 phút · Xe đưa đón riêng",
    durationEn: "150 mins · Private chauffeur",
    privilegeVi: "Tặng hộp trà Ô Long thượng hạng",
    privilegeEn: "Includes artisanal Oolong gift tin",
    descVi:
      "Chuyến du ngoạn riêng tư tới vùng trà cổ hơn 100 năm tuổi ở độ cao 1,650m, tự tay sao trà cùng nghệ nhân bản địa và ngắm biển mây lúc chiều tà.",
    descEn:
      "Journey to century-old tea terraces at 1,650m altitude for a private tea-roasting session with master artisans.",
    image: "/images/nhu-hero-villa-4k.jpg",
    recommendedVillaSlug: "hillside-residence-private-pool",
    recommendedVillaName: "Hillside Residence (HR-03)",
  },
  {
    id: "pine-cinema",
    timeSlot: "19:30 – 22:00",
    category: "EVENING",
    titleVi: "Rạp Chiếu Phim Ngoài Trời Bên Hồ Bơi & Lò Sưởi Đá",
    titleEn: "Starlight Poolside Cinema & Fireplace S'mores",
    durationVi: "150 phút · Thiết lập riêng tại sân hiên",
    durationEn: "150 mins · Private terrace setup",
    privilegeVi: "Kèm rượu vang nóng Mulled Wine",
    privilegeEn: "Includes spiced mulled wine & blankets",
    descVi:
      "Quản gia biến sân hiên hồ bơi của bạn thành rạp chiếu phim 4K dưới bầu trời sao Đà Lạt, kèm chăn lông cừu ấm, lò sưởi gỗ thông và rượu vang đỏ đun nóng.",
    descEn:
      "Your pool deck transforms into a private starlight theater with cashmere throws, crackling pine fire, and spiced mulled wine.",
    image: "/images/nhu-private-dining.jpg",
    recommendedVillaSlug: "sanctuary-pool-villa",
    recommendedVillaName: "Sanctuary Pool Villa (PV-02)",
  },
];

export const ExperiencesPage = () => {
  const { lang } = useTranslation();
  const isVi = lang === "vi";

  const [activeFilter, setActiveFilter] = useState<string>("ALL");
  const [selectedIds, setSelectedIds] = useState<string[]>(["kayak-mist", "thermal-hydro", "pine-cinema"]);

  const filteredRituals = useMemo(() => {
    if (activeFilter === "ALL") return CURATED_RITUALS;
    return CURATED_RITUALS.filter((r) => r.category === activeFilter);
  }, [activeFilter]);

  const toggleRitual = (id: string) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  const selectedRituals = CURATED_RITUALS.filter((r) => selectedIds.includes(r.id));

  return (
    <ContentShell
      eyebrow={isVi ? "Nghệ Thuật Sống Chậm & Trải Nghiệm" : "Curated Highland Rituals"}
      title={isVi ? "Những Ngày Thở Cùng Rừng Thông." : "Days with a slower pulse."}
      copy={
        isVi
          ? "Không có lịch trình khuôn mẫu. Hãy tự tay thiết kế nhịp điệu một ngày của riêng bạn từ bình minh sương mù trên mặt hồ đến đêm lửa ấm bên hiên biệt thự."
          : "Compose your own bespoke sanctuary rhythm — from dawn mist kayaking on Tuyen Lam Lake to starlight cinema beside your heated pool."
      }
    >
      {/* Category Mood Selector */}
      <div className="mb-10 flex flex-wrap items-center justify-between gap-4">
        <div className="flex flex-wrap gap-2">
          {[
            { id: "ALL", label: isVi ? "✦ Tất Cả Trải Nghiệm (05)" : "✦ All Rituals (05)" },
            { id: "DAWN", label: isVi ? "🌅 Bình Minh Sương Sớm" : "🌅 Dawn Mist" },
            { id: "WELLNESS", label: isVi ? "🧘 Thủy Trị Liệu & Tắm Rừng" : "🧘 Wellness & Spa" },
            { id: "EXPLORE", label: isVi ? "🍃 Du Ngoạn Bản Địa" : "🍃 Highland Discovery" },
            { id: "EVENING", label: isVi ? "🔥 Đêm Lửa Ấm & Ngắm Sao" : "🔥 Starlight Evening" },
          ].map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveFilter(tab.id)}
              className={`rounded-full px-5 py-2.5 text-xs font-semibold uppercase tracking-wider transition ${
                activeFilter === tab.id
                  ? "bg-brand-forest text-white shadow-md"
                  : "bg-brand-paper text-brand-charcoal hover:bg-brand-white"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      <div className="grid gap-8 lg:grid-cols-[1.2fr_0.8fr]">
        {/* Left: Interactive Ritual Cards */}
        <div className="space-y-6">
          {filteredRituals.map((ritual) => {
            const isAdded = selectedIds.includes(ritual.id);
            return (
              <article
                key={ritual.id}
                className={`group overflow-hidden rounded-[2.2rem] border transition duration-500 ${
                  isAdded
                    ? "border-brand-forest bg-brand-paper shadow-[0_24px_80px_rgba(32,52,43,0.14)]"
                    : "border-transparent bg-brand-paper/85 shadow-[0_18px_60px_rgba(32,52,43,0.06)] hover:bg-brand-paper"
                }`}
              >
                <div className="grid gap-6 p-4 sm:grid-cols-[0.44fr_0.56fr] sm:p-6">
                  <div className="relative aspect-[4/3] overflow-hidden rounded-[1.5rem] sm:aspect-auto sm:h-full">
                    <img
                      src={ritual.image}
                      alt={isVi ? ritual.titleVi : ritual.titleEn}
                      className="h-full w-full object-cover transition duration-700 group-hover:scale-105"
                    />
                    <span className="absolute left-3 top-3 rounded-full bg-black/65 px-3 py-1 text-[0.65rem] font-bold uppercase tracking-widest text-amber-300 backdrop-blur-md">
                      🕒 {ritual.timeSlot}
                    </span>
                  </div>

                  <div className="flex flex-col justify-between py-1">
                    <div>
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <span className="text-[0.68rem] font-bold uppercase tracking-[0.18em] text-brand-sage">
                          {isVi ? ritual.durationVi : ritual.durationEn}
                        </span>
                        <span className="rounded-full bg-brand-forest/10 px-3 py-0.5 text-[0.65rem] font-semibold text-brand-forest">
                          {isVi ? ritual.privilegeVi : ritual.privilegeEn}
                        </span>
                      </div>

                      <h2 className="mt-2 font-serif text-2xl leading-snug text-brand-charcoal sm:text-3xl">
                        {isVi ? ritual.titleVi : ritual.titleEn}
                      </h2>

                      <p className="mt-3 text-sm leading-relaxed text-brand-ink/70">
                        {isVi ? ritual.descVi : ritual.descEn}
                      </p>
                    </div>

                    <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-brand-stone/50 pt-4">
                      <Link
                        href={`/villas/${ritual.recommendedVillaSlug}`}
                        className="text-xs font-semibold text-brand-forest hover:underline"
                      >
                        🏡 {ritual.recommendedVillaName} →
                      </Link>

                      <button
                        type="button"
                        onClick={() => toggleRitual(ritual.id)}
                        className={`rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-wider transition ${
                          isAdded
                            ? "bg-brand-forest text-white"
                            : "border border-brand-forest text-brand-forest hover:bg-brand-forest hover:text-white"
                        }`}
                      >
                        {isAdded
                          ? isVi
                            ? "✓ Đã thêm vào lịch trình"
                            : "✓ In Your Itinerary"
                          : isVi
                          ? "+ Thêm vào lịch trình"
                          : "+ Add to Itinerary"}
                      </button>
                    </div>
                  </div>
                </div>
              </article>
            );
          })}
        </div>

        {/* Right: Sticky Personal Stay Itinerary Composer */}
        <aside className="h-fit rounded-[2.2rem] bg-brand-forest-deep p-7 text-white shadow-[0_28px_90px_rgba(12,28,22,0.24)] lg:sticky lg:top-28">
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage-light">
            🧭 {isVi ? "Bản Thiết Kế Lịch Trình Của Riêng Bạn" : "Your Bespoke Day Dossier"}
          </p>
          <h3 className="mt-2 font-serif text-3xl text-white">
            {isVi
              ? `${selectedRituals.length} Trải Nghiệm Đã Chọn`
              : `${selectedRituals.length} Curated Moments Selected`}
          </h3>
          <p className="mt-2 text-xs leading-relaxed text-brand-paper/75">
            {isVi
              ? "Quản gia riêng sẽ chuẩn bị sẵn thuyền gỗ, bàn trà và liệu trình Spa theo đúng khung giờ bạn chọn dưới đây."
              : "Your private butler will pre-arrange every selected ritual around your preferred waking hour."}
          </p>

          <div className="mt-6 space-y-3 border-t border-white/15 pt-5">
            {selectedRituals.length === 0 ? (
              <p className="py-6 text-center text-sm text-brand-paper/60">
                {isVi
                  ? "Chạm '+ Thêm vào lịch trình' ở các trải nghiệm bên trái để thiết kế ngày nghỉ của bạn."
                  : "Select rituals on the left to compose your bespoke day."}
              </p>
            ) : (
              selectedRituals.map((item, idx) => (
                <div
                  key={item.id}
                  className="flex items-start justify-between gap-3 rounded-2xl bg-white/10 p-4 backdrop-blur-sm"
                >
                  <div>
                    <span className="text-[0.65rem] font-bold uppercase tracking-widest text-amber-300">
                      0{idx + 1} · {item.timeSlot}
                    </span>
                    <p className="mt-1 font-serif text-lg text-white">
                      {isVi ? item.titleVi : item.titleEn}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => toggleRitual(item.id)}
                    className="text-xs text-white/60 hover:text-white"
                    aria-label="Remove"
                  >
                    ✕
                  </button>
                </div>
              ))
            )}
          </div>

          <div className="mt-7 space-y-3">
            <Link
              href="/villas"
              className="inline-flex min-h-12 w-full items-center justify-center rounded-full bg-amber-300 px-6 text-xs font-bold uppercase tracking-[0.15em] text-brand-forest-deep transition hover:bg-white"
            >
              {isVi ? "Chọn Biệt Thự Cho Lịch Trình Này →" : "Choose Sanctuary for This Itinerary →"}
            </Link>
            <Link
              href="/contact"
              className="inline-flex min-h-11 w-full items-center justify-center rounded-full border border-white/25 px-6 text-xs font-semibold uppercase tracking-[0.15em] text-white transition hover:bg-white/10"
            >
              {isVi ? "Gửi Lịch Trình Tới Quản Gia" : "Send Itinerary to Head Butler"}
            </Link>
          </div>
        </aside>
      </div>
    </ContentShell>
  );
};
