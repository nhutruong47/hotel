"use client";
import { useState, useEffect, useMemo, type ReactNode } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api, API_PATHS, ApiError } from "../../../shared/api/client";
import { useTranslation } from "../../../shared/i18n/hooks";
import { ContentShell, Promotion, CountdownTimer, getCategoryLabels, categoryColors, PromotionCard, FeaturedBanner, PromoCodeValidator, SeasonalDealsBanner } from "../shared";

export const ContactPage = () => {
      const [name, setName] = useState('');
      const [email, setEmail] = useState('');
      const [phone, setPhone] = useState('');
      const [subject, setSubject] = useState('');
      const [message, setMessage] = useState('');
      const [type, setType] = useState('GENERAL');
      const [success, setSuccess] = useState<string | null>(null);
      const [errorMsg, setErrorMsg] = useState<string | null>(null);

      const submit = async (e: React.FormEvent) => {
        e.preventDefault();
        setErrorMsg(null);
        setSuccess(null);

        if (name.trim().length < 2 || !email.includes('@') || message.trim().length < 5) {
          setErrorMsg('Please fill in all required fields correctly.');
          return;
        }

        try {
          const result = await api.post<{ success: boolean; message: string; ticketId: string }>(
            API_PATHS.contact,
            { name, email, phone, subject, message, type }
          );
          setSuccess(`${result.message} (${result.ticketId})`);
          setName('');
          setEmail('');
          setPhone('');
          setSubject('');
          setMessage('');
        } catch (err) {
          setErrorMsg('KhÃ´ng thá»ƒ gá»­i tin nháº¯n. Vui lÃ²ng thá»­ láº¡i.');
        }
      };

      const contactTypes = [
        { value: 'GENERAL', label: 'Chung' },
        { value: 'RESERVATION', label: 'Äáº·t phÃ²ng' },
        { value: 'SUPPORT', label: 'Há»— trá»£' },
        { value: 'FEEDBACK', label: 'Pháº£n há»“i' },
        { value: 'PARTNERSHIP', label: 'Há»£p tÃ¡c' },
        { value: 'EMERGENCY', label: 'Kháº©n cáº¥p' },
      ];

      return (
        <ContentShell
          eyebrow="Contact"
          title="Speak with the stay team."
          copy="For exact address guidance, arrival timing, special occasions, or long-stay requests, contact the reservations team."
        >
          <div className="grid gap-8 lg:grid-cols-[0.8fr_1fr]">
            {/* Contact Info */}
            <div className="space-y-6">
              <div className="rounded-[2rem] bg-brand-paper p-7">
                <h2 className="text-3xl text-brand-charcoal">Reservations</h2>
                <p className="mt-5 text-brand-ink/64">reservations@nhuvillas.com</p>
                <p className="mt-2 text-brand-ink/64">Viet Nam</p>
              </div>

              <div className="rounded-[2rem] bg-brand-paper p-7">
                <h3 className="text-xl text-brand-charcoal">Business Hours</h3>
                <dl className="mt-4 space-y-2 text-sm text-brand-ink/64">
                  <div className="flex justify-between">
                    <dt>Monday - Friday</dt>
                    <dd>08:00 - 20:00</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt>Saturday - Sunday</dt>
                    <dd>09:00 - 18:00</dd>
                  </div>
                  <div className="flex justify-between border-t border-brand-stone/30 pt-2">
                    <dt className="font-semibold text-brand-coral">Emergency</dt>
                    <dd className="font-semibold text-brand-coral">24/7</dd>
                  </div>
                </dl>
              </div>

              <div className="rounded-[2rem] bg-brand-paper p-7">
                <h3 className="text-xl text-brand-charcoal">Quick Contact</h3>
                <div className="mt-4 space-y-3">
                  <a href="tel:+84123456789" className="flex items-center gap-3 text-brand-ink/64 hover:text-brand-forest">
                    <span>ðŸ“ž</span> +84 123 456 789
                  </a>
                  <a href="mailto:reservations@nhuvillas.com" className="flex items-center gap-3 text-brand-ink/64 hover:text-brand-forest">
                    <span>âœ‰ï¸</span> reservations@nhuvillas.com
                  </a>
                </div>
              </div>

              <div className="rounded-[2rem] bg-brand-sand p-7">
                <h3 className="text-xl text-brand-charcoal">Need immediate help?</h3>
                <p className="mt-2 text-sm text-brand-ink/62">
                  Check our{' '}
                  <Link href="/faq" className="text-brand-forest underline">
                    FAQ
                  </Link>{' '}
                  or use our AI chat assistant for instant support.
                </p>
              </div>
            </div>

            {/* Contact Form */}
            <form onSubmit={submit} className="space-y-5 rounded-[2rem] bg-brand-paper p-7">
              <div className="grid gap-5 sm:grid-cols-2">
                <label className="grid gap-2">
                  <span className="text-sm font-semibold text-brand-charcoal">
                    Name <span className="text-brand-coral">*</span>
                  </span>
                  <input
                    required
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Your name"
                    className="h-14 rounded-full border border-brand-stone px-5"
                  />
                </label>
                <label className="grid gap-2">
                  <span className="text-sm font-semibold text-brand-charcoal">
                    Email <span className="text-brand-coral">*</span>
                  </span>
                  <input
                    required
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="Email"
                    className="h-14 rounded-full border border-brand-stone px-5"
                  />
                </label>
              </div>

              <div className="grid gap-5 sm:grid-cols-2">
                <label className="grid gap-2">
                  <span className="text-sm font-semibold text-brand-charcoal">Phone</span>
                  <input
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="Phone number"
                    className="h-14 rounded-full border border-brand-stone px-5"
                  />
                </label>
                <label className="grid gap-2">
                  <span className="text-sm font-semibold text-brand-charcoal">Type</span>
                  <select
                    value={type}
                    onChange={(e) => setType(e.target.value)}
                    className="h-14 rounded-full border border-brand-stone px-5"
                  >
                    {contactTypes.map((t) => (
                      <option key={t.value} value={t.value}>
                        {t.label}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <label className="grid gap-2">
                <span className="text-sm font-semibold text-brand-charcoal">
                  Subject <span className="text-brand-coral">*</span>
                </span>
                <input
                  required
                  value={subject}
                  onChange={(e) => setSubject(e.target.value)}
                  placeholder="Subject"
                  className="h-14 rounded-full border border-brand-stone px-5"
                />
              </label>

              <label className="grid gap-2">
                <span className="text-sm font-semibold text-brand-charcoal">
                  Message <span className="text-brand-coral">*</span>
                </span>
                <textarea
                  required
                  minLength={5}
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  placeholder="How can we help?"
                  className="min-h-36 rounded-[1.25rem] border border-brand-stone p-5"
                />
              </label>

              {errorMsg && (
                <p className="rounded-full bg-brand-coral/10 px-4 py-3 text-sm text-brand-ink">{errorMsg}</p>
              )}
              {success && (
                <p className="rounded-full bg-brand-sage/15 px-4 py-3 text-sm text-brand-forest">{success}</p>
              )}

              <button
                type="submit"
                className="min-h-12 w-full rounded-full bg-brand-forest px-6 text-xs font-semibold uppercase tracking-[0.14em] text-brand-white transition hover:bg-brand-forest-deep"
              >
                Send message
              </button>
            </form>
          </div>
        </ContentShell>
      );
    };

