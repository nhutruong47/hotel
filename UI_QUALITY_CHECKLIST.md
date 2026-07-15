# UI QUALITY CHECKLIST

Official review document for Như Villas UI sprints from Sprint B3 onward.

This document is a scorecard and approval gate. It does not replace the Luxury Experience Blueprint. It converts that blueprint into measurable review criteria.

## 0. Review Rules

- Scope: public luxury UI only. Do not use this checklist to approve Admin, Auth, Report, or Booking Write logic unless a sprint explicitly includes those areas.
- Required pass threshold: every final category must score at least 95.
- Any category below 95 means the sprint is not approved.
- Reviewer must list root causes and improvement actions for every category below 95.
- Do not compare against generic hotel templates. Compare against Aman Resorts, Six Senses, Rosewood, Bulgari Hotels, and Airbnb Luxe quality.
- Do not approve SaaS, dashboard, Material Design, Bootstrap, or generic template patterns.
- Do not approve UI that is visually worse than the previous public flow.
- Do not approve if build, responsive layout, accessibility, or Core Web Vitals are broken.

Scoring scale:

| Score | Meaning |
|---|---|
| 95-100 | Luxury production quality. Approved if no blocking issue exists. |
| 85-94 | Good direction but not sprint-pass quality. Must improve. |
| 70-84 | Functional but below luxury standard. Not approved. |
| 50-69 | Partial implementation or visible quality debt. Not approved. |
| 0-49 | Missing, broken, inaccessible, or off-brand. Not approved. |

Current score note:

- "Current Baseline Score" captures the B2.5 known state from Sprint B1/B2 analysis.
- For every later sprint, replace it with the actual score from that sprint review.

## 1. Hero Quality Scorecard

Target total score: 96 or higher.

| Criterion | Current Baseline Score | Target Score | Max Score Conditions | Common Failures | Improvement Actions |
|---|---:|---:|---|---|---|
| Visual Impact | 60 | 96 | First 5 seconds feel cinematic, memorable, place-specific, and image-led. Hero has real resort media, clear depth, and no empty gradient-only composition. | Generic stock crop, dark image with unreadable text, no sense of Như Villas, flat background. | Use art-directed hero video or image, crop around architecture/nature/depth, add subtle overlay and foreground/midground/background layers. |
| Luxury Feeling | 58 | 97 | Feels calm, private, expensive, and restrained. No noisy decoration. Luxury comes from material, silence, scale, photography, and spacing. | SaaS-style hero, loud color, over-designed badges, generic marketing copy. | Reduce visual clutter, use natural palette, refine copy, increase whitespace, let media carry emotion. |
| Typography | 55 | 96 | H1 max 2 lines desktop, readable on mobile, refined display font, body text under 20 words, no clipped italic descenders. | H1 too large, 3-4 line headline, mixed fonts without reason, text wraps awkwardly. | Adjust type scale to image, use one display family and one body family, test 375px, 768px, 1024px, 1440px. |
| Hierarchy | 58 | 96 | Eye lands on brand/place, then H1, then CTA. Max 4 text elements in hero. CTA visible without scroll. | Too many micro-labels, hidden CTA, overlong subcopy, trust strip inside hero. | Remove secondary microcopy, keep headline/subtext/CTA, move trust content below hero. |
| CTA | 20 | 96 | Primary CTA says "Check Availability" or equivalent booking intent. Secondary CTA has distinct intent. Both are high contrast and one-line. | CTA missing, button has no action, duplicate intent, low contrast, label wraps. | Add clear primary CTA, define secondary "Explore Villas", ensure button contrast and target size. |
| Conversion | 25 | 95 | Hero starts booking path without pressure. CTA connects to booking/search or villa selection. Value is clear within 5 seconds. | No booking path, vague "Book Now", CTA not wired, form pushed too early. | Link hero CTA to availability or villa collection, add lightweight booking entry after hero. |
| Motion | 15 | 95 | Hero has motivated reveal: media settles, text enters in sequence, CTA appears last. Reduced motion disables complex movement. | Static but claims cinematic, excessive zoom, no reduced motion, animation jank. | Add subtle timeline, animate only transform/opacity, use reduced-motion fallback. |
| Performance | 45 | 95 | LCP under 2.5s, hero media preloaded/eager, explicit aspect ratio, optimized WebP/AVIF/poster, no layout shift. | Lazy-loaded hero, 4K image on mobile, video too heavy, font shift. | Use responsive srcset, poster, preload hero, compress media, reserve dimensions. |
| Accessibility | 64 | 95 | Text contrast passes AA/AAA, image has meaningful alt or decorative handling, skip link works, CTA keyboard accessible. | White text over bright image, missing alt, no focus state, keyboard trap. | Strengthen overlay, audit contrast, verify Tab order and focus-visible. |
| Responsive | 60 | 96 | Hero works from 320px to 1920px with no horizontal scroll, CTA visible, no text overlap, mobile crop intentional. | Desktop shrunk onto mobile, H1 overflow, image subject cropped away. | Create mobile crop strategy, reduce type scale, use min-height with dynamic viewport units. |
| SEO | 62 | 98 | One H1, meaningful title/description, crawlable hero copy, canonical and OG match real route. | Multiple H1s, image-only headline, metadata mismatch, missing OG image. | Validate heading tree, align metadata, ensure hero text is real text. |

