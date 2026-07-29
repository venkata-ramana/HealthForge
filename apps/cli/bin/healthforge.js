#!/usr/bin/env node

const { HealthForgeClient } = require("../../../packages/sdk-js/src");

async function main() {
  const [, , command = "help", ...args] = process.argv;
  const options = parseArgs(args);
  const client = new HealthForgeClient({
    baseUrl: options.baseUrl || process.env.HEALTHFORGE_API_BASE_URL || "http://localhost:8080",
    actorId: options.actorId || process.env.HEALTHFORGE_ACTOR_ID || "cli.reviewer",
    actorRole: options.actorRole || process.env.HEALTHFORGE_ACTOR_ROLE || "reviewer",
    organizationId: options.organizationId || process.env.HEALTHFORGE_ORG_ID || "tenant.alpha"
  });

  switch (command) {
    case "help":
      return printHelp();
    case "brief:list":
      return print(await client.listBriefs());
    case "brief:create":
      return print(await client.createBrief({
        corpus_id: options.corpusId || "mvp-regulatory-corpus",
        corpus_version: options.corpusVersion || "2026-07-24-expanded-web-core-v4",
        question: required(options.question, "--question is required"),
        project_context: required(options.context, "--context is required")
      }));
    case "workspace:overview":
      return print(await client.getWorkspaceOverview());
    case "developer:overview":
      return print(await client.getDeveloperOverview());
    case "repo:guide":
      return print(await client.getRepoGuidance({
        briefId: required(options.briefId, "--brief-id is required"),
        repositoryName: options.repoName || "healthforge-local-repo",
        workspaceRoot: options.workspaceRoot || process.cwd(),
        repositoryInventory: await loadInventory(options),
        changedFiles: listOption(options.changedFile)
      }));
    case "labs:overview":
      return print(await client.getSyntheticLabsOverview());
    case "labs:run":
      return print(await client.runSyntheticLab(required(args[0], "template id is required")));
    default:
      console.error(`Unknown command: ${command}`);
      printHelp();
      process.exitCode = 1;
  }
}

function printHelp() {
  console.log(`
HealthForge CLI

Commands:
  help
  brief:list
  brief:create --question "..." --context "..."
  workspace:overview
  developer:overview
  repo:guide --brief-id brief_x --repo-name HealthForge --workspace-root /path
  labs:overview
  labs:run <templateId>

Common options:
  --base-url http://localhost:8080
  --actor-id cli.reviewer
  --actor-role reviewer|approver|administrator|auditor
  --organization-id tenant.alpha
  --inventory-file /path/to/files.txt
  --changed-file apps/platform-api/src/main/resources/static/app.js
`);
}

function parseArgs(args) {
  const options = {};
  for (let index = 0; index < args.length; index += 1) {
    const token = args[index];
    if (!token.startsWith("--")) {
      continue;
    }
    const key = camelCase(token.slice(2));
    const next = args[index + 1];
    if (!next || next.startsWith("--")) {
      options[key] = true;
      continue;
    }
    if (options[key] === undefined) {
      options[key] = next;
    } else if (Array.isArray(options[key])) {
      options[key].push(next);
    } else {
      options[key] = [options[key], next];
    }
    index += 1;
  }
  return options;
}

function camelCase(value) {
  return value.replace(/-([a-z])/g, (_, letter) => letter.toUpperCase());
}

function required(value, message) {
  if (value === undefined || value === null || value === "") {
    throw new Error(message);
  }
  return value;
}

function listOption(value) {
  if (!value) {
    return [];
  }
  return Array.isArray(value) ? value : [value];
}

async function loadInventory(options) {
  if (!options.inventoryFile) {
    return [];
  }
  const fs = require("node:fs/promises");
  const contents = await fs.readFile(options.inventoryFile, "utf8");
  return contents
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(Boolean);
}

function print(payload) {
  console.log(JSON.stringify(payload, null, 2));
}

main().catch(error => {
  console.error(error.message);
  process.exitCode = 1;
});
