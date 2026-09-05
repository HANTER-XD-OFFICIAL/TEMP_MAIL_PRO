# Temp Mail Pro — 24/7 Telegram Bot & Web Service
**Lead Developer:** MD RASEL (Hanter XD Official)  
**Bot Username:** `@TEMPMAILPRO34_bot`  
**Direct Link:** [https://t.me/TEMPMAILPRO34_bot](https://t.me/TEMPMAILPRO34_bot)  
**Token:** `8659662216:AAHfxsv6-XG3k2K75lMWfidI10T2KeGEXwI`

---

## ⚡ Features
- **100% Free & Unlimited**: Generate disposable email inboxes on the fly.
- **Smart OTP Detection**: Automatically extracts and highlights 4–8 digit verification codes with a 1-tap copy button.
- **24/7 Uptime on Render**: Equipped with Express `/health` endpoints and keep-alive ping support.
- **Seamless Android App Integration**: Sync with the Temp Mail Pro v2.6.0 Android App using your Chat ID.

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
- **Key:** `BOT_TOKEN`  
  **Value:** `8659662216:AAHfxsv6-XG3k2K75lMWfidI10T2KeGEXwI`
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
