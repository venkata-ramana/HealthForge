# Phase 8 enterprise integrations

Phase 8 adds governed enterprise-delivery surfaces around the review-first core of HealthForge.

## What is now supported

- governed GitHub and Jira tracker writeback with approval linkage
- Slack and Teams collaboration notification packaging for review-ready, approval-needed, and workflow-handoff events
- Confluence, SharePoint, and Notion-style documentation export packaging
- organization-scoped workflow events, webhook subscriptions, and observable webhook delivery history
- environment promotion and rollback guidance for demo, staging, and private deployment paths

## Safety model

- preview remains the default posture for integrations
- explicit approval acknowledgement is required before packaging notifications, documentation exports, or tracker payloads
- direct send/publish behavior is represented through auditable local stubs in this phase
- connector target labels, credentials, and environment enablement remain deployment concerns rather than repo concerns

## New API surfaces

- `POST /v1/collaboration/notifications`
- `POST /v1/documentation-exports`
- `POST /v1/automation/webhook-subscriptions`
- `POST /v1/automation/events`
- `GET /v1/automation/status`
- `GET /v1/enterprise/deployment-promotion-guide`

## Why this matters

These features let teams show how approved HealthForge outputs can move into enterprise delivery systems without skipping human review, traceability, or environment controls.