Hero pass conditions:

- Total score must be 96 or higher.
- CTA, Accessibility, Performance, and Responsive must each be at least 95.
- If the hero looks less premium than the previous Thymeleaf public hero, sprint fails.

## 2. Navbar Quality Scorecard

Target total score: 95 or higher.

| Criterion | Current Baseline Score | Target Score | Max Score Conditions | Common Failures | Improvement Actions |
|---|---:|---:|---|---|---|
| Glass Effect | 25 | 96 | Glass feels physical: translucent fill, inner border, subtle highlight, solid fallback. | Plain transparent nav, generic blur, unreadable over media. | Use layered glass token, test over light/dark hero areas, add fallback background. |
| Blur | 20 | 95 | Blur only on fixed/sticky nav or overlay. It improves readability without GPU cost. | Heavy blur on scrolling containers, blur too weak, blur causing text fuzziness. | Limit blur scope, tune opacity, verify performance on mobile. |
| Sticky | 20 | 95 | Nav remains reachable after hero and does not block content. Height 64-72px desktop, max 80px. | Oversized 96px nav, content hidden under nav, unstable sticky behavior. | Define sticky offset, reserve top spacing where needed, reduce height. |
| Shrink | 0 | 95 | Nav reduces height/brand scale after scroll and increases background opacity smoothly. | No state change, sudden jump, layout shift. | Add scroll state with transform/opacity only, no layout-thrashing calculations. |
| Mobile | 35 | 96 | Hamburger target at least 44px, menu opens smoothly, focus is trapped, CTA and language are accessible. | Menu missing, tiny tap target, close button inaccessible, background scrolls. | Use modal/drawer behavior, body scroll lock, focus return, visible CTA. |
| Accessibility | 60 | 96 | Semantic nav, aria-label, aria-expanded, aria-controls, Escape closes mobile menu, focus ring visible. | Divs as buttons, no aria state, keyboard cannot close menu. | Use semantic buttons/links, test Tab/Shift+Tab/Escape. |
| CTA | 35 | 96 | Primary CTA visible desktop, available in mobile menu, clear booking intent, no duplicate intent. | Button has no destination, vague label, hidden on mobile. | Standardize "Check Availability", route to booking/search, keep one intent. |
| Navigation | 55 | 95 | 5-7 max items, labels clear: Villas, Experiences, Dining, Gallery, Location. Active state exists. | Too many links, route mismatch, dead anchors, jargon. | Align nav with IA and actual routes, remove filler links. |
| Responsive | 55 | 96 | One-line desktop nav, no wrapping at 1024px, no horizontal scroll mobile. | Two-line nav, logo/CTA collision, language switch overflow. | Reduce labels, switch to hamburger earlier, test 1024px and 375px. |
| Motion | 10 | 95 | Hide on scroll down, reveal on scroll up, shrink state smooth, menu links stagger tastefully. | Jumpy hide, motion for no reason, no reduced motion. | Use transform-only transitions, add reduced-motion static behavior. |

Navbar pass conditions:

- Total score must be 95 or higher.
- Mobile, Accessibility, CTA, and Navigation must each be at least 95.
- Nav must never feel like SaaS, admin, Bootstrap, or Material Design.

