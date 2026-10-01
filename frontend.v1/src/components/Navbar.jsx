import React from 'react';
import { LogOut } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();

  return (
    <header className="top-navbar">
      <div className="navbar-left">
        <span className="workspace-tag">Workspace: Main College Workspace</span>
      </div>

      <div className="navbar-right">

        <button onClick={logout} className="btn-logout" title="Sign Out">
          <LogOut size={16} /> Logout
        </button>
      </div>
    </header>
  );
}