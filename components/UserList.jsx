import React, { useEffect, useState } from 'react';
import axios from 'axios'; // Import axios for HTTP requests

const UserList = ({ setActiveUser }) => {
  const [users, setUsers] = useState([]);

  // Fetch users from the backend API when the component mounts
  useEffect(() => {
    const fetchUsers = async () => {
      try {
        const response = await axios.get('http://localhost:5000/api/users');
        setUsers(response.data); // Store the fetched users in state
      } catch (error) {
        console.error('Error fetching users:', error);
      }
    };

    fetchUsers();
  }, []); // Empty dependency array to run only once when the component mounts

  return (
    <div className="user-list">
      <h3>Users</h3>
      <ul>
        {users.map((user) => (
          <li key={user._id} onClick={() => setActiveUser(user)}>
            <img src={`/assets/${user.image}`} alt={user.name} className="user-image" />
            <span>{user.name}</span>
            <span className={`status ${user.online ? 'online' : 'offline'}`} />
          </li>
        ))}
      </ul>
    </div>
  );
};

export default UserList;
