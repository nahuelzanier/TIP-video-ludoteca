import { BrowserRouter, Routes, Route } from "react-router-dom";
import Navbar from "./components/Navbar/Navbar";
import Home from "./pages/Home";
import Game from "./pages/Game";
import Perfil from "./pages/Perfil/Perfil";
import Configuracion from "./pages/Configuracion/Configuracion";
import Foro from "./pages/Foro/Foro";
import JuegoAleatorio from "./pages/JuegoAleatorio/JuegoAleatorio";
import CerrarSesion from "./pages/CerrarSesion/CerrarSesion";

function App() {
    return (
        <BrowserRouter>
            <Navbar />
            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/game/:id" element={<Game />} />
                <Route path="/perfil" element={<Perfil />} />
                <Route path="/configuracion" element={<Configuracion />} />
                <Route path="/foro" element={<Foro />} />
                <Route path="/juego-aleatorio" element={<JuegoAleatorio />} />
                <Route path="/logout" element={<CerrarSesion />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;