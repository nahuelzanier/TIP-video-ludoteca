import { afterEach, describe, expect, it, vi } from "vitest";

const harness = vi.hoisted(() => ({
  cleanups: [] as Array<() => void>,
  notifications: 0,
}));

vi.mock("react", async (importOriginal) => {
  const actual = await importOriginal<typeof import("react")>();
  return {
    ...actual,
    useSyncExternalStore: (subscribe: (listener: () => void) => () => void,
      getSnapshot: () => string) => {
      harness.cleanups.push(subscribe(() => { harness.notifications += 1; }));
      return getSnapshot();
    },
  };
});

import { setAppLanguage, useTranslation } from "./i18n";

afterEach(() => {
  setAppLanguage("en");
  harness.cleanups.splice(0).forEach((cleanup) => cleanup());
  harness.notifications = 0;
});

describe("i18n", () => {
  it("returns English text and falls back for untranslated strings", () => {
    const { language, t } = useTranslation();

    expect(language).toBe("en");
    expect(t("Settings")).toBe("Settings");
    expect(t("Untranslated label")).toBe("Untranslated label");
  });

  it("notifies subscribers and translates after changing language", () => {
    useTranslation();
    setAppLanguage("es");
    const { language, t } = useTranslation();

    expect(harness.notifications).toBe(1);
    expect(language).toBe("es");
    expect(t("Settings")).toBe("Configuración");

    setAppLanguage("es");
    expect(harness.notifications).toBe(1);
  });
});
