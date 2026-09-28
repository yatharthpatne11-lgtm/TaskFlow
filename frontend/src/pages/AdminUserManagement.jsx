import React, { useState, useEffect } from 'react';
import { Shield, UserPlus, Mail, UserCheck, Lock } from 'lucide-react';
import apiClient from '../services/apiClient';

export default function AdminUserManagement() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    try {
      const res = await apiClient.get('/users');
      if (res.data.success) {
        setUsers(res.data.data);
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch user list.');
    } finally {
      setLoading(false);
    }
  };

  const handleRoleChange = async (userId, newRoleId) => {
    try {
      await apiClient.put('/users/role', { userId, roleId: newRoleId });
      loadUsers();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update user role.');
    }
  };

  if (loading) return <div className="loading-spinner">Loading System Users & Roles...</div>;

  return (
    <div className="admin-container">
      <div className="admin-header">
        <div>
          <h2>User Role & Authorization Control</h2>
          <p className="text-muted">Manage system-wide permissions and roles (ADMIN, MANAGER, MEMBER, VIEWER).</p>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="table-card">
        <table className="user-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>User Name</th>
              <th>Email Address</th>
              <th>Current Role</th>
              <th>Status</th>
              <th>Action / Change Role</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>#{u.id}</td>
                <td>
                  <div className="user-cell">
                    <span className="avatar-sm">{u.name.charAt(0)}</span>
                    <strong>{u.name}</strong>
                  </div>
                </td>
                <td>
                  <span className="email-text"><Mail size={14} /> {u.email}</span>
                </td>
                <td>
                  <span className={`role-badge role-${u.systemRoleName.toLowerCase()}`}>
                    {u.systemRoleName}
                  </span>
                </td>
                <td>
                  <span className="status-pill active">{u.status}</span>
                </td>
                <td>
                  <select
                    className="role-select"
                    value={u.systemRoleId}
                    onChange={(e) => handleRoleChange(u.id, parseInt(e.target.value))}
                  >
                    <option value={1}>ADMIN</option>
                    <option value={2}>MANAGER</option>
                    <option value={3}>MEMBER</option>
                    <option value={4}>VIEWER</option>
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}