import { Link } from "react-router-dom";
import type { ForumSummary } from "../../types/User";
import "./UserActivity.css";

const dateFormat = new Intl.DateTimeFormat("en-US", { dateStyle: "medium" });

interface UserActivityProps {
    forums: ForumSummary[];
    loading?: boolean;
    error?: string;
}

function UserActivity({ forums, loading = false, error = "" }: UserActivityProps) {
    return (
        <section className="user-activity">
            <h2>Activity</h2>

            {loading ? (
                <p className="user-activity-empty" role="status">Loading discussions...</p>
            ) : error ? (
                <p className="user-activity-empty" role="alert">{error}</p>
            ) : forums.length === 0 ? (
                <p className="user-activity-empty">No discussions started yet.</p>
            ) : (
                <ul className="user-activity-list">
                    {forums.map((forum) => (
                        <li className="user-activity-item" key={forum.id}>
                            <Link className="user-activity-cover-link" to={`/forum/${forum.id}`}>
                                {forum.coverImageUrl ? (
                                    <>
                                        <img
                                            className="user-activity-cover"
                                            src={forum.coverImageUrl}
                                            alt=""
                                            loading="lazy"
                                            onError={(event) => {
                                                event.currentTarget.hidden = true;
                                                event.currentTarget.nextElementSibling?.removeAttribute("hidden");
                                            }}
                                        />
                                        <span
                                            className="user-activity-cover-fallback"
                                            hidden
                                            aria-hidden="true"
                                        >
                                            No cover image
                                        </span>
                                    </>
                                ) : (
                                    <span className="user-activity-cover-fallback" aria-hidden="true">
                                        No cover image
                                    </span>
                                )}
                            </Link>
                            <div className="user-activity-item-body">
                                <Link to={`/forum/${forum.id}`}>{forum.title}</Link>
                                <time dateTime={forum.createdAt}>
                                    {dateFormat.format(new Date(forum.createdAt))}
                                </time>
                            </div>
                        </li>
                    ))}
                </ul>
            )}
        </section>
    );
}

export default UserActivity;
