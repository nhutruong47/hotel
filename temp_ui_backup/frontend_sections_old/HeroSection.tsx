import { useEffect, useRef } from 'react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

export default function HeroSection() {
  const sectionRef = useRef<HTMLElement>(null)
  const headlineRef = useRef<HTMLHeadingElement>(null)
  const subtextRef = useRef<HTMLParagraphElement>(null)
  const labelRef = useRef<HTMLParagraphElement>(null)
  const imageRef = useRef<HTMLImageElement>(null)
  const overlayRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!sectionRef.current) return

    // Respect prefers-reduced-motion
    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (prefersReduced) return

    const ctx = gsap.context(() => {
      const tl = gsap.timeline({ delay: 0.3 })

      tl.from(labelRef.current, {
        opacity: 0,
        y: 30,
        duration: 1,
        ease: 'power3.out',
      })
        .from(
          headlineRef.current,
          {
            opacity: 0,
            y: 60,
            duration: 1.4,
            ease: 'power3.out',
          },
          '-=0.6'
        )
        .from(
          subtextRef.current,
          {
            opacity: 0,
            y: 40,
            duration: 1,
            ease: 'power3.out',
          },
          '-=0.8'
        )

      gsap.to(imageRef.current, {
        yPercent: 20,
        ease: 'none',
        scrollTrigger: {
          trigger: sectionRef.current,
          start: 'top top',
          end: 'bottom top',
          scrub: true,
        },
      })

      gsap.to(overlayRef.current, {
        opacity: 0.6,
        ease: 'none',
        scrollTrigger: {
          trigger: sectionRef.current,
          start: 'top top',
          end: 'bottom top',
          scrub: true,
        },
      })
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
    <section
      ref={sectionRef}
      className="h-[100svh] lg:h-[110vh] relative flex flex-col items-center justify-center overflow-hidden"
      aria-label="Welcome to Nhu Villas"
    >

      {/* Background - eager load for LCP */}
      <img
        ref={imageRef}
        src={`https://images.unsplash.com/photo-1582719478250-c894e4dc240e?auto=format&fit=crop&w=2560&q=80`}
        srcSet={`https://images.unsplash.com/photo-1582719478250-c894e4dc240e?auto=format&fit=crop&w=640&q=75 640w,
          https://images.unsplash.com/photo-1582719478250-c894e4dc240e?auto=format&fit=crop&w=1280&q=80 1280w,
          https://images.unsplash.com/photo-1582719478250-c894e4dc240e?auto=format&fit=crop&w=2560&q=80 2560w`}
        sizes="100vw"

        alt="Aerial view of Nhu Villas luxury resort surrounded by pine forests and tranquil waterways"
        className="absolute inset-0 w-full h-[120%] object-cover -top-[10%]"
        loading="eager"
        fetchPriority="high"
        decoding="async"
      />


      <div
        ref={overlayRef}
        className="absolute inset-0 bg-[var(--color-brand-sand)] opacity-20 pointer-events-none"
      />

      <div className="absolute inset-0 bg-gradient-to-b from-[var(--color-brand-sand)]/50 via-transparent to-[var(--color-brand-sand)] pointer-events-none" />

      {/* Content */}
      <div className="relative z-10 max-w-5xl w-full text-center px-6 space-y-6 lg:space-y-8 mt-16">

        <p
          ref={labelRef}
          className="text-[10px] lg:text-[11px] tracking-[0.35em] uppercase text-gray-600"
        >
          A Sanctuary for the Soul
        </p>


        <h1
          ref={headlineRef}
          className="font-serif text-[2.8rem] leading-[1.1] sm:text-6xl md:text-7xl lg:text-[clamp(4.5rem,8vw,7rem)] lg:leading-[1.05] text-[var(--color-brand-charcoal)]"
        >
          Discover Stillness
          <br />
          <span className="italic font-light">in Every Moment.</span>
        </h1>


        <p
          ref={subtextRef}
          className="text-base sm:text-lg lg:text-xl font-light text-gray-600 max-w-2xl mx-auto leading-relaxed pt-2"
        >
          Embrace the harmony of nature and minimalist luxury. Nhu Villas offers
          a retreat where time slows down.
        </p>
      </div>


      {/* Scroll Indicator - hidden on short viewports */}
      <div
        className="absolute bottom-10 lg:bottom-14 left-1/2 -translate-x-1/2 flex flex-col items-center gap-3 z-10 hidden sm:flex"
        aria-hidden="true"
      >

        <span className="text-[10px] tracking-[0.25em] uppercase text-gray-500">
          Scroll to explore
        </span>
        <div className="w-[1px] h-10 bg-gray-400 animate-pulse" />
      </div>
    </section>
  )
}

