import { Link } from 'react-router-dom';

export default function ForbiddenPage() {
  return (
    <div className="page">
      <div className="error-state">
        <div className="error-icon">⊘</div>
        <h2>Access denied</h2>
        <p>Your account does not have permission for this page.</p>
        <Link to="/" className="btn-primary" style={{ display: 'inline-block', marginTop: 16 }}>
          Back to dashboard
        </Link>
      </div>
    </div>
  );
}
