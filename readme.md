
# REDDARK

![author](https://img.shields.io/badge/author-wanzofc-red?style=for-the-badge&logo=github) ![status](https://img.shields.io/badge/status-learning-orange?style=for-the-badge) ![platform](https://img.shields.io/badge/platform-android%20%7C%20nodejs-blue?style=for-the-badge)

> reddark adalah project kecil yang masih sangat sederhana dan jauh dari kata sempurna. project ini dibuat hanya untuk belajar, eksplorasi, dan pengembangan pribadi supaya bisa terus berkembang sedikit demi sedikit. saya (wanzofc) bukan developer besar, hanya mencoba membuat sesuatu yang berguna.

---

### preview apk v2

link preview apk v2:
[![download apk](https://img.shields.io/badge/download-apk%20v2-success?style=flat&logo=android)](https://example.com/reddark-v2.apk)
`https://sfile.mobi/yMZIt1IoK29`

---

### fitur

berikut poin-poin fitur sederhana:

*   backend websocket dan http.
*   aplikasi android sederhana.
*   deadauther, evil twin attack
*   scanning wifi ssid/BSSID
*   DOXXING
*   scan QR
*   dll
*   struktur ringan agar mudah dipahami.

---

### instalasi backend

<details>
<summary><b>klik untuk melihat panduan setup</b></summary>
<br>

setup

```bash
npm install
```

jalankan server

```bash
node index.js
```

konfigurasi server wajib

```javascript
server.listen(PORT, '0.0.0.0', () => {
    console.log(`server with websocket is running. access it from your phone at http://ISI_IP_VPS_OR_LAPTOP_ANDA_NOTE_ROOT_DAN_WIFI_NIRKABEL:${PORT}`);
});
```

</details>

---

### pembuatan apk android

<details>
<summary><b>klik untuk melihat panduan build</b></summary>
<br>

build debug

```bash
./gradlew assembleDebug
```

build release

```bash
./gradlew assembleRelease
```

hasil apk akan muncul di:

`android_app/app/build/outputs/apk/`

</details>

---

### struktur direktori

```text
reddark/
├── backend/
│   ├── index.js
│   ├── package.json
│   └── ...
└── android_app/
    ├── app/
    ├── gradle/
    └── ...
```

---

### kelemahan & keterbatasan

*   fitur masih sangat sedikit
*   keamanan belum kuat
*   belum dioptimasi skala besar
*   dokumentasi seadanya

project ini masih belajar, jadi mohon maklum jika banyak kekurangan.

---

### lisensi

project ini menggunakan lisensi bebas namun dengan syarat:
anda bebas memakai, memperbaiki, mengubah atau memodifikasi, tetapi jangan menghapus nama author (**wanzofc**) dan jangan diperjualbelikan ulang tanpa izin.

---

### jasa pembuatan website & aplikasi (promo)

walaupun project ini sederhana, saya tetap membuka jasa pembuatan:

| layanan | keterangan |
| :--- | :--- |
| website company profile | untuk branding usaha |
| toko online sederhana | siap jualan |
| web admin & dashboard | manajemen data |
| api backend ringan | nodejs/php |
| aplikasi android basic | webview atau native ringan |

**hubungi**

![telegram](https://img.shields.io/badge/telegram-@maverick__dark-blue?style=flat&logo=telegram)
![whatsapp](https://img.shields.io/badge/whatsapp-0895--2634--6592-green?style=flat&logo=whatsapp)

telegram: `@maverick_dark`
whatsapp: `15489195889` atau `+62 895-2634-6592`

harga sangat terjangkau, cocok untuk pemula dan belajar.
website company profile

toko online sederhana

web admin & dashboard

api backend ringan

aplikasi android basic

```
