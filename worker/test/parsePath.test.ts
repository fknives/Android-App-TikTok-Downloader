import assert from "node:assert/strict";
import test from "node:test";

// frontend.js sets up the page on import, so stub just enough DOM to reach parsePath().
const stubElement = () => ({
  addEventListener() {},
  dataset: {},
  value: "",
  disabled: false,
});

(globalThis as any).document = {
  getElementById: stubElement,
  querySelector: stubElement,
};
(globalThis as any).location = { pathname: "/" };

const { parsePath } = await import("../public/frontend.js");

test("empty path", () => {
  assert.deepEqual(parsePath("/"), { value: "", plausible: false });
});

test("bare short code", () => {
  assert.deepEqual(parsePath("/ZN8SyMfnP"), {
    value: "https://vm.tiktok.com/ZN8SyMfnP/",
    plausible: true,
  });
});

test("video path", () => {
  assert.deepEqual(parsePath("/@user/video/123"), {
    value: "https://www.tiktok.com/@user/video/123",
    plausible: true,
  });
});

test("full URL with scheme", () => {
  assert.deepEqual(parsePath("/https://www.tiktok.com/@user/video/123"), {
    value: "https://www.tiktok.com/@user/video/123",
    plausible: true,
  });
});

test("scheme collapsed by Cloudflare", () => {
  assert.deepEqual(parsePath("/https:/www.tiktok.com/@user/video/123"), {
    value: "https://www.tiktok.com/@user/video/123",
    plausible: true,
  });
});

test("percent-encoded URL", () => {
  assert.deepEqual(
    parsePath("/https%3A%2F%2Fwww.tiktok.com%2F%40user%2Fvideo%2F123"),
    {
      value: "https://www.tiktok.com/@user/video/123",
      plausible: true,
    },
  );
});

test("non-TikTok host", () => {
  assert.deepEqual(parsePath("/https:/evil.com/x"), {
    value: "evil.com/x",
    plausible: false,
  });
});
