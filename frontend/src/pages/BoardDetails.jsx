import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Plus, Filter, Search } from 'lucide-react';
import TaskCard from '../components/TaskCard';
import TaskModal from '../components/TaskModal';
import MyActionsList from '../components/MyActionsList';
import { fetchBoardTasks, updateTaskPosition, deleteTaskById } from '../services/taskApi';
import { useAuth } from '../context/AuthContext';

const COLUMNS = [
  { id: 1, name: 'BACKLOG' },
  { id: 2, name: 'TO DO' },
  { id: 3, name: 'IN PROGRESS' },
  { id: 4, name: 'IN REVIEW' },
  { id: 5, name: 'DONE' }
];

export default function BoardDetails() {
  const { id: boardId } = useParams();
  const { user } = useAuth();
  
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedPriority, setSelectedPriority] = useState('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [activeTask, setActiveTask] = useState(null);
  const [loadError, setLoadError] = useState(null);

  useEffect(() => {
    loadTasks();
  }, [boardId]);

  const loadTasks = async () => {
    try {
      setLoadError(null);
      const res = await fetchBoardTasks(boardId || 1);
      if (res.success) {
        setTasks(res.data);
      } else {
        setLoadError(res.message || 'The server could not load this board.');
      }
    } catch (err) {
      console.error('Failed to load board tasks', err);
      setLoadError(err.response?.data?.message || err.message || 'Could not reach the server.');
    } finally {
      setLoading(false);
    }
  };

  const handleDragStart = (e, taskId) => {
    e.dataTransfer.setData('taskId', taskId);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
  };

  const handleDrop = async (e, columnId) => {
    e.preventDefault();
    const taskId = parseInt(e.dataTransfer.getData('taskId'));
    const task = tasks.find((t) => t.id === taskId);

    if (!task || task.columnId === columnId) return;

    // Optimistic UI Update
    const updatedTasks = tasks.map((t) =>
      t.id === taskId ? { ...t, columnId, columnName: COLUMNS.find((c) => c.id === columnId)?.name } : t
    );
    setTasks(updatedTasks);

    try {
      await updateTaskPosition({
        id: taskId,
        columnId: columnId,
        position: 0
      });
    } catch (err) {
      console.error('Failed to update task position', err);
      loadTasks(); // Revert on failure
    }
  };

  const handleDeleteTask = async (taskId) => {
    if (!window.confirm('Are you sure you want to delete this task?')) return;
    try {
      await deleteTaskById(taskId);
      setTasks(tasks.filter((t) => t.id !== taskId));
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to delete task');
    }
  };

  // Merge only the saved note so the card and the My Actions table stay in sync
  const handleQuickNoteSaved = (updatedTask) => {
    setTasks((prev) =>
      prev.map((t) => (t.id === updatedTask.id ? { ...t, quickNote: updatedTask.quickNote } : t))
    );
  };

  const filteredTasks = tasks.filter((task) => {
    const matchesSearch = task.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (task.description && task.description.toLowerCase().includes(searchQuery.toLowerCase()));
    const matchesPriority = selectedPriority === 'ALL' || task.priority === selectedPriority;
    return matchesSearch && matchesPriority;
  });

  if (loading) return <div className="loading-spinner">Loading Kanban Board...</div>;

  return (
    <div className="kanban-container">
      <div className="kanban-header">
        <div>
          <h2>Main Project Kanban Board</h2>
          <p className="text-muted">Drag cards across columns to update task progress in real-time.</p>
        </div>

        {user?.systemRoleName !== 'VIEWER' && (
          <button className="btn-primary" onClick={() => { setActiveTask(null); setIsModalOpen(true); }}>
            <Plus size={16} /> Add Task
          </button>
        )}
      </div>

      {loadError && (
        <div role="alert" style={{ background: '#fdecea', color: '#8a1f17', border: '1px solid #f5c2bd', borderRadius: 8, padding: '10px 14px', margin: '0 0 16px' }}>
          <strong>Couldn't load the board:</strong> {loadError}{' '}
          <button className="btn-primary" style={{ marginLeft: 8, padding: '4px 10px' }} onClick={loadTasks}>Retry</button>
        </div>
      )}

      {/* Filter Toolbar */}
      <div className="board-toolbar">
        <div className="search-box">
          <Search size={16} color="#94a3b8" />
          <input
            type="text"
            placeholder="Search tasks..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        <div className="filter-box">
          <Filter size={16} color="#94a3b8" />
          <select
            value={selectedPriority}
            onChange={(e) => setSelectedPriority(e.target.value)}
          >
            <option value="ALL">All Priorities</option>
            <option value="LOW">LOW</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HIGH">HIGH</option>
            <option value="URGENT">URGENT</option>
          </select>
        </div>
      </div>

      {/* Kanban Board Grid */}
      <div className="kanban-grid">
        {COLUMNS.map((col) => {
          const colTasks = filteredTasks.filter((t) => t.columnId === col.id);
          return (
            <div
              key={col.id}
              className="kanban-column"
              onDragOver={handleDragOver}
              onDrop={(e) => handleDrop(e, col.id)}
            >
              <div className="column-header">
                <h3>{col.name}</h3>
                <span className="task-count">{colTasks.length}</span>
              </div>

              <div className="column-body">
                {colTasks.map((task) => (
                  <TaskCard
                    key={task.id}
                    task={task}
                    onDragStart={handleDragStart}
                    onClick={() => { setActiveTask(task); setIsModalOpen(true); }}
                    onDelete={handleDeleteTask}
                    onQuickNoteSaved={handleQuickNoteSaved}
                  />
                ))}
              </div>
            </div>
          );
        })}
      </div>

      {/* Personal agenda: only tasks assigned to the logged-in user */}
      <MyActionsList tasks={tasks} columns={COLUMNS} onQuickNoteSaved={handleQuickNoteSaved} />

      {isModalOpen && (
        <TaskModal
          boardId={boardId || 1}
          task={activeTask}
          onClose={() => setIsModalOpen(false)}
          onTaskSaved={() => { setIsModalOpen(false); loadTasks(); }}
        />
      )}
    </div>
  );
}