import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

test("the production bundle describes the local Clover cash-rounding app", async () => {
  const serverBundle = await readFile(new URL("../dist/server/index.js", import.meta.url), "utf8");

  assert.match(serverBundle, /Cash totals, rounded to a nickel/);
  assert.match(serverBundle, /Round cash/);
  assert.match(serverBundle, /No donations/);
  assert.match(serverBundle, /No cloud service/);
  assert.match(serverBundle, /Cash only, by design/);
  assert.doesNotMatch(serverBundle, /Connect Clover|Create pairing code|Choose a campaign/i);
});
