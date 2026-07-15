import { useEffect, useRef } from 'react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

const villas = [
  {
    name: 'The Garden Villa',
    detail: '1 Bedroom · Private Onsen · Zen Garden',
    price: 'From $450',
    image: 'https://images.unsplash.com/photo-1618221118493-9cfa1a1c00da?auto=format&fit=crop&w=1000&q=80',
    imageSm: 'https://images.unsplash.com/photo-1618221118493-9cfa1a1c00da?auto=format&fit=crop&w=500&q=75',
    alt: 'Serene garden villa bedroom with floor-to-ceiling windows overlooking a zen garden',
  },
  {
    name: 'The Lake Retreat',
    detail: '2 Bedrooms · Infinity Pool · Lake View',
    price: 'From $750',
    image: 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1000&q=80',
    imageSm: 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=500&q=75',
    alt: 'Spacious lake retreat living room with panoramic water views',
  },
  {
    name: 'The Imperial Pavilion',
    detail: '3 Bedrooms · Private Spa · Butler',
    price: 'From $1,200',
    image: 'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=1000&q=80',
    imageSm: 'https://images.unsplash.com/photo-1600596542815-ffad4c1539a9?auto=format&fit=crop&w=500&q=75',
    alt: 'Grand imperial pavilion exterior set within manicured tropical gardens',
  },
]

export default function VillaSection() {
  const sectionRef = useRef<HTMLElement>(null)
  const headerRef = useRef<HTMLDivElement>(null)
  const cardsRef = useRef<(HTMLElement | null)[]>([])

  useEffect(() => {
    if (!sectionRef.current) return
    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (prefersReduced) return

    const ctx = gsap.context(() => {
      gsap.from(headerRef.current, {
        opacity: 0,
        y: 40,
        duration: 1,
        ease: 'power3.out',
        scrollTrigger: { trigger: headerRef.current, start: 'top 80%' },
      })

      cardsRef.current.forEach((card, i) => {
        if (!card) return
        gsap.from(card, {
          opacity: 0,
          y: 80,
          duration: 1.2,
          delay: i * 0.15,
          ease: 'power3.out',
          scrollTrigger: { trigger: card, start: 'top 85%' },
        })

        const img = card.querySelector('img')
        if (img) {
          gsap.to(img, {
            scale: 1.08,
            ease: 'none',
            scrollTrigger: {
              trigger: card,
              start: 'top bottom',
              end: 'bottom top',
              scrub: true,
            },
          })
        }
      })
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
    <section
      ref={sectionRef}
      id="villas"
      className="py-20 sm:py-28 lg:py-40 bg-[var(--color-brand-stone)] px-6 md:px-12"
      aria-labelledby="villas-heading"
    >
      <div className="max-w-[1400px] mx-auto">
        <div ref={headerRef} className="text-center mb-16 lg:mb-28 space-y-4 lg:space-y-5">
          <p className="text-[10px] lg:text-[11px] tracking-[0.3em] uppercase text-[var(--color-brand-sage)]">
            Accommodations
          </p>
          <h2
            id="villas-heading"
            className="text-3xl sm:text-4xl lg:text-[3.5rem] font-serif text-[var(--color-brand-charcoal)]"
          >
            Our Sanctuaries
          </h2>
          <p className="text-base lg:text-lg font-light text-gray-500 max-w-lg mx-auto">
            Three distinct spaces, each designed to nurture stillness.
          </p>
        </div>

        {/* Cards Grid — 1 col mobile, 2 col tablet, 3 col desktop */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8 lg:gap-14">
          {villas.map((villa, i) => (
            <article
              key={villa.name}
              ref={(el) => { cardsRef.current[i] = el }}
              className={`group cursor-pointer ${i === 1 ? 'lg:translate-y-16' : ''}`}
            >
              <div className="w-full aspect-[4/5] sm:aspect-[3/4] overflow-hidden rounded-[2px] mb-6 lg:mb-8">
                <img
                  src={villa.image}
                  srcSet={`${villa.imageSm} 500w, ${villa.image} 1000w`}
                  sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
                  alt={villa.alt}
                  className="w-full h-full object-cover transition-transform duration-700 ease-out group-hover:scale-[1.04]"
                  loading="lazy"
                  decoding="async"
                />
              </div>

              <div className="space-y-2 lg:space-y-3">
                <h3 className="font-serif text-xl lg:text-[1.6rem] text-[var(--color-brand-charcoal)]">
                  {villa.name}
                </h3>
                <p className="text-sm text-gray-500 font-light">{villa.detail}</p>
                <div className="flex justify-between items-center pt-4 lg:pt-5 border-t border-gray-300/60">
                  <span className="text-sm tracking-[0.1em] text-[var(--color-brand-charcoal)]">
                    {villa.price}
                  </span>
                  <span className="text-[11px] tracking-[0.2em] uppercase text-gray-400 group-hover:text-[var(--color-brand-sage)] transition-colors duration-300 group-focus-within:text-[var(--color-brand-sage)]">
                    Discover →
                  </span>
                </div>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
