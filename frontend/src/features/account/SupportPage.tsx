'use client';

import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { api, ApiError } from '../../shared/api/client';

const FAQS = [
  {
    category: 'Booking',
    items: [
      {
        q: 'How do I make a reservation?',
        a: 'Browse our villas, select your dates, and click "Book Now". You will receive a confirmation email once our team reviews and approves your booking.',
      },
      {
        q: 'How long does booking approval take?',
        a: 'Our team typically reviews and responds to booking requests within 2-4 hours during business hours (7 AM – 10 PM). You will receive an email notification immediately after approval.',
      },
      {
        q: 'Can I modify my booking dates?',
        a: 'Date modifications are subject to availability. Please contact us directly via the Support form or by phone and our team will assist you promptly.',
      },
    ],
  },
  {
    category: 'Cancellation & Refunds',
    items: [
      {
        q: 'What is the cancellation policy?',
        a: 'Cancellations made 7+ days before check-in receive a full refund. Cancellations made 3-7 days prior receive a 50% refund. Cancellations within 72 hours are non-refundable. Special conditions may apply during peak seasons.',
      },
      {
        q: 'How long does a refund take?',
        a: 'Approved refunds are processed within 5-7 business days and returned to your original payment method. Bank processing times may vary.',
      },
      {
        q: 'What happens if the property cancels my booking?',
        a: 'In the rare event we must cancel your booking, you will receive a full refund and our team will work with you to find an alternative or offer compensation.',
      },
    ],
  },
  {
    category: 'During Your Stay',
    items: [
      {
        q: 'What time is check-in and check-out?',
        a: 'Standard check-in is 2:00 PM and check-out is 12:00 PM (noon). Early check-in and late check-out may be arranged based on availability — please contact us in advance.',
      },
      {
        q: 'Is airport transfer available?',
        a: 'Yes, we offer private airport transfer services. Please contact our concierge team when making your booking to arrange pick-up.',
      },
    ],
  },
];

