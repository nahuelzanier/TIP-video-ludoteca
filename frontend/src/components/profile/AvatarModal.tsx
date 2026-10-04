import { useEffect, useRef, useState } from "react";
import { MAX_AVATAR_BYTES } from "../../services/userService";
import "./AvatarModal.css";

interface AvatarModalProps {
  username: string;
  currentAvatarUrl: string | null;
  saving: boolean;
  removing: boolean;
  error: string;
  onSave: (file: File) => void;
  onRemove: () => void;
  onClose: () => void;
}

const ACCEPTED_TYPES = ["image/png", "image/jpeg"];

function AvatarModal({
  username,
  currentAvatarUrl,
  saving,
  removing,
  error,
  onSave,
  onRemove,
  onClose,
}: AvatarModalProps) {
  const [file, setFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [localError, setLocalError] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);
  const busy = saving || removing;

  const shownUrl = previewUrl ?? currentAvatarUrl;
  const initial = username.charAt(0).toUpperCase();

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !busy) {
        onClose();
      }
    };

    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [busy, onClose]);

  function selectFile(selected: File | undefined) {
    if (!selected) {
      return;
    }

    if (!ACCEPTED_TYPES.includes(selected.type)) {
      setLocalError("La foto tiene que ser un archivo PNG o JPEG.");
      return;
    }

    if (selected.size > MAX_AVATAR_BYTES) {
      setLocalError("La foto no puede superar los 2 MB.");
      return;
    }

    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }

    setLocalError("");
    setFile(selected);
    setPreviewUrl(URL.createObjectURL(selected));
  }

  return (
    <div
      className="modal-overlay"
      onClick={() => {
        if (!busy) {
          onClose();
        }
      }}
    >
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="avatar-modal-title"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="avatar-modal-title">Cambiar foto de perfil</h2>

        <div className="avatar-modal-preview">
          {shownUrl ? (
            <img src={shownUrl} alt="" />
          ) : (
            <span className="avatar-modal-initial">{initial}</span>
          )}
        </div>

        <label className="modal-label" htmlFor="avatar-input">
          Elegí una imagen
        </label>

        <input
          id="avatar-input"
          ref={inputRef}
          className="avatar-modal-input"
          type="file"
          accept="image/png,image/jpeg"
          disabled={busy}
          onChange={(event) => selectFile(event.target.files?.[0])}
        />

        <div className="modal-meta">
          <span>PNG o JPEG, hasta 2 MB. Se muestra como imagen cuadrada.</span>

          {file && <span className="avatar-modal-file">{file.name}</span>}
        </div>

        {(localError || error) && (
          <p className="modal-error" role="alert">
            {localError || error}
          </p>
        )}

        <div className="modal-actions">
          {currentAvatarUrl && (
            <button
              className="modal-remove"
              type="button"
              onClick={onRemove}
              disabled={busy}
            >
              {removing ? "Quitando..." : "Quitar foto"}
            </button>
          )}

          <button
            className="modal-cancel"
            type="button"
            onClick={onClose}
            disabled={busy}
          >
            Cancelar
          </button>

          <button
            className="modal-save"
            type="button"
            onClick={() => file && onSave(file)}
            disabled={busy || !file}
          >
            {saving ? "Guardando..." : "Guardar foto"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default AvatarModal;