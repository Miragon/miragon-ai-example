const { readFileSync } = require("node:fs");

const toolCall = JSON.parse(readFileSync(0, "utf8"));
const { file_path, path, command } = toolCall.tool_input;
const target = [file_path, path, command].join(" ").replaceAll(".env.example", "");

const touchesEnvFile = /\.env\b/.test(target);

if (touchesEnvFile) {
  console.error("Blocked: .env files hold local secrets. Read .env.example for the variable names, or ask the user for the value.");
  process.exit(2);
}