## 3. Villa Collection Scorecard

Target total score: 96 or higher.

| Criterion | Current Baseline Score | Target Score | Max Score Conditions | Common Failures | Improvement Actions |
|---|---:|---:|---|---|---|
| Card Layout | 15 | 96 | Editorial villa cards, asymmetric rhythm, generous whitespace, entire card/link semantics clear. | Three equal generic cards, dashboard cards, card inside card. | Use varied image sizes, article semantics, no nested decorative containers. |
| Image Quality | 20 | 98 | Images look resort-specific, high resolution, well cropped, real/generative art-directed, no broken URLs. | Generic bedroom stock, dark crop, repeated image tone. | Create villa-specific art direction and responsive image set. |
| Image Ratio | 20 | 96 | Cards use intentional 4:5, 3:4, or editorial ratio. Detail pages use wider cinematic media. | Mixed accidental ratios, CLS from unreserved images, mobile crop broken. | Define aspect-ratio tokens and test cards across breakpoints. |
| Hover | 10 | 95 | Hover gently reveals intent: image scale, text shift, CTA emphasis. Keyboard focus equivalent exists. | Shadow pop, over-animation, hover-only affordance. | Use transform/opacity only, add focus-visible styling. |
| Typography | 30 | 96 | Villa name, specs, price, and CTA form clear hierarchy. Text does not overwhelm image. | Tiny unreadable meta, overlong descriptions, inconsistent fonts. | Limit copy, use body font for meta, display font for villa name. |
| Price | 10 | 95 | Price is transparent, readable, not visually aggressive. Includes unit/night or stay context. | Missing price, fake scarcity, price hidden until late. | Add "From ... / night", clarify taxes/fees later in booking. |
| CTA | 10 | 96 | "View Villa" and "Check Dates" have distinct roles. Card click and CTA do not conflict. | CTA missing, duplicate "Book Now" everywhere, tiny tap target. | Define card-level navigation and explicit booking CTA. |
| Loading | 0 | 95 | Loading state preserves layout, does not block whole page for section-level data. | Full-page spinner, blank villa section. | Add section-level loading with skeleton cards. |
| Skeleton | 0 | 95 | Skeleton matches final card geometry at least 90%, reserves image ratio and text lines. | Generic spinner, skeleton mismatch causing layout jump. | Build card-shaped skeleton with exact media ratio. |
| Empty State | 0 | 95 | Empty state explains no villas match current filters/dates and offers next action. | "No data", technical API message, blank area. | Add copy and CTA: adjust dates, clear filters, contact concierge. |
| Conversion | 15 | 96 | Collection guides user from desire to detail/availability. Best use cases are scannable. | Browsing dead-end, too much text, no availability path. | Add clear villa intent tags and route to detail or booking search. |

Villa Collection pass conditions:

- Total score must be 96 or higher.
- Image Quality, CTA, Loading/Skeleton, Empty State, and Conversion must each be at least 95.

## 4. Booking Experience Scorecard

Target total score: 96 or higher.

| Criterion | Current Baseline Score | Target Score | Max Score Conditions | Common Failures | Improvement Actions |
|---|---:|---:|---|---|---|
| Search | 10 | 96 | Booking starts as concierge-like stay planning, not a long form. Dates, guests, villa, and preferences are easy. | Traditional dense form, too many fields, no guidance. | Convert to step-based search cards with clear labels. |
| Calendar | 0 | 96 | Date range is clear, unavailable dates visible, mobile bottom sheet usable. | Native date inputs only, no availability context, tiny tap targets. | Use accessible calendar pattern with range summary and disabled states. |
| Availability | 0 | 97 | Availability feedback is immediate, clear, and calm: available, unavailable, request concierge. | Silent failure, API status text, fake urgency. | Add inline availability states and helpful alternatives. |
| Guest Selector | 0 | 95 | Adults/children/rooms use steppers or chips, min/max rules clear, touch targets pass. | Free numeric input only, unclear limits, hidden validation. | Use steppers with helper copy and validation. |
| Pricing | 0 | 97 | Pricing is transparent: nightly rate, nights, fees/taxes if available, total before confirmation. | Hidden fees, price appears at end, unclear currency. | Add itinerary price summary and currency formatting. |
| UX | 15 | 96 | Fewest steps possible, no dead ends, clear progress, recoverable errors. | Long form, user loses context, no back/edit. | Keep summary visible, allow edit per section. |
| Confirmation | 0 | 96 | Review screen reads like an itinerary: villa, dates, guests, inclusions, policy. | Raw form summary, missing policy, no confidence. | Design confirmation as stay summary with trust notes. |
| Success Page | 0 | 96 | Success confirms booking/request, gives booking code, email status, concierge contact, next step. | Plain success text, no next action, no reassurance. | Add confirmation details, contact route, calendar/download if appropriate. |
| Conversion | 10 | 96 | Funnel reduces anxiety and supports both instant booking and concierge request. | Too much friction, no trust, no mobile sticky summary. | Add trust copy, payment/security cues, sticky booking summary. |

