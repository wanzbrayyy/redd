
const express = require('express');
const bodyParser = require('body-parser');
const jwt = require('jsonwebtoken');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const app = express();
app.use(bodyParser.json());
app.use(cors());
const PORT = 3000;
const SECRET = "redrak-secret-key-please-change";
let usersFile = path.join(__dirname, 'users.json');
function loadUsers(){
    try {
        let raw = fs.readFileSync(usersFile,'utf8');
        return JSON.parse(raw);
    } catch(e){ return []; }
}
function saveUsers(users){
    fs.writeFileSync(usersFile, JSON.stringify(users, null, 2), 'utf8');
}
if(!fs.existsSync(usersFile)) saveUsers([]);

app.get('/', (req,res)=> res.json({ok:true, msg:"RedRak API"}));

app.post('/api/register', (req,res)=>{
    let {name, email, password} = req.body || {};
    if(!email || !password || !name) return res.status(400).json({error:"missing"});
    let users = loadUsers();
    let exists = users.find(u=>u.email===email);
    if(exists) return res.status(409).json({error:"exists"});
    let id = Date.now().toString(36);
    let user = {id, name, email, password, avatar:null, created: new Date().toISOString()};
    users.push(user);
    saveUsers(users);
    let token = jwt.sign({id:user.id,email:user.email}, SECRET, {expiresIn:'30d'});
    res.json({token, profile:{id:user.id,name:user.name,email:user.email,avatar:user.avatar}});
});

app.post('/api/login', (req,res)=>{
    let {email,password} = req.body || {};
    if(!email||!password) return res.status(400).json({error:"missing"});
    let users = loadUsers();
    let user = users.find(u=>u.email===email && u.password===password);
    if(!user) return res.status(401).json({error:"invalid"});
    let token = jwt.sign({id:user.id,email:user.email}, SECRET, {expiresIn:'30d'});
    res.json({token, profile:{id:user.id,name:user.name,email:user.email,avatar:user.avatar}});
});

app.get('/api/profile', (req,res)=>{
    let auth = req.headers['authorization']||'';
    if(!auth.startsWith('Bearer ')) return res.status(401).json({error:"noauth"});
    let token = auth.slice(7);
    try {
        let payload = jwt.verify(token, SECRET);
        let users = loadUsers();
        let user = users.find(u=>u.id===payload.id);
        if(!user) return res.status(404).json({error:"notfound"});
        res.json({id:user.id,name:user.name,email:user.email,avatar:user.avatar});
    } catch(e) { res.status(401).json({error:"invalid"}); }
});

const multer = require('multer');
const upload = multer({ dest: path.join(__dirname,'uploads') });
app.post('/api/upload-avatar', upload.single('avatar'), (req,res)=>{
    let auth = req.headers['authorization']||'';
    if(!auth.startsWith('Bearer ')) return res.status(401).json({error:"noauth"});
    let token = auth.slice(7);
    try {
        let payload = jwt.verify(token, SECRET);
        let users = loadUsers();
        let user = users.find(u=>u.id===payload.id);
        if(!user) return res.status(404).json({error:"notfound"});
        if(!req.file) return res.status(400).json({error:"nofile"});
        let newPath = path.join(__dirname,'uploads', payload.id + path.extname(req.file.originalname || req.file.filename));
        fs.renameSync(req.file.path, newPath);
        user.avatar = '/uploads/' + path.basename(newPath);
        saveUsers(users);
        res.json({ok:true,avatar:user.avatar});
    } catch(e){ res.status(401).json({error:"invalid"}); }
});

app.get('/uploads/:file', (req,res)=>{
    let p = path.join(__dirname,'uploads', req.params.file);
    if(fs.existsSync(p)) res.sendFile(p);
    else res.status(404).send('Not found');
});

app.get('/api/_list_users', (req,res)=>{
    let users = loadUsers();
    res.json(users.map(u=>({id:u.id,name:u.name,email:u.email,avatar:u.avatar,created:u.created})));
});

app.listen(PORT, ()=> console.log("RedRak API running on port",PORT));
