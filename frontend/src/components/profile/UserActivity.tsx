import { Link } from "react-router-dom";
import type { ForumSummary } from "../../types/User";
import "./UserActivity.css";

const dateFormat = new Intl.DateTimeFormat("es-AR", { dateStyle: "medium" });

interface UserActivityProps {
    forums: ForumSummary[];
}

function UserActivity({ forums }: UserActivityProps) {
    return (
        <section className="user-activity">
            <h2>Mi actividad</h2>

            {forums.length === 0 ? (
                <p className="user-activity-empty">Todavía no creó ningún foro</p>
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
