"use client";
import { useState } from "react";
import Link from "next/link";
import { useCurrency, useTranslation } from "../../../shared/i18n/hooks";
import { ContentShell } from "../shared";

type CulinaryChapter = {
  id: string;
  time: string;
  badgeVi: string;
  badgeEn: string;
  titleVi: string;
  titleEn: string;
  subtitleVi: string;
  subtitleEn: string;
  priceUsd: number;
  includedForGuests: boolean;
  image: string;
  sommelierVi: string;
  sommelierEn: string;
  courses: {
    nameVi: string;
    nameEn: string;
    detailVi: string;
    detailEn: string;
  }[];
};

const CULINARY_CHAPTERS: CulinaryChapter[] = [
  {
    id: "floating-breakfast",
    time: "07:00 – 10:30",
    badgeVi: "01 · BỮA SÁNG NGHỆ THUẬT",
    badgeEn: "01 · MORNING RITUAL",
    titleVi: "Khay Bữa Sáng Nổi Trên Hồ Vô Cực Ấm 30°C",
    titleEn: "Floating Infinity Pool & Terrace Breakfast",
    subtitleVi:
      "Thức giấc theo nhịp điệu riêng và thưởng thức bữa sáng nóng hổi giữa làn khói sương bay là là trên mặt hồ bơi riêng của bạn.",
    subtitleEn:
      "Served at your preferred waking hour either on a handcrafted rattan floating tray or your sun-dappled pine terrace.",
    priceUsd: 0,
    includedForGuests: true,
    image: "/images/nhu-infinity-pool.jpg",
    sommelierVi: "Kết hợp cùng: Cà phê Arabica Cầu Đất ủ thủ công, Trà Ô Long hoa lan & Nước ép dâu tây tươi hái sáng sớm.",
    sommelierEn: "Paired with: Hand-poured Cau Dat Arabica, Orchid Oolong tea & cold-pressed highland strawberry elixir.",
    courses: [
      {
        nameVi: "Trứng Benedict Cá Hồi Xông Khói Gỗ Thông & Bơ Sáp Đà Lạt",
        nameEn: "Pine-Smoked Salmon Eggs Benedict & Highland Avocado",
        detailVi: "Bánh mì Sourdough lên men tự nhiên 36 giờ, sốt Hollandaise nghệ tây.",
        detailEn: "36-hour naturally leavened sourdough with saffron hollandaise.",
      },
      {
        nameVi: "Phở Bò Wagyu Hoặc Mì Quảng Đà Lạt Nấu Chậm 12 Giờ",
        nameEn: "12-Hour Slow-Simmered Wagyu Broth & Artisanal Noodles",
        detailVi: "Nước dùng ninh từ xương bò và thảo quả nướng, rau thơm hữu cơ tại vườn.",
        detailEn: "Clarified bone consommé with charred star anise and garden herbs.",
      },
      {
        nameVi: "Khay Bánh Ngọt Pháp Nướng Mới & Mứt Hoa Hồng Thủ Công",
        nameEn: "Warm French Viennoiserie & Organic Rose Petal Jam",
        detailVi: "Croissant bơ Pháp, bánh sừng bò hạnh nhân và sữa chua Hy Lạp mật ong rừng.",
        detailEn: "Cultured butter croissants, almond danish, and wildflower honey yogurt.",
      },
    ],
  },
  {
    id: "high-tea",
    time: "15:00 – 17:00",
    badgeVi: "02 · TRÀ CHIỀU ĐỒI THÔNG",
    badgeEn: "02 · AFTERNOON BOTANICALS",
    titleVi: "Nghi Thức Trà Chiều Thảo Mộc & Bánh Ngọt Cao Nguyên",
    titleEn: "Highland Botanical Afternoon Tea",
    subtitleVi:
      "Khi nắng chiều nhuộm vàng rừng thông, Quản gia bày biện bàn trà ấm áp ngay tại hiên gỗ nhìn ra mặt Hồ Tuyền Lâm.",
    subtitleEn:
      "As golden hour filters through the pines, enjoy a three-tier artisanal tea service on your private timber deck.",
    priceUsd: 0,
    includedForGuests: true,
    image: "/images/nhu-garden-courtyard.jpg",
    sommelierVi: "Kết hợp cùng: Trà Ô Long Nướng Than Hồng, Trà Hoa Cúc Chi Mật Ong Rừng & Rượu vang sủi Prosecco.",
    sommelierEn: "Paired with: Charcoal-Roasted Oolong, Wild Chamomile Honey Infusion & chilled Prosecco.",
    courses: [
      {
        nameVi: "Bánh Scone Nóng Dùng Kèm Kem Tươi Đà Lạt & Mứt Dâu Tây",
        nameEn: "Warm Buttermilk Scones with Clotted Cream & Berry Compote",
        detailVi: "Nướng mới trước 15 phút khi phục vụ tại biệt thự.",
        detailEn: "Baked fresh 15 minutes prior to butler delivery.",
      },
      {
        nameVi: "Tart Gan Ngỗng Áp Chảo & Sung Mỹ Ngâm Rượu Vang Đỏ",
        nameEn: "Seared Foie Gras Tartlet & Mulled Wine Fig",
        detailVi: "Điểm xuyết lá vàng 24K và hạt thông rang bơ.",
        detailEn: "Garnished with toasted pine nuts and aged balsamic reduction.",
      },
      {
        nameVi: "Mousse Bơ Sáp & Sô-cô-la Đen 72% Lâm Đồng",
        nameEn: "Lam Dong 72% Single-Origin Dark Chocolate & Avocado Mousse",
        detailVi: "Vị đắng thanh tao hòa quyện cùng độ béo mịn tự nhiên.",
        detailEn: "Velvety highland cacao balanced with artisanal sea salt.",
      },
    ],
  },
  {
    id: "chefs-table",
    time: "18:30 – 22:00",
    badgeVi: "03 · TIỆC TỐI FINE DINING RIÊNG TƯ",
    badgeEn: "03 · IN-VILLA CHEF'S TABLE",
    titleVi: "Thực Đơn 6 Món Bếp Trưởng Phục Vụ Bên Lò Sưởi & Hồ Bơi",
    titleEn: "6-Course Private Chef's Table by Your Fireplace",
    subtitleVi:
      "Đầu bếp riêng và Quản gia trực tiếp chế biến, trình bày bàn tiệc ánh nến ngay tại phòng ăn hoặc sân hiên biệt thự của bạn.",
    subtitleEn:
      "A private chef prepares and plates a bespoke six-course tasting menu right at your villa's dining pavilion.",
    priceUsd: 95,
    includedForGuests: false,
    image: "/images/nhu-private-dining.jpg",
    sommelierVi: "Kết hợp cùng: Gói Sommelier 4 ly rượu vang Pháp & Chile tuyển chọn theo từng món ăn.",
    sommelierEn: "Paired with: Optional 4-glass Old & New World Sommelier wine flight.",
    courses: [
      {
        nameVi: "Khai Vị: Sò Điệp Hokkaido Áp Chảo & Súp Kem Atiso Đà Lạt Nấm Truffle",
        nameEn: "Starter: Pan-Seared Hokkaido Scallop & Black Truffle Artichoke Velouté",
        detailVi: "Atiso tươi thu hoạch tại vườn Cầu Đất nấu chậm cùng kem tươi và dầu nấm Truffle đen.",
        detailEn: "Heirloom Da Lat artichoke heart purée finished with Périgord truffle oil.",
      },
      {
        nameVi: "Món Chính: Thăn Nội Bò Wagyu Nướng Đá Nóng & Cá Tầm Hồ Tuyền Lâm",
        nameEn: "Main: Charcoal-Grilled Wagyu Tenderloin & Tuyen Lam Sturgeon",
        detailVi: "Phục vụ kèm măng tây nướng bơ thảo mộc, khoai tây nghiền mịn và sốt rượu vang Pinot Noir.",
        detailEn: "Accompanied by charred baby asparagus, pomme purée, and Pinot Noir jus.",
      },
      {
        nameVi: "Tráng Miệng: Bánh Soufflé Chanh Dây Nóng & Kem Vani Bourbon",
        nameEn: "Dessert: Warm Passionfruit Soufflé & Madagascar Vanilla Bean Gelato",
        detailVi: "Chế biến trực tiếp tại bàn cùng rượu mùi Grand Marnier.",
        detailEn: "Flambéed tableside by your private butler.",
      },
    ],
  },
];

