import React, { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import { createNewTask, assignTask, fetchAssignableUsers } from '../services/taskApi';

export default function TaskModal({ boardId, task, onClose, onTaskSaved }) {
  const [title, setTitle] = useState(task ? task.title : '');
  const [description, setDescription] = useState(task ? task.description : '');
  const [priority, setPriority] = useState(task ? task.priority : 'MEDIUM');
  const [columnId, setColumnId] = useState(task ? task.columnId : 1);
  const [estimatedEffort, setEstimatedEffort] = useState(task ? task.estimatedEffortHours : 2.0);
  const [assigneeId, setAssigneeId] = useState(task && task.assigneeId ? String(task.assigneeId) : '');
  const [people, setPeople] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Load the people this task can be assigned to
  useEffect(() => {
    let cancelled = false;
    fetchAssignableUsers()
      .then((res) => { if (!cancelled && res.success) setPeople(res.data || []); })
      .catch((err) => {
        if (!cancelled) setError(err.response?.data?.message || 'Could not load the list of people to assign.');
      });
    return () => { cancelled = true; };
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const chosen = assigneeId === '' ? null : parseInt(assigneeId, 10);

      if (task) {
        // Existing task: only the assignee can be changed here
        await assignTask(task.id, chosen);
      } else {
        await createNewTask({
          title,
          description,
          boardId: parseInt(boardId),
          columnId: parseInt(columnId),
          priority,
          assigneeId: chosen,
          estimatedEffortHours: parseFloat(estimatedEffort),
          position: 0
        });
      }
      onTaskSaved();
    } catch (err) {
      setError(err.response?.data?.message || (task ? 'Failed to assign task' : 'Failed to create task'));
    } finally {
      setLoading(false);
    }
  };

  // If the current assignee is not in the list (e.g. later deactivated), still show them
  const currentMissing = task && task.assigneeId && !people.some((p) => p.id === task.assigneeId);

  return (
    <div className="modal-overlay">
      <div className="modal-card">
        <div className="modal-header">
          <h3>{task ? 'Task Details' : 'Create New Task'}</h3>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Close">
            <X size={20} />
          </button>
        </div>

        {error && <div className="error-banner">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Title</label>
            <input type="text" required value={title} onChange={(e) => setTitle(e.target.value)} disabled={!!task} />
          </div>

          <div className="form-group">
            <label>Description</label>
            <textarea rows="3" value={description} onChange={(e) => setDescription(e.target.value)} disabled={!!task} />
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Priority</label>
              <select value={priority} onChange={(e) => setPriority(e.target.value)} disabled={!!task}>
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="URGENT">URGENT</option>
              </select>
            </div>

            <div className="form-group">
              <label>Column</label>
              <select value={columnId} onChange={(e) => setColumnId(e.target.value)} disabled={!!task}>
                <option value={1}>BACKLOG</option>
                <option value={2}>TO DO</option>
                <option value={3}>IN PROGRESS</option>
                <option value={4}>IN REVIEW</option>
                <option value={5}>DONE</option>
              </select>
            </div>
          </div>

          <div className="form-group">
            <label>Assign to</label>
            <select value={assigneeId} onChange={(e) => setAssigneeId(e.target.value)}>
              <option value="">Unassigned</option>
              {currentMissing && <option value={String(task.assigneeId)}>{task.assigneeName || `User #${task.assigneeId}`}</option>}
              {people.map((p) => (
                <option key={p.id} value={String(p.id)}>{p.name} ({p.systemRoleName})</option>
              ))}
            </select>
          </div>

          <div className="modal-actions">
            <button type="button" className="btn-secondary" onClick={onClose}>{task ? 'Close' : 'Cancel'}</button>
            <button type="submit" className="btn-primary" disabled={loading}>
              {loading ? 'Saving...' : (task ? 'Save Assignee' : 'Save Task')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}