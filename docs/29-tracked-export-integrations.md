# Tracked Export Integrations

The tracked export integration layer maps approved work items into GitHub- and Jira-ready payloads and now supports governed writeback modes in addition to preview.

## Purpose

Provide a safer bridge between approved HealthForge artifacts and downstream engineering tools.

This integration layer now sits beside broader Phase 8 enterprise-delivery surfaces for collaboration notifications, documentation publishing packages, and webhook automation.

## Current behavior

The current version supports:

- preview-only export mode
- governed writeback mode with explicit approval record linkage
- GitHub-ready payload shape
- Jira-ready payload shape
- audit-event capture of preview, blocked writeback, executed writeback, and retry actions

## Safety model

- only approved Brief work items can be previewed
- explicit acknowledgement is required
- approver or administrator role is required
- governed writeback requires a valid Brief approval record
- retries preserve prior event linkage and retry count metadata
- provider credentials remain out of source control

## Non-goals

- background synchronization
- hidden side effects
- storing external provider secrets in the repo

## Current boundary

Preview remains the safer default.

Writeback is now governed rather than open-ended:

- operator intent is explicit
- approval linkage is explicit
- execution status is visible
- retry metadata is preserved
- retention and audit telemetry remain organization scoped
