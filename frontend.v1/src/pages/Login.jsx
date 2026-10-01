import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Lock, Mail, ArrowRight, ShieldCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Please check credentials.');
    } finally {
      setLoading(false);
    }
  };

  const fillDemoAccount = (demoEmail) => {
    setEmail(demoEmail);
    setPassword('Password@123');
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-brand">
          <div className="logo-icon-lg">TF</div>
          <h1>TaskFlow</h1>
          <p>Multi-User Kanban Management System</p>
        </div>

        {error && <div className="auth-error-alert">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label><Mail size={16} /> Email Address</label>
            <input 
              type="email" 
              required 
              placeholder="admin@taskflow.local" 
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label><Lock size={16} /> Password</label>
            <input 
              type="password" 
              required 
              placeholder="••••••••" 
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <button type="submit" className="btn-auth-submit" disabled={loading}>
            {loading ? 'Authenticating...' : 'Sign In'} <ArrowRight size={16} />
          </button>
        </form>

        <div className="demo-accounts-panel">
          <h4><ShieldCheck size={14} /> Quick Demo Accounts (Password: Password@123)</h4>
          <div className="demo-chips">
            <button type="button" onClick={() => fillDemoAccount('admin@taskflow.local')}>Admin</button>
            <button type="button" onClick={() => fillDemoAccount('manager@taskflow.local')}>Manager</button>
            <button type="button" onClick={() => fillDemoAccount('member@taskflow.local')}>Member</button>
            <button type="button" onClick={() => fillDemoAccount('viewer@taskflow.local')}>Viewer</button>
          </div>
        </div>

        <div className="auth-footer">
          <span>Don't have an account? </span>
          <Link to="/register">Create Account</Link>
        </div>
      </div>
    </div>
  );
}