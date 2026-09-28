import { useParams } from "react-router-dom";
import "./user.css";

function User() {
    const { id } = useParams<{ id: string }>();

    return (
        <main className="user-page">
            <h1>Perfil de usuario</h1>
            <p className="user-mock">
                Esta es una página mock. El perfil público del usuario {id} todavía no
                está implementado.
            </p>
        </main>
    );
}

export default User;
