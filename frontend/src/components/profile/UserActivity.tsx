import type { ForumDiscussion } from "../../services/forumService";
import ForumDiscussionCard from "../forum/ForumDiscussionCard";
import "./UserActivity.css";

interface UserActivityProps {
    forums: ForumDiscussion[];
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
                <div className="user-activity-list">
                    {forums.map((forum) => (
                        <ForumDiscussionCard key={forum.id} discussion={forum} />
                    ))}
                </div>
            )}
        </section>
    );
}

export default UserActivity;
