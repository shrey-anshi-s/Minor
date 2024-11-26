import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './Login.css'; // Ensure you have a CSS file for styling

const Login = ({ setLoggedIn, setEmail }) => {
  const [email, setEmailInput] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault(); // Prevent default form submission
    setError(null); // Reset error state
    setLoading(true); // Start loading state

    try {
      const response = await fetch('/api/users/login', { // Adjust the endpoint as necessary
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Login failed');
      }

      const data = await response.json();
      const token = data.token;

      // Store the token in local storage for future API calls
      localStorage.setItem('token', token);
      setLoggedIn(true); // Update logged-in state
      setEmail(email); // Set the user's email in the state

      navigate('/ChatApp'); // Navigate to ChatApp component after successful login
    } catch (error) {
      setError(error.message); // Set error message to state
      alert(error.message); // Show the error in a basic alert
    } finally {
      setLoading(false); // Stop loading state
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <h2 className="login-title">Login</h2>
        <form onSubmit={handleLogin}>
          <input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmailInput(e.target.value)}
            className="login-input"
            required
          />
          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="login-input"
            required
          />
          {error && <div className="error-message">{error}</div>}
          <button className="login-button" type="submit" disabled={loading}>
            {loading ? 'Logging in...' : 'Log in'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;
