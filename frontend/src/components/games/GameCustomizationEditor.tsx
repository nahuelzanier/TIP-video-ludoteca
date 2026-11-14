import { useState, type FormEvent } from "react";
import type { GameCustomization } from "../../types/Game";
import { useTranslation } from "../../services/i18n";
import { saveGameCustomization } from "../../services/gameCustomizationService";
import "./GameCustomizationEditor.css";

interface Props {
  gameId: string;
  customization: GameCustomization;
  onChange: (customization: GameCustomization) => void;
}

function GameCustomizationEditor({ gameId, customization, onChange }: Props) {
  const { t } = useTranslation();
  const [draft, setDraft] = useState(customization);
  const [open, setOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  function update<K extends keyof GameCustomization>(key: K, value: GameCustomization[K]) {
    const next = { ...draft, [key]: value };
    setDraft(next);
    onChange(next);
    setMessage("");
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setMessage("");
    try {
      const saved = await saveGameCustomization(gameId, draft);
      onChange(saved);
      setDraft(saved);
      setMessage(t("Game page appearance saved."));
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : t("Could not save the appearance."));
    } finally {
      setSaving(false);
    }
  }

  function reset() {
    setDraft(customization);
    onChange(customization);
    setMessage("");
    setError("");
  }

  return (
    <section className="game-customization">
      <button className="game-customization__toggle" type="button" onClick={() => setOpen((value) => !value)} aria-expanded={open}>
        {open ? t("Close appearance editor") : t("Customize this game page")}
      </button>
      {open && (
        <form className="game-customization__form" onSubmit={handleSubmit}>
          <p>{t("Changes preview immediately and are visible to everyone after saving.")}</p>
          <div className="game-customization__fields">
            <label>{t("Page background")}<input type="color" value={draft.backgroundColor} onChange={(event) => update("backgroundColor", event.target.value)} /></label>
            <label>{t("Accent color")}<input type="color" value={draft.accentColor} onChange={(event) => update("accentColor", event.target.value)} /></label>
            <label>{t("Text color")}<input type="color" value={draft.textColor} onChange={(event) => update("textColor", event.target.value)} /></label>
            <label>{t("Font")}
              <select value={draft.fontFamily} onChange={(event) => update("fontFamily", event.target.value as GameCustomization["fontFamily"])}>
                <option value="SYSTEM">{t("System default")}</option>
                <option value="ARIAL">Arial</option>
                <option value="VERDANA">Verdana</option>
                <option value="TREBUCHET">Trebuchet MS</option>
                <option value="GEORGIA">Georgia</option>
                <option value="COURIER">Courier New</option>
              </select>
            </label>
            <label>{t("Background style")}
              <select value={draft.backgroundStyle} onChange={(event) => update("backgroundStyle", event.target.value as GameCustomization["backgroundStyle"])}>
                <option value="SOLID">{t("Solid")}</option>
                <option value="GRADIENT">{t("Soft gradient")}</option>
              </select>
            </label>
          </div>
          {error && <p className="game-customization__feedback game-customization__feedback--error" role="alert">{error}</p>}
          {message && <p className="game-customization__feedback" role="status">{message}</p>}
          <div className="game-customization__actions">
            <button type="button" className="game-customization__reset" onClick={reset}>{t("Reset unsaved changes")}</button>
            <button type="submit" disabled={saving}>{saving ? t("Saving…") : t("Save appearance")}</button>
          </div>
        </form>
      )}
    </section>
  );
}

export default GameCustomizationEditor;
