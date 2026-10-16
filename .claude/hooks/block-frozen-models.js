const { readFileSync } = require("node:fs");

const toolCall = JSON.parse(readFileSync(0, "utf8"));
const command = toolCall.tool_input.command ?? "";

const readOnlyCommands = new Set(["show", "find", "metrics", "render", "validate", "guide", "kinds", "--help", "--version"]);
const bpmnCliCalls = [...command.matchAll(/(?:^|[\s;&|(])(?:bpmn|@miragon\/bpmn-cli(?:@\S+)?)\s+(\S+)/g)];
const changesAModel = bpmnCliCalls.some(([, subcommand]) => !readOnlyCommands.has(subcommand));
const namesTheFrozenModels = command.includes("use-cases/bike-leasing/original/");

if (changesAModel && namesTheFrozenModels) {
  console.error("Blocked: the models in docs/use-cases/bike-leasing/original/ are frozen. Read them with bpmn show, find or render; change only docs/use-cases/bike-leasing/process/.");
  process.exit(2);
}
