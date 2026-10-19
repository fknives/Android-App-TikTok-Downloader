import assert from "node:assert/strict";
import test from "node:test";

// frontend.js sets up the page on import, so stub just enough DOM to reach normalizeTikTokUrl().
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

const { normalizeTikTokUrl } = await import("../public/frontend.js");

test("empty input", () => {
  assert.equal(normalizeTikTokUrl(""), "");
  assert.equal(normalizeTikTokUrl("   "), "");
  assert.equal(normalizeTikTokUrl(null as any), "");
  assert.equal(normalizeTikTokUrl(undefined as any), "");
});

test("adds https when the scheme is missing", () => {
  assert.equal(
    normalizeTikTokUrl("www.tiktok.com/@user/video/123"),
    "https://www.tiktok.com/@user/video/123",
  );
});

test("trims surrounding whitespace", () => {
  assert.equal(
    normalizeTikTokUrl("  vm.tiktok.com/ZN8SyMfnP/  "),
    "https://vm.tiktok.com/ZN8SyMfnP/",
  );
});

test("keeps an existing http or https scheme", () => {
  assert.equal(
    normalizeTikTokUrl("http://vm.tiktok.com/ZN8SyMfnP/"),
    "http://vm.tiktok.com/ZN8SyMfnP/",
  );
  assert.equal(
    normalizeTikTokUrl("https://www.tiktok.com/@user/video/123"),
    "https://www.tiktok.com/@user/video/123",
  );
});

test("accepts tiktok.com and its subdomains", () => {
  assert.equal(
    normalizeTikTokUrl("https://tiktok.com/x"),
    "https://tiktok.com/x",
  );
  assert.equal(
    normalizeTikTokUrl("https://a.b.tiktok.com/x"),
    "https://a.b.tiktok.com/x",
  );
});

test("host matching is case-insensitive", () => {
  assert.equal(
    normalizeTikTokUrl("HTTPS://WWW.TIKTOK.COM/@user/video/123"),
    "HTTPS://WWW.TIKTOK.COM/@user/video/123",
  );
});

test("rejects other hosts", () => {
  assert.equal(normalizeTikTokUrl("https://evil.com/x"), "");
  assert.equal(normalizeTikTokUrl("https://tiktok.com.evil.com/x"), "");
  assert.equal(normalizeTikTokUrl("https://eviltiktok.com/x"), "");
});

test("rejects userinfo and explicit ports", () => {
  assert.equal(normalizeTikTokUrl("https://tiktok.com@evil.com/x"), "");
  assert.equal(normalizeTikTokUrl("https://www.tiktok.com:443/x"), "");
});

test("rejects non-http schemes", () => {
  assert.equal(normalizeTikTokUrl("ftp://www.tiktok.com/x"), "");
});

test("rejects a scheme-relative URL", () => {
  assert.equal(normalizeTikTokUrl("//www.tiktok.com/x"), "");
});
