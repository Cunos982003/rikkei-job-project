import React, { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Briefcase,
  MessageSquarePlus,
  Send,
  Sparkles,
  Square,
  User,
  AlertCircle,
  Clock,
} from "lucide-react";
import { toast } from "sonner";
import rikkeiBuddyAvatar from "../../assets/rikkei-buddy.jpg";
import {
  careerChatApi,
  type AiQuota,
  type ChatMessageItem,
  type ConversationItem,
  type ConversationMode,
} from "../../api/endpoints/careerChat.api";
import { jobsApi, type JobPostingItem } from "../../api/endpoints/jobs.api";
import { Button } from "../../components/common/Button";
import { MarkdownRenderer } from "../../components/common/MarkdownRenderer";

const SAMPLE_QUESTIONS = [
  "Mình nên học gì để làm Java Backend?",
  "Gợi ý lộ trình nâng cao kỹ năng từ hồ sơ hiện tại của mình",
  "Cách viết CV nổi bật cho vị trí Fresher / Junior IT?",
  "Mình cần chuẩn bị gì trước buổi phỏng vấn kỹ thuật?",
];

function formatResetTime(isoString?: string): string {
  if (!isoString) return "00:00 ngày mai";
  try {
    const d = new Date(isoString);
    return d.toLocaleString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
    });
  } catch {
    return isoString;
  }
}

