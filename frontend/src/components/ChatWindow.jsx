import React from 'react';

const ChatWindow = ({ messages }) => (
  <div className="chat-window">
    {messages.map(message => (
      <div key={message.id} className="message">
        <span className="user">{message.user}:</span>
        <span className="content">{message.content}</span>
      </div>
    ))}
  </div>
);

export default ChatWindow;
