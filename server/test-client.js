const { io } = require('socket.io-client');
const socket = io('http://localhost:3000');

socket.on('connect', () => {
  console.log('Connected:', socket.id);
  socket.emit('subscribe', { cities: ['London'] });
  console.log('Subscribed to London, waiting for weather_update...');
});

socket.on('weather_update', (data) => {
  console.log('Weather update:', data.cityName, data.temperature + '°C', data.description);
  socket.disconnect();
  process.exit(0);
});

socket.on('weather_alert', (data) => {
  console.log('Alert:', data.cityName, data.type, data.message);
});

socket.on('connect_error', (err) => {
  console.error('Connection failed:', err.message);
  process.exit(1);
});

setTimeout(() => {
  console.log('Timeout - no update received in 15s');
  process.exit(1);
}, 15000);
