// App.js
import React from "react";
import { BrowserRouter as Router, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./AuthContext";
import LoginPage from "./LoginPage";
import ChatApp from "./ChatApp"; // Your chat application component
import PrivateRoute from "./PrivateRoute"; // Create this component
import Home from "./Home"; // Import your Home component

const App = () => {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/" element={<Home />} /> {/* Home page */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/chat" element={<PrivateRoute component={ChatApp} />} />
          {/* Add other routes here */}
        </Routes>
      </Router>
    </AuthProvider>
  );
};

export default App;
