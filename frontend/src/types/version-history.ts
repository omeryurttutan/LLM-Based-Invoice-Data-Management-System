import { InvoiceData } from './invoice';

export enum ChangeSource {
    MANUAL_EDIT = 'MANUAL_EDIT',
    LLM_EXTRACTION = 'LLM_EXTRACTION',
    LLM_RE_EXTRACTION = 'LLM_RE_EXTRACTION',
    VERIFICATION = 'VERIFICATION',
    STATUS_CHANGE = 'STATUS_CHANGE',
    REVERT = 'REVERT',
    BULK_UPDATE = 'BULK_UPDATE'
}

export enum ChangeType {
    ADDED = 'ADDED',
    REMOVED = 'REMOVED',
    MODIFIED = 'MODIFIED'
}

export interface InvoiceVersionSummary {
    id: string;
    versionNumber: number;
    changeSource: ChangeSource;
    changeSummary: string;
    changedFields: string[];
    changedBy: string; // Name or email
    createdAt: string;
}

export interface InvoiceVersionDetail {
    id: string;
    versionNumber: number;
    snapshotData: InvoiceData; // Reusing InvoiceData from invoice.ts
    changeSource: ChangeSource;
    changeSummary: string;
    changedBy: string;
    createdAt: string;
}

export interface FieldChange {
    fieldName: string;
    fieldLabel: string; // Turkish label from backend
    oldValue: any;
    newValue: any;
    changeType: ChangeType;
}

export interface ItemChange {
    itemId?: string;
    changeType: ChangeType;
    // For modified/added items, we might want to show relevant fields
    description?: string;
    totalAmount?: number;
    // If it's a modification, we might get field-level diffs for the item
    changes?: FieldChange[];
}

export interface VersionDiff {
    fromVersion: number;
    toVersion: number;
    changes: FieldChange[];
    itemChanges: ItemChange[];
}
