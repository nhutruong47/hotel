import { useEffect, type RefObject } from 'react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

/**
 * Fade-in + slide-up animation triggered on scroll.
 */
export function useReveal(ref: RefObject<HTMLElement | null>, options?: {
  y?: number
  duration?: number
  delay?: number
  start?: string
}) {
  useEffect(() => {
    if (!ref.current) return
    const el = ref.current

    gsap.set(el, { opacity: 0, y: options?.y ?? 60 })

    const tween = gsap.to(el, {
      opacity: 1,
      y: 0,
      duration: options?.duration ?? 1.2,
      delay: options?.delay ?? 0,
      ease: 'power3.out',
      scrollTrigger: {
        trigger: el,
        start: options?.start ?? 'top 85%',
        toggleActions: 'play none none none',
      },
    })

    return () => {
      tween.kill()
    }
  }, [ref, options?.delay, options?.duration, options?.start, options?.y])
}

/**
 * Parallax effect: element moves at a different speed than scroll.
 */
export function useParallax(ref: RefObject<HTMLElement | null>, speed: number = 0.3) {
  useEffect(() => {
    if (!ref.current) return
    const el = ref.current

    const tween = gsap.to(el, {
      yPercent: speed * 100,
      ease: 'none',
      scrollTrigger: {
        trigger: el,
        start: 'top bottom',
        end: 'bottom top',
        scrub: true,
      },
    })

    return () => {
      tween.kill()
    }
  }, [ref, speed])
}

/**
 * Text reveal: characters animate in one by one.
 */
export function useTextReveal(ref: RefObject<HTMLElement | null>) {
  useEffect(() => {
    if (!ref.current) return
    const el = ref.current
    const text = el.textContent || ''

    // Split text into spans without parsing HTML.
    el.textContent = ''
    text.split('').forEach((char) => {
      const span = document.createElement('span')
      span.className = 'inline-block'
      span.style.opacity = '0'
      span.style.transform = 'translateY(20px)'
      span.textContent = char === ' ' ? '\u00a0' : char
      el.appendChild(span)
    })

    const chars = el.querySelectorAll('span')

    const tween = gsap.to(chars, {
      opacity: 1,
      y: 0,
      duration: 0.6,
      stagger: 0.02,
      ease: 'power2.out',
      scrollTrigger: {
        trigger: el,
        start: 'top 80%',
        toggleActions: 'play none none none',
      },
    })

    return () => {
      tween.kill()
      el.textContent = text
    }
  }, [ref])
}
