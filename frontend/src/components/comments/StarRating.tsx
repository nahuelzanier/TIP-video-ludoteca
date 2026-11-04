import "./StarRating.css";

const STARS = [1, 2, 3, 4, 5] as const;

interface StarRatingProps {
  value: number;
  onChange?: (value: number) => void;
  label?: string;
}

function StarRating({ value, onChange, label = "Rating" }: StarRatingProps) {
  if (onChange === undefined) {
    return (
      <span
        className="star-rating star-rating--readonly"
        role="img"
        aria-label={`${value} out of 5 stars`}
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
          aria-label={`${star} ${star === 1 ? "star" : "stars"}`}
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
          aria-label="Clear rating"
        >
          Clear rating
        </button>
      )}
    </div>
  );
}

export default StarRating;
