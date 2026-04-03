'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { formatDistanceToNow, format } from 'date-fns';
import {
  MessageSquare,
  Send,
  Loader2,
  AlertCircle,
  ArrowLeft,
  Circle,
  User,
} from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { useLanguage } from '@/lib/i18n';
import {
  messagingApi,
  ApiError,
  type ConversationDto,
  type MessageDto,
} from '@/lib/api';
import Button from '@/components/ui/Button';

export default function MessagesPage() {
  const router = useRouter();
  const { user, token, isLoading: authLoading } = useAuth();
  const { t } = useLanguage();

  const [conversations, setConversations] = useState<ConversationDto[]>([]);
  const [selectedConvId, setSelectedConvId] = useState<string | null>(null);
  const [messages, setMessages] = useState<MessageDto[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [loadingConvs, setLoadingConvs] = useState(true);
  const [loadingMsgs, setLoadingMsgs] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const selectedConv = conversations.find((c) => c.id === selectedConvId);

  const fetchConversations = useCallback(async () => {
    if (!token) return;
    try {
      const data = await messagingApi.conversations(token);
      setConversations(data);
    } catch (err) {
      if (loadingConvs) {
        setError(err instanceof ApiError ? err.message : 'Failed to load conversations');
      }
    } finally {
      setLoadingConvs(false);
    }
  }, [token, loadingConvs]);

  const fetchMessages = useCallback(
    async (convId: string) => {
      if (!token) return;
      try {
        const data = await messagingApi.messages(convId, token);
        setMessages(data);
      } catch (err) {
        setError(err instanceof ApiError ? err.message : 'Failed to load messages');
      } finally {
        setLoadingMsgs(false);
      }
    },
    [token]
  );

  useEffect(() => {
    if (authLoading) return;
    if (!user || !token) {
      router.push('/login');
      return;
    }
    fetchConversations();
  }, [user, token, authLoading, router, fetchConversations]);

  useEffect(() => {
    if (!selectedConvId || !token) return;

    setLoadingMsgs(true);
    fetchMessages(selectedConvId);
    messagingApi.markRead(selectedConvId, token).catch(() => {});

    // Poll for new messages every 5 seconds
    pollRef.current = setInterval(() => {
      fetchMessages(selectedConvId);
      fetchConversations();
    }, 5000);

    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, [selectedConvId, token, fetchMessages, fetchConversations]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  async function handleSend(e: React.FormEvent) {
    e.preventDefault();
    if (!newMessage.trim() || !selectedConvId || !token) return;

    setSending(true);
    try {
      const msg = await messagingApi.send(selectedConvId, newMessage.trim(), token);
      setMessages((prev) => [...prev, msg]);
      setNewMessage('');
      fetchConversations();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to send message');
    } finally {
      setSending(false);
    }
  }

  function handleSelectConversation(convId: string) {
    setSelectedConvId(convId);
    setMessages([]);
    setError('');
  }

  if (authLoading || (!user && !error)) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <Loader2 className="h-8 w-8 animate-spin text-rose-500" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto h-screen flex flex-col">
        {/* Header */}
        <div className="px-4 py-4 border-b border-gray-200 bg-white">
          <div className="flex items-center gap-3">
            {selectedConvId && (
              <button
                type="button"
                onClick={() => setSelectedConvId(null)}
                className="md:hidden p-1.5 rounded-lg hover:bg-gray-100 transition-colors cursor-pointer"
              >
                <ArrowLeft className="h-5 w-5 text-gray-600" />
              </button>
            )}
            <MessageSquare className="h-6 w-6 text-rose-500" />
            <h1 className="text-lg font-bold text-gray-900">
              {selectedConv
                ? selectedConv.otherParticipant.name
                : t.messaging.title}
            </h1>
          </div>
        </div>

        {error && (
          <div className="mx-4 mt-2 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 flex items-center gap-2">
            <AlertCircle className="h-4 w-4 shrink-0" />
            {error}
            <button
              type="button"
              onClick={() => setError('')}
              className="ml-auto text-red-500 hover:text-red-700 cursor-pointer"
            >
              {t.common.close}
            </button>
          </div>
        )}

        <div className="flex-1 flex overflow-hidden">
          {/* Conversation list */}
          <div
            className={`w-full md:w-80 md:border-r border-gray-200 bg-white flex flex-col overflow-y-auto ${
              selectedConvId ? 'hidden md:flex' : 'flex'
            }`}
          >
            {loadingConvs ? (
              <div className="flex items-center justify-center py-20 text-gray-400">
                <Loader2 className="h-6 w-6 animate-spin" />
              </div>
            ) : conversations.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 text-gray-400">
                <MessageSquare className="h-10 w-10 mb-2" />
                <p className="text-sm">{t.messaging.noConversations}</p>
              </div>
            ) : (
              conversations.map((conv) => {
                const isSelected = conv.id === selectedConvId;
                const isUnread =
                  conv.lastMessage &&
                  !conv.lastMessage.isRead &&
                  conv.lastMessage.senderId !== user?.id;

                return (
                  <button
                    key={conv.id}
                    type="button"
                    onClick={() => handleSelectConversation(conv.id)}
                    className={`w-full text-left px-4 py-3 border-b border-gray-50 transition-colors cursor-pointer ${
                      isSelected
                        ? 'bg-rose-50 border-l-2 border-l-rose-500'
                        : 'hover:bg-gray-50'
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <div className="shrink-0 w-10 h-10 rounded-full bg-rose-100 flex items-center justify-center">
                        <User className="h-5 w-5 text-rose-600" />
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center justify-between gap-2">
                          <p
                            className={`text-sm truncate ${
                              isUnread
                                ? 'font-bold text-gray-900'
                                : 'font-medium text-gray-700'
                            }`}
                          >
                            {conv.otherParticipant.name}
                          </p>
                          <div className="flex items-center gap-1.5 shrink-0">
                            {isUnread && (
                              <Circle className="h-2.5 w-2.5 fill-rose-500 text-rose-500" />
                            )}
                            {conv.lastMessage && (
                              <span className="text-xs text-gray-400">
                                {formatDistanceToNow(new Date(conv.lastMessage.createdAt), {
                                  addSuffix: true,
                                })}
                              </span>
                            )}
                          </div>
                        </div>
                        {conv.topic && (
                          <p className="text-xs text-rose-500 font-medium truncate mt-0.5">
                            {conv.topic}
                          </p>
                        )}
                        {conv.lastMessage && (
                          <p className="text-xs text-gray-400 truncate mt-0.5">
                            {conv.lastMessage.senderId === user?.id ? `${t.messaging.you}: ` : ''}
                            {conv.lastMessage.content}
                          </p>
                        )}
                      </div>
                    </div>
                  </button>
                );
              })
            )}
          </div>

          {/* Message area */}
          <div
            className={`flex-1 flex flex-col bg-gray-50 ${
              selectedConvId ? 'flex' : 'hidden md:flex'
            }`}
          >
            {!selectedConvId ? (
              <div className="flex-1 flex flex-col items-center justify-center text-gray-400">
                <MessageSquare className="h-12 w-12 mb-3" />
                <p className="text-base font-medium text-gray-500">
                  {t.messaging.title}
                </p>
                <p className="text-sm mt-1">
                  {t.messaging.noConversations}
                </p>
              </div>
            ) : (
              <>
                {/* Messages */}
                <div className="flex-1 overflow-y-auto px-4 py-4 space-y-3">
                  {loadingMsgs ? (
                    <div className="flex items-center justify-center py-20">
                      <Loader2 className="h-6 w-6 animate-spin text-gray-400" />
                    </div>
                  ) : messages.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-20 text-gray-400">
                      <p className="text-sm">{t.messaging.noConversations}</p>
                    </div>
                  ) : (
                    messages.map((msg) => {
                      const isMine = msg.senderId === user?.id;
                      return (
                        <div
                          key={msg.id}
                          className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}
                        >
                          <div
                            className={`max-w-[80%] sm:max-w-[70%] rounded-2xl px-4 py-2.5 ${
                              isMine
                                ? 'bg-rose-500 text-white rounded-br-md'
                                : 'bg-white text-gray-900 border border-gray-100 rounded-bl-md shadow-sm'
                            }`}
                          >
                            {!isMine && (
                              <p className="text-xs font-medium text-rose-500 mb-0.5">
                                {msg.senderName}
                              </p>
                            )}
                            <p className="text-sm whitespace-pre-wrap break-words">
                              {msg.content}
                            </p>
                            <p
                              className={`text-[10px] mt-1 ${
                                isMine ? 'text-rose-200' : 'text-gray-400'
                              }`}
                            >
                              {format(new Date(msg.createdAt), 'h:mm a')}
                            </p>
                          </div>
                        </div>
                      );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </div>

                {/* Input */}
                <div className="border-t border-gray-200 bg-white px-4 py-3">
                  <form onSubmit={handleSend} className="flex items-end gap-2">
                    <textarea
                      value={newMessage}
                      onChange={(e) => setNewMessage(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' && !e.shiftKey) {
                          e.preventDefault();
                          handleSend(e);
                        }
                      }}
                      placeholder={t.messaging.typeMessage}
                      rows={1}
                      className="flex-1 resize-none rounded-xl border border-gray-200 px-4 py-2.5 text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-rose-500/30 focus:border-rose-500"
                    />
                    <Button
                      type="submit"
                      size="md"
                      disabled={!newMessage.trim()}
                      loading={sending}
                    >
                      <Send className="h-4 w-4" />
                    </Button>
                  </form>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
