import React from 'react';
import { useNavigate } from 'react-router-dom';
import './Home.css'; // Import the CSS file

const Home = ({ loggedIn, email }) => {
  const navigate = useNavigate();

  const handleLoginClick = () => {
    navigate('/Login');
  };

  const handleSignupClick = () => {
    navigate('/Signup');
  };

  return (
    <div className="main-container">
      <div className="title-container">
        <h1>Welcome to the Chat App!</h1>
      </div>
      <div className="button-container">
        {loggedIn ? (
          <div>
            <h2 className="email-text">Your email: {email}</h2>
            <button className="logout-button" onClick={() => { /* Handle logout logic */ }}>
              Log out
            </button>
          </div>
        ) : (
          <div className="auth-buttons">
            <button className="login-button" onClick={handleLoginClick}>Log in</button>
            <button className="signup-button" onClick={handleSignupClick}>Sign up</button>
          </div>
        )}
      </div>
    </div>
  );
};

export default Home;
