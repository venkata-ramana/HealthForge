# Tracked Export Integrations

The Phase 4 tracked export integration layer maps approved work items into GitHub- and Jira-ready preview payloads.

## Purpose

Provide a safer bridge between approved HealthForge artifacts and downstream engineering tools.

## Current behavior

The first version supports:

- preview-only export mode
- GitHub-ready payload shape
- Jira-ready payload shape
- audit-event capture of export preview actions

## Safety model

- only approved Brief work items can be previewed
- explicit acknowledgement is required
- administrator role is required
- direct writeback is rejected in this phase
- provider credentials remain out of source control

## Non-goals

- automatic ticket creation
- background synchronization
- hidden side effects
- storing external provider secrets in the repo
