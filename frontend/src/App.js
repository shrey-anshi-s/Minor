import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import Home from './components/Home'; // Adjust the path if needed
import Login from './components/Login'; // Adjust the path if needed
import ChatApp from './components/ChatApp'; // Adjust the path if needed
import Signup from './components/Signup'; // Import the Signup component

function App() {
    return (
        <Router>
            <div>
                <Routes>
                    <Route path="/" element={<Home />} />
                    <Route path="/login" element={<Login />} />
                    <Route path="/signup" element={<Signup />} /> {/* Add the Signup route */}
                    <Route path="/chat" element={<ChatApp />} />
                </Routes>
            </div>
        </Router>
    );
}

export default App;