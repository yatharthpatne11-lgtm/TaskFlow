import React, { useState, useEffect, useCallback } from 'react';
import { UserPlus, Mail, X, RefreshCw } from 'lucide-react';
import apiClient from '../services/apiClient';
import { useAuth } from '../context/AuthContext';

const ROLES = [
  { id: 1, name: 'ADMIN' },
  { id: 2, name: 'MANAGER' },
  { id: 3, name: 'MEMBER' },
  { id: 4, name: 'VIEWER' },
];
const STATUSES = ['ACTIVE', 'INACTIVE', 'SUSPENDED'];

const errorText = (err, fallback) => {
  const status = err.response?.status;
  const msg = err.response?.data?.message || err.message || fallback;
  return status ? `${msg} (HTTP ${status})` : msg;
};

export default function AdminUserManagement() {
  const { user: me } = useAuth();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busyId, setBusyId] = useState(null);

  // "Add user" form
  const [showAdd, setShowAdd] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');
  const [form, setForm] = useState({ name: '', email: '', password: '', roleId: 3 });

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await apiClient.get('/users');
      if (res.data.success) {
        setUsers(res.data.data || []);
      } else {
        setError(res.data.message || 'Failed to fetch user list.');
      }
    } catch (err) {
      setError(errorText(err, 'Failed to fetch user list.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const updateUser = async (u, path, payload, successMsg) => {
    setBusyId(u.id);
    setNotice('');
    setError('');
    try {
      await apiClient.put(path, { userId: u.id, ...payload });
      setNotice(successMsg);
      await loadUsers();
    } catch (err) {
      setError(errorText(err, 'Update failed.'));
    } finally {
      setBusyId(null);
    }
  };

  const handleRoleChange = (u, roleId) => {
    const role = ROLES.find((r) => r.id === roleId)?.name;
    updateUser(u, '/users/role', { roleId }, `${u.name} is now ${role}. It applies immediately.`);
  };

  const handleStatusChange = (u, status) =>
    updateUser(u, '/users/status', { status }, `${u.name} is now ${status}.`);

  const openAdd = () => {
    setForm({ name: '', email: '', password: '', roleId: 3 });
    setFormError('');
    setShowAdd(true);
  };

  const handleAddUser = async (e) => {
    e.preventDefault();
    setSaving(true);
    setFormError('');
    try {
      await apiClient.post('/users', { ...form, roleId: Number(form.roleId) });
      setShowAdd(false);
      setNotice(`User ${form.name} created.`);
      await loadUsers();
    } catch (err) {
      setFormError(errorText(err, 'Could not create user.'));
    } finally {
      setSaving(false);
    }
  };

  if (me && me.systemRoleName !== 'ADMIN') {
    return (
      <div className="admin-container">
        <div className="error-banner">Access Denied: only administrators can manage users.</div>
      </div>
    );
  }

  if (loading && users.length === 0 && !error) {
    return <div className="loading-spinner">Loading System Users & Roles...</div>;
  }

  return (
    <div className="admin-container">
      <div className="admin-header">
        <div>
          <h2>User Role & Authorization Control</h2>
          <p className="text-muted">Manage system-wide permissions and roles (ADMIN, MANAGER, MEMBER, VIEWER).</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button type="button" className="btn-secondary" onClick={loadUsers} disabled={loading}>
            <RefreshCw size={16} /> Refresh
          </button>
          <button type="button" className="btn-primary" onClick={openAdd}>
            <UserPlus size={16} /> Add User
          </button>
        </div>
      </div>

      {error && (
        <div className="error-banner" role="alert">
          {error}{' '}
          <button type="button" className="btn-secondary" style={{ marginLeft: 8, padding: '4px 10px' }} onClick={loadUsers}>
            Retry
          </button>
        </div>
      )}
      {notice && (
        <div className="error-banner" style={{ background: 'var(--moss-tint)', color: 'var(--moss-dark)' }} role="status">
          {notice}
        </div>
      )}

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
            {users.length === 0 && !loading && (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: 28 }}>
                  {error ? 'No data could be loaded.' : 'No users found.'}
                </td>
              </tr>
            )}
            {users.map((u) => {
              const isMe = me && me.id === u.id;
              const busy = busyId === u.id;
              return (
                <tr key={u.id}>
                  <td>#{u.id}</td>
                  <td>
                    <div className="user-cell">
                      <span className="avatar-sm">{(u.name || '?').charAt(0)}</span>
                      <strong>{u.name}</strong>{isMe && <span className="text-muted"> (you)</span>}
                    </div>
                  </td>
                  <td>
                    <span className="email-text"><Mail size={14} /> {u.email}</span>
                  </td>
                  <td>
                    <span className={`role-badge role-${(u.systemRoleName || '').toLowerCase()}`}>
                      {u.systemRoleName}
                    </span>
                  </td>
                  <td>
                    <select
                      className="role-select"
                      value={u.status}
                      disabled={isMe || busy}
                      onChange={(e) => handleStatusChange(u, e.target.value)}
                      title={isMe ? 'You cannot change your own status' : 'Change account status'}
                    >
                      {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </td>
                  <td>
                    <select
                      className="role-select"
                      value={u.systemRoleId}
                      disabled={isMe || busy}
                      onChange={(e) => handleRoleChange(u, parseInt(e.target.value, 10))}
                      title={isMe ? 'You cannot change your own role' : 'Change role'}
                    >
                      {ROLES.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                    </select>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {showAdd && (
        <div className="modal-overlay">
          <div className="modal-card">
            <div className="modal-header">
              <h3>Add New User</h3>
              <button type="button" className="modal-close" onClick={() => setShowAdd(false)} aria-label="Close">
                <X size={20} />
              </button>
            </div>

            {formError && <div className="error-banner">{formError}</div>}

            <form onSubmit={handleAddUser}>
              <div className="form-group">
                <label>Full name</label>
                <input type="text" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Email address</label>
                <input type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Temporary password (min 6 characters)</label>
                <input type="password" required minLength={6} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Role</label>
                <select value={form.roleId} onChange={(e) => setForm({ ...form, roleId: e.target.value })}>
                  {ROLES.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
                </select>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-secondary" onClick={() => setShowAdd(false)}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={saving}>{saving ? 'Creating...' : 'Create User'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}