Booking pass conditions:

- Total score must be 96 or higher.
- Pricing, Availability, Accessibility, and Conversion must each be at least 95.
- Booking UI must not modify booking write logic unless sprint scope explicitly allows it.

## 5. Motion Scorecard

Target total score: 95 or higher.

| Criterion | Current Baseline Score | Target Score | Max Score Conditions | Common Failures | Improvement Actions |
|---|---:|---:|---|---|---|
| Hero Timeline | 15 | 96 | Media, heading, subtext, and CTA reveal in a calm sequence with no delay frustration. | Everything appears at once, excessive delay, no reduced motion. | Define timeline under 1.5s and reduced-motion static fallback. |
| Scroll Storytelling | 10 | 95 | Each scroll section has scene purpose, pacing, and emotional function. | Random reveal on every block, no story arc. | Define scene name, focal point, trigger, emotion, and UX goal. |
| Reveal | 20 | 95 | Reveal uses transform/opacity, duration 0.6-1.2s, stagger where useful. | Animate top/left/margin, huge movement, default ease. | Standardize reveal tokens and trigger thresholds. |
| Parallax | 10 | 95 | Subtle image-only parallax, no text nausea, disabled for reduced motion/mobile when needed. | Heavy parallax, scroll jank, motion sickness. | Limit yPercent, use ScrollTrigger carefully, add reduced-motion branch. |
| Gallery | 0 | 95 | Gallery motion supports exploration: mask reveal, horizontal pan, or swipe. Mobile becomes stacked/swipe. | Autoplay chaos, broken swipe, heavy layout shift. | Use one gallery motion pattern and test mobile. |
| Lenis | 10 | 95 | Lenis integrates with GSAP ticker, disabled/reduced where needed, no scroll trap. | Smooth scroll fights native behavior, anchor links broken. | Sync with ScrollTrigger, test anchors and keyboard navigation. |
| GSAP | 20 | 95 | GSAP only for timelines/pinning/storytelling, uses context cleanup, no global selector leaks. | Memory leaks, stuck ScrollTriggers, no cleanup. | Use scoped refs, ctx.revert, invalidateOnRefresh, anticipatePin. |
| Reduced Motion | 20 | 98 | All complex motion collapses to static/fade. Video autoplay/scroll hijack stopped where appropriate. | Reduced motion ignored, parallax still runs. | Add media query/hook gate to every motion component. |
| Performance | 25 | 95 | Animation holds 60 FPS, transform/opacity only, no large blur on scrolling content. | GPU repaint storm, filter blur over big areas, JS scroll listeners. | Profile FPS, remove expensive effects, use IntersectionObserver/ScrollTrigger. |

Motion pass conditions:

- Total score must be 95 or higher.
- Reduced Motion and Performance must each be at least 95.
- Any motion that cannot be explained as hierarchy, storytelling, feedback, or state transition fails review.

## 6. Design System Checklist

PASS only when every item below follows the Luxury Experience Blueprint and AI_PROJECT_FRAMEWORK.

