import React from 'react';

const UserList = ({ users, setActiveUser }) => (
  <div className="user-list">
    <h3>Users</h3>
    <ul>
      {users.map(user => (
        <li key={user.id} onClick={() => setActiveUser(user)}>
          <img src={`/assets/${user.image}`} alt={user.name} className="user-image" />
          <span>{user.name}</span>
          <span className={`status ${user.online ? 'online' : 'offline'}`} />
        </li>
      ))}
    </ul>
  </div>
);

export default UserList;
