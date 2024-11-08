import React, { useState, useEffect } from 'react';
import UserList from './components/UserList';
import ChatWindow from './components/ChatWindow';
import MessageInput from './components/MessageInput';

const App = () => {
  const [messages, setMessages] = useState([]);
  const [users, setUsers] = useState([
    { id: 1, name: 'Alice', online: true, image: 'alice.jpg' },
    { id: 2, name: 'Bob', online: false, image: 'bob.jpg' },
    { id: 3, name: 'Charlie', online: true, image: 'charlie.jpg' }
  ]);
  const [activeUser, setActiveUser] = useState(null);

  const sendMessage = (content) => {
    const newMessage = { id: messages.length + 1, user: 'You', content };
    setMessages([...messages, newMessage]);
  };

  return (
    <div className="app">
      <UserList users={users} setActiveUser={setActiveUser} />
      {activeUser && (
        <div className="chat-container">
          <ChatWindow messages={messages} />
          <MessageInput sendMessage={sendMessage} />
        </div>
      )}
    </div>
  );
};

export default App;
