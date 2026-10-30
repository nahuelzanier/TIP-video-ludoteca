import "./StarRating.css";

const STARS = [1, 2, 3, 4, 5] as const;

interface StarRatingProps {
  value: number;
  onChange?: (value: number) => void;
  label?: string;
}

function StarRating({ value, onChange, label = "Valoración" }: StarRatingProps) {
  if (onChange === undefined) {
    return (
      <span
        className="star-rating star-rating--readonly"
        role="img"
        aria-label={`${value} de 5 estrellas`}
      >
        <span aria-hidden="true">
          {"★".repeat(value)}
          {"☆".repeat(5 - value)}
        </span>
      </span>
    );
  }

  return (
    <div className="star-rating" role="group" aria-label={label}>
      {STARS.map((star) => (
        <button
          key={star}
          type="button"
          className="star-rating__star"
          onClick={() => onChange(star === value ? 0 : star)}
          aria-label={`${star} ${star === 1 ? "estrella" : "estrellas"}`}
          aria-pressed={star <= value}
        >
          <span aria-hidden="true">{star <= value ? "★" : "☆"}</span>
        </button>
      ))}

      {value > 0 && (
        <button
          type="button"
          className="star-rating__clear"
          onClick={() => onChange(0)}
          aria-label="Quitar valoración"
        >
          Quitar valoración
        </button>
      )}
    </div>
  );
}

export default StarRating;