import { useEffect } from "react";
import { BrowserRouter, Routes, Route, Outlet } from "react-router-dom";
import Navbar from "./components/navbar/Navbar";
import { getCurrentUser } from "./services/authService";
import { applyUserSettings, getUserSettings } from "./services/settingsService";

import Home from "./pages/Home";
import Game from "./pages/Game";
import AuthPage from "./pages/AuthPage";
import Forum from "./pages/forum/Forum";
import Profile from "./pages/profile/Profile";
import Settings from "./pages/settings/Settings";
import UploadGame from "./pages/uploadgame/UploadGame";
import Search from "./pages/search/Search";
import User from "./pages/user/User";
import Favorites from "./pages/favorites/Favorites";
import ForumDiscussionPage from "./pages/forum/ForumDiscussionPage";
import ForumNewDiscussion from "./pages/forum/ForumNewDiscussion";
import AdminReview from "./pages/admin/AdminReview";
import AdminSetup from "./pages/admin/AdminSetup";

function MainLayout() {
  useEffect(() => {
    let isCurrentRequest = true;
    const loadPreferences = () => {
      getCurrentUser()
        .then(async (user) => {
          if (!user) {
            document.documentElement.dataset.theme = "light";
            document.documentElement.lang = "en";
            return;
          }
          const settings = await getUserSettings();
          if (isCurrentRequest) applyUserSettings(settings);
        })
        .catch(() => undefined);
    };
    loadPreferences();
    window.addEventListener("ludarium:auth-changed", loadPreferences);

    return () => {
      isCurrentRequest = false;
      window.removeEventListener("ludarium:auth-changed", loadPreferences);
    };
  }, []);

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
          <Route path="/forum" element={<Forum />} />
          <Route path="/favorites" element={<Favorites />} />
          <Route path="/search" element={<Search />} />
          <Route path="/user/:username" element={<User />} />
          <Route path="/games/upload" element={<UploadGame />} />
          <Route path="/admin/review" element={<AdminReview />} />
          <Route path="/admin/setup" element={<AdminSetup />} />
          <Route path="/forum/:discussionId" element={<ForumDiscussionPage />} />
          <Route path="/forum/new" element={<ForumNewDiscussion />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
