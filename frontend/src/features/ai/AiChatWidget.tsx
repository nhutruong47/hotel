'use client';

import { useEffect, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, API_PATHS } from '../../shared/api/client';
import type { ChatMessage } from './aiTypes';
import { useTranslation } from '../../shared/i18n/hooks';

type SessionUser = {
  id: number;
  username: string;
  email: string;
  fullName?: string;
  role: string;
};

export const AiChatWidget = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [input, setInput] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const queryClient = useQueryClient();
  const { t } = useTranslation();

  const sessionQuery = useQuery({
    queryKey: ['session'],
    queryFn: () => api.get<{ user?: SessionUser }>(API_PATHS.auth.session),
    retry: false,
    staleTime: 30_000,
  });

  const isLoggedIn = !!sessionQuery.data?.user;

  const historyQuery = useQuery({
    queryKey: ['ai-history'],
    queryFn: () => api.get<ChatMessage[]>(API_PATHS.ai.history),
    enabled: isOpen && isLoggedIn,
  });

  const chatMutation = useMutation({
    mutationFn: (message: string) =>
      api.post<{ response: string }>(API_PATHS.ai.recommend, { message }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ai-history'] });
      setInput('');
    },
  });

  const clearHistoryMutation = useMutation({
    mutationFn: () => api.delete(API_PATHS.ai.clear),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ai-history'] });
    },
  });

  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [isOpen, historyQuery.data, chatMutation.isPending]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || chatMutation.isPending) return;
    chatMutation.mutate(input);
  };

  return (
    <>
      <button
        onClick={() => setIsOpen(true)}
        className={`fixed bottom-6 right-6 z-50 flex h-14 w-14 items-center justify-center rounded-full bg-brand-forest-deep text-brand-white shadow-xl transition-transform hover:scale-105 active:scale-95 ${
          isOpen ? 'scale-0 opacity-0' : 'scale-100 opacity-100'
        }`}
        aria-label="Open AI Assistant"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M14 9a2 2 0 0 1-2 2H6l-4 4V4c0-1.1.9-2 2-2h8a2 2 0 0 1 2 2v5Z"/>
          <path d="M18 9h2a2 2 0 0 1 2 2v11l-4-4h-6a2 2 0 0 1-2-2v-1"/>
        </svg>
      </button>

      <div
        className={`fixed bottom-6 right-6 z-50 flex h-[500px] max-h-[80vh] w-[350px] flex-col overflow-hidden rounded-2xl border border-brand-ink/10 bg-brand-paper shadow-2xl transition-all duration-300 ${
          isOpen ? 'translate-y-0 opacity-100' : 'pointer-events-none translate-y-10 opacity-0'
        }`}
      >
        <div className="flex items-center justify-between bg-brand-forest-deep px-4 py-3 text-brand-white">
          <div className="flex items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 2a2 2 0 0 1 2 2c0 1.1-.9 2-2 2s-2-.9-2-2 .9-2 2-2z"/><path d="M16 11V6a4 4 0 0 0-8 0v5"/><path d="M12 11v10"/><path d="m8 15 4-4 4 4"/><path d="m8 21 4-4 4 4"/></svg>
            <h3 className="font-serif font-medium tracking-wide">{t('common.aiTitle')}</h3>
          </div>
          <button
            onClick={() => setIsOpen(false)}
            className="rounded-full p-1 hover:bg-brand-white/20 transition-colors"
            aria-label="Close chat"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
          </button>
        </div>

        {!isLoggedIn ? (
          <div className="flex flex-1 items-center justify-center p-6 text-center text-brand-ink/60">
            <p className="text-sm">{t('common.aiLoginPrompt')}</p>
          </div>
        ) : (
          <>
            <div className="flex-1 overflow-y-auto p-4 space-y-4 text-sm bg-brand-white/40">
              {historyQuery.isLoading ? (
                <div className="flex items-center justify-center h-full text-brand-ink/40">{t('common.loadingHistory')}</div>
              ) : historyQuery.data?.length === 0 ? (
                <div className="flex items-center justify-center h-full text-brand-ink/60 text-center">
                  <p dangerouslySetInnerHTML={{ __html: t('common.aiGreeting') }}></p>
                </div>
              ) : (
                historyQuery.data?.map((msg, index) => (
                  <div key={msg.id ?? index} className="space-y-4">
                    <div className="flex justify-end">
                      <div className="max-w-[85%] rounded-2xl rounded-tr-sm bg-brand-forest-deep px-4 py-2 text-brand-white shadow-sm">
                        {msg.userMessage}
                      </div>
                    </div>
                    <div className="flex justify-start">
                      <div className="max-w-[90%] rounded-2xl rounded-tl-sm bg-brand-paper border border-brand-ink/10 px-4 py-2 text-brand-ink shadow-sm whitespace-pre-wrap">
                        {msg.aiResponse}
                      </div>
                    </div>
                  </div>
                ))
              )}
              {chatMutation.isPending && (
                <div className="flex justify-start">
                  <div className="max-w-[90%] rounded-2xl rounded-tl-sm bg-brand-paper border border-brand-ink/10 px-4 py-2 text-brand-ink/60 shadow-sm flex items-center gap-2">
                    <span className="w-2 h-2 bg-brand-ink/40 rounded-full animate-bounce"></span>
                    <span className="w-2 h-2 bg-brand-ink/40 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></span>
                    <span className="w-2 h-2 bg-brand-ink/40 rounded-full animate-bounce" style={{ animationDelay: '0.4s' }}></span>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            <form onSubmit={handleSubmit} className="border-t border-brand-ink/10 bg-brand-paper p-3">
              {historyQuery.data && historyQuery.data.length > 0 && (
                <div className="flex justify-center mb-2">
                  <button
                    type="button"
                    onClick={() => clearHistoryMutation.mutate()}
                    className="text-[0.65rem] uppercase tracking-wider text-brand-ink/40 hover:text-brand-ink/80 transition-colors"
                  >
                    {t('common.clearHistory')}
                  </button>
                </div>
              )}
              <div className="relative">
                <input
                  type="text"
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  placeholder={t('common.aiPlaceholder')}
                  className="w-full rounded-full border border-brand-ink/20 bg-brand-white px-4 py-2.5 pr-12 text-sm text-brand-ink placeholder:text-brand-ink/40 focus:border-brand-forest-deep focus:outline-none focus:ring-1 focus:ring-brand-forest-deep transition-all"
                  disabled={chatMutation.isPending}
                />
                <button
                  type="submit"
                  disabled={!input.trim() || chatMutation.isPending}
                  className="absolute right-1 top-1 bottom-1 flex aspect-square items-center justify-center rounded-full bg-brand-forest-deep text-brand-white transition-transform hover:scale-105 disabled:opacity-50 disabled:hover:scale-100"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>
                </button>
              </div>
            </form>
          </>
        )}
      </div>
    </>
  );
};
