import React, { useState } from "react";
import Modal from "./Modal"; // Importing the modal component
import "/services/ChatApp.css"; // Importing the CSS file

function ChatApp() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [currentUser, setCurrentUser] = useState("User 1");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const chattingWith = currentUser === "User 1" ? "User 2" : "User 1";

  const handleSend = () => {
    if (input.trim()) {
      setMessages([...messages, { user: currentUser, text: input }]);
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
          <div className="user-item active-user">
            <div className="user-icon">U1</div>
            User 1
          </div>
          <div className="user-item">
            <div className="user-icon">U2</div>
            User 2
          </div>
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
                className={`chat-message ${msg.user === currentUser ? 'current-user' : 'other-user'}`}
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
      {isModalOpen && <Modal currentUser={currentUser} onClose={handleToggleModal} />}
    </div>
  );
}

export default ChatApp;
