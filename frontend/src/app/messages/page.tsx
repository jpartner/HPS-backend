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
  Plus,
  Archive,
  ArchiveRestore,
  Flag,
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

function displayName(participant: ConversationDto['otherParticipant']): string {
  if (participant.handle) return `@${participant.handle}`;
  return participant.name || participant.email;
}

export default function MessagesPage() {
  const router = useRouter();
  const { user, token, isLoading: authLoading } = useAuth();
  const { t } = useLanguage();

  const [conversations, setConversations] = useState<ConversationDto[]>([]);
  const [showArchived, setShowArchived] = useState(false);
  const [selectedConvId, setSelectedConvId] = useState<string | null>(null);
  const [messages, setMessages] = useState<MessageDto[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [loadingConvs, setLoadingConvs] = useState(true);
  const [loadingMsgs, setLoadingMsgs] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');

  // New conversation modal
  const [showNewConv, setShowNewConv] = useState(false);
  const [newConvHandle, setNewConvHandle] = useState('');
  const [newConvMessage, setNewConvMessage] = useState('');
  const [creatingConv, setCreatingConv] = useState(false);

  // Report
  const [reportingMsgId, setReportingMsgId] = useState<string | null>(null);
  const [reportReason, setReportReason] = useState('');

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const selectedConv = conversations.find((c) => c.id === selectedConvId);

  const fetchConversations = useCallback(async () => {
    if (!token) return;
    try {
      const data = await messagingApi.conversations(token, showArchived);
      setConversations(data);
    } catch (err) {
      if (loadingConvs) {
        setError(err instanceof ApiError ? err.message : 'Failed to load conversations');
      }
    } finally {
      setLoadingConvs(false);
    }
  }, [token, showArchived, loadingConvs]);

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
    if (!user || !token) { router.push('/login'); return; }
    fetchConversations();
  }, [user, token, authLoading, router, fetchConversations]);

  useEffect(() => {
    if (!selectedConvId || !token) return;
    setLoadingMsgs(true);
    fetchMessages(selectedConvId);
    messagingApi.markRead(selectedConvId, token).catch(() => {});

    pollRef.current = setInterval(() => {
      fetchMessages(selectedConvId);
      fetchConversations();
    }, 5000);

    return () => { if (pollRef.current) clearInterval(pollRef.current); };
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

  async function handleNewConversation(e: React.FormEvent) {
    e.preventDefault();
    if (!newConvHandle.trim() || !newConvMessage.trim() || !token) return;
    setCreatingConv(true);
    try {
      const handle = newConvHandle.trim().replace(/^@/, '');
      const conv = await messagingApi.create(
        { participantHandle: handle, initialMessage: newConvMessage.trim() },
        token
      );
      setShowNewConv(false);
      setNewConvHandle('');
      setNewConvMessage('');
      fetchConversations();
      setSelectedConvId(conv.id);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to create conversation');
    } finally {
      setCreatingConv(false);
    }
  }

  async function handleArchive(convId: string) {
    if (!token) return;
    try {
      if (showArchived) {
        await messagingApi.unarchive(convId, token);
      } else {
        await messagingApi.archive(convId, token);
      }
      if (selectedConvId === convId) setSelectedConvId(null);
      fetchConversations();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to archive');
    }
  }

  async function handleReport() {
    if (!reportingMsgId || !reportReason.trim() || !token) return;
    try {
      await messagingApi.report(reportingMsgId, reportReason.trim(), token);
      setReportingMsgId(null);
      setReportReason('');
      setError('');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to report message');
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
          <div className="flex items-center justify-between">
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
                {selectedConv ? displayName(selectedConv.otherParticipant) : t.messaging.title}
              </h1>
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => { setShowArchived(!showArchived); setSelectedConvId(null); setLoadingConvs(true); }}
                className={`p-2 rounded-lg transition-colors cursor-pointer ${showArchived ? 'bg-rose-100 text-rose-600' : 'hover:bg-gray-100 text-gray-500'}`}
                title={showArchived ? 'Show active' : 'Show archived'}
              >
                <Archive className="h-5 w-5" />
              </button>
              <button
                type="button"
                onClick={() => setShowNewConv(true)}
                className="p-2 rounded-lg hover:bg-gray-100 text-gray-500 transition-colors cursor-pointer"
                title="New conversation"
              >
                <Plus className="h-5 w-5" />
              </button>
            </div>
          </div>
        </div>

        {error && (
          <div className="mx-4 mt-2 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 flex items-center gap-2">
            <AlertCircle className="h-4 w-4 shrink-0" />
            {error}
            <button type="button" onClick={() => setError('')} className="ml-auto text-red-500 hover:text-red-700 cursor-pointer">
              {t.common.close}
            </button>
          </div>
        )}

        {/* New conversation modal */}
        {showNewConv && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setShowNewConv(false)}>
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 p-6" onClick={(e) => e.stopPropagation()}>
              <h2 className="text-lg font-bold text-gray-900 mb-4">New Conversation</h2>
              <form onSubmit={handleNewConversation} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">To (handle)</label>
                  <input
                    value={newConvHandle}
                    onChange={(e) => setNewConvHandle(e.target.value.toLowerCase().replace(/[^a-z0-9_@]/g, ''))}
                    placeholder="@username"
                    className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-rose-500/30 focus:border-rose-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Message</label>
                  <textarea
                    value={newConvMessage}
                    onChange={(e) => setNewConvMessage(e.target.value)}
                    rows={3}
                    placeholder="Type your message..."
                    className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-rose-500/30 focus:border-rose-500 resize-none"
                  />
                </div>
                <div className="flex justify-end gap-2">
                  <Button type="button" variant="secondary" onClick={() => setShowNewConv(false)}>Cancel</Button>
                  <Button type="submit" loading={creatingConv} disabled={!newConvHandle.trim() || !newConvMessage.trim()}>
                    Send
                  </Button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Report modal */}
        {reportingMsgId && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setReportingMsgId(null)}>
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 p-6" onClick={(e) => e.stopPropagation()}>
              <h2 className="text-lg font-bold text-gray-900 mb-4">Report Message</h2>
              <textarea
                value={reportReason}
                onChange={(e) => setReportReason(e.target.value)}
                rows={3}
                placeholder="Why are you reporting this message?"
                className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-rose-500/30 focus:border-rose-500 resize-none mb-4"
              />
              <div className="flex justify-end gap-2">
                <Button type="button" variant="secondary" onClick={() => setReportingMsgId(null)}>Cancel</Button>
                <Button onClick={handleReport} disabled={!reportReason.trim()}>Report</Button>
              </div>
            </div>
          </div>
        )}

        <div className="flex-1 flex overflow-hidden">
          {/* Conversation list */}
          <div className={`w-full md:w-80 md:border-r border-gray-200 bg-white flex flex-col overflow-y-auto ${selectedConvId ? 'hidden md:flex' : 'flex'}`}>
            {showArchived && (
              <div className="px-4 py-2 bg-amber-50 border-b border-amber-100 text-xs font-medium text-amber-700">
                Showing archived conversations
              </div>
            )}
            {loadingConvs ? (
              <div className="flex items-center justify-center py-20 text-gray-400">
                <Loader2 className="h-6 w-6 animate-spin" />
              </div>
            ) : conversations.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-20 text-gray-400">
                <MessageSquare className="h-10 w-10 mb-2" />
                <p className="text-sm">{showArchived ? 'No archived conversations' : t.messaging.noConversations}</p>
              </div>
            ) : (
              conversations.map((conv) => {
                const isSelected = conv.id === selectedConvId;
                const isUnread = conv.lastMessage && !conv.lastMessage.isRead && conv.lastMessage.senderId !== user?.id;

                return (
                  <div key={conv.id} className={`group relative border-b border-gray-50 ${isSelected ? 'bg-rose-50 border-l-2 border-l-rose-500' : 'hover:bg-gray-50'}`}>
                    <button
                      type="button"
                      onClick={() => handleSelectConversation(conv.id)}
                      className="w-full text-left px-4 py-3 cursor-pointer"
                    >
                      <div className="flex items-start gap-3">
                        <div className="shrink-0 w-10 h-10 rounded-full bg-rose-100 flex items-center justify-center">
                          <User className="h-5 w-5 text-rose-600" />
                        </div>
                        <div className="min-w-0 flex-1">
                          <div className="flex items-center justify-between gap-2">
                            <p className={`text-sm truncate ${isUnread ? 'font-bold text-gray-900' : 'font-medium text-gray-700'}`}>
                              {displayName(conv.otherParticipant)}
                            </p>
                            <div className="flex items-center gap-1.5 shrink-0">
                              {isUnread && <Circle className="h-2.5 w-2.5 fill-rose-500 text-rose-500" />}
                              {conv.lastMessage && (
                                <span className="text-xs text-gray-400">
                                  {formatDistanceToNow(new Date(conv.lastMessage.createdAt), { addSuffix: true })}
                                </span>
                              )}
                            </div>
                          </div>
                          {conv.topic && <p className="text-xs text-rose-500 font-medium truncate mt-0.5">{conv.topic}</p>}
                          {conv.lastMessage && (
                            <p className="text-xs text-gray-400 truncate mt-0.5">
                              {conv.lastMessage.senderId === user?.id ? `${t.messaging.you}: ` : ''}
                              {conv.lastMessage.content}
                            </p>
                          )}
                        </div>
                      </div>
                    </button>
                    {/* Archive button on hover */}
                    <button
                      type="button"
                      onClick={(e) => { e.stopPropagation(); handleArchive(conv.id); }}
                      className="absolute right-2 top-2 p-1.5 rounded-lg opacity-0 group-hover:opacity-100 hover:bg-gray-200 transition-all cursor-pointer text-gray-400 hover:text-gray-600"
                      title={showArchived ? 'Unarchive' : 'Archive'}
                    >
                      {showArchived ? <ArchiveRestore className="h-4 w-4" /> : <Archive className="h-4 w-4" />}
                    </button>
                  </div>
                );
              })
            )}
          </div>

          {/* Message area */}
          <div className={`flex-1 flex flex-col bg-gray-50 ${selectedConvId ? 'flex' : 'hidden md:flex'}`}>
            {!selectedConvId ? (
              <div className="flex-1 flex flex-col items-center justify-center text-gray-400">
                <MessageSquare className="h-12 w-12 mb-3" />
                <p className="text-base font-medium text-gray-500">{t.messaging.title}</p>
                <p className="text-sm mt-1">{t.messaging.noConversations}</p>
              </div>
            ) : (
              <>
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
                        <div key={msg.id} className={`group flex ${isMine ? 'justify-end' : 'justify-start'}`}>
                          <div className={`max-w-[80%] sm:max-w-[70%] rounded-2xl px-4 py-2.5 ${
                            isMine
                              ? 'bg-rose-500 text-white rounded-br-md'
                              : 'bg-white text-gray-900 border border-gray-100 rounded-bl-md shadow-sm'
                          }`}>
                            {!isMine && (
                              <p className="text-xs font-medium text-rose-500 mb-0.5">
                                {msg.senderHandle ? `@${msg.senderHandle}` : msg.senderName}
                              </p>
                            )}
                            <p className="text-sm whitespace-pre-wrap break-words">{msg.content}</p>
                            <div className={`flex items-center gap-2 mt-1 ${isMine ? 'justify-end' : 'justify-between'}`}>
                              <p className={`text-[10px] ${isMine ? 'text-rose-200' : 'text-gray-400'}`}>
                                {format(new Date(msg.createdAt), 'h:mm a')}
                              </p>
                              {!isMine && (
                                <button
                                  type="button"
                                  onClick={() => { setReportingMsgId(msg.id); setReportReason(''); }}
                                  className="opacity-0 group-hover:opacity-100 transition-opacity text-gray-300 hover:text-red-500 cursor-pointer"
                                  title="Report message"
                                >
                                  <Flag className="h-3 w-3" />
                                </button>
                              )}
                            </div>
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
                        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend(e); }
                      }}
                      placeholder={t.messaging.typeMessage}
                      rows={1}
                      className="flex-1 resize-none rounded-xl border border-gray-200 px-4 py-2.5 text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-rose-500/30 focus:border-rose-500"
                    />
                    <Button type="submit" size="md" disabled={!newMessage.trim()} loading={sending}>
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
