export type Role = 'OWNER' | 'ADMIN' | 'OPERATOR' | 'APPROVER' | 'VIEWER';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  organizationId: string;
  status: string;
  createdAt: string;
}

export interface Organization {
  id: string;
  name: string;
  slug: string;
  defaultCurrency: string;
  timezone: string;
  status: string;
  createdAt: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInMs: number;
  user: User;
  organization: Organization;
}

export interface Supplier {
  id: string;
  organizationId: string;
  supplierCode: string;
  name: string;
  taxIdentifier?: string;
  email?: string;
  address?: string;
  defaultCurrency: string;
  status: string;
  createdAt: string;
}

export interface PurchaseOrderLine {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  taxRate: number;
  lineTotal: number;
}

export interface PurchaseOrder {
  id: string;
  organizationId: string;
  supplierId?: string;
  purchaseOrderNumber: string;
  issueDate: string;
  currency: string;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  status: string;
  createdAt: string;
  lines?: PurchaseOrderLine[];
}

export type ProcessingStatus =
  | 'RECEIVED'
  | 'EXTRACTION_PENDING'
  | 'EXTRACTED'
  | 'VALIDATING'
  | 'NEEDS_REVIEW'
  | 'READY_FOR_APPROVAL'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'EXPORT_PENDING'
  | 'EXPORTED'
  | 'FAILED';

export type ValidationStatus = 'PENDING' | 'PASSED' | 'FAILED' | 'WARNING';
export type ApprovalStatus = 'NOT_REQUIRED' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'CHANGES_REQUESTED';

export interface InvoiceLine {
  id: string;
  description: string;
  quantity: number;
  unitPrice: number;
  taxRate: number;
  lineTotal: number;
}

export interface RuleEvaluation {
  ruleName: string;
  passed: boolean;
  severity: string;
  message: string;
  field?: string;
}

export interface ValidationResult {
  overallStatus: ValidationStatus;
  rulesEvaluated: number;
  passedRules: number;
  failedRules: number;
  warningsCount: number;
  evaluations: RuleEvaluation[];
  reviewReasons: string[];
}

export interface PoMatchResult {
  outcome: 'MATCHED' | 'PARTIALLY_MATCHED' | 'MISMATCHED' | 'NO_PURCHASE_ORDER' | 'NEEDS_REVIEW';
  purchaseOrderNumber?: string;
  supplierMatches: boolean;
  currencyMatches: boolean;
  invoiceTotal?: number;
  poTotal?: number;
  amountDifference?: number;
  message?: string;
}

export interface Invoice {
  id: string;
  organizationId: string;
  supplierId?: string;
  supplierName?: string;
  purchaseOrderId?: string;
  purchaseOrderNumber?: string;
  invoiceNumber: string;
  invoiceDate?: string;
  dueDate?: string;
  currency: string;
  subtotal: number;
  taxAmount: number;
  totalAmount: number;
  validationStatus: ValidationStatus;
  approvalStatus: ApprovalStatus;
  processingStatus: ProcessingStatus;
  documentId?: string;
  workflowExecutionId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface InvoiceDetail extends Invoice {
  lines: InvoiceLine[];
  supplier?: Supplier;
  validationResult?: ValidationResult;
  poMatchResult?: PoMatchResult;
  extractedData?: string;
  originalFilename?: string;
  fileSize?: number;
}

export type NodeType =
  | 'START'
  | 'AI_EXTRACTION'
  | 'VALIDATE_INVOICE'
  | 'MATCH_PURCHASE_ORDER'
  | 'CONDITION'
  | 'APPROVAL'
  | 'NOTIFICATION'
  | 'EXPORT_INVOICE'
  | 'MANUAL_REVIEW'
  | 'END';

export interface WorkflowNode {
  id: string;
  type: NodeType;
  label: string;
  config?: Record<string, unknown>;
  x?: number;
  y?: number;
}

export interface WorkflowEdge {
  id: string;
  source: string;
  target: string;
  conditionBranch?: string;
}

export interface WorkflowGraph {
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
}

export interface WorkflowDefinition {
  id: string;
  organizationId: string;
  name: string;
  description?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkflowVersion {
  id: string;
  workflowDefinitionId: string;
  versionNumber: number;
  graphDefinitionJson: string;
  validationResultJson?: string;
  publishedAt: string;
  createdAt: string;
}

export interface WorkflowExecution {
  id: string;
  organizationId: string;
  workflowDefinitionId: string;
  workflowVersionId: string;
  businessReferenceType: string;
  businessReferenceId: string;
  status: 'QUEUED' | 'RUNNING' | 'WAITING_APPROVAL' | 'WAITING_RETRY' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  triggerType: string;
  startedAt?: string;
  completedAt?: string;
  errorCode?: string;
  errorMessage?: string;
  createdAt: string;
}

export interface WorkflowNodeExecution {
  id: string;
  workflowExecutionId: string;
  nodeId: string;
  nodeType: string;
  status: 'PENDING' | 'RUNNING' | 'WAITING_APPROVAL' | 'WAITING_RETRY' | 'COMPLETED' | 'FAILED' | 'SKIPPED';
  attemptNumber: number;
  outputSnapshot?: string;
  errorDetails?: string;
  startedAt?: string;
  completedAt?: string;
  durationMs?: number;
  createdAt: string;
}

export interface ApprovalTask {
  id: string;
  organizationId: string;
  workflowExecutionId?: string;
  invoiceId: string;
  assignedUserId?: string;
  assignedRole: string;
  status: 'PENDING' | 'DECIDED' | 'CANCELLED';
  decisionDeadline?: string;
  createdAt: string;
  invoice?: Invoice;
}

export interface ExportRecord {
  id: string;
  organizationId: string;
  invoiceId: string;
  exportFormat: string;
  destination: string;
  idempotencyKey?: string;
  status: string;
  payloadSummary?: string;
  errorMessage?: string;
  exportedAt?: string;
  createdAt: string;
}

export interface AuditEvent {
  id: string;
  organizationId: string;
  actorUserId?: string;
  actorType: string;
  action: string;
  entityType: string;
  entityId: string;
  beforeState?: string;
  afterState?: string;
  metadata?: string;
  correlationId?: string;
  createdAt: string;
}

export interface OverviewAnalytics {
  totalInvoices: number;
  invoicesPendingReview: number;
  pendingApprovals: number;
  approvedInvoices: number;
  rejectedInvoices: number;
  exportedInvoices: number;
  workflowSuccessRate: number;
  avgProcessingTimeSec: number;
  failedExecutions: number;
  totalAiTokens: number;
  estimatedAiCost: number;
  avgAiLatencyMs: number;
}

export interface AppNotification {
  id: string;
  organizationId: string;
  userId?: string;
  title: string;
  message: string;
  type: string;
  read: boolean;
  linkUrl?: string;
  createdAt: string;
}

export interface OrganizationSettings {
  id: string;
  organizationId: string;
  approvalThresholdAmount: number;
  matchingToleranceAmount: number;
  duplicateWindowDays: number;
  autoExportApproved: boolean;
  updatedAt: string;
}

export interface IntegrationConfig {
  id: string;
  organizationId: string;
  name: string;
  integrationType: string;
  endpointUrl?: string;
  authType: string;
  apiKeyMasked?: string;
  status: string;
  lastTestedAt?: string;
  lastTestResult?: string;
  createdAt: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