export const CareerAssistantPage: React.FC = () => {
  const [sidebarTab, setSidebarTab] = useState<ConversationMode>("CAREER");
  const [conversations, setConversations] = useState<ConversationItem[]>([]);
  const [activeConvId, setActiveConvId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessageItem[]>([]);
  const [quota, setQuota] = useState<AiQuota | null>(null);
  const [quotaError, setQuotaError] = useState<string | null>(null);

  const [selectedJobId, setSelectedJobId] = useState<string>("");
  const [input, setInput] = useState("");
  const [isLoadingConvs, setIsLoadingConvs] = useState(true);
  const [isLoadingMsgs, setIsLoadingMsgs] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamingContent, setStreamingContent] = useState("");

  const abortControllerRef = useRef<AbortController | null>(null);
  const messagesContainerRef = useRef<HTMLDivElement | null>(null);
  const shouldAutoScrollRef = useRef(true);

  const activeConversation = useMemo(
    () => conversations.find((c) => c.id === activeConvId) || null,
    [conversations, activeConvId]
  );

  // Load approved jobs for the Interview Practice tab
  const { data: jobsRes, isLoading: isJobsLoading } = useQuery({
    queryKey: ["approvedJobsForAssistantInterview"],
    queryFn: async () => {
      try {
        return await jobsApi.getJobs({ status: "APPROVED", size: 100 });
      } catch {
        return null;
      }
    },
    staleTime: 5 * 60 * 1000,
  });

  const approvedJobs: JobPostingItem[] = useMemo(() => {
    if (!jobsRes?.data) return [];
    if (Array.isArray(jobsRes.data)) return jobsRes.data;
    if ("items" in jobsRes.data && Array.isArray(jobsRes.data.items)) {
      return jobsRes.data.items;
    }
    return [];
  }, [jobsRes]);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      careerChatApi.listConversations(),
      careerChatApi.getQuota(),
    ])
      .then(([convRes, quotaRes]) => {
        if (cancelled) return;
        const list = convRes.data ?? [];
        setConversations(list);
        if (quotaRes.data) {
          setQuota(quotaRes.data);
          if (quotaRes.data.remaining <= 0) {
            setQuotaError(
              `Bạn đã dùng hết ${quotaRes.data.limit} lượt hôm nay. Đặt lại lúc ${formatResetTime(quotaRes.data.resetAt)}.`
            );
          }
        }
        if (list.length > 0) {
          setActiveConvId((prev) => prev ?? list[0].id);
          setSidebarTab(list[0].mode);
        }
      })
      .catch(() => {
        if (!cancelled) toast.error("Không thể tải danh sách hội thoại AI.");
      })
      .finally(() => {
        if (!cancelled) setIsLoadingConvs(false);
      });

    return () => {
      cancelled = true;
      abortControllerRef.current?.abort();
    };
  }, []);

  useEffect(() => {
    if (!activeConvId) {
      return;
    }
    let cancelled = false;
    careerChatApi
      .getMessages(activeConvId)
      .then((res) => {
        if (!cancelled) {
          setMessages(res.data ?? []);
        }
      })
      .catch(() => {
        if (!cancelled) {
          toast.error("Không thể tải lịch sử tin nhắn.");
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoadingMsgs(false);
      });
    return () => {
      cancelled = true;
    };
  }, [activeConvId]);

  useEffect(() => {
    const container = messagesContainerRef.current;
    if (!container || !shouldAutoScrollRef.current) return;
    container.scrollTop = container.scrollHeight;
  }, [messages, streamingContent]);

  const handleCreateCareerChat = async () => {
    if (isStreaming || isCreating) return;
    setIsCreating(true);
    try {
      const res = await careerChatApi.createConversation({ mode: "CAREER" });
      if (res.data) {
        setConversations((prev) => [res.data, ...prev]);
        setActiveConvId(res.data.id);
        setSidebarTab("CAREER");
        setMessages([]);
      }
    } catch {
      toast.error("Không thể tạo cuộc trò chuyện mới.");
    } finally {
      setIsCreating(false);
    }
  };

  const handleStartInterview = async () => {
    if (!selectedJobId) {
      toast.warning("Vui lòng chọn một tin tuyển dụng (JD) trước khi bắt đầu.");
      return;
    }
    if (isStreaming || isCreating) return;
    setIsCreating(true);
    try {
      const res = await careerChatApi.createConversation({
        mode: "INTERVIEW",
        jobId: selectedJobId,
      });
      if (res.data) {
        setConversations((prev) => [res.data, ...prev]);
        setActiveConvId(res.data.id);
        setSidebarTab("INTERVIEW");
      }
    } catch {
      toast.error("Không thể khởi tạo buổi luyện phỏng vấn.");
    } finally {
      setIsCreating(false);
    }
  };

  const handleStopStreaming = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    if (streamingContent.trim()) {
      setMessages((prev) => [
        ...prev,
        {
          id: `stopped-${Date.now()}`,
          conversationId: activeConvId || "",
          role: "ASSISTANT",
          content: streamingContent + "\n\n*(Đã dừng trả lời)*",
          tokens: 0,
          createdAt: new Date().toISOString(),
        },
      ]);
    }
    setStreamingContent("");
    setIsStreaming(false);
  };

  const handleSend = async (overrideContent?: string) => {
    const text = (overrideContent ?? input).trim();
    if (!text || isStreaming) return;
    if (quota && quota.remaining <= 0) {
      setQuotaError(
        `Bạn đã dùng hết ${quota.limit} lượt hôm nay. Đặt lại lúc ${formatResetTime(quota.resetAt)}.`
      );
      return;
    }

    let targetConvId = activeConvId;
    if (!targetConvId) {
      setIsCreating(true);
      try {
        const res = await careerChatApi.createConversation({ mode: "CAREER" });
        if (!res.data) return;
        targetConvId = res.data.id;
        setConversations((prev) => [res.data, ...prev]);
        setActiveConvId(res.data.id);
      } catch {
        toast.error("Không thể tạo cuộc trò chuyện mới.");
        return;
      } finally {
        setIsCreating(false);
      }
    }

    if (!overrideContent) {
      setInput("");
    }
    setQuotaError(null);

    const tempUserId = `temp-user-${Date.now()}`;
    const optimisticUserMsg: ChatMessageItem = {
      id: tempUserId,
      conversationId: targetConvId,
      role: "USER",
      content: text,
      tokens: 0,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, optimisticUserMsg]);
    setIsStreaming(true);
    setStreamingContent("");

    const controller = new AbortController();
    abortControllerRef.current = controller;

    try {
      await careerChatApi.streamMessage(
        targetConvId,
        text,
        {
          onUserMessage: (savedUser) => {
            setMessages((prev) =>
              prev.map((m) => (m.id === tempUserId ? savedUser : m))
            );
            // Refresh conversation title if it was default
            setConversations((prev) =>
              prev.map((c) =>
                c.id === targetConvId && c.title === "Tư vấn nghề nghiệp"
                  ? {
                      ...c,
                      title: text.length <= 60 ? text : text.slice(0, 57) + "...",
                    }
                  : c
              )
            );
          },
          onDelta: (chunk) => {
            setStreamingContent((prev) => prev + chunk);
          },
          onDone: (payload) => {
            setStreamingContent("");
            setIsStreaming(false);
            abortControllerRef.current = null;
            setMessages((prev) => {
              const withoutTemp = prev.filter(
                (m) => m.id !== tempUserId && m.id !== payload.userMessage.id
              );
              return [...withoutTemp, payload.userMessage, payload.assistantMessage];
            });
            setQuota(payload.quota);
            if (payload.quota.remaining <= 0) {
              setQuotaError(
                `Bạn đã dùng hết ${payload.quota.limit} lượt hôm nay. Đặt lại lúc ${formatResetTime(payload.quota.resetAt)}.`
              );
            }
          },
          onQuotaExceeded: (message, latestQuota) => {
            setIsStreaming(false);
            setStreamingContent("");
            abortControllerRef.current = null;
            setMessages((prev) => prev.filter((m) => m.id !== tempUserId));
            if (latestQuota) setQuota(latestQuota);
            setQuotaError(message);
            toast.error(message);
          },
          onError: (message) => {
            setIsStreaming(false);
            setStreamingContent("");
            abortControllerRef.current = null;
            toast.error(message);
            void careerChatApi.getMessages(targetConvId).then((res) => {
              if (res.data) setMessages(res.data);
            }).catch(() => {});
          },
        },
        controller.signal
      );
    } catch (err) {
      if (err instanceof DOMException && err.name === "AbortError") {
        // Handled in handleStopStreaming
        return;
      }
      setIsStreaming(false);
      setStreamingContent("");
      abortControllerRef.current = null;
      toast.error("Mất kết nối khi nhận phản hồi từ AI.");
    }
  };

  const filteredConversations = useMemo(
    () => conversations.filter((c) => c.mode === sidebarTab),
    [conversations, sidebarTab]
  );

  return (
    <div className="grid h-[calc(100vh-13rem)] min-h-[560px] grid-cols-1 overflow-hidden rounded-2xl border border-border bg-surface shadow-xs lg:grid-cols-[300px_1fr]">
      {/* Left Sidebar: Mode Tabs + Conversation List */}
      <aside className="flex flex-col border-b border-border bg-slate-50/70 lg:border-r lg:border-b-0">
        {/* Mode Switcher Tabs */}
        <div className="grid grid-cols-2 gap-1 border-b border-border p-2">
          <button
            type="button"
            onClick={() => setSidebarTab("CAREER")}
            className={`flex items-center justify-center gap-1.5 rounded-lg px-2.5 py-2 text-xs font-semibold transition-colors ${
              sidebarTab === "CAREER"
                ? "bg-primary text-white shadow-2xs"
                : "text-muted hover:bg-slate-200/70 hover:text-foreground"
            }`}
          >
            <Sparkles className="h-3.5 w-3.5" />
            Nghề nghiệp
          </button>
          <button
            type="button"
            onClick={() => setSidebarTab("INTERVIEW")}
            className={`flex items-center justify-center gap-1.5 rounded-lg px-2.5 py-2 text-xs font-semibold transition-colors ${
              sidebarTab === "INTERVIEW"
                ? "bg-primary text-white shadow-2xs"
                : "text-muted hover:bg-slate-200/70 hover:text-foreground"
            }`}
          >
            <Briefcase className="h-3.5 w-3.5" />
            Luyện phỏng vấn
          </button>
        </div>

        {/* Action Panel per Tab */}
        <div className="border-b border-border p-3">
          {sidebarTab === "CAREER" ? (
            <Button
              variant="primary"
              size="sm"
              className="w-full"
              onClick={handleCreateCareerChat}
              isLoading={isCreating}
              disabled={isStreaming}
              leftIcon={<MessageSquarePlus className="h-4 w-4" />}
            >
              Cuộc trò chuyện mới
            </Button>
          ) : (
            <div className="space-y-2">
              <label
                htmlFor="interview-jd-select"
                className="block text-[11px] font-semibold uppercase tracking-wide text-muted"
              >
                Chọn JD để luyện phỏng vấn
              </label>
              <select
                id="interview-jd-select"
                value={selectedJobId}
                onChange={(e) => setSelectedJobId(e.target.value)}
                disabled={isJobsLoading || isCreating || isStreaming}
                className="w-full rounded-lg border border-border bg-surface px-2.5 py-2 text-xs text-foreground focus:border-primary focus:outline-none"
              >
                <option value="">
                  {isJobsLoading ? "Đang tải danh sách JD..." : "-- Chọn tin tuyển dụng --"}
                </option>
                {approvedJobs.map((job) => (
                  <option key={job.id} value={job.id}>
                    {job.title}
                    {job.companyName ? ` (${job.companyName})` : ""}
                  </option>
                ))}
              </select>
              <Button
                variant="primary"
                size="sm"
                className="w-full"
                onClick={handleStartInterview}
                isLoading={isCreating}
                disabled={!selectedJobId || isStreaming}
              >
                Bắt đầu phỏng vấn
              </Button>
            </div>
          )}
        </div>

        {/* Conversation List */}
        <div className="flex-1 overflow-y-auto p-2 space-y-1">
          {isLoadingConvs ? (
            <div className="p-3 text-xs text-muted">Đang tải hội thoại...</div>
          ) : filteredConversations.length === 0 ? (
            <div className="p-4 text-center text-xs text-muted">
              {sidebarTab === "CAREER"
                ? "Chưa có cuộc trò chuyện nghề nghiệp nào."
                : "Chưa có buổi phỏng vấn nào. Hãy chọn JD ở trên để bắt đầu!"}
            </div>
          ) : (
            filteredConversations.map((conv) => {
              const isActive = conv.id === activeConvId;
              return (
                <button
                  key={conv.id}
                  type="button"
                  onClick={() => {
                    if (!isStreaming && conv.id !== activeConvId) {
                      setIsLoadingMsgs(true);
                      setActiveConvId(conv.id);
                    }
                  }}
                  className={`w-full rounded-xl px-3 py-2.5 text-left transition-colors ${
                    isActive
                      ? "border border-primary-border bg-primary-light text-primary"
                      : "hover:bg-slate-200/60 text-foreground"
                  }`}
                >
                  <div className="truncate text-xs font-semibold">{conv.title}</div>
                  <div className="mt-1 flex items-center justify-between text-[11px] text-muted">
                    <span>{conv.mode === "INTERVIEW" ? "Phỏng vấn AI" : "Tư vấn"}</span>
                    <span>
                      {new Date(conv.createdAt).toLocaleDateString("vi-VN", {
                        day: "2-digit",
                        month: "2-digit",
                      })}
                    </span>
                  </div>
                </button>
              );
            })
          )}
        </div>
      </aside>

      {/* Right Panel: Chat Window */}
      <section className="flex min-h-0 flex-col bg-surface">
        {/* Header with Quota */}
        <header className="flex flex-wrap items-center justify-between gap-2 border-b border-border px-4 py-3">
          <div className="flex items-center gap-2.5 min-w-0">
            <img
              src={rikkeiBuddyAvatar}
              alt="Rikkei Buddy"
              className="h-9 w-9 shrink-0 rounded-full object-cover border border-border shadow-2xs"
            />
            <div className="min-w-0">
              <h2 className="truncate text-sm font-bold text-foreground">
                {activeConversation ? activeConversation.title : "Rikkei Buddy"}
              </h2>
              <p className="text-[11px] text-muted">
                {activeConversation?.mode === "INTERVIEW"
                  ? "Rikkei Buddy · Luyện phỏng vấn (Chấm điểm chuẩn AI-03)"
                  : "Rikkei Buddy · Your Learning & Career Companion."}
              </p>
            </div>
          </div>

          {/* Remaining Quota Badge */}
          <div
            className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-semibold ${
              quota && quota.remaining <= 5
                ? "border-amber-300 bg-amber-50 text-amber-800"
                : "border-border bg-slate-50 text-foreground"
            }`}
            title={quota ? `Đặt lại lúc ${formatResetTime(quota.resetAt)}` : undefined}
          >
            <Clock className="h-3.5 w-3.5 text-primary" />
            <span>
              Còn{" "}
              <strong>{quota ? quota.remaining : 30}</strong>/{quota ? quota.limit : 30} lượt hôm nay
            </span>
          </div>
        </header>

        {/* Messages Area */}
        <div
          ref={messagesContainerRef}
          onScroll={(e) => {
            const el = e.currentTarget;
            shouldAutoScrollRef.current =
              el.scrollHeight - el.scrollTop - el.clientHeight < 120;
          }}
          className="flex-1 overflow-y-auto p-4 space-y-4"
        >
          {isLoadingMsgs ? (
            <div className="py-10 text-center text-xs text-muted">Đang tải tin nhắn...</div>
          ) : messages.length === 0 && !isStreaming ? (
            <div className="mx-auto flex max-w-xl flex-col items-center justify-center py-8 text-center">
              <img
                src={rikkeiBuddyAvatar}
                alt="Rikkei Buddy"
                className="mb-3 h-16 w-16 rounded-full object-cover border-2 border-primary/20 shadow-sm"
              />
              <h3 className="text-base font-bold text-foreground">
                Rikkei Buddy
              </h3>
              <p className="text-[11px] font-medium text-primary">
                Your Learning &amp; Career Companion.
              </p>
              <p className="mt-2.5 whitespace-pre-line text-xs leading-relaxed text-muted">
                {"Xin chào! Mình là Rikkei Buddy – người đồng hành cùng bạn trên hành trình học tập và phát triển sự nghiệp.\n\nBạn cần hỗ trợ học tập, tìm kiếm công việc, hoàn thiện CV hay luyện phỏng vấn? Mình luôn sẵn sàng cùng bạn tìm ra bước tiếp theo!"}
              </p>

              <div className="mt-5 grid w-full grid-cols-1 gap-2 sm:grid-cols-2">
                {SAMPLE_QUESTIONS.map((q) => (
                  <button
                    key={q}
                    type="button"
                    onClick={() => void handleSend(q)}
                    disabled={Boolean(quota && quota.remaining <= 0)}
                    className="rounded-xl border border-border bg-slate-50 p-3 text-left text-xs font-medium text-foreground transition-colors hover:border-primary hover:bg-primary-light/40 disabled:opacity-50"
                  >
                    “{q}”
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <>
              {messages.map((msg) => {
                const isUser = msg.role === "USER";
                return (
                  <div
                    key={msg.id}
                    className={`flex items-start gap-2.5 ${isUser ? "justify-end" : "justify-start"}`}
                  >
                    {!isUser && (
                      <img
                        src={rikkeiBuddyAvatar}
                        alt="Rikkei Buddy"
                        className="h-7 w-7 shrink-0 rounded-full object-cover border border-border"
                      />
                    )}
                    <div
                      className={`max-w-[82%] rounded-2xl px-4 py-2.5 ${
                        isUser
                          ? "bg-primary text-white text-sm whitespace-pre-wrap"
                          : "border border-border bg-slate-50/90 text-foreground"
                      }`}
                    >
                      {isUser ? msg.content : <MarkdownRenderer content={msg.content} />}
                    </div>
                    {isUser && (
                      <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-slate-200 text-slate-700">
                        <User className="h-4 w-4" />
                      </div>
                    )}
                  </div>
                );
              })}

              {isStreaming && (
                <div className="flex items-start gap-2.5 justify-start">
                  <img
                    src={rikkeiBuddyAvatar}
                    alt="Rikkei Buddy"
                    className="h-7 w-7 shrink-0 rounded-full object-cover border border-border animate-pulse"
                  />
                  <div className="max-w-[82%] rounded-2xl border border-border bg-slate-50/90 px-4 py-2.5">
                    {streamingContent ? (
                      <MarkdownRenderer content={streamingContent} />
                    ) : (
                      <span className="text-xs text-muted">Đang suy nghĩ...</span>
                    )}
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Quota Exceeded Banner */}
        {quotaError && (
          <div className="mx-4 mb-2 flex items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-xs text-rose-700">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{quotaError}</span>
          </div>
        )}

        {/* Input Composer */}
        <form
          onSubmit={(e) => {
            e.preventDefault();
            void handleSend();
          }}
          className="border-t border-border p-3"
        >
          <div className="flex items-end gap-2">
            <textarea
              rows={2}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  void handleSend();
                }
              }}
              placeholder={
                activeConversation?.mode === "INTERVIEW"
                  ? "Nhập câu trả lời phỏng vấn của bạn (Enter để gửi, Shift+Enter xuống dòng)..."
                  : "Hỏi trợ lý về nghề nghiệp, kỹ năng, học tập hoặc tuyển dụng..."
              }
              disabled={isStreaming || Boolean(quota && quota.remaining <= 0)}
              className="flex-1 resize-none rounded-xl border border-border bg-surface px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none disabled:bg-slate-100"
            />
            {isStreaming ? (
              <Button
                type="button"
                variant="danger"
                size="sm"
                onClick={handleStopStreaming}
                leftIcon={<Square className="h-3.5 w-3.5" />}
              >
                Dừng trả lời
              </Button>
            ) : (
              <Button
                type="submit"
                variant="primary"
                size="sm"
                disabled={!input.trim() || Boolean(quota && quota.remaining <= 0)}
                leftIcon={<Send className="h-3.5 w-3.5" />}
              >
                Gửi
              </Button>
            )}
          </div>
        </form>
      </section>
    </div>
  );
};