| Area | PASS Conditions | FAIL Conditions |
|---|---|---|
| Color Palette | Uses approved natural luxury palette: forest, pine, sage, stone, rice paper, ivory, aged gold/clay accent. No pure black/white on large surfaces. | Random colors, SaaS blue/purple, oversaturated accent, inconsistent warm/cool neutrals. |
| Typography | Max 2 type families. Display font for H1/H2, refined sans for body/UI. Font loading matches declared tokens. | Browser default, Inter as default, mismatched Google font vs CSS token, text clipping. |
| Grid | 12 columns desktop, 8 tablet, 4 mobile. Max width 1320-1440px. Mobile collapse explicit. | Bootstrap-like equal columns everywhere, no max width, horizontal scroll. |
| Spacing | Base-8 rhythm, large luxury section padding, enough negative space for reading. | Crowded content, arbitrary spacing, dense dashboard feeling. |
| Border Radius | Small radius for images/cards, pill for buttons/language, consistent rule. | Mixed radii without system, oversized rounded cards everywhere. |
| Shadow | Soft tinted shadows only when useful. Preference for depth via spacing, border, and material. | Harsh black shadows, generic shadow-md, excessive elevation. |
| Button | Clear hierarchy, contrast pass, one-line label, active/focus/disabled/loading states. | Duplicate CTA intent, low contrast, no focus, wrapped label. |
| Form | Label always present, helper/error text linked, booking uses luxury step/card patterns. | Placeholder-as-label, long traditional form, hidden errors. |
| Icon | One icon family, light stroke, semantic aria-label where needed. | Mixed icon sets, hand-rolled decorative SVGs, thick Material/FontAwesome style. |

Design System sprint gate:

- No hardcoded one-off color/font/spacing unless documented and approved.
- No visible UI element may violate the Blueprint palette, type, radius, motion, or accessibility standards.

## 7. SEO Checklist

| Item | PASS Conditions | FAIL Conditions |
|---|---|---|
| H1 | Exactly one H1 per route, descriptive and crawlable. | Multiple H1s, missing H1, image-only H1. |
| Heading Hierarchy | H2/H3 follow logical structure: Story, Villas, Amenities, Experience, Reviews, Location, Booking, FAQ. | Skipped levels, headings used only for styling. |
| Metadata | Title and description match page intent and route. | Generic Vite title, stale route title, keyword stuffing. |
| Open Graph | OG title, description, image, URL, and locale are present and match real page. | Placeholder OG image, broken URL, mismatch with canonical. |
| Schema | Hotel/LodgingBusiness, FAQ, and Breadcrumb JSON-LD valid when content exists. | Fake rating/review data, schema mismatch, invalid JSON. |
| FAQ | Real user questions about stay, transfer, cancellation, villa choice, spa/dining. | Generic FAQ, hidden text, unsupported claims. |
| Internal Link | Hero, villas, details, booking, footer, FAQ, and related sections link naturally. | Dead anchors, "click here", sitemap routes not implemented. |
| Image Alt | Content images have specific alt text. Decorative images are ignored correctly. | Empty alt on meaningful image, keyword stuffing, repeated "Gallery". |
| Sitemap | Contains only real routes and correct lastmod. | Nonexistent `/experience`, `/about`, or `/contact` if routes are not implemented. |
| Canonical | Canonical matches production URL and route. | Canonical points to wrong domain or missing route. |

SEO sprint gate:

- SEO score target is 98.
- Any missing H1, broken canonical, invalid schema, or sitemap route mismatch blocks approval.

## 8. Performance Checklist

| Item | PASS Conditions | FAIL Conditions |
|---|---|---|
| LCP | Under 2.5s. Hero image/video poster is eager, high priority, preloaded where appropriate. | Hero lazy-loaded, oversized media, render-blocking fonts. |
| CLS | Under 0.1. Images/videos reserve dimensions or aspect ratio. | Layout jumps when images/fonts/data load. |
| INP | Under 200ms. Interactions respond immediately. | Heavy JS on click, React state used for continuous scroll/mouse motion. |
| Lazy Loading | Below-fold images lazy and async decode. | Lazy LCP or eager all images. |
| Code Splitting | Non-critical routes/sections and heavy libraries split or deferred. | One large bundle, unused JS warnings severe. |
| Image Optimization | WebP/AVIF preferred, responsive `srcset` and `sizes`, correct mobile sizes. | 4K images for small cards, PNG/JPG bloat. |
| Font Loading | Only needed weights, preconnect/preload where needed, `font-display: swap`. | Too many weights, font mismatch, FOIT/FOUT causing layout shift. |
| Bundle Size | Motion/GSAP/Lenis used only where justified. Heavy modules lazy-loaded. | Three.js/GSAP loaded for static sections, duplicate animation libraries fighting. |

