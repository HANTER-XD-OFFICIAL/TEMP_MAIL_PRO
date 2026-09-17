# Temp Mail Pro — 24/7 Official Web Application & Telegram Bot
**Lead Developer:** MD RASEL (Hanter XD Official)  
**Support Email:** [hanterxdofficial@gmail.com](mailto:hanterxdofficial@gmail.com)  
**Official Bot:** `@TEMPMAILPRO34_bot` ([https://t.me/TEMPMAILPRO34_bot](https://t.me/TEMPMAILPRO34_bot))  
**Official Website:** Hosted directly at your server root (`/`)  
**Security:** Token Vault Encrypted XOR Protection  

---

## ⚡ Complete Feature Parity with Android App
The official web version is located in `telegram-bot/public` and is automatically served at the root URL:
- **Active Disposable Mailbox**: Live email address with 1-click copy, QR code scanner modal, and status indicators.
- **10-Minute Countdown Timer & Extensions**: Real-time ticker with `+10m`, `+5m`, and `Reset` buttons.
- **Full Action Toolbar**: Random email generator, Custom mailbox creator, Domain Hub (30+ domains), Refresh, Saved Accounts Vault, and Panic Shredder.
- **Live Inbox & Smart OTP Hero**: Auto-refreshes every 8 seconds, detects 4-8 digit OTP codes (Facebook, Telegram, Google, WhatsApp, etc.), with 1-click copy cards.
- **Gmail-Style Email Reader Modal**: HTML Rich View & Plain Text view, full headers, copy body, delete, and forward to Telegram.
- **Multi-Language Engine**: 8 Languages supported with instant switching (English, বাংলা, Español, العربية, हिन्दी, Русский, Português, اردو).
- **Dark/Light Mode & Audio Notifications**: Modern Material Design 3 and Web Audio API synthesized chimes.
- **Developer Help Desk**: Direct messaging form delivering directly to MD RASEL's Telegram in real time.
- **Android APK Download**: Instant link to download `TempMailPro_v2.6.0.apk`.

---

## 🚀 How to Deploy on Render.com (Step-by-Step)

### Step 1: Sign Up / Sign In on Render
1. Visit [https://render.com](https://render.com)
2. Click **Sign in with GitHub**.

### Step 2: Create a New Web Service
1. In the Render Dashboard, click **New +** (top right) and select **Web Service**.
2. Select **Build and deploy from a Git repository**.
3. Choose your repository: `https://github.com/HANTER-XD-OFFICIAL/TEMP_MAIL_PRO`.

### Step 3: Configure Service Details
Fill in the configuration fields:

| Configuration Field | Value |
| :--- | :--- |
| **Name** | `tempmail-pro-bot` |
| **Region** | `Singapore` (or Frankfurt / Oregon) |
| **Branch** | `main` |
| **Root Directory** | `telegram-bot` *(Mandatory)* |
| **Runtime** | `Node` |
| **Build Command** | `npm install` |
| **Start Command** | `npm start` |
| **Instance Type** | `Free` ($0/month) |

### Step 4: Add Environment Variables
Under **Environment Variables**, click **Add Environment Variable** and add:
- **Key:** `BOT_TOKEN` *(Optional: code already includes encrypted token fallback, or paste your secret token here)*  
  **Value:** `[Your Secret Telegram Bot Token]`
- **Key:** `PORT`  
  **Value:** `3000`

### Step 5: Click Deploy
Click **Deploy Web Service**. Render will install dependencies and start the bot:
```text
🚀 Temp Mail Pro Bot Server running on port 3000
🤖 Bot Username: @TEMPMAILPRO34_bot
👨‍💻 Developer: MD RASEL (Hanter XD Official)
```

---

## 🕒 How to Keep the Bot 24/7 Alive (Zero Sleep)
Render's free tier sleeps after 15 minutes of inactivity. To prevent sleeping:
1. Go to [https://uptimerobot.com](https://uptimerobot.com) or [https://cron-job.org](https://cron-job.org) (100% Free).
2. Create a new monitor:
   - **Type:** `HTTP(s)`
   - **URL:** `https://your-render-subdomain.onrender.com/health`
   - **Interval:** `Every 5 minutes`
3. Save the monitor. Your bot will remain online 24/7 without stopping or sleeping!