export const DiningPage = () => {
  const { lang } = useTranslation();
  const { formatCurrency } = useCurrency();
  const isVi = lang === "vi";

  const [activeChapter, setActiveChapter] = useState<CulinaryChapter>(CULINARY_CHAPTERS[0]);
  const [dietaryPreference, setDietaryPreference] = useState<string>("STANDARD");

  return (
    <ContentShell
      eyebrow={isVi ? "Nghệ Thuật Ẩm Thực Tại Biệt Thự" : "In-Villa Gastronomy"}
      title={isVi ? "Mỗi Bữa Ăn Là Một Tác Phẩm Riêng Tư." : "Seasonal menus, served with care."}
      copy={
        isVi
          ? "Tại Nhu Villas, nhà hàng sang trọng nhất chính là sân hiên nhìn ra rừng thông của riêng bạn. Từ khay bữa sáng nổi trên mặt hồ ấm đến tiệc tối 6 món do Bếp trưởng trực tiếp phục vụ."
          : "From unhurried floating breakfasts to private six-course Chef's Table dinners beside your fireplace, every meal is composed around your villa."
      }
    >
      {/* 1. Interactive Culinary Chapter Switcher */}
      <div className="mb-10 grid gap-4 md:grid-cols-3">
        {CULINARY_CHAPTERS.map((chapter) => {
          const isSelected = activeChapter.id === chapter.id;
          return (
            <button
              key={chapter.id}
              type="button"
              onClick={() => setActiveChapter(chapter)}
              className={`rounded-[1.75rem] border p-6 text-left transition duration-300 ${
                isSelected
                  ? "border-brand-forest bg-brand-forest-deep text-white shadow-[0_24px_70px_rgba(12,28,22,0.22)]"
                  : "border-brand-stone/60 bg-brand-paper text-brand-charcoal hover:border-brand-forest"
              }`}
            >
              <div className="flex items-center justify-between text-xs font-bold uppercase tracking-widest">
                <span className={isSelected ? "text-amber-300" : "text-brand-sage"}>
                  {isVi ? chapter.badgeVi : chapter.badgeEn}
                </span>
                <span className="opacity-75">🕒 {chapter.time}</span>
              </div>
              <h2 className={`mt-3 font-serif text-2xl ${isSelected ? "text-white" : "text-brand-charcoal"}`}>
                {isVi ? chapter.titleVi : chapter.titleEn}
              </h2>
              <p className={`mt-2 text-xs font-semibold ${isSelected ? "text-emerald-300" : "text-brand-forest"}`}>
                {chapter.includedForGuests
                  ? isVi
                    ? "✓ Miễn phí đi kèm mọi kỳ nghỉ"
                    : "✓ Complimentary with Every Stay"
                  : `${formatCurrency(chapter.priceUsd)} / ${isVi ? "khách" : "guest"}`}
              </p>
            </button>
          );
        })}
      </div>

      {/* 2. Interactive Tasting Menu Stage */}
      <div className="grid gap-8 rounded-[2.4rem] bg-brand-paper p-6 shadow-[0_24px_90px_rgba(32,52,43,0.09)] lg:grid-cols-[0.95fr_1.05fr] sm:p-10">
        <div className="flex flex-col justify-between space-y-6">
          <div>
            <div className="relative aspect-[16/11] overflow-hidden rounded-[1.75rem]">
              <img
                src={activeChapter.image}
                alt={isVi ? activeChapter.titleVi : activeChapter.titleEn}
                className="h-full w-full object-cover transition duration-700"
              />
              <div className="absolute bottom-4 left-4 right-4 rounded-2xl bg-black/65 p-4 text-white backdrop-blur-md">
                <p className="text-[0.65rem] font-bold uppercase tracking-[0.2em] text-amber-300">
                  🍷 SOMMELIER & TEA PAIRING
                </p>
                <p className="mt-1 text-xs leading-relaxed text-brand-paper/90">
                  {isVi ? activeChapter.sommelierVi : activeChapter.sommelierEn}
                </p>
              </div>
            </div>

            <h3 className="mt-6 font-serif text-3xl text-brand-charcoal sm:text-4xl">
              {isVi ? activeChapter.titleVi : activeChapter.titleEn}
            </h3>
            <p className="mt-3 text-sm leading-7 text-brand-ink/70 sm:text-base">
              {isVi ? activeChapter.subtitleVi : activeChapter.subtitleEn}
            </p>
          </div>

          {/* Dietary Customizer */}
          <div className="rounded-2xl bg-brand-white p-5">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-brand-sage">
              🌿 {isVi ? "Tùy Chỉnh Chế Độ Dinh Dưỡng Cá Nhân" : "Bespoke Dietary Adaptation"}
            </p>
            <div className="mt-3 flex flex-wrap gap-2">
              {[
                { id: "STANDARD", labelVi: "Nguyên bản Bếp trưởng", labelEn: "Chef's Signature" },
                { id: "VEGETARIAN", labelVi: "Thực dưỡng / Thuần chay", labelEn: "Plant-Based / Vegan" },
                { id: "GLUTEN_FREE", labelVi: "Không Gluten / Keto", labelEn: "Gluten-Free / Keto" },
                { id: "KIDS", labelVi: "Thực đơn Gia đình & Trẻ nhỏ", labelEn: "Family & Young Guests" },
              ].map((diet) => (
                <button
                  key={diet.id}
                  type="button"
                  onClick={() => setDietaryPreference(diet.id)}
                  className={`rounded-full px-3.5 py-1.5 text-xs font-semibold transition ${
                    dietaryPreference === diet.id
                      ? "bg-brand-forest text-white"
                      : "bg-brand-sand text-brand-charcoal hover:bg-brand-stone/60"
                  }`}
                >
                  {isVi ? diet.labelVi : diet.labelEn}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Right: Course-by-Course Breakdown */}
        <div className="flex flex-col justify-between rounded-[1.9rem] bg-brand-white p-6 sm:p-8">
          <div>
            <div className="flex items-center justify-between border-b border-brand-stone pb-4">
              <span className="text-xs font-bold uppercase tracking-[0.22em] text-brand-forest">
                📜 {isVi ? "Thực Đơn Tuyển Chọn Theo Mùa" : "Seasonal Tasting Menu"}
              </span>
              <span className="rounded-full bg-brand-sand px-3 py-1 text-xs font-semibold text-brand-charcoal">
                {dietaryPreference}
              </span>
            </div>

            <div className="mt-6 space-y-6">
              {activeChapter.courses.map((course, idx) => (
                <div key={course.nameVi} className="border-b border-brand-stone/40 pb-5 last:border-none">
                  <span className="text-[0.65rem] font-bold uppercase tracking-[0.2em] text-brand-sage">
                    COURSE 0{idx + 1}
                  </span>
                  <h4 className="mt-1 font-serif text-2xl text-brand-charcoal">
                    {isVi ? course.nameVi : course.nameEn}
                  </h4>
                  <p className="mt-1.5 text-sm leading-relaxed text-brand-ink/65">
                    {isVi ? course.detailVi : course.detailEn}
                  </p>
                </div>
              ))}
            </div>
          </div>

          <div className="mt-8 flex flex-wrap items-center justify-between gap-4 rounded-2xl bg-brand-sand/70 p-5">
            <div>
              <p className="text-xs font-semibold uppercase tracking-wider text-brand-sage">
                {isVi ? "Đặt Bàn Tiệc Tại Biệt Thự" : "In-Villa Dining Reservation"}
              </p>
              <p className="mt-0.5 font-serif text-xl text-brand-charcoal">
                {activeChapter.includedForGuests
                  ? isVi
                    ? "Đã bao gồm trong giá biệt thự"
                    : "Complimentary with your villa stay"
                  : `${formatCurrency(activeChapter.priceUsd)} / ${isVi ? "khách" : "guest"}`}
              </p>
            </div>
            <div className="flex gap-2">
              <Link
                href="/villas"
                className="rounded-full bg-brand-forest px-5 py-3 text-xs font-semibold uppercase tracking-[0.14em] text-white transition hover:bg-brand-forest-deep"
              >
                {isVi ? "Chọn Biệt Thự →" : "Book Villa →"}
              </Link>
              <Link
                href="/contact"
                className="rounded-full border border-brand-forest px-5 py-3 text-xs font-semibold uppercase tracking-[0.14em] text-brand-forest transition hover:bg-brand-forest hover:text-white"
              >
                {isVi ? "Đặt Thực Đơn Riêng" : "Reserve Table"}
              </Link>
            </div>
          </div>
        </div>
      </div>
    </ContentShell>
  );
};
