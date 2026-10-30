import { useEffect, useRef, useState } from "react";
import "./EditDescriptionModal.css";

interface EditDescriptionModalProps {
    initialDescription: string;
    maxLength: number;
    saving: boolean;
    error: string;
    onSave: (description: string) => void;
    onClose: () => void;
}

function EditDescriptionModal({
    initialDescription,
    maxLength,
    saving,
    error,
    onSave,
    onClose,
}: EditDescriptionModalProps) {
    const [description, setDescription] = useState(initialDescription);
    const textareaRef = useRef<HTMLTextAreaElement>(null);

    const isOverLimit = description.length > maxLength;

    useEffect(() => {
        textareaRef.current?.focus();
    }, []);

    useEffect(() => {
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape" && !saving) {
                onClose();
            }
        };

        document.addEventListener("keydown", handleKeyDown);

        return () => {
            document.removeEventListener("keydown", handleKeyDown);
        };
    }, [onClose, saving]);

    return (
        <div
            className="modal-overlay"
            onClick={() => {
                if (!saving) {
                    onClose();
                }
            }}
        >
            <div
                className="modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="edit-description-title"
                onClick={(event) => event.stopPropagation()}
            >
                <h2 id="edit-description-title">Editar descripción</h2>

                <label className="modal-label" htmlFor="description-input">
                    Contá algo sobre vos
                </label>

                <textarea
                    id="description-input"
                    ref={textareaRef}
                    className={`modal-textarea${
                        isOverLimit ? " modal-textarea--invalid" : ""
                    }`}
                    value={description}
                    rows={5}
                    aria-describedby="description-counter"
                    aria-invalid={isOverLimit}
                    onChange={(event) => setDescription(event.target.value)}
                />

                <div className="modal-meta">
                    <span>
                        {isOverLimit
                            ? `Te pasaste por ${description.length - maxLength} caracteres.`
                            : "Podés dejar la descripción vacía si no querés escribir nada."}
                    </span>

                    <span
                        id="description-counter"
                        className={`modal-counter${
                            isOverLimit ? " modal-counter--over" : ""
                        }`}
                        aria-live="polite"
                    >
                        {description.length}/{maxLength}
                    </span>
                </div>

                {error && (
                    <p className="modal-error" role="alert">
                        {error}
                    </p>
                )}

                <div className="modal-actions">
                    <button
                        className="modal-cancel"
                        type="button"
                        onClick={onClose}
                        disabled={saving}
                    >
                        Cancelar
                    </button>

                    <button
                        className="modal-save"
                        type="button"
                        onClick={() => onSave(description)}
                        disabled={saving || isOverLimit}
                    >
                        {saving ? "Guardando..." : "Guardar"}
                    </button>
                </div>
            </div>
        </div>
    );
}

export default EditDescriptionModal;
