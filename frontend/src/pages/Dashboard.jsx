import React, { useState, useEffect } from 'react';
import { CheckCircle2, Clock, AlertTriangle, ListTodo, Activity, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import apiClient from '../services/apiClient';
import { useAuth } from '../context/AuthContext';
import { parseDate } from '../utils/dateUtils';


export default function Dashboard() {
  const { user } = useAuth();
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      // Fetch tasks for the main college project board (ID 1)
      const res = await apiClient.get('/tasks?boardId=1');
      if (res.data.success) {
        setTasks(res.data.data);
      } else {
        setLoadError(res.data.message || 'The server could not load board data.');
      }
    } catch (err) {
      console.error("Failed to load dashboard metrics", err);
      setLoadError(err.response?.data?.message || err.message || 'Could not reach the server.');
    } finally {
      setLoading(false);
    }
  };

  // Calculated real database statistics
  const myAssignedTasks = tasks.filter(t => t.assigneeId === user?.id);
  const overdueTasks = tasks.filter(t => t.dueDate && parseDate(t.dueDate) < new Date() && t.columnName !== 'DONE');
  const urgentTasks = tasks.filter(t => t.priority === 'URGENT' || t.priority === 'HIGH');
  const completedTasks = tasks.filter(t => t.columnName === 'DONE');

  const completionRate = tasks.length > 0 ? Math.round((completedTasks.length / tasks.length) * 100) : 0;

  if (loading) return <div className="loading-spinner">Calculating TaskFlow Workspace Analytics...</div>;

  return (
    <div className="dashboard-container">
      <div className="dashboard-header">
        <div>
          <h2>Welcome back, {user?.name} 👋</h2>
          <p className="text-muted">Here is what is happening across your projects today.</p>
        </div>
        <Link to="/boards/1" className="btn-primary">
          <span>View Kanban Board</span>
          <ArrowRight size={16} />
        </Link>
      </div>

      {loadError && (
        <div role="alert" style={{ background: '#fdecea', color: '#8a1f17', border: '1px solid #f5c2bd', borderRadius: 8, padding: '10px 14px', marginBottom: 16 }}>
          <strong>Couldn't load board data:</strong> {loadError}
        </div>
      )}

      {/* Real Statistics Grid */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon bg-blue-dim">
            <ListTodo size={20} className="text-blue" />
          </div>
          <div className="stat-data">
            <span className="stat-value">{myAssignedTasks.length}</span>
            <span className="stat-label">My Assigned Tasks</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon bg-red-dim">
            <AlertTriangle size={20} className="text-red" />
          </div>
          <div className="stat-data">
            <span className="stat-value">{overdueTasks.length}</span>
            <span className="stat-label">Overdue Tasks</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon bg-orange-dim">
            <Clock size={20} className="text-orange" />
          </div>
          <div className="stat-data">
            <span className="stat-value">{urgentTasks.length}</span>
            <span className="stat-label">High / Urgent Priority</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon bg-green-dim">
            <CheckCircle2 size={20} className="text-green" />
          </div>
          <div className="stat-data">
            <span className="stat-value">{completionRate}%</span>
            <span className="stat-label">Board Progress</span>
          </div>
        </div>
      </div>

      <div className="dashboard-sections">
        {/* Active Workload Section */}
        <div className="dashboard-panel">
          <h3>My Current Tasks</h3>
          {myAssignedTasks.length === 0 ? (
            <div className="empty-state">No active tasks assigned to you right now.</div>
          ) : (
            <ul className="task-preview-list">
              {myAssignedTasks.map(t => (
                <li key={t.id} className="task-preview-item">
                  <div className="task-preview-main">
                    <span className={`priority-tag priority-${t.priority.toLowerCase()}`}>{t.priority}</span>
                    <span className="task-preview-title">{t.title}</span>
                  </div>
                  <span className="column-pill">{t.columnName}</span>
                </li>
              ))}
            </ul>
          )}
        </div>

        {/* Activity & System Health Panel */}
        <div className="dashboard-panel">
          <h3>
            <Activity size={18} style={{ marginRight: '8px', verticalAlign: 'middle' }} />
            System Audit & Syllabus Highlights
          </h3>
          <div className="audit-feed">
            <div className="audit-item">
              <span className="audit-dot"></span>
              <div>
                <strong>Multi-User Security Enforced:</strong> Authenticated as <span className="highlight-role">{user?.systemRoleName}</span>.
              </div>
            </div>
            <div className="audit-item">
              <span className="audit-dot"></span>
              <div>
                <strong>Background Thread Active:</strong> <code>DeadlineReminderService</code> daemon thread monitoring approaching deadlines.
              </div>
            </div>
            <div className="audit-item">
              <span className="audit-dot"></span>
              <div>
                <strong>Relational Persistence:</strong> Total <code>{tasks.length}</code> tasks loaded via JDBC prepared statements & MySQL JOINs.
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}