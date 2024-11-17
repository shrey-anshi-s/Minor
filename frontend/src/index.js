import React from "react";
import ReactDOM from "react-dom";
import "./styles/ChatApp.css"; // Importing the CSS for global styles
import App from "./App"; // Import the main App component

ReactDOM.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
  document.getElementById("root") // Ensures the App is rendered in the correct HTML element
);
