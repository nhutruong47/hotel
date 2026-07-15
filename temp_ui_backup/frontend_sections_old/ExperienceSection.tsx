import { useEffect, useRef } from 'react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

const experiences = [
  {
    title: 'Holistic Spa',
    description:
      'Restore your balance with ancient therapies and organic botanical treatments, performed in open-air pavilions surrounded by bamboo forests.',
    image: 'https://images.unsplash.com/photo-1544161515-4ab6ce6db874?auto=format&fit=crop&w=1200&q=80',
    imageSm: 'https://images.unsplash.com/photo-1544161515-4ab6ce6db874?auto=format&fit=crop&w=600&q=75',
    alt: 'Peaceful spa treatment room with warm candlelight and natural botanical elements',
  },
  {
    title: 'Omakase Dining',
    description:
      'A culinary journey honoring local ingredients and Japanese precision. Savor dishes crafted daily by our master chefs in an intimate setting.',
    image: 'https://images.unsplash.com/photo-1414235077428-338988a2e8c0?auto=format&fit=crop&w=1200&q=80',
    imageSm: 'https://images.unsplash.com/photo-1414235077428-338988a2e8c0?auto=format&fit=crop&w=600&q=75',
    alt: 'Elegantly plated omakase course in a refined dining atmosphere',
  },
]

export default function ExperienceSection() {
  const sectionRef = useRef<HTMLElement>(null)
  const itemsRef = useRef<(HTMLDivElement | null)[]>([])

  useEffect(() => {
    if (!sectionRef.current) return
    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (prefersReduced) return

    const ctx = gsap.context(() => {
      itemsRef.current.forEach((item, i) => {
        if (!item) return

        const img = item.querySelector('.exp-image')
        const text = item.querySelector('.exp-text')

        if (img) {
          gsap.from(img, {
            clipPath: i % 2 === 0 ? 'inset(0% 100% 0% 0%)' : 'inset(0% 0% 0% 100%)',
            duration: 1.4,
            ease: 'power4.inOut',
            scrollTrigger: { trigger: item, start: 'top 65%' },
          })
        }

        if (text) {
          gsap.from(text.children, {
            opacity: 0,
            y: 40,
            duration: 1,
            stagger: 0.12,
            ease: 'power3.out',
            scrollTrigger: { trigger: text, start: 'top 75%' },
          })
        }
      })
    }, sectionRef)

    return () => ctx.revert()
  }, [])

  return (
    <section
      ref={sectionRef}
      id="experience"
      className="py-20 sm:py-28 lg:py-40 px-6 md:px-12"
      aria-labelledby="experience-heading"
    >
      <div className="max-w-[1400px] mx-auto">
        <div className="text-center mb-16 lg:mb-32 space-y-4 lg:space-y-5">
          <p className="text-[10px] lg:text-[11px] tracking-[0.3em] uppercase text-[var(--color-brand-sage)]">
            Wellness & Dining
          </p>
          <h2
            id="experience-heading"
            className="text-3xl sm:text-4xl lg:text-[3.5rem] font-serif text-[var(--color-brand-charcoal)]"
          >
            Curated Experiences
          </h2>
        </div>

        <div className="space-y-20 sm:space-y-28 lg:space-y-48">
          {experiences.map((exp, i) => (
            <div
              key={exp.title}
              ref={(el) => { itemsRef.current[i] = el }}
              className={`flex flex-col gap-10 lg:gap-24 items-center ${
                i % 2 === 0 ? 'md:flex-row' : 'md:flex-row-reverse'
              }`}
            >
              <div
                className="exp-image w-full md:w-3/5 aspect-[4/3] overflow-hidden rounded-[2px]"
                style={{ clipPath: 'inset(0% 0% 0% 0%)' }}
              >
                <img
                  src={exp.image}
                  srcSet={`${exp.imageSm} 600w, ${exp.image} 1200w`}
                  sizes="(max-width: 768px) 100vw, 60vw"
                  alt={exp.alt}
                  className="w-full h-full object-cover"
                  loading="lazy"
                  decoding="async"
                />
              </div>

              <div className="exp-text w-full md:w-2/5 space-y-6 lg:space-y-8">
                <h3 className="font-serif text-2xl sm:text-3xl lg:text-[2.5rem] text-[var(--color-brand-charcoal)] leading-snug">
                  {exp.title}
                </h3>
                <p className="text-base lg:text-[17px] font-light text-gray-500 leading-[1.85]">
                  {exp.description}
                </p>
                <a
                  href="#"
                  className="inline-block pb-2 border-b border-[var(--color-brand-charcoal)] text-[11px] tracking-[0.2em] uppercase hover:text-gray-500 hover:border-gray-400 transition-colors duration-300 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[var(--color-brand-sage)]"
                >
                  Learn More
                </a>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
