import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Kanban, CheckSquare, Users, Shield } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Sidebar() {
  const { user } = useAuth();

  return (
    <aside className="sidebar">
      <div className="brand-header">
        <div className="logo-icon">TF</div>
        <h2>TaskFlow</h2>
      </div>

      <nav className="nav-menu">
        <NavLink to="/dashboard" className={({ isActive }) => (isActive ? 'nav-item active' : 'nav-item')}>
          <LayoutDashboard size={18} />
          <span>Dashboard</span>
        </NavLink>

        <NavLink to="/boards/1" className={({ isActive }) => (isActive ? 'nav-item active' : 'nav-item')}>
          <Kanban size={18} />
          <span>Kanban Board</span>
        </NavLink>

        {user && user.systemRoleName === 'ADMIN' && (
          <NavLink to="/admin/users" className={({ isActive }) => (isActive ? 'nav-item active' : 'nav-item')}>
            <Users size={18} />
            <span>User Management</span>
          </NavLink>
        )}
      </nav>

      <div className="user-profile-widget">
        <div className="avatar">{user?.name ? user.name.charAt(0).toUpperCase() : 'U'}</div>
        <div className="user-info">
          <span className="user-name">{user?.name}</span>
          <span className={`role-badge role-${user?.systemRoleName?.toLowerCase()}`}>
            {user?.systemRoleName}
          </span>
        </div>
      </div>
    </aside>
  );
}