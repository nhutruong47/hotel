'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { api, API_PATHS } from '../../shared/api/client';
import { useTranslation } from '../../shared/i18n/hooks';

type Blog = {
  id: number;
  title: string;
  category?: string;
  readTime?: string;
  excerpt?: string;
  content?: string;
  author?: string;
  imageUrl?: string;
  createdAt?: string;
  recommendedVillaSlug?: string;
  recommendedVillaName?: string;
};

const EDITORIAL_ARTICLES: Blog[] = [
  {
    id: 101,
    title: 'Kiến Trúc Của Sự Tĩnh Lặng: Khi Đá Tự Nhiên, Gỗ Tuyết Tùng Và Mặt Hồ Đối Thoại',
    category: 'ARCHITECTURE',
    readTime: '6 phút đọc',
    author: 'Kiến Trúc Sư Trần Minh Nhật',
    imageUrl: '/images/nhu-hero-villa-4k.jpg',
    createdAt: '2026-10-15',
    recommendedVillaSlug: 'hillside-residence-private-pool',
    recommendedVillaName: 'Hillside Residence (HR-03)',
    excerpt:
      'Khám phá triết lý thiết kế mật độ thấp (chỉ 12% diện tích xây dựng) tại Nhu Villas, nơi mỗi căn biệt thự ẩn mình dưới tán thông cổ thụ Hồ Tuyền Lâm.',
    content: `Tại Nhu Villas, chúng tôi bắt đầu bản vẽ đầu tiên không phải từ những bức tường, mà từ hướng gió thổi qua mặt Hồ Tuyền Lâm và góc nghiêng của nắng sớm xuyên qua rừng thông ở độ cao 1,550 mét.\n\n1. Vật liệu thở cùng cao nguyên\nToàn bộ hệ đá bazan xẻ thô, gỗ thông già tự nhiên và vải lanh hữu cơ được tuyển chọn thủ công. Khi nhiệt độ Đà Lạt xuống 16°C vào ban đêm, hệ sàn gỗ ấm và hồ bơi gia nhiệt 30°C tạo nên sự cân bằng nhiệt độ hoàn hảo.\n\n2. Thủy đình phản chiếu\nCác hồ bơi vô cực tại Hillside Residence và Water Courtyard Villa được thiết kế không viền, nối liền tầm mắt với mặt nước Hồ Tuyền Lâm, biến mỗi khoảnh khắc hoàng hôn thành một bức tranh thủy mặc sống động.`,
  },
  {
    id: 102,
    title: 'Nghi Thức Bữa Sáng Nổi & Trà Ô Long Cầu Đất Giữa Sương Sớm Đà Lạt',
    category: 'GASTRONOMY',
    readTime: '4 phút đọc',
    author: 'Bếp Trưởng Lê Hoàng Nam',
    imageUrl: '/images/nhu-private-dining.jpg',
    createdAt: '2026-10-12',
    recommendedVillaSlug: 'garden-pool-villa-da-lat',
    recommendedVillaName: 'Garden Pool Villa (GV-01)',
    excerpt:
      'Từ những búp trà Ô Long hái sương lúc 5 giờ sáng tại đồi trà Cầu Đất đến bàn tiệc Farm-to-Table phục vụ riêng tại sân hiên biệt thự của bạn.',
    content: `Ẩm thực tại Nhu Villas không bị đóng khung trong giờ giấc nhà hàng. Quản gia và đội ngũ đầu bếp chuẩn bị bữa sáng đúng vào khoảnh khắc bạn thức giấc.\n\n• Trà Ô Long Cầu Đất ủ lạnh & nóng: Được thu hoạch từ độ cao 1,650m, mang hương hoa lan rừng thanh khiết.\n• Rau củ hữu cơ thu hoạch trong ngày: Atiso tươi, dâu tây giống Nhật, măng tây nướng bơ thảo mộc và bánh sừng bò nướng mới mỗi sáng.\n• Tiệc tối Chef's Table bên hồ bơi: Khi màn đêm buông xuống, sân hiên biệt thự biến thành nhà hàng Fine Dining chỉ dành riêng cho hai người hoặc gia đình bạn.`,
  },
  {
    id: 103,
    title: 'Thủy Trị Liệu Rừng Thông (Shinrin-Yoku): Hành Trình Chữa Lành Thân – Tâm – Trí',
    category: 'WELLNESS',
    readTime: '5 phút đọc',
    author: 'Chuyên Gia Trị Liệu Mai Lâm',
    imageUrl: '/images/nhu-infinity-pool.jpg',
    createdAt: '2026-10-08',
    recommendedVillaSlug: 'water-courtyard-villa',
    recommendedVillaName: 'Water Courtyard Villa (WC-05)',
    excerpt:
      'Sự kết hợp giữa nghệ thuật tắm rừng Nhật Bản (Shinrin-Yoku) và liệu pháp ngâm bồn thảo mộc nóng nhìn ra thung lũng thông nguyên sinh.',
    content: `Nghiên cứu khoa học chỉ ra rằng hợp chất Phytoncide tiết ra từ rừng thông già giúp giảm 40% hormone căng thẳng cortisol chỉ sau 2 giờ hít thở sâu.\n\nTại Nhu Villas, mỗi kỳ nghỉ đều tích hợp liệu trình Wellness toàn diện:\n1. Đi bộ thiền hành lúc bình minh trên cung đường mòn riêng ven hồ.\n2. Ngâm hồ bơi nước ấm khoáng chất 30°C giữa làn sương mờ.\n3. Trị liệu chuông xoay Himalaya và massage đá nóng ngay tại phòng Spa riêng của biệt thự.`,
  },
  {
    id: 104,
    title: 'Cẩm Nang 48 Giờ Độc Bản Tại Hồ Tuyền Lâm & Đỉnh Pinhatt Cho Cặp Đôi',
    category: 'DALAT GUIDE',
    readTime: '7 phút đọc',
    author: 'Nhu Concierge Team',
    imageUrl: '/images/nhu-location-landscape.jpg',
    createdAt: '2026-10-02',
    recommendedVillaSlug: 'forest-pavilion',
    recommendedVillaName: 'Forest Pavilion (FP-06)',
    excerpt:
      'Lịch trình gợi ý từ Quản gia Nhu Villas: Chèo thuyền kayak ngắm sương tan lúc 6h sáng, thưởng trà chiều trong rừng và ngắm sao đêm bên lò sưởi.',
    content: `Nếu bạn chỉ có 2 ngày cuối tuần tại Đà Lạt, đừng dành thời gian trong phố đông. Hãy để thiên nhiên Hồ Tuyền Lâm ôm trọn kỳ nghỉ của bạn:\n\n• Ngày 1 — Chạm vào sự tĩnh lặng: Xe Mercedes V-Class đón bạn tại sân bay Liên Khương, đưa thẳng về biệt thự qua cung đường đèo Prenn rợp bóng thông. Thưởng thức trà chiều thảo mộc và ngắm hoàng hôn từ hồ bơi vô cực.\n• Ngày 2 — Khám phá mặt hồ phẳng lặng: Chèo thuyền gỗ riêng trên mặt hồ lúc sương sớm, ghé thăm Thiền Viện Trúc Lâm và khép lại ngày bằng bữa tối BBQ rượu vang dưới bầu trời đầy sao.`,
  },
];

