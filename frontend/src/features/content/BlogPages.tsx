"use client";
'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { api, API_PATHS } from '../../shared/api/client';

type Blog = {
  id: number;
  title: string;
  excerpt?: string;
  content?: string;
  author?: string;
  imageUrl?: string;
  createdAt?: string;
};

export function BlogsPage() {
  const blogs = useQuery({
    queryKey: ['blogs'],
    queryFn: () => api.get<Blog[]>(API_PATHS.blogs),
    retry: false,
  });

  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
      <div className="mx-auto max-w-[1180px]">
        <p className="mb-5 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">Journal</p>
        <h1 className="max-w-[12ch] text-5xl leading-[1.04] text-brand-charcoal sm:text-7xl">Villa notes.</h1>
        <div className="mt-12 grid gap-6 md:grid-cols-2">
          {(blogs.data ?? []).map((blog) => (
            <Link key={blog.id} href={`/blogs/${blog.id}`} className="overflow-hidden rounded-2xl bg-brand-paper shadow-[0_16px_50px_rgba(32,52,43,0.08)] transition hover:-translate-y-0.5">
              {blog.imageUrl ? <img src={blog.imageUrl} alt={blog.title} className="aspect-[16/9] w-full object-cover" /> : null}
              <div className="p-6">
                <p className="text-xs font-semibold uppercase tracking-[0.14em] text-brand-sage">{blog.author || 'Nhu Villas'}</p>
                <h2 className="mt-3 text-2xl text-brand-charcoal">{blog.title}</h2>
                <p className="mt-3 text-sm leading-7 text-brand-ink/62">{blog.excerpt || blog.content?.slice(0, 180) || ''}</p>
              </div>
            </Link>
          ))}
        </div>
        {!blogs.isLoading && (blogs.data ?? []).length === 0 ? (
          <p className="mt-10 rounded-2xl bg-brand-paper p-6 text-brand-ink/60">No journal posts yet.</p>
        ) : null}
      </div>
    </section>
  );
}

export function BlogDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const blog = useQuery({
    queryKey: ['blog', id],
    queryFn: () => api.get<Blog>(API_PATHS.blog(id)),
    retry: false,
    enabled: Boolean(id),
  });

  return (
    <section className="min-h-screen bg-brand-sand px-5 pb-24 pt-32 sm:px-8 lg:px-12 lg:pt-40">
      <article className="mx-auto max-w-[860px]">
        <Link href="/blogs" className="text-sm font-semibold uppercase tracking-[0.14em] text-brand-forest">Back to journal</Link>
        {blog.data ? (
          <>
            <p className="mt-8 text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">{blog.data.author || 'Nhu Villas'}</p>
            <h1 className="mt-4 text-5xl leading-[1.05] text-brand-charcoal sm:text-7xl">{blog.data.title}</h1>
            {blog.data.imageUrl ? <img src={blog.data.imageUrl} alt={blog.data.title} className="mt-10 aspect-[16/9] w-full rounded-2xl object-cover" /> : null}
            <div className="mt-10 whitespace-pre-wrap text-base leading-8 text-brand-ink/72">
              {blog.data.content || blog.data.excerpt}
            </div>
          </>
        ) : (
          <div className="mt-10 h-72 animate-pulse rounded-2xl bg-brand-paper" />
        )}
      </article>
    </section>
  );
}

