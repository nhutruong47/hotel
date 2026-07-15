import { useEffect, useRef } from 'react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

export default function SunsetCTASection() {
  const sectionRef = useRef<HTMLElement>(null)
  const textRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!sectionRef.current) return
    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (prefersReduced) return

    const ctx = gsap.context(() => {
      gsap.from(textRef.current, {
        opacity: 0,
        y: 80,
        duration: 1.6,
        ease: 'power3.out',
        scrollTrigger: {
          trigger: sectionRef.current,
          start: 'top 40%',
        },
      })

      const bg = sectionRef.current?.querySelector('.sunset-bg')
      if (bg) {
        gsap.to(bg, {
          scale: 1.1,
          ease: 'none',
          scrollTrigger: {
            trigger: sectionRef.current,
            start: 'top bottom',
            end: 'bottom top',
            scrub: true,
          },
        })
      }
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
    <section
      ref={sectionRef}
      id="reserve"
      className="relative h-[70svh] sm:h-[75svh] lg:h-[80vh] flex items-center justify-center overflow-hidden"
      aria-labelledby="cta-heading"
    >
      <img
        src="https://images.unsplash.com/photo-1506929562872-bb421503ef21?auto=format&fit=crop&w=2560&q=80"
        srcSet="
          https://images.unsplash.com/photo-1506929562872-bb421503ef21?auto=format&fit=crop&w=640&q=75 640w,
          https://images.unsplash.com/photo-1506929562872-bb421503ef21?auto=format&fit=crop&w=1280&q=80 1280w,
          https://images.unsplash.com/photo-1506929562872-bb421503ef21?auto=format&fit=crop&w=2560&q=80 2560w
        "
        sizes="100vw"
        alt="Golden sunset casting warm light over an endless ocean horizon"
        className="sunset-bg absolute inset-0 w-full h-full object-cover"
        loading="lazy"
        decoding="async"
      />
      <div className="absolute inset-0 bg-black/40" />

      <div ref={textRef} className="relative z-10 text-center px-6 max-w-3xl">
        <p className="text-[10px] lg:text-[11px] tracking-[0.35em] uppercase text-[var(--color-brand-sage)] mb-6 lg:mb-8">
          Your Journey Begins
        </p>
        <h2
          id="cta-heading"
          className="font-serif text-4xl sm:text-5xl lg:text-7xl text-white leading-[1.1] mb-8 lg:mb-10 italic"
        >
          Let stillness
          <br />
          find you.
        </h2>
        <a
          href="#booking"
          className="inline-block px-10 py-4 sm:px-14 sm:py-5 bg-white text-[var(--color-brand-charcoal)] text-[11px] tracking-[0.2em] uppercase hover:bg-[var(--color-brand-sand)] transition-colors duration-300 rounded-[2px] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-white"
          role="link"
        >
          Check Availability
        </a>
      </div>
    </section>
  )
}