export function BlogsPage() {
  const { lang } = useTranslation();
  const isVi = lang === 'vi';
  const [activeCategory, setActiveCategory] = useState<string>('ALL');

  const blogsQuery = useQuery({
    queryKey: ['blogs'],
    queryFn: () => api.get<Blog[]>(API_PATHS.blogs),
    retry: false,
  });

  const allBlogs = useMemo(() => {
    const apiBlogs = blogsQuery.data ?? [];
    if (apiBlogs.length === 0) return EDITORIAL_ARTICLES;
    return [...apiBlogs, ...EDITORIAL_ARTICLES];
  }, [blogsQuery.data]);

  const filteredBlogs = useMemo(() => {
    if (activeCategory === 'ALL') return allBlogs;
    return allBlogs.filter((b) => (b.category || 'ARCHITECTURE') === activeCategory);
  }, [allBlogs, activeCategory]);

  const coverStory = filteredBlogs[0] || allBlogs[0];
  const restStories = filteredBlogs.slice(1);

  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-28 pt-32 sm:px-8 lg:px-12 lg:pt-40">
      <div className="mx-auto max-w-[1380px]">
        {/* Editorial Masthead */}
        <div className="flex flex-col justify-between gap-6 border-b border-brand-stone pb-10 lg:flex-row lg:items-end">
          <div>
            <p className="mb-4 text-xs font-semibold uppercase tracking-[0.24em] text-brand-sage">
              📖 {isVi ? 'Ấn Phẩm Kiến Trúc & Phong Cách Sống' : 'The Nhu Villas Editorial Journal'}
            </p>
            <h1 className="max-w-[14ch] font-serif text-5xl leading-[1.03] text-brand-charcoal sm:text-7xl">
              {isVi ? 'Nhật Ký Đồi Thông & Mặt Hồ.' : 'Villa Notes & Highland Stories.'}
            </h1>
          </div>
          <div className="flex flex-wrap gap-2">
            {[
              { id: 'ALL', label: isVi ? 'Tất cả bài viết' : 'All Stories' },
              { id: 'ARCHITECTURE', label: isVi ? 'Kiến trúc' : 'Architecture' },
              { id: 'GASTRONOMY', label: isVi ? 'Ẩm thực' : 'Gastronomy' },
              { id: 'WELLNESS', label: isVi ? 'Chữa lành' : 'Wellness' },
              { id: 'DALAT GUIDE', label: isVi ? 'Cẩm nang Đà Lạt' : 'Da Lat Guide' },
            ].map((cat) => (
              <button
                key={cat.id}
                type="button"
                onClick={() => setActiveCategory(cat.id)}
                className={`rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-wider transition ${
                  activeCategory === cat.id
                    ? 'bg-brand-forest text-white shadow-sm'
                    : 'bg-brand-paper text-brand-ink/70 hover:bg-brand-white'
                }`}
              >
                {cat.label}
              </button>
            ))}
          </div>
        </div>

        {/* Cover Story Showcase */}
        {coverStory && (
          <Link
            href={`/blogs/${coverStory.id}`}
            className="group mt-12 grid items-center gap-8 overflow-hidden rounded-[2.2rem] bg-brand-forest-deep p-6 text-white shadow-[0_28px_90px_rgba(12,28,22,0.22)] lg:grid-cols-[1.15fr_0.85fr] sm:p-10"
          >
            <div className="relative aspect-[16/10] overflow-hidden rounded-[1.5rem]">
              <img
                src={coverStory.imageUrl || '/images/nhu-hero-villa-4k.jpg'}
                alt={coverStory.title}
                className="h-full w-full object-cover transition duration-700 group-hover:scale-[1.03]"
              />
              <span className="absolute left-4 top-4 rounded-full bg-amber-300 px-3.5 py-1 text-[0.65rem] font-bold uppercase tracking-widest text-brand-forest-deep">
                ★ COVER STORY
              </span>
            </div>
            <div className="space-y-5 lg:px-4">
              <div className="flex items-center gap-3 text-xs uppercase tracking-[0.18em] text-brand-sage-light">
                <span>{coverStory.category || 'EDITORIAL'}</span>
                <span>·</span>
                <span>{coverStory.readTime || '5 phút đọc'}</span>
              </div>
              <h2 className="font-serif text-3xl leading-tight text-white transition group-hover:text-amber-200 sm:text-4xl">
                {coverStory.title}
              </h2>
              <p className="text-sm leading-relaxed text-brand-paper/80 sm:text-base">
                {coverStory.excerpt || coverStory.content?.slice(0, 200)}
              </p>
              <div className="flex items-center justify-between border-t border-white/15 pt-5 text-xs">
                <span className="text-brand-paper/70">{coverStory.author || 'Nhu Villas Editorial'}</span>
                <span className="font-semibold uppercase tracking-widest text-amber-300">
                  {isVi ? 'Đọc toàn bộ bài viết →' : 'Read Full Story →'}
                </span>
              </div>
            </div>
          </Link>
        )}

        {/* Asymmetric Editorial Grid */}
        <div className="mt-10 grid gap-8 md:grid-cols-2 lg:grid-cols-3">
          {restStories.map((blog) => (
            <Link
              key={blog.id}
              href={`/blogs/${blog.id}`}
              className="group flex flex-col justify-between overflow-hidden rounded-[2rem] bg-brand-paper p-3 shadow-[0_18px_60px_rgba(32,52,43,0.08)] transition duration-500 hover:-translate-y-1.5 hover:shadow-[0_28px_80px_rgba(32,52,43,0.14)]"
            >
              <div>
                <div className="relative aspect-[16/10] overflow-hidden rounded-[1.5rem]">
                  <img
                    src={blog.imageUrl || '/images/nhu-villa-interior.jpg'}
                    alt={blog.title}
                    className="h-full w-full object-cover transition duration-700 group-hover:scale-[1.04]"
                  />
                  <span className="absolute left-3 top-3 rounded-full bg-black/55 px-3 py-1 text-[0.62rem] font-semibold uppercase tracking-widest text-white backdrop-blur-md">
                    {blog.category || 'JOURNAL'}
                  </span>
                </div>
                <div className="p-5">
                  <p className="text-xs font-semibold uppercase tracking-[0.14em] text-brand-sage">
                    {blog.author || 'Nhu Villas'} · {blog.readTime || '5 phút đọc'}
                  </p>
                  <h3 className="mt-2 font-serif text-2xl leading-snug text-brand-charcoal transition group-hover:text-brand-forest">
                    {blog.title}
                  </h3>
                  <p className="mt-3 line-clamp-3 text-sm leading-relaxed text-brand-ink/65">
                    {blog.excerpt || blog.content?.slice(0, 160)}
                  </p>
                </div>
              </div>
              <div className="px-5 pb-4 pt-2 text-xs font-semibold uppercase tracking-wider text-brand-forest">
                {isVi ? 'Khám phá câu chuyện →' : 'Read Article →'}
              </div>
            </Link>
          ))}
        </div>
      </div>
    </section>
  );
}

export function BlogDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const { lang } = useTranslation();
  const isVi = lang === 'vi';

  const localFallback = EDITORIAL_ARTICLES.find((a) => String(a.id) === String(id));

  const blogQuery = useQuery({
    queryKey: ['blog', id],
    queryFn: () => api.get<Blog>(API_PATHS.blog(id)),
    retry: false,
    enabled: Boolean(id) && !localFallback,
  });

  const article = localFallback ?? blogQuery.data ?? EDITORIAL_ARTICLES[0];

  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-28 pt-32 sm:px-8 lg:px-12 lg:pt-40">
      <article className="mx-auto max-w-[920px]">
        <Link
          href="/blogs"
          className="inline-flex items-center gap-2 rounded-full border border-brand-stone bg-brand-paper px-4 py-2 text-xs font-semibold uppercase tracking-[0.16em] text-brand-forest hover:border-brand-forest"
        >
          &larr; {isVi ? 'Quay lại Tạp chí' : 'Back to Journal'}
        </Link>

        <div className="mt-8 rounded-[2.2rem] bg-brand-paper p-6 shadow-[0_24px_80px_rgba(32,52,43,0.08)] sm:p-12">
          <div className="flex flex-wrap items-center gap-3 text-xs font-semibold uppercase tracking-[0.2em] text-brand-sage">
            <span>{article.category || 'EDITORIAL'}</span>
            <span>·</span>
            <span>{article.readTime || '5 phút đọc'}</span>
            <span>·</span>
            <span>{article.author || 'Nhu Villas'}</span>
          </div>

          <h1 className="mt-4 font-serif text-4xl leading-[1.08] text-brand-charcoal sm:text-6xl">
            {article.title}
          </h1>

          {article.imageUrl && (
            <img
              src={article.imageUrl}
              alt={article.title}
              className="mt-8 aspect-[16/9] w-full rounded-[1.5rem] object-cover shadow-md"
            />
          )}

          <div className="mt-8 space-y-5 whitespace-pre-line text-base leading-8 text-brand-ink/80 sm:text-lg">
            {article.content || article.excerpt}
          </div>

          {/* Recommended Sanctuary Tie-in Card */}
          <div className="mt-12 flex flex-col items-start justify-between gap-4 rounded-[1.5rem] bg-brand-forest-deep p-6 text-white sm:flex-row sm:items-center sm:p-8">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-brand-sage-light">
                {isVi ? 'Biệt Thự Gợi Ý Từ Bài Viết' : 'Featured Sanctuary in This Story'}
              </p>
              <h3 className="mt-1 font-serif text-2xl text-white">
                {article.recommendedVillaName || 'Hillside Residence (HR-03)'}
              </h3>
            </div>
            <Link
              href={`/villas/${article.recommendedVillaSlug || 'hillside-residence-private-pool'}`}
              className="rounded-full bg-brand-paper px-6 py-3 text-xs font-semibold uppercase tracking-[0.15em] text-brand-forest-deep transition hover:bg-white"
            >
              {isVi ? 'Trải Nghiệm Biệt Thự Này →' : 'Explore This Sanctuary →'}
            </Link>
          </div>
        </div>
      </article>
    </section>
  );
}
