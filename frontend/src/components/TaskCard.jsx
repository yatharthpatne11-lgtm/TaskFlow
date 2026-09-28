import React from 'react';
import { Clock, Trash2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { parseDate } from '../utils/dateUtils';

export default function TaskCard({ task, onDragStart, onClick, onDelete }) {
  const { user } = useAuth();
  const due = parseDate(task.dueDate);
  const isOverdue = due && due < new Date() && task.columnName !== 'DONE';
  const isAuthorizedToDelete = user?.systemRoleName === 'ADMIN' || user?.systemRoleName === 'MANAGER';

  return (
    <div 
      className="task-card"
      draggable={user?.systemRoleName !== 'VIEWER'}
      onDragStart={(e) => onDragStart(e, task.id)}
      onClick={onClick}
    >
      <div className="card-header">
        <span className={`priority-tag priority-${task.priority.toLowerCase()}`}>
          {task.priority}
        </span>
        {isAuthorizedToDelete && (
          <button 
            className="btn-icon-danger" 
            onClick={(e) => {
              e.stopPropagation();
              onDelete(task.id);
            }}
          >
            <Trash2 size={14} />
          </button>
        )}
      </div>

      <h4 className="task-title">{task.title}</h4>
      {task.description && <p className="task-desc">{task.description}</p>}

      <div className="card-footer">
        <div className="assignee">
          <span className="avatar-sm">
            {task.assigneeName ? task.assigneeName.charAt(0) : '?'}
          </span>
          <span className="assignee-name">{task.assigneeName || 'Unassigned'}</span>
        </div>

        {due && (
          <div className={isOverdue ? 'due-date overdue' : 'due-date'}>
            <Clock size={12} />
            <span>{due.toLocaleDateString()}</span>
          </div>
        )}
      </div>
    </div>
  );
}