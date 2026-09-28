import { BrowserRouter, Routes, Route, Outlet } from "react-router-dom";
import Navbar from "./components/Navbar/Navbar";

import Home from "./pages/Home";
import Game from "./pages/Game";
import AuthPage from "./pages/AuthPage";
import Forum from "./pages/forum/forum";
import Profile from "./pages/profile/profile";
import RandomGame from "./pages/randomgame/randomgame";
import Settings from "./pages/settings/settings";
import Search from "./pages/search/search";
import User from "./pages/user/user";

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
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;