import React, { useState } from "react";
import Modal from "./Modal"; // Importing the modal component
import UserList from "./UserList"; // Importing the UserList component
import "./ChatApp.css"; // Importing the CSS file

function ChatApp() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [currentUser , setCurrentUser ] = useState(null); // Initialize as null
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Determine who the user is chatting with based on currentUser 
  const chattingWith = currentUser  ? (currentUser .name === "User  1" ? "User  2" : "User  1") : "Select a user";

  const handleSend = () => {
    if (input.trim()) {
      setMessages([...messages, { user: currentUser ?.name, text: input }]);
      setInput("");
    }
  };

  const handleToggleModal = () => {
    setIsModalOpen(!isModalOpen);
  };

  return (
    <div className="app-container">
      <header className="header">
        <div className="icon">🔒</div>
        <h1 className="title">Chat App</h1>
        <div className="icon" onClick={handleToggleModal}>👤</div>
      </header>
      <div className="main-container">
        <aside className="user-list">
          <h2 className="user-list-title">Users</h2>
          <User List setCurrentUser ={setCurrentUser } /> {/* Use UserList component */}
        </aside>
        <div className="chat-window">
          <div className="chat-header">
            <div className="icon">👤</div>
            <h2>Chatting with {chattingWith}</h2>
          </div>
          <div className="chat-messages">
            {messages.map((msg, index) => (
              <div
                key={index}
                className={`chat-message ${msg.user === currentUser ?.name ? 'current-user' : 'other-user'}`}
              >
                {msg.text}
              </div>
            ))}
          </div>
          <div className="chat-input-container">
            <input
              type="text"
              placeholder="Type a message..."
              value={input}
              onChange={(e) => setInput(e.target.value)}
              className="chat-input"
            />
            <button onClick={handleSend} className="send-button">
              Send
            </button>
          </div>
        </div>
      </div>
      {isModalOpen && <Modal currentUser ={currentUser } onClose={handleToggleModal} />}
    </div>
  );
}

export default ChatApp;