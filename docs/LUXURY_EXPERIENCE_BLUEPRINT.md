# LUXURY EXPERIENCE BLUEPRINT

Official UI source of truth for Nhu Villas. Every public UI sprint must follow this blueprint and the `UI_QUALITY_CHECKLIST.md`.

## 1. Brand Experience

Users should feel calm, private, cinematic, and gently cared for within the first 5 seconds. The brand should not feel like a SaaS product, admin template, travel marketplace, or generic hotel theme.

- Brand emotion: stillness, privacy, warmth, quiet confidence.
- Brand personality: refined, attentive, grounded, human, never loud.
- Brand voice: short, sensory, precise, concierge-like.
- Visual identity: nature-led luxury, architectural calm, warm neutrals, forest depth, water, stone, paper, aged gold accents.
- Luxury experience: space, silence, image quality, typography, restraint, and clear conversion moments.

## 2. User Journey

Recommended public journey:

1. Landing
2. Hero
3. Story
4. Villa Collection
5. Villa Detail
6. Amenities
7. Experience
8. Review
9. Location
10. Booking
11. Footer

This order works because it moves from emotion to confidence to action. Users first understand the place, then inspect villas, then validate comfort and trust, then commit to booking.

## 3. Hero Blueprint

- Layout: cinematic full viewport, content lower-left or center-left, max 4 text elements.
- Media: short silent resort video preferred; art-directed image fallback at 2400px.
- Overlay: dark forest gradient from left and bottom, tuned for contrast without hiding the place.
- Typography: one H1, max 2 lines, refined display serif; body text under 20 words.
- CTA: primary `Check Availability`, secondary `Explore Villas` when distinct.
- Scroll indicator: subtle visual line or geometric cue only; no instructional text.
- Motion: media settles with slow scale, text reveals in sequence, CTA last.
- Camera feeling: quiet arrival shot, natural depth, no fast cuts.
- Depth: foreground texture, midground villa, background sky/water/forest.
- Performance: preload/eager LCP media, responsive `srcset`, explicit dimensions, no layout shift.

Current target score: 96+.

## 4. Navbar Blueprint

- Glass: translucent material with subtle border and readable fallback.
- Blur: restrained and only where it improves contrast.
- Sticky: reachable after Hero without covering content.
- Shrink: smaller height and stronger background after scroll.
- Hide/reveal: hide on scroll down, reveal on scroll up.
- Mobile: accessible drawer, 44px targets, focus return, Escape close.
- CTA: one booking intent, available on desktop and mobile.
- Language: compact, not visually dominant.
- Accessibility: semantic `nav`, aria labels/state, keyboard support.

## 5. Villa Collection Blueprint

- Cards: editorial villa cards, not dashboard cards.
- Hover: gentle image scale and CTA emphasis; keyboard focus equivalent.
- Image ratio: 4:5 or 3:4 for cards, cinematic ratios for detail pages.
- Badge: restrained, useful, never noisy.
- Price: transparent `From ... / night`.
- CTA: `View Villa` and `Check Dates` with distinct roles.
- Skeleton: same geometry as final cards.
- Loading: section-level, no full-page spinner.
- Empty state: calm explanation and next action.

## 6. Booking Experience Blueprint

Booking should feel like stay planning with a private concierge, not a traditional long form.

- Search: dates, guests, villa and preferences surfaced progressively.
- Calendar: accessible range selection, unavailable dates visible.
- Guest selector: steppers/chips with clear min/max rules.
- Price: itinerary-style summary with fees/taxes if available.
- Availability: calm immediate status, no fake urgency.
- Confirmation: stay itinerary summary.
- Success: booking/request code, email status, concierge contact, next step.

## 7. Motion Blueprint

Required motion:

- Hero media settle and copy reveal.
- Scroll storytelling reveals for major sections.
- Image mask or soft reveal for gallery.
- Subtle image-only parallax where performance allows.
- Reduced motion fallback for every complex motion.

Avoid:

- Random bouncing, spinning, noisy loops.
- Text parallax that harms readability.
- Heavy blur on scrolling layers.
- Scroll hijacking that breaks keyboard or anchors.

Lenis and GSAP are allowed only when they serve storytelling, hierarchy, feedback, or state transition.

## 8. Design System

- Colors: forest, pine, sage, stone, rice paper, ivory, aged gold or clay accent.
- Typography: Cormorant Garamond for display, Outfit for body/UI.
- Radius: 2px to 8px for cards/images; pill only for compact controls/buttons where appropriate.
- Shadow: soft tinted shadows only when needed.
- Border: low-contrast natural borders.
- Grid: 12 columns desktop, 8 tablet, 4 mobile, max width 1320-1440px.
- Spacing: base-8 rhythm, generous section padding.
- Icons: one light-stroke family.
- Buttons: clear hierarchy, high contrast, one-line labels, visible focus.
- Forms: label always present, helper/error text linked, booking as guided experience.

## 9. SEO Experience

- Heading hierarchy: one H1 per route; H2 sections for Story, Villas, Amenities, Experience, Reviews, Location, Booking, FAQ.
- Internal links: Hero to Villas/Booking, Villa Collection to detail, Footer to key routes.
- FAQ: real stay questions about dates, cancellation, transfer, villa choice, spa/dining.
- Story content: place-specific, sensory, concise.
- Image strategy: meaningful alt, responsive sizes, real resort/villa imagery.
- Schema: Hotel/LodgingBusiness, Breadcrumb, FAQ when content exists.
- Metadata: route-specific title/description/OG/canonical.

## 10. Conversion Strategy

- CTA placement: Hero, Villa Collection, Villa Detail, Booking summary, Footer.
- Trust: reviews, transparent pricing, policies, concierge contact.
- Social proof: specific reviews and guest context; no fake ratings.
- Scarcity: only truthful availability, never pressure copy.
- Funnel: desire -> inspect -> trust -> plan stay -> confirm.

## 11. Quality Target

| Area | Target |
|---|---:|
| Architecture | >=95 |
| UI | >=95 |
| UX | >=95 |
| Luxury Experience | >=95 |
| Motion | >=95 |
| SEO | >=98 |
| Performance | >=95 |
| Accessibility | >=95 |
| Responsive | >=95 |
| Conversion | >=95 |

## 12. UI Upgrade Roadmap

1. Sprint B3: Hero
2. Sprint B4: Navbar
3. Sprint B5: Villa Collection
4. Sprint B6: Booking UI
5. Sprint B7: Experience
6. Sprint B8: Gallery
7. Sprint B9: Footer
8. Sprint B10: SEO, Performance, Accessibility

This order protects quality because the Hero sets the emotional bar, Navbar defines movement and access, Villas create inspection confidence, Booking converts, and later content deepens trust.