export function SupportPage() {
  const [openFaq, setOpenFaq] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [formState, setFormState] = useState({ subject: '', category: '', message: '', name: '', email: '' });
  const [submitMsg, setSubmitMsg] = useState<string | null>(null);
  const [submitOk, setSubmitOk] = useState(false);

  const filteredFAQS = FAQS.map((cat) => ({
    ...cat,
    items: cat.items.filter((item) =>
      item.q.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.a.toLowerCase().includes(searchQuery.toLowerCase())
    ),
  })).filter((cat) => cat.items.length > 0);

  const submitMutation = useMutation<{ message: string }, ApiError, typeof formState>({
    mutationFn: (body) => api.post<{ message: string }>('/api/v1/contact', body),
    onSuccess: (data) => {
      setSubmitOk(true);
      setSubmitMsg(data.message || 'Your message has been sent. We will get back to you shortly.');
      setFormState({ subject: '', category: '', message: '', name: '', email: '' });
    },
    onError: (err) => {
      setSubmitOk(false);
      setSubmitMsg(err.message || 'Failed to send message. Please try again.');
    },
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!formState.message.trim() || !formState.name.trim() || !formState.email.trim()) {
      setSubmitOk(false);
      setSubmitMsg('Please fill in all required fields.');
      return;
    }
    submitMutation.mutate(formState);
  }

  const inputCls = 'mt-2 h-12 w-full rounded-full border border-brand-stone bg-brand-white px-5 text-sm text-brand-charcoal placeholder:text-brand-ink/40 focus:border-brand-forest focus:outline-none focus:ring-1 focus:ring-brand-forest transition-all';

  return (
    <div>
      <div className="mb-7">
        <p className="text-xs font-semibold uppercase tracking-[0.22em] text-brand-sage">Account</p>
        <h1 className="mt-2 text-4xl text-brand-charcoal sm:text-5xl">Support</h1>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
        {/* ── Left: FAQ + Contact Form ── */}
        <div className="space-y-6">
          {/* FAQ */}
          <div className="rounded-2xl bg-brand-paper p-6 shadow-[0_4px_24px_rgba(32,52,43,0.07)] sm:p-8">
            <h2 className="text-xl font-semibold text-brand-charcoal">Frequently Asked Questions</h2>
            
            <div className="mt-4">
              <input 
                type="text" 
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search FAQs..."
                className="h-10 w-full rounded-full border border-brand-stone bg-brand-white px-5 text-sm text-brand-charcoal placeholder:text-brand-ink/40 focus:border-brand-forest focus:outline-none focus:ring-1 focus:ring-brand-forest transition-all"
              />
            </div>

            <div className="mt-6 space-y-6">
              {filteredFAQS.length === 0 && (
                <p className="text-sm text-brand-ink/60">No FAQs found matching your search.</p>
              )}
              {filteredFAQS.map((cat) => (
                <div key={cat.category}>
                  <p className="mb-3 text-[0.65rem] font-semibold uppercase tracking-[0.2em] text-brand-sage">{cat.category}</p>
                  <div className="space-y-2">
                    {cat.items.map((item) => {
                      const id = `${cat.category}-${item.q}`;
                      const isOpen = openFaq === id;
                      return (
                        <div key={id} className="overflow-hidden rounded-xl border border-brand-ink/6">
                          <button
                            type="button"
                            onClick={() => setOpenFaq(isOpen ? null : id)}
                            className="flex w-full items-center justify-between gap-3 px-5 py-4 text-left text-sm font-semibold text-brand-charcoal transition hover:bg-brand-ink/3"
                          >
                            {item.q}
                            <svg
                              xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                              className={`shrink-0 text-brand-ink/40 transition-transform ${isOpen ? 'rotate-180' : ''}`}
                            >
                              <path d="m6 9 6 6 6-6"/>
                            </svg>
                          </button>
                          {isOpen && (
                            <p className="border-t border-brand-ink/6 px-5 py-4 text-sm leading-relaxed text-brand-ink/70">
                              {item.a}
                            </p>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Contact Form */}
          <div className="rounded-2xl bg-brand-paper p-6 shadow-[0_4px_24px_rgba(32,52,43,0.07)] sm:p-8">
            <h2 className="text-xl font-semibold text-brand-charcoal">Send a Message</h2>
            <p className="mt-1 text-sm text-brand-ink/60">We typically respond within 2-4 hours during business hours.</p>

            <form className="mt-6 space-y-4" onSubmit={handleSubmit}>
              <div className="grid gap-4 sm:grid-cols-2">
                <label className="block">
                  <span className="text-sm font-semibold text-brand-charcoal">Name *</span>
                  <input value={formState.name} onChange={(e) => setFormState({ ...formState, name: e.target.value })} required className={inputCls} placeholder="Your full name" />
                </label>
                <label className="block">
                  <span className="text-sm font-semibold text-brand-charcoal">Email *</span>
                  <input type="email" value={formState.email} onChange={(e) => setFormState({ ...formState, email: e.target.value })} required className={inputCls} placeholder="your@email.com" />
                </label>
              </div>

              <label className="block">
                <span className="text-sm font-semibold text-brand-charcoal">Category</span>
                <select value={formState.category} onChange={(e) => setFormState({ ...formState, category: e.target.value })} className={inputCls}>
                  <option value="">Select a topic…</option>
                  <option value="booking">Booking Question</option>
                  <option value="cancellation">Cancellation & Refund</option>
                  <option value="payment">Payment Issue</option>
                  <option value="checkin">Check-In / Check-Out</option>
                  <option value="general">General Enquiry</option>
                </select>
              </label>

              <label className="block">
                <span className="text-sm font-semibold text-brand-charcoal">Subject</span>
                <input value={formState.subject} onChange={(e) => setFormState({ ...formState, subject: e.target.value })} className={inputCls} placeholder="Brief summary of your issue" />
              </label>

              <label className="block">
                <span className="text-sm font-semibold text-brand-charcoal">Message *</span>
                <textarea
                  value={formState.message}
                  onChange={(e) => setFormState({ ...formState, message: e.target.value })}
                  required
                  rows={5}
                  className="mt-2 w-full rounded-[1.25rem] border border-brand-stone bg-brand-white p-5 text-sm text-brand-charcoal placeholder:text-brand-ink/40 focus:border-brand-forest focus:outline-none focus:ring-1 focus:ring-brand-forest transition-all"
                  placeholder="Please describe your question or issue in detail…"
                />
              </label>

              {submitMsg && (
                <p className={`rounded-[1rem] px-5 py-3.5 text-sm ${submitOk ? 'bg-brand-forest/10 text-brand-forest' : 'bg-red-50 text-red-700'}`}>
                  {submitMsg}
                </p>
              )}

              <button
                type="submit"
                disabled={submitMutation.isPending}
                className="rounded-full bg-brand-forest px-7 py-3 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep disabled:opacity-50"
              >
                {submitMutation.isPending ? 'Sending…' : 'Send Message'}
              </button>
            </form>
          </div>
        </div>

        {/* ── Right: Contact Info ── */}
        <div className="space-y-5">
          {/* Direct contact */}
          <div className="rounded-2xl bg-brand-paper p-6 shadow-[0_4px_24px_rgba(32,52,43,0.07)]">
            <h2 className="text-lg font-semibold text-brand-charcoal">Contact Us Directly</h2>
            <p className="mt-1 text-xs text-brand-ink/55">Available 7 AM – 10 PM, 7 days a week.</p>

            <div className="mt-5 space-y-4">
              {[
                {
                  label: 'Phone',
                  value: '+84 901 234 567',
                  href: 'tel:+84901234567',
                  icon: <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.61 3.41 2 2 0 0 1 3.6 1.2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.91 8.37a16 16 0 0 0 6.72 6.72l1.73-1.73a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg>,
                },
                {
                  label: 'WhatsApp',
                  value: '+84 901 234 567',
                  href: 'https://wa.me/84901234567',
                  icon: <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>,
                },
                {
                  label: 'Email',
                  value: 'hello@nhuvillas.com',
                  href: 'mailto:hello@nhuvillas.com',
                  icon: <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>,
                },
              ].map(({ label, value, href, icon }) => (
                <a
                  key={label}
                  href={href}
                  target={href.startsWith('http') ? '_blank' : undefined}
                  rel="noreferrer"
                  className="flex items-center gap-4 rounded-xl border border-brand-ink/6 px-5 py-4 transition hover:bg-brand-ink/3"
                >
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-forest/8 text-brand-forest">
                    {icon}
                  </span>
                  <div>
                    <p className="text-[0.65rem] font-semibold uppercase tracking-wider text-brand-ink/40">{label}</p>
                    <p className="text-sm font-medium text-brand-charcoal">{value}</p>
                  </div>
                </a>
              ))}
            </div>
          </div>

          {/* Quick links */}
          <div className="rounded-2xl bg-brand-paper p-6 shadow-[0_4px_24px_rgba(32,52,43,0.07)]">
            <h2 className="text-lg font-semibold text-brand-charcoal">Quick Actions</h2>
            <div className="mt-4 space-y-2">
              {[
                { label: 'View My Bookings', href: '/account/bookings' },
                { label: 'Manage Profile',    href: '/account/profile' },
                { label: 'Browse Villas',     href: '/villas' },
              ].map(({ label, href }) => (
                <a
                  key={label}
                  href={href}
                  className="flex items-center justify-between rounded-xl border border-brand-ink/6 px-5 py-3.5 text-sm font-medium text-brand-ink transition hover:bg-brand-ink/3"
                >
                  {label}
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-brand-ink/30"><path d="M5 12h14"/><path d="m12 5 7 7-7 7"/></svg>
                </a>
              ))}
            </div>
          </div>

          {/* Future live chat placeholder */}
          <div className="rounded-2xl border border-dashed border-brand-sage/40 bg-brand-sage/5 p-6 text-center">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-xl bg-brand-sage/15 text-brand-sage">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
            </div>
            <p className="mt-3 text-sm font-semibold text-brand-charcoal">Live Chat — Coming Soon</p>
            <p className="mt-1 text-xs text-brand-ink/50">Real-time concierge support will be available here.</p>
          </div>
        </div>
      </div>
    </div>
  );
}
