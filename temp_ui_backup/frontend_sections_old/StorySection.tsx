import { useEffect, useRef } from 'react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

export default function StorySection() {
  const sectionRef = useRef<HTMLElement>(null)
  const imageRef = useRef<HTMLDivElement>(null)
  const textRef = useRef<HTMLDivElement>(null)
  const accentRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!sectionRef.current) return
    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (prefersReduced) return

    const ctx = gsap.context(() => {
      gsap.from(imageRef.current, {
        clipPath: 'inset(100% 0% 0% 0%)',
        duration: 1.4,
        ease: 'power4.inOut',
        scrollTrigger: {
          trigger: sectionRef.current,
          start: 'top 70%',
        },
      })

      const textEls = textRef.current?.children
      if (textEls) {
        gsap.from(Array.from(textEls), {
          opacity: 0,
          y: 50,
          duration: 1,
          stagger: 0.15,
          ease: 'power3.out',
          scrollTrigger: {
            trigger: textRef.current,
            start: 'top 75%',
          },
        })
      }

      gsap.to(accentRef.current, {
        y: -40,
        ease: 'none',
        scrollTrigger: {
          trigger: sectionRef.current,
          start: 'top bottom',
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
      id="story"
      className="py-20 sm:py-28 lg:py-40 px-6 md:px-12 max-w-[1400px] mx-auto flex flex-col md:flex-row gap-12 lg:gap-20 xl:gap-32 items-center"
      aria-labelledby="story-heading"
    >
      {/* Image */}
      <div className="w-full md:w-1/2 relative">
        <div
          ref={imageRef}
          className="aspect-[3/4] sm:aspect-[4/5] md:aspect-[3/4] overflow-hidden rounded-[2px]"
          style={{ clipPath: 'inset(0% 0% 0% 0%)' }}
        >
          <img
            src="https://images.unsplash.com/photo-1542314831-c6a4d27ce66f?auto=format&fit=crop&w=1200&q=80"
            srcSet="
              https://images.unsplash.com/photo-1542314831-c6a4d27ce66f?auto=format&fit=crop&w=600&q=75 600w,
              https://images.unsplash.com/photo-1542314831-c6a4d27ce66f?auto=format&fit=crop&w=1200&q=80 1200w
            "
            sizes="(max-width: 768px) 100vw, 50vw"
            alt="Japanese minimalist architecture bathed in warm natural light, showcasing raw wood and clean lines"
            className="w-full h-full object-cover"
            loading="lazy"
            decoding="async"
          />
        </div>
        <div
          ref={accentRef}
          className="hidden md:block absolute -bottom-10 -right-10 w-2/3 aspect-[4/5] bg-[var(--color-brand-stone)] -z-10 rounded-[2px]"
          aria-hidden="true"
        />
      </div>

      {/* Text */}
      <div ref={textRef} className="w-full md:w-1/2 space-y-6 lg:space-y-10">
        <p className="text-[10px] lg:text-[11px] tracking-[0.3em] uppercase text-[var(--color-brand-sage)]">
          Our Philosophy
        </p>

        <h2
          id="story-heading"
          className="text-3xl sm:text-4xl lg:text-[3.2rem] font-serif text-[var(--color-brand-charcoal)] leading-[1.15]"
        >
          Rooted in nature,
          <br />
          <span className="italic">designed for peace.</span>
        </h2>

        <p className="text-base lg:text-[17px] font-light text-gray-500 leading-[1.85]">
          Inspired by Japanese Wabi-Sabi and contemporary minimalism, every villa
          at Như is crafted to blend seamlessly with its natural surroundings. We
          believe that true luxury lies in simplicity, space, and silence.
        </p>

        <a
          href="#about"
          className="inline-block pb-2 border-b border-[var(--color-brand-charcoal)] text-[11px] tracking-[0.2em] uppercase hover:text-gray-500 hover:border-gray-400 transition-colors duration-300 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[var(--color-brand-sage)]"
        >
          Discover Our Story
        </a>
      </div>
    </section>
  )
}
