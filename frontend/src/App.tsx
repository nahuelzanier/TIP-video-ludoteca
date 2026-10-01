import { BrowserRouter, Routes, Route, Outlet } from "react-router-dom";
import Navbar from "./components/navbar/Navbar";

import Home from "./pages/Home";
import Game from "./pages/Game";
import AuthPage from "./pages/AuthPage";
import Forum from "./pages/forum/Forum";
import Profile from "./pages/profile/Profile";
import RandomGame from "./pages/randomgame/RandomGame";
import Settings from "./pages/settings/Settings";
import UploadGame from "./pages/uploadgame/UploadGame";
import Search from "./pages/search/Search";
import User from "./pages/user/User";

function MainLayout() {
  return (
    <>
      <Navbar />
      <Outlet />
    </>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<MainLayout />}>
          <Route path="/" element={<Home />} />
          <Route path="/game/:id" element={<Game />} />
          <Route path="/login" element={<AuthPage mode="login" />} />
          <Route path="/register" element={<AuthPage mode="register" />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/settings" element={<Settings />} />
          <Route path="/randomgame" element={<RandomGame />} />
          <Route path="/forum" element={<Forum />} />
          <Route path="/search" element={<Search />} />
          <Route path="/user/:username" element={<User />} />
          <Route path="/games/upload" element={<UploadGame />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;