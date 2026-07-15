import { useEffect, useRef } from 'react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

const scenes = [
  {
    image: 'https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=1600&q=80',
    alt: 'Resort entrance framed by tropical gardens and bamboo gates',
    caption: 'Arrive',
    description: 'Pass through the bamboo gate and leave the world behind.',
  },
  {
    image: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=1600&q=80',
    alt: 'Tranquil water reflection pond surrounded by greenery',
    caption: 'Breathe',
    description: 'Walk along the reflection pond. Let your breath slow.',
  },
  {
    image: 'https://images.unsplash.com/photo-1540518614846-7eded433c457?auto=format&fit=crop&w=1600&q=80',
    alt: 'Minimalist bedroom interior with natural wood and linen textiles',
    caption: 'Rest',
    description: 'Sink into an interior sculpted from wood, stone, and light.',
  },
  {
    image: 'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=1600&q=80',
    alt: 'Private infinity pool with panoramic valley view at golden hour',
    caption: 'Float',
    description: 'Your private pool dissolves into the horizon.',
  },
]

/**
 * Desktop: horizontal scroll pinned storytelling.
 * Mobile: vertical stacked with full-viewport images (no horizontal scroll).
 */
export default function ScrollStorySection() {
  const sectionRef = useRef<HTMLElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!sectionRef.current || !containerRef.current) return

    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    // Only do horizontal scroll on desktop-width screens
    const isMobile = window.innerWidth < 768
    if (prefersReduced || isMobile) return

    const ctx = gsap.context(() => {
      const container = containerRef.current!
      const totalWidth = container.scrollWidth - window.innerWidth

      gsap.to(container, {
        x: -totalWidth,
        ease: 'none',
        scrollTrigger: {
          trigger: sectionRef.current,
          start: 'top top',
          end: () => `+=${totalWidth}`,
          pin: true,
          scrub: 1,
          anticipatePin: 1,
          invalidateOnRefresh: true,
        },
      })
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
    <section
      ref={sectionRef}
      className="relative overflow-hidden"
      aria-label="A visual journey through Như Villas"
    >
      {/* Desktop: horizontal panels */}
      <div
        ref={containerRef}
        className="hidden md:flex h-screen"
        style={{ width: `${scenes.length * 100}vw` }}
      >
        {scenes.map((scene, i) => (
          <div
            key={i}
            className="relative w-screen h-screen flex-shrink-0 flex items-end"
            role="img"
            aria-label={scene.alt}
          >
            <img
              src={scene.image}
              alt={scene.alt}
              className="absolute inset-0 w-full h-full object-cover"
              loading="lazy"
              decoding="async"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />

            <div className="relative z-10 p-12 lg:p-20 max-w-2xl mb-8 lg:mb-16">
              <p className="text-[11px] tracking-[0.35em] uppercase text-[var(--color-brand-sage)] mb-4">
                Scene {String(i + 1).padStart(2, '0')}
              </p>
              <h2 className="font-serif text-5xl lg:text-7xl text-white mb-6 italic">
                {scene.caption}
              </h2>
              <p className="text-lg text-gray-300 font-light leading-relaxed">
                {scene.description}
              </p>
            </div>
          </div>
        ))}
      </div>

      {/* Mobile: vertical stacked panels */}
      <div className="md:hidden">
        {scenes.map((scene, i) => (
          <div
            key={i}
            className="relative h-[85svh] flex items-end"
            role="img"
            aria-label={scene.alt}
          >
            <img
              src={scene.image}
              alt={scene.alt}
              className="absolute inset-0 w-full h-full object-cover"
              loading="lazy"
              decoding="async"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />

            <div className="relative z-10 p-8 max-w-lg mb-8">
              <p className="text-[10px] tracking-[0.35em] uppercase text-[var(--color-brand-sage)] mb-3">
                Scene {String(i + 1).padStart(2, '0')}
              </p>
              <h2 className="font-serif text-4xl text-white mb-4 italic">
                {scene.caption}
              </h2>
              <p className="text-base text-gray-300 font-light leading-relaxed">
                {scene.description}
              </p>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}
