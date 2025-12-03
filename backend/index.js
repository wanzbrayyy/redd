const express = require('express');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const multer = require('multer');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = 3000;
const JWT_SECRET = 'your-super-secret-key-change-this';
const USERS_DB_PATH = path.join(__dirname, 'users.json');
const UPLOADS_DIR = path.join(__dirname, 'uploads');

if (!fs.existsSync(UPLOADS_DIR)) {
    fs.mkdirSync(UPLOADS_DIR);
}

app.use(cors());
app.use(express.json());
app.use('/uploads', express.static(UPLOADS_DIR));

const readUsers = () => {
    if (!fs.existsSync(USERS_DB_PATH)) {
        return [];
    }
    const data = fs.readFileSync(USERS_DB_PATH);
    return JSON.parse(data);
};

const writeUsers = (users) => {
    fs.writeFileSync(USERS_DB_PATH, JSON.stringify(users, null, 2));
};

const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, UPLOADS_DIR);
    },
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, req.user.id + '-' + uniqueSuffix + path.extname(file.originalname));
    }
});

const upload = multer({ storage: storage });

const authMiddleware = (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ message: 'Authorization token required' });
    }
    const token = authHeader.split(' ')[1];
    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        req.user = decoded;
        next();
    } catch (error) {
        return res.status(401).json({ message: 'Invalid or expired token' });
    }
};

app.post('/api/register', (req, res) => {
    const { name, email, password } = req.body;
    if (!name || !email || !password) {
        return res.status(400).json({ message: 'Name, email, and password are required' });
    }

    const users = readUsers();
    if (users.find(u => u.email === email)) {
        return res.status(409).json({ message: 'Email already registered' });
    }

    const hashedPassword = bcrypt.hashSync(password, 8);
    const newUser = {
        id: users.length > 0 ? Math.max(...users.map(u => u.id)) + 1 : 1,
        name,
        email,
        password: hashedPassword,
        avatarUrl: null
    };
    users.push(newUser);
    writeUsers(users);

    const token = jwt.sign({ id: newUser.id, email: newUser.email }, JWT_SECRET, { expiresIn: '24h' });
    const { password: _, ...profile } = newUser;
    res.status(201).json({ token, profile });
});

app.post('/api/login', (req, res) => {
    const { email, password } = req.body;
    if (!email || !password) {
        return res.status(400).json({ message: 'Email and password are required' });
    }

    const users = readUsers();
    const user = users.find(u => u.email === email);
    if (!user || !bcrypt.compareSync(password, user.password)) {
        return res.status(401).json({ message: 'Invalid email or password' });
    }

    const token = jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: '24h' });
    const { password: _, ...profile } = user;
    res.status(200).json({ token, profile });
});

app.get('/api/profile', authMiddleware, (req, res) => {
    const users = readUsers();
    const user = users.find(u => u.id === req.user.id);
    if (!user) {
        return res.status(404).json({ message: 'User not found' });
    }
    const { password: _, ...profile } = user;
    res.status(200).json(profile);
});

app.put('/api/profile', authMiddleware, (req, res) => {
    const { name } = req.body;
    if (!name) {
        return res.status(400).json({ message: 'Name is required' });
    }

    let users = readUsers();
    const userIndex = users.findIndex(u => u.id === req.user.id);
    if (userIndex === -1) {
        return res.status(404).json({ message: 'User not found' });
    }

    users[userIndex].name = name;
    writeUsers(users);
    
    const { password: _, ...updatedProfile } = users[userIndex];
    res.status(200).json({ message: 'Profile updated successfully', profile: updatedProfile });
});

app.post('/api/profile/avatar', authMiddleware, upload.single('avatar'), (req, res) => {
    if (!req.file) {
        return res.status(400).json({ message: 'No file uploaded' });
    }
    
    let users = readUsers();
    const userIndex = users.findIndex(u => u.id === req.user.id);
    if (userIndex === -1) {
        return res.status(404).json({ message: 'User not found' });
    }

    const avatarUrl = `${req.protocol}://${req.get('host')}/uploads/${req.file.filename}`;
    users[userIndex].avatarUrl = avatarUrl;
    writeUsers(users);

    res.status(200).json({ message: 'Avatar uploaded successfully', avatarUrl });
});

app.listen(PORT, () => {
    console.log(`Server is running on http://localhost:${PORT}`);
});