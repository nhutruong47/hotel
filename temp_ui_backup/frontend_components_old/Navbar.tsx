import { useEffect, useRef } from 'react'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

export default function Navbar() {
  const navRef = useRef<HTMLElement>(null)
  const menuRef = useRef<HTMLDivElement>(null)
  const menuBtnRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (!navRef.current) return

    const showAnim = gsap
      .from(navRef.current, {
        yPercent: -100,
        paused: true,
        duration: 0.3,
        ease: 'power2.out',
      })
      .progress(1)

    ScrollTrigger.create({
      start: 'top top',
      end: 'max',
      onUpdate: (self) => {
        if (self.direction === -1) showAnim.play()
        else showAnim.reverse()
      },
    })

    return () => { showAnim.kill() }
  }, [])

  const toggleMenu = () => {
    const menu = menuRef.current
    if (!menu) return
    const isOpen = menu.getAttribute('data-open') === 'true'
    menu.setAttribute('data-open', isOpen ? 'false' : 'true')
    menuBtnRef.current?.setAttribute('aria-expanded', isOpen ? 'false' : 'true')

    if (!isOpen) {
      menu.classList.remove('translate-x-full')
      menu.classList.add('translate-x-0')
    } else {
      menu.classList.remove('translate-x-0')
      menu.classList.add('translate-x-full')
    }
  }

  return (
    <nav
      ref={navRef}
      className="fixed w-full top-0 z-50"
      role="navigation"
      aria-label="Main Navigation"
    >
      <div className="bg-[var(--color-brand-sand)]/80 backdrop-blur-xl border-b border-[var(--color-brand-stone)]/60">
        <div className="max-w-[1400px] mx-auto px-6 lg:px-8 h-20 lg:h-24 flex items-center justify-between">
          {/* Logo */}
          <a
            href="/"
            className="font-serif text-xl lg:text-2xl tracking-[0.25em] uppercase text-[var(--color-brand-charcoal)] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[var(--color-brand-sage)]"
            aria-label="Như Villas — Home"
          >
            Như Villas
          </a>

          {/* Desktop Nav Links */}
          <div className="hidden lg:flex items-center gap-10 xl:gap-12" role="menubar">
            {['Story', 'Villas', 'Experience', 'Dining'].map((item) => (
              <a
                key={item}
                href={`#${item.toLowerCase()}`}
                role="menuitem"
                className="text-[11px] tracking-[0.2em] uppercase text-gray-500 hover:text-[var(--color-brand-ink)] transition-colors duration-300 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[var(--color-brand-sage)]"
              >
                {item}
              </a>
            ))}
          </div>

          {/* Desktop CTA */}
          <a
            href="#reserve"
            className="hidden lg:inline-block px-7 py-3 bg-[var(--color-brand-charcoal)] text-white text-[11px] tracking-[0.2em] uppercase hover:bg-black transition-colors duration-300 rounded-[2px] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[var(--color-brand-sage)]"
          >
            Reserve
          </a>

          {/* Mobile Menu Button */}
          <button
            ref={menuBtnRef}
            onClick={toggleMenu}
            className="lg:hidden w-10 h-10 flex flex-col items-center justify-center gap-1.5 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[var(--color-brand-sage)]"
            aria-label="Open menu"
            aria-expanded="false"
            aria-controls="mobile-menu"
          >
            <span className="w-6 h-[1.5px] bg-[var(--color-brand-charcoal)]" />
            <span className="w-4 h-[1.5px] bg-[var(--color-brand-charcoal)]" />
          </button>
        </div>
      </div>

      {/* Mobile Menu Overlay */}
      <div
        ref={menuRef}
        id="mobile-menu"
        data-open="false"
        className="lg:hidden fixed inset-0 top-0 bg-[var(--color-brand-sand)] z-40 flex flex-col items-center justify-center gap-10 translate-x-full transition-transform duration-500 ease-out"
        role="menu"
      >
        <button
          onClick={toggleMenu}
          className="absolute top-6 right-6 w-10 h-10 flex items-center justify-center text-2xl text-[var(--color-brand-charcoal)] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[var(--color-brand-sage)]"
          aria-label="Close menu"
        >
          ✕
        </button>
        {['Story', 'Villas', 'Experience', 'Dining'].map((item) => (
          <a
            key={item}
            href={`#${item.toLowerCase()}`}
            role="menuitem"
            onClick={toggleMenu}
            className="font-serif text-3xl tracking-[0.15em] text-[var(--color-brand-charcoal)] hover:text-[var(--color-brand-sage)] transition-colors"
          >
            {item}
          </a>
        ))}
        <a
          href="#reserve"
          onClick={toggleMenu}
          className="mt-8 px-10 py-4 bg-[var(--color-brand-charcoal)] text-white text-xs tracking-[0.2em] uppercase rounded-[2px]"
        >
          Reserve Now
        </a>
      </div>
    </nav>
  )
}
