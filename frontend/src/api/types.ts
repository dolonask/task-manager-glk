export type Role = "admin" | "board" | "head" | "employee" | "observer";
export type TaskStatus = "new" | "in_progress" | "done" | "overdue";
export type TransferStatus = "draft" | "pending" | "approved" | "rejected" | "revision" | "applied";
export type Priority = "high" | "medium" | "low";

export interface ApiErrorBody {
  error: {
    code: string;
    message: string;
    field?: string;
  };
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
}

export interface UserSummary {
  id: string;
  fullName: string;
  login: string;
  role: Role;
  departmentId: string | null;
  curatedDepartmentIds: string[];
}

export interface UserResponse extends UserSummary {
  isActive: boolean;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: UserSummary;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface DepartmentResponse {
  id: string;
  name: string;
  curatorId: string | null;
  curatorName: string | null;
  headId: string | null;
  headName: string | null;
}

export interface CreateDepartmentRequest {
  name: string;
  curatorId?: string;
  headId?: string;
}

export interface UpdateDepartmentRequest {
  name?: string;
  curatorId?: string;
  headId?: string;
}

export interface SubtaskResponse {
  id: string;
  taskId: string;
  title: string;
  description: string | null;
  assigneeId: string | null;
  assigneeName: string | null;
  deadline: string;
  done: boolean;
}

export interface CreateSubtaskRequest {
  title: string;
  description?: string;
  assigneeId?: string;
  deadline: string;
}

export interface UpdateSubtaskRequest {
  title?: string;
  description?: string;
  assigneeId?: string;
  deadline?: string;
}

export interface TransferRequestResponse {
  id: string;
  taskId: string;
  taskTitle: string;
  departmentId: string;
  departmentName: string;
  initiatorId: string;
  initiatorName: string;
  approverId: string | null;
  approverName: string | null;
  currentDeadline: string;
  proposedDeadline: string;
  justification: string;
  status: TransferStatus;
  decisionReason: string | null;
  submittedAt: string | null;
  decidedAt: string | null;
  appliedAt: string | null;
  appliedById: string | null;
  appliedByName: string | null;
}

export interface CreateTransferRequestRequest {
  proposedDeadline: string;
  justification: string;
}

export interface DecisionRequest {
  decisionReason: string;
}

export interface TaskListItemResponse {
  id: string;
  title: string;
  description: string;
  creatorId: string;
  creatorName: string;
  departmentId: string;
  departmentName: string;
  assigneeId: string | null;
  assigneeName: string | null;
  priority: Priority;
  initialDeadline: string;
  currentDeadline: string;
  status: TaskStatus;
  progress: number;
  createdAt: string;
  closedAt: string | null;
}

export interface TaskDetailResponse extends TaskListItemResponse {
  subtasks: SubtaskResponse[];
  transferRequests: TransferRequestResponse[];
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  departmentId: string;
  assigneeId?: string;
  priority: Priority;
  initialDeadline: string;
}

export interface UpdateTaskRequest {
  title?: string;
  description?: string;
  priority?: Priority;
  assigneeId?: string;
}

export interface CreateUserRequest {
  fullName: string;
  login: string;
  password: string;
  role: Role;
  departmentId?: string;
}

export interface UpdateUserRequest {
  fullName?: string;
  login?: string;
  password?: string;
  role?: Role;
  departmentId?: string;
  isActive?: boolean;
}

export interface AuditLogEntryResponse {
  id: string;
  ts: string;
  actorId: string | null;
  actorName: string | null;
  entityType: string;
  entityId: string;
  action: string;
  before: unknown;
  after: unknown;
}

export interface NotificationResponse {
  id: string;
  message: string;
  entityType: string;
  entityId: string;
  read: boolean;
  createdAt: string;
}

export interface AnalyticsSummaryResponse {
  totalTasks: number;
  doneTasks: number;
  inProgressTasks: number;
  overdueTasks: number;
  completionRate: number;
  onTimeRate: number;
}

export interface DepartmentAnalyticsResponse {
  departmentId: string;
  departmentName: string;
  totalTasks: number;
  doneTasks: number;
  overdueTasks: number;
  completionRate: number;
}

export interface BoardMemberAnalyticsResponse {
  boardMemberId: string;
  fullName: string;
  totalTasks: number;
  statusBreakdown: Record<string, number>;
}

export interface DepartmentTransferStat {
  departmentId: string;
  departmentName: string;
  transferCount: number;
}

export interface TransferRequestAnalyticsResponse {
  total: number;
  approvedRate: number;
  rejectedRate: number;
  avgShiftDays: number;
  topDepartments: DepartmentTransferStat[];
}

export interface RegistryResponse {
  upcoming: TaskListItemResponse[];
  overdue: TaskListItemResponse[];
}
