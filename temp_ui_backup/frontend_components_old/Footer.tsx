import { useEffect, useRef } from 'react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

export default function Footer() {
  const footerRef = useRef<HTMLElement>(null)

  useEffect(() => {
    if (!footerRef.current) return
    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (prefersReduced) return

    const ctx = gsap.context(() => {
      const cols = footerRef.current?.querySelectorAll('.footer-col')
      if (cols) {
        gsap.from(Array.from(cols), {
          opacity: 0,
          y: 30,
          duration: 0.8,
          stagger: 0.1,
          ease: 'power3.out',
          scrollTrigger: {
            trigger: footerRef.current,
            start: 'top 85%',
          },
        })
      }
    }, footerRef)

    return () => ctx.revert()
  }, [])

  return (
    <footer
      ref={footerRef}
      className="bg-[var(--color-brand-charcoal)] text-[var(--color-brand-stone)] pt-16 sm:pt-20 lg:pt-28 pb-10 lg:pb-12 px-6 md:px-12"
      role="contentinfo"
    >
      <div className="max-w-[1400px] mx-auto grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-12 gap-10 lg:gap-8 mb-16 lg:mb-24">
        {/* Brand */}
        <div className="footer-col sm:col-span-2 lg:col-span-4 space-y-5 lg:space-y-6">
          <div className="font-serif text-2xl lg:text-3xl tracking-[0.2em] uppercase text-white">
            Như Villas
          </div>
          <p className="text-sm font-light text-gray-400 max-w-xs leading-[1.8]">
            A luxury boutique resort blending Japanese minimalism with profound
            tranquility, nestled within ancient pine forests.
          </p>
        </div>

        {/* Navigation */}
        <nav className="footer-col lg:col-span-2 space-y-4 lg:space-y-5" aria-label="Footer Navigation">
          <h4 className="text-[11px] tracking-[0.2em] uppercase text-white mb-2 lg:mb-4">
            Explore
          </h4>
          <a href="#villas" className="block text-sm font-light text-gray-400 hover:text-white transition-colors duration-300 focus-visible:outline-1 focus-visible:outline-offset-2 focus-visible:outline-white">
            Villas
          </a>
          <a href="#experience" className="block text-sm font-light text-gray-400 hover:text-white transition-colors duration-300 focus-visible:outline-1 focus-visible:outline-offset-2 focus-visible:outline-white">
            Spa & Wellness
          </a>
          <a href="#dining" className="block text-sm font-light text-gray-400 hover:text-white transition-colors duration-300 focus-visible:outline-1 focus-visible:outline-offset-2 focus-visible:outline-white">
            Dining
          </a>
          <a href="#" className="block text-sm font-light text-gray-400 hover:text-white transition-colors duration-300 focus-visible:outline-1 focus-visible:outline-offset-2 focus-visible:outline-white">
            Gallery
          </a>
        </nav>

        {/* Contact */}
        <address className="footer-col lg:col-span-2 space-y-4 lg:space-y-5 not-italic">
          <h4 className="text-[11px] tracking-[0.2em] uppercase text-white mb-2 lg:mb-4">
            Contact
          </h4>
          <p className="text-sm font-light text-gray-400 leading-[1.8]">
            123 Tranquil Lane
            <br />
            Pine Forest, Việt Nam
          </p>
          <p className="text-sm font-light text-gray-400 leading-[1.8]">
            <a href="tel:+84123456789" className="hover:text-white transition-colors">+84 123 456 789</a>
            <br />
            <a href="mailto:reserve@nhuvillas.com" className="hover:text-white transition-colors">reserve@nhuvillas.com</a>
          </p>
        </address>

        {/* Newsletter */}
        <div className="footer-col sm:col-span-2 lg:col-span-4 space-y-5 lg:space-y-6">
          <h4 className="text-[11px] tracking-[0.2em] uppercase text-white mb-2 lg:mb-4">
            Newsletter
          </h4>
          <p className="text-sm font-light text-gray-400">
            Subscribe for exclusive offers and seasonal stories.
          </p>
          <form className="flex border-b border-gray-600 pb-3" onSubmit={(e) => e.preventDefault()}>
            <label htmlFor="footer-email" className="sr-only">Email address</label>
            <input
              id="footer-email"
              type="email"
              placeholder="Email Address"
              autoComplete="email"
              className="bg-transparent border-none outline-none w-full text-sm font-light placeholder-gray-500 text-white focus-visible:outline-none"
              required
            />
            <button
              type="submit"
              className="text-[11px] tracking-[0.15em] uppercase text-gray-400 hover:text-white transition-colors duration-300 whitespace-nowrap focus-visible:outline-1 focus-visible:outline-offset-2 focus-visible:outline-white"
            >
              Submit
            </button>
          </form>
        </div>
      </div>

      {/* Bottom Bar */}
      <div className="border-t border-gray-800 pt-6 lg:pt-8 flex flex-col sm:flex-row justify-between items-center gap-4 text-[11px] font-light text-gray-500">
        <p>© 2026 Như Villas. All rights reserved.</p>
        <div className="flex gap-6 lg:gap-8">
          <a href="#" className="hover:text-white transition-colors duration-300 focus-visible:outline-1 focus-visible:outline-offset-2 focus-visible:outline-white">
            Privacy Policy
          </a>
          <a href="#" className="hover:text-white transition-colors duration-300 focus-visible:outline-1 focus-visible:outline-offset-2 focus-visible:outline-white">
            Terms of Service
          </a>
        </div>
      </div>
    </footer>
  )
}
