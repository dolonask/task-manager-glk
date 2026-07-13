import type { CSSProperties } from "react";
import type { Priority, TaskStatus, TransferStatus } from "../api/types";

const pillStyle: CSSProperties = {
  display: "inline-block",
  borderRadius: "var(--radius-pill)",
  fontWeight: 700,
  fontSize: 11,
  padding: "4px 11px",
  whiteSpace: "nowrap",
};

const taskStatusMap: Record<TaskStatus, { label: string; bg: string; text: string }> = {
  new: { label: "Новая", bg: "var(--status-new-bg)", text: "var(--status-new-text)" },
  in_progress: { label: "В работе", bg: "var(--status-in-progress-bg)", text: "var(--status-in-progress-text)" },
  done: { label: "Выполнено", bg: "var(--status-done-bg)", text: "var(--status-done-text)" },
  overdue: { label: "Просрочено", bg: "var(--status-overdue-bg)", text: "var(--status-overdue-text)" },
};

const transferStatusMap: Record<TransferStatus, { label: string; bg: string; text: string }> = {
  draft: { label: "Черновик", bg: "var(--status-new-bg)", text: "var(--status-new-text)" },
  pending: { label: "На согласовании", bg: "var(--status-in-progress-bg)", text: "var(--status-in-progress-text)" },
  approved: { label: "Согласована", bg: "var(--status-new-bg)", text: "var(--color-text-table)" },
  rejected: { label: "Отклонена", bg: "var(--status-overdue-bg)", text: "var(--status-overdue-text)" },
  revision: { label: "На доработке", bg: "var(--status-overdue-bg)", text: "var(--status-overdue-text)" },
  applied: { label: "Исполнена", bg: "var(--status-done-bg)", text: "var(--status-done-text)" },
};

export function TaskStatusBadge({ status }: { status: TaskStatus }) {
  const cfg = taskStatusMap[status];
  return <span style={{ ...pillStyle, background: cfg.bg, color: cfg.text }}>{cfg.label}</span>;
}

export function TransferStatusBadge({ status }: { status: TransferStatus }) {
  const cfg = transferStatusMap[status];
  return <span style={{ ...pillStyle, background: cfg.bg, color: cfg.text }}>{cfg.label}</span>;
}

const priorityMap: Record<Priority, { label: string; color: string }> = {
  high: { label: "Высокий", color: "var(--priority-high)" },
  medium: { label: "Средний", color: "var(--priority-medium)" },
  low: { label: "Низкий", color: "var(--priority-low)" },
};

export function PriorityText({ priority }: { priority: Priority }) {
  const cfg = priorityMap[priority];
  return <span style={{ color: cfg.color, fontWeight: 700 }}>{cfg.label}</span>;
}
