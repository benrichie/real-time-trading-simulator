// src/components/layout/Navbar.tsx
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import './Navbar.css';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
       <div className="navbar-left">
         <a
           href="https://github.com/benrichie"
           target="_blank"
           rel="noopener noreferrer"
           className="navbar-github"
         >
           <img src="/githubimage.png" alt="GitHub" className="github-logo" />
         </a>
       </div>


      <div className="navbar-center">
        <h1 className="navbar-title">live trading simulator</h1>
      </div>

      <div className="navbar-right">
        <span className="navbar-username">{user?.username}</span>
        <button className="btn-logout" onClick={handleLogout}>
          logout
        </button>
      </div>
    </nav>
  );
};