Performance sprint gate:

- Performance score target is 95.
- LCP, CLS, INP, and image optimization are blocking checks.

## 9. Accessibility Checklist

| Item | PASS Conditions | FAIL Conditions |
|---|---|---|
| WCAG AA | Lighthouse Accessibility 95+, manual checks pass WCAG 2.1 AA. | Contrast, keyboard, semantic, or ARIA blockers. |
| Contrast | Body text 4.5:1, large text 3:1, hero CTA readable on image. | White text over bright image, pale placeholder text, low contrast glass. |
| Keyboard Navigation | All flows work with Tab, Shift+Tab, Enter, Space, Escape. | Mouse-only card/menu/calendar, focus lost. |
| Focus State | Visible focus for links, buttons, inputs, menu, calendar, carousel controls. | `outline: none`, focus hidden behind overlay. |
| Screen Reader | Semantic landmarks, labels, aria-expanded/controls, meaningful alt. | Div buttons, unlabeled icon buttons, noisy decorative content. |
| Reduced Motion | Complex motion disabled or reduced to fade/static. | Parallax, pinning, autoplay, or horizontal pan still active. |
| Touch Target | Mobile interactive elements at least 44x44px with enough spacing. | Tiny language switch, close button, calendar dates, carousel controls. |

Accessibility sprint gate:

- Accessibility score target is 95.
- Keyboard Navigation, Contrast, Reduced Motion, and Focus State are blocking checks.

## 10. Final Sprint Approval

Every UI sprint from B3 onward must submit this final score table:

| Category | Score | Required | PASS/FAIL | Cause if below target | Improvement Plan |
|---|---:|---:|---|---|---|
| UI |  | 95 |  |  |  |
| UX |  | 95 |  |  |  |
| Luxury Experience |  | 95 |  |  |  |
| Motion |  | 95 |  |  |  |
| SEO |  | 98 |  |  |  |
| Performance |  | 95 |  |  |  |
| Accessibility |  | 95 |  |  |  |
| Responsive |  | 95 |  |  |  |
| Conversion |  | 95 |  |  |  |

Mandatory final checklist:

- [ ] No source area outside sprint scope was modified.
- [ ] No Auth, Booking Write, Admin, Report, or database schema changes were made unless explicitly approved.
- [ ] Old Thymeleaf public flow was not deleted before React replacement is stable.
- [ ] Build/typecheck/lint status is reported.
- [ ] Desktop, tablet, and mobile screenshots or visual inspection are reported.
- [ ] Accessibility manual keyboard test is reported.
- [ ] Reduced motion behavior is reported.
- [ ] Performance audit or limitation is reported.
- [ ] SEO route/metadata/schema status is reported.
- [ ] File changed list is reported.
- [ ] File not changed list is reported for protected areas.

Approval rule:

- If any final category is below 95, sprint status is FAILED.
- If SEO is below 98, sprint status is FAILED.
- If any blocking check fails, sprint status is FAILED even if average score is high.
- Do not move to the next sprint until the user approves the current sprint report.

## 11. Sprint Review Report Template

Use this template at the end of every UI sprint.

```md
## Sprint [ID] Review

### Scope
- Requested:
- Completed:
- Not included:

### Files Changed
- 

### Files Not Changed
- Auth:
- Booking Write:
- Admin:
- Report:
- Database:
- Thymeleaf legacy:

### Scorecard
| Category | Score | Required | PASS/FAIL |
|---|---:|---:|---|
| UI |  | 95 |  |
| UX |  | 95 |  |
| Luxury Experience |  | 95 |  |
| Motion |  | 95 |  |
| SEO |  | 98 |  |
| Performance |  | 95 |  |
| Accessibility |  | 95 |  |
| Responsive |  | 95 |  |
| Conversion |  | 95 |  |

### Blocking Issues
- 

### Risks
- 

### Checklist
- [ ] UI target met
- [ ] UX target met
- [ ] Luxury Experience target met
- [ ] Motion target met
- [ ] SEO target met
- [ ] Performance target met
- [ ] Accessibility target met
- [ ] Responsive target met
- [ ] Conversion target met

### Approval Status
PASS/FAILED:
Reason:
Next sprint allowed only after user approval:
```

