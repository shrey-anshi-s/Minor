export class WebSocketService {
  constructor() {
    this.callbacks = new Map();
    this.socket = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }

  connect(url, token, onMessage) {
    return new Promise((resolve, reject) => {
      try {
        // Initiate the WebSocket connection with the provided URL and token
        this.socket = new WebSocket(`${url}?token=${token}`);

        // Connection open handler
        this.socket.onopen = () => {
          console.log('Connected to WebSocket');
          this.reconnectAttempts = 0;
          resolve();
        };

        // Message handler
        this.socket.onmessage = (event) => {
          const data = JSON.parse(event.data);
          const callback = this.callbacks.get(data.type);
          
          // Call specific callback based on message type
          if (callback) {
            callback(data.payload);
          }
          
          // Call the general onMessage handler
          if (onMessage) {
            onMessage(data);
          }
        };

        // Connection close handler
        this.socket.onclose = () => {
          console.log('WebSocket connection closed');
          this.handleReconnect(url, token, onMessage);
        };

        // Error handler
        this.socket.onerror = (error) => {
          console.error('WebSocket error:', error);
          reject(error);
        };

      } catch (error) {
        console.error('WebSocket connection error:', error);
        reject(error);
      }
    });
  }

  handleReconnect(url, token, onMessage) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const timeout = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000);
      
      setTimeout(() => {
        console.log(`Attempting to reconnect... (${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
        this.connect(url, token, onMessage);
      }, timeout);
    }
  }

  subscribe(type, callback) {
    this.callbacks.set(type, callback);
  }

  unsubscribe(type) {
    this.callbacks.delete(type);
  }

  send(type, payload) {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify({ type, payload }));
    } else {
      console.error('WebSocket is not connected');
    }
  }

  disconnect() {
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
  }
}

// Export an instance of the WebSocket service
export const websocketService = new WebSocketService();
export default websocketService;
