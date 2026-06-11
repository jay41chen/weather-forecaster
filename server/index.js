require('dotenv').config();
const express = require('express');
const { createServer } = require('http');
const { Server } = require('socket.io');

const API_KEY = process.env.OPEN_WEATHER_API_KEY;
const PORT = parseInt(process.env.PORT || '3000', 10);
const POLL_INTERVAL = parseInt(process.env.POLL_INTERVAL_MS || '60000', 10);

if (!API_KEY) {
  console.error('OPEN_WEATHER_API_KEY is required. Copy .env.example to .env and fill it in.');
  process.exit(1);
}

const app = express();
app.use(express.json());
app.use(express.static('public'));
const httpServer = createServer(app);
const io = new Server(httpServer, { cors: { origin: '*' } });

// socketId → Set<cityName>
const subscriptions = new Map();
// cityName → { timer, lastKnown }
const cityPollers = new Map();

function subscriberCount(city) {
  let count = 0;
  for (const cities of subscriptions.values()) {
    if (cities.has(city)) count++;
  }
  return count;
}

async function fetchWeather(city) {
  const url = `https://api.openweathermap.org/data/2.5/weather?q=${encodeURIComponent(city)}&appid=${API_KEY}&units=metric`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`API ${res.status}: ${res.statusText}`);
  return res.json();
}

function toPayload(data) {
  return {
    cityName: data.name,
    temperature: data.main.temp,
    feelsLike: data.main.feels_like,
    description: data.weather[0].description,
    iconCode: data.weather[0].icon,
    humidity: data.main.humidity,
    windSpeed: data.wind.speed,
    pressure: data.main.pressure,
    timestamp: data.dt,
  };
}

function detectAlert(city, prev, curr) {
  const tempDiff = Math.abs(curr.temperature - prev.temperature);
  const categoryChanged = prev.iconCode.replace(/[dn]$/, '') !== curr.iconCode.replace(/[dn]$/, '');

  if (tempDiff > 5) {
    const direction = curr.temperature > prev.temperature ? 'TEMPERATURE_RISE' : 'TEMPERATURE_DROP';
    return {
      cityName: city,
      type: direction,
      message: `Temperature ${direction === 'TEMPERATURE_RISE' ? 'rose' : 'dropped'} ${tempDiff.toFixed(1)}°C`,
      timestamp: curr.timestamp,
    };
  }

  if (categoryChanged) {
    return {
      cityName: city,
      type: 'CATEGORY_CHANGE',
      message: `Weather changed from "${prev.description}" to "${curr.description}"`,
      timestamp: curr.timestamp,
    };
  }

  return null;
}

function hasChanged(prev, curr) {
  return prev.temperature !== curr.temperature
    || prev.description !== curr.description
    || prev.iconCode !== curr.iconCode;
}

async function pollCity(city) {
  try {
    const data = await fetchWeather(city);
    const payload = toPayload(data);
    const poller = cityPollers.get(city);
    const prev = poller?.lastKnown;

    if (!prev || hasChanged(prev, payload)) {
      io.emit('weather_update', payload);
      console.log(`[update] ${city}: ${payload.temperature}°C, ${payload.description}`);

      if (prev) {
        const alert = detectAlert(city, prev, payload);
        if (alert) {
          io.emit('weather_alert', alert);
          console.log(`[alert] ${city}: ${alert.type} — ${alert.message}`);
        }
      }
    }

    if (poller) poller.lastKnown = payload;
  } catch (err) {
    console.error(`[error] polling ${city}:`, err.message);
  }
}

function startPolling(city) {
  if (cityPollers.has(city)) return;
  console.log(`[poll] starting for ${city}`);
  pollCity(city);
  const timer = setInterval(() => pollCity(city), POLL_INTERVAL);
  cityPollers.set(city, { timer, lastKnown: null });
}

function stopPolling(city) {
  const poller = cityPollers.get(city);
  if (!poller) return;
  console.log(`[poll] stopping for ${city}`);
  clearInterval(poller.timer);
  cityPollers.delete(city);
}

app.get('/admin/status', (req, res) => {
  const subs = {};
  for (const [city] of cityPollers) {
    subs[city] = subscriberCount(city);
  }
  res.json({ connections: subscriptions.size, subscriptions: subs });
});

app.post('/admin/test-update', (req, res) => {
  const payload = {
    cityName: req.body.cityName || 'London',
    temperature: req.body.temperature ?? 25.0,
    feelsLike: req.body.feelsLike ?? 24.0,
    description: req.body.description || 'clear sky',
    iconCode: req.body.iconCode || '01d',
    humidity: req.body.humidity ?? 60,
    windSpeed: req.body.windSpeed ?? 3.5,
    pressure: req.body.pressure ?? 1013,
    timestamp: Math.floor(Date.now() / 1000),
  };
  io.emit('weather_update', payload);
  console.log(`[admin] test weather_update: ${payload.cityName} ${payload.temperature}°C`);
  res.json({ sent: payload });
});

app.post('/admin/test-alert', (req, res) => {
  const alert = {
    cityName: req.body.cityName || 'London',
    type: req.body.type || 'TEMPERATURE_RISE',
    message: req.body.message || 'Temperature rose 6.0°C (test alert)',
    timestamp: Math.floor(Date.now() / 1000),
  };
  io.emit('weather_alert', alert);
  console.log(`[admin] test weather_alert: ${alert.cityName} ${alert.type}`);
  res.json({ sent: alert });
});

io.on('connection', (socket) => {
  console.log(`[connect] ${socket.id}`);
  subscriptions.set(socket.id, new Set());

  socket.on('subscribe', (data) => {
    const cities = data?.cities;
    if (!Array.isArray(cities)) return;

    const sub = subscriptions.get(socket.id);
    cities.forEach((city) => {
      sub.add(city);
      startPolling(city);
    });
    console.log(`[subscribe] ${socket.id} → ${cities.join(', ')}`);
  });

  socket.on('disconnect', () => {
    console.log(`[disconnect] ${socket.id}`);
    const cities = subscriptions.get(socket.id) || new Set();
    subscriptions.delete(socket.id);

    for (const city of cities) {
      if (subscriberCount(city) === 0) {
        stopPolling(city);
      }
    }
  });
});

httpServer.listen(PORT, () => {
  console.log(`Server listening on port ${PORT}`);
});
