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
                            <Link to={`/forum/${forum.id}`}>{forum.title}</Link>

                            <time dateTime={forum.createdAt}>
                                {dateFormat.format(new Date(forum.createdAt))}
                            </time>
                        </li>
                    ))}
                </ul>
            )}
        </section>
    );
}

export default UserActivity;
