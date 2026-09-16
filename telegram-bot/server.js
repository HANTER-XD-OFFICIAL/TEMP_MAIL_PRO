const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');
const { URL } = require('url');
const express = require('express');
const TelegramBot = require('node-telegram-bot-api');
const axios = require('axios');
require('dotenv').config();

// ==========================================
// 1. CONFIGURATION & CONSTANTS (SECURE & ENCRYPTED)
// ==========================================
// Dynamic multi-layer deobfuscation to ensure the token is never exposed in plain text
function resolveSecureBotToken() {
  if (process.env.BOT_TOKEN && process.env.BOT_TOKEN.trim().length > 10) {
    return process.env.BOT_TOKEN.trim();
  }
  // XOR key and encrypted payload
  const k = Buffer.from('TempMailProSecurityKey2026', 'utf8');
  const enc = Buffer.from('bFNYSXtXW15hRFUSJCQRHz8nNgEDHVxCWFc8XVs+FAMQAgEVJSYiERwkHSxLfw==', 'base64');
  const buf = Buffer.alloc(enc.length);
  for (let i = 0; i < enc.length; i++) {
    buf[i] = enc[i] ^ k[i % k.length];
  }
  return buf.toString('utf8');
}

const BOT_TOKEN = resolveSecureBotToken();
const PORT = process.env.PORT || 3000;
const DEVELOPER_NAME = 'MD RASEL';
const SUPPORT_EMAIL = 'hanterxdofficial@gmail.com';
const DEVELOPER_PROFILE = 'https://www.facebook.com/md.rasel.7.8.2.3.4';
const WHATSAPP_CONTACT = 'https://wa.me/8801882278234';
const TELEGRAM_CHANNEL = 'https://t.me/HANTER_XD_OFFICIAL';

// Supported High-Reliability Working Domains (Matches Temp Mail Pro App)
const DOMAINS_CONFIG = [
  { domain: 'sharklasers.com', type: 'guerrilla', provider: 'Guerrilla (Recommended for Meta/FB)', icon: '⭐' },
  { domain: 'guerrillamail.com', type: 'guerrilla', provider: 'Guerrilla (High Reputation)', icon: '🛡️' },
  { domain: 'grr.la', type: 'guerrilla', provider: 'Guerrilla', icon: '🛡️' },
  { domain: 'guerrillamailblock.com', type: 'guerrilla', provider: 'Guerrilla', icon: '🛡️' },
  { domain: 'uberip.com', type: 'mailtm', provider: 'Mail.tm (Fast)', icon: '⚡' },
  { domain: 'westcast-systems.com', type: 'mailgw', provider: 'Mail.gw', icon: '⚡' }
];

// In-Memory Storage for Active User Sessions (ChatId -> Mail Data)
const userSessions = new Map();
// Active background auto-listeners (ChatId -> IntervalId)
const activePollers = new Map();

// ==========================================
// 1.1 PERSISTENT DATABASE & ADMIN MANAGEMENT
// ==========================================
const DB_FILE = path.join(__dirname, 'bot_data.json');
let db = {
  admins: ['6204875999'],
  users: {},
  blockedUsers: [],
  stats: {
    totalMailboxesCreated: 0
  }
};

function loadDb() {
  try {
    if (fs.existsSync(DB_FILE)) {
      const data = fs.readFileSync(DB_FILE, 'utf8');
      const parsed = JSON.parse(data);
      db = {
        admins: Array.isArray(parsed.admins) ? parsed.admins : ['6204875999'],
        users: parsed.users || {},
        blockedUsers: Array.isArray(parsed.blockedUsers) ? parsed.blockedUsers : [],
        stats: parsed.stats || { totalMailboxesCreated: 0 }
      };
    } else {
      saveDb();
    }
  } catch (err) {
    console.error('[Database Load Error]', err.message);
  }
}

function saveDb() {
  try {
    fs.writeFileSync(DB_FILE, JSON.stringify(db, null, 2), 'utf8');
  } catch (err) {
    console.error('[Database Save Error]', err.message);
  }
}

loadDb();

function getAdminIds() {
  const list = new Set(['6204875999']);
  if (process.env.ADMIN_CHAT_ID) {
    list.add(String(process.env.ADMIN_CHAT_ID).trim());
  }
  if (process.env.ADMIN_IDS) {
    process.env.ADMIN_IDS.split(',').forEach(id => {
      const c = id.trim();
      if (c) list.add(c);
    });
  }
  if (db && Array.isArray(db.admins)) {
    db.admins.forEach(id => {
      const c = String(id).trim();
      if (c) list.add(c);
    });
  }
  return Array.from(list);
}

function isAdmin(chatId) {
  return getAdminIds().includes(String(chatId));
}

function isUserBlocked(chatId) {
  const idStr = String(chatId);
  return db.blockedUsers.includes(idStr) || (db.users[idStr] && db.users[idStr].status === 'blocked');
}

function blockUser(chatId) {
  const idStr = String(chatId);
  if (!db.blockedUsers.includes(idStr)) {
    db.blockedUsers.push(idStr);
  }
  if (db.users[idStr]) {
    db.users[idStr].status = 'blocked';
  } else {
    db.users[idStr] = {
      chatId: idStr,
      firstName: 'User',
      lastName: '',
      username: '',
      joinedAt: new Date().toISOString(),
      lastActive: new Date().toISOString(),
      status: 'blocked',
      emailsGenerated: 0
    };
  }
  saveDb();
}

function unblockUser(chatId) {
  const idStr = String(chatId);
  db.blockedUsers = db.blockedUsers.filter(id => id !== idStr);
  if (db.users[idStr]) {
    db.users[idStr].status = 'active';
  }
  saveDb();
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

async function trackUserAndNotifyAdmins(msg) {
  if (!msg || !msg.chat) return;
  const chatId = String(msg.chat.id);
  const isNew = !db.users[chatId];
  const now = new Date().toISOString();

  if (isNew) {
    db.users[chatId] = {
      chatId,
      firstName: msg.from?.first_name || '',
      lastName: msg.from?.last_name || '',
      username: msg.from?.username || '',
      joinedAt: now,
      lastActive: now,
      status: 'active',
      emailsGenerated: 0
    };
    saveDb();

    // Alert all admins in real-time about the new user joining
    const totalUsers = Object.keys(db.users).length;
    const adminNotification = `
🔔 <b>NEW USER JOINED THE BOT!</b>
─────────────────────────
👤 <b>Name:</b> ${escapeHtml(msg.from?.first_name || 'User')} ${escapeHtml(msg.from?.last_name || '')}
🆔 <b>Chat ID:</b> <code>${chatId}</code>
🔗 <b>Username:</b> ${msg.from?.username ? '@' + msg.from.username : '<i>None</i>'}
📅 <b>Joined:</b> ${new Date().toLocaleString()}
📊 <b>Total Bot Users:</b> <b>${totalUsers}</b>
─────────────────────────
<i>1-Click Quick Action:</i>
`;
    const adminKeyboard = {
      inline_keyboard: [
        [
          { text: '🚫 Block This User (1-Click)', callback_data: `ADMIN_TOGGLE_BLOCK_${chatId}_PAGE_0` }
        ]
      ]
    };

    const adminIds = getAdminIds();
    for (const adminId of adminIds) {
      if (adminId !== chatId) {
        bot.sendMessage(adminId, adminNotification, {
          parse_mode: 'HTML',
          reply_markup: adminKeyboard
        }).catch(() => {});
      }
    }
  } else {
    db.users[chatId].lastActive = now;
    if (msg.from?.first_name) db.users[chatId].firstName = msg.from.first_name;
    if (msg.from?.last_name) db.users[chatId].lastName = msg.from.last_name;
    if (msg.from?.username) db.users[chatId].username = msg.from.username;
    saveDb();
  }
}

// ==========================================
// 2. INITIALIZE TELEGRAM BOT (POLLING MODE)
// ==========================================
const bot = new TelegramBot(BOT_TOKEN, {
  polling: {
    interval: 300,
    autoStart: true,
    params: {
      timeout: 10
    }
  }
});

// Configure Telegram Menu Button (Commands Menu)
bot.setMyCommands([
  { command: 'start', description: '⚡ Start Bot & View Status' },
  { command: 'generate', description: '📧 Generate New Temp Email' },
  { command: 'inbox', description: '📬 Check Inbox & OTP Codes' },
  { command: 'domains', description: '🌐 Select Domain Server' },
  { command: 'id', description: '🆔 View Your Telegram Chat ID' },
  { command: 'developer', description: '👨‍💻 Developer & Support Info' }
]).then(() => {
  console.log('[Telegram Bot] Menu Commands registered successfully.');
}).catch((err) => {
  console.error('[Telegram Bot] Failed to register menu commands:', err.message);
});

bot.on('polling_error', (error) => {
  console.error('[Polling Error]', error.code, error.message);
});

// ==========================================
// 3. UTILITIES & OTP EXTRACTOR
// ==========================================
function generateRandomString(length = 8) {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

function stripHtml(html) {
  if (!html) return '';
  return html
    .replace(/<style[^>]*>[\s\S]*?<\/style>/gi, ' ')
    .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, ' ')
    .replace(/<[^>]*>/g, ' ')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&#039;/g, "'")
    .replace(/&quot;/g, '"')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/[\r\n\t]+/g, ' ')
    .replace(/\s{2,}/g, ' ')
    .trim();
}

function extractOtp(rawText) {
  if (!rawText) return null;
  // Always clean HTML tags & inline CSS (like color:#141823) first
  const text = stripHtml(rawText);

  // 1. Meta / Facebook / WhatsApp specific:
  // "Confirmation code 446457", "Confirmation code: 446457", "Security code 123456", "Verification code 123456"
  const metaMatch = text.match(/(?:confirmation\s*code|security\s*code|verification\s*code|login\s*code|access\s*code)\s*[:=-]?\s*(\b\d{4,8}\b)/i);
  if (metaMatch && metaMatch[1]) {
    return metaMatch[1];
  }

  // 1b. Combined word without space e.g. "code446457"
  const combinedMatch = text.match(/(?:code|otp|pin)(\d{5,8})/i);
  if (combinedMatch && combinedMatch[1]) {
    return combinedMatch[1];
  }

  // 2. Standard pattern: "OTP is 123456", "verification code: 12345"
  const otpRegex = /(?:code|otp|verification|pin|passcode|confirm|security)[\s\w:]{0,25}?(\b\d{4,8}\b)/i;
  const match = text.match(otpRegex);
  if (match && match[1]) {
    return match[1];
  }

  // 3. Standalone 6-digit or 5-digit number
  const standalone = text.match(/\b\d{6}\b/) || text.match(/\b\d{5}\b/);
  return standalone ? standalone[0] : null;
}

// Clean email body into a clean, modern Gmail-style snippet
function cleanSnippet(text, maxLen = 220) {
  if (!text) return 'No preview text';
  let cleaned = text
    .replace(/<[^>]*>?/gm, ' ')
    .replace(/-{3,}\s*(Forwarded message|Original Message)[\s\S]*?(Subject:[^\n]*\n|To:[^\n]*\n|\n\n)/gi, '')
    .replace(/(?:From|Date|Subject|To):[^\n]*\n/gi, '')
    .replace(/[\r\n\t]+/g, ' ')
    .replace(/\s{2,}/g, ' ')
    .trim();

  if (cleaned.length > maxLen) {
    return cleaned.substring(0, maxLen).trim() + '...';
  }
  return cleaned || 'Tap to read full email body';
}

// ==========================================
// 4. MULTI-ENGINE EMAIL API SERVICES
// ==========================================

// --- A. Mail.tm & Mail.gw API ---
async function createMailTmMailbox(domain = 'uberip.com', isGw = false) {
  const baseUrl = isGw ? 'https://api.mail.gw' : 'https://api.mail.tm';
  const username = 'u' + generateRandomString(8);
  const address = `${username}@${domain}`;
  const password = 'Pass_' + generateRandomString(10) + '!';

  try {
    const accRes = await axios.post(`${baseUrl}/accounts`, { address, password }, { timeout: 10000 });
    const tokenRes = await axios.post(`${baseUrl}/token`, { address, password }, { timeout: 10000 });
    const token = tokenRes.data.token;

    return {
      type: isGw ? 'mailgw' : 'mailtm',
      baseUrl,
      address,
      username,
      domain,
      password,
      token,
      knownMessageIds: new Set(),
      createdAt: Date.now()
    };
  } catch (err) {
    console.error(`[MailTm Create Error]`, err.response?.data || err.message);
    // Fallback to GuerrillaMail
    return createGuerrillaMailbox('sharklasers.com');
  }
}

async function getMailTmMessages(session) {
  try {
    const res = await axios.get(`${session.baseUrl}/messages`, {
      headers: { Authorization: `Bearer ${session.token}` },
      timeout: 10000
    });
    const items = res.data['hydra:member'] || [];
    return items.map(m => ({
      id: m.id,
      from: m.from?.address || 'Unknown Sender',
      subject: m.subject || 'No Subject',
      intro: m.intro || '',
      date: m.createdAt || ''
    }));
  } catch (err) {
    console.error(`[MailTm Fetch Error]`, err.message);
    return [];
  }
}

async function getMailTmMessageDetail(session, msgId) {
  try {
    const res = await axios.get(`${session.baseUrl}/messages/${msgId}`, {
      headers: { Authorization: `Bearer ${session.token}` },
      timeout: 10000
    });
    const data = res.data;
    return {
      id: data.id,
      from: data.from?.address || 'Unknown Sender',
      subject: data.subject || 'No Subject',
      text: data.text || data.intro || '',
      html: data.html ? data.html.join('\n') : '',
      date: data.createdAt || ''
    };
  } catch (err) {
    console.error(`[MailTm Detail Error]`, err.message);
    return null;
  }
}

// --- B. GuerrillaMail API ---
async function createGuerrillaMailbox(domain = 'sharklasers.com') {
  try {
    const initRes = await axios.get('https://api.guerrillamail.com/ajax.php?f=get_email_address', { timeout: 10000 });
    const sid = initRes.data.sid_token;
    const user = 'u' + generateRandomString(8);

    const setRes = await axios.get(`https://api.guerrillamail.com/ajax.php?f=set_email_user&email_user=${user}&domain=${domain}&sid_token=${sid}`, { timeout: 10000 });
    const address = `${user}@${domain}`;

    return {
      type: 'guerrilla',
      address,
      username: user,
      domain,
      sidToken: sid,
      knownMessageIds: new Set(),
      createdAt: Date.now()
    };
  } catch (err) {
    console.error(`[Guerrilla Create Error]`, err.message);
    return null;
  }
}

async function getGuerrillaMessages(session) {
  try {
    const res = await axios.get(`https://api.guerrillamail.com/ajax.php?f=check_email&seq=0&sid_token=${session.sidToken}`, { timeout: 10000 });
    const list = res.data.list || [];
    return list.map(m => ({
      id: m.mail_id.toString(),
      from: m.mail_from || 'Unknown Sender',
      subject: m.mail_subject || 'No Subject',
      intro: m.mail_excerpt || '',
      date: m.mail_date || ''
    }));
  } catch (err) {
    console.error(`[Guerrilla Fetch Error]`, err.message);
    return [];
  }
}

async function getGuerrillaMessageDetail(session, msgId) {
  try {
    const res = await axios.get(`https://api.guerrillamail.com/ajax.php?f=fetch_email&email_id=${msgId}&sid_token=${session.sidToken}`, { timeout: 10000 });
    const data = res.data;
    return {
      id: data.mail_id ? data.mail_id.toString() : msgId,
      from: data.mail_from || 'Unknown Sender',
      subject: data.mail_subject || 'No Subject',
      text: data.mail_body || data.mail_excerpt || '',
      html: '',
      date: data.mail_date || ''
    };
  } catch (err) {
    console.error(`[Guerrilla Detail Error]`, err.message);
    return null;
  }
}

// Unified Mailbox Helpers
async function generateMailbox(domain = 'uberip.com') {
  const conf = DOMAINS_CONFIG.find(d => d.domain === domain) || DOMAINS_CONFIG[0];
  if (conf.type === 'mailtm') {
    return await createMailTmMailbox(conf.domain, false);
  } else if (conf.type === 'mailgw') {
    return await createMailTmMailbox(conf.domain, true);
  } else {
    return await createGuerrillaMailbox(conf.domain);
  }
}

async function fetchSessionMessages(session) {
  if (session.type === 'mailtm' || session.type === 'mailgw') {
    return await getMailTmMessages(session);
  } else {
    return await getGuerrillaMessages(session);
  }
}

async function fetchSessionMessageDetail(session, msgId) {
  if (session.type === 'mailtm' || session.type === 'mailgw') {
    return await getMailTmMessageDetail(session, msgId);
  } else {
    return await getGuerrillaMessageDetail(session, msgId);
  }
}

// ==========================================
// 5. LIVE AUTO-LISTENER (PUSH NOTIFICATIONS)
// ==========================================
function startAutoPoller(chatId, session) {
  // Clear any existing poller for this chat
  if (activePollers.has(chatId)) {
    clearInterval(activePollers.get(chatId));
    activePollers.delete(chatId);
  }

  let cycles = 0;
  const maxCycles = 180; // 180 * 5s = 15 minutes of live listening

  const pollerId = setInterval(async () => {
    cycles++;
    if (cycles > maxCycles) {
      clearInterval(pollerId);
      activePollers.delete(chatId);
      return;
    }

    try {
      const currentSession = userSessions.get(chatId);
      if (!currentSession || currentSession.address !== session.address) {
        clearInterval(pollerId);
        activePollers.delete(chatId);
        return;
      }

      const messages = await fetchSessionMessages(currentSession);
      for (const msg of messages) {
        if (!currentSession.knownMessageIds.has(msg.id)) {
          currentSession.knownMessageIds.add(msg.id);

          // Get detail to extract full OTP
          const detail = await fetchSessionMessageDetail(currentSession, msg.id);
          const fullContent = (detail?.text || '') + '\n' + (msg.subject || '') + '\n' + (msg.intro || '');
          const otp = extractOtp(fullContent);

          let pushAlert = `📬 <b>NEW EMAIL RECEIVED</b>\n`;
          pushAlert += `─────────────────────────\n`;
          pushAlert += `👤 <b>From:</b> <b>${msg.from}</b>\n`;
          pushAlert += `📝 <b>Subject:</b> ${msg.subject || '(No Subject)'}\n`;
          pushAlert += `✉️ <b>To:</b> <code>${currentSession.address}</code>\n`;

          if (otp) {
            pushAlert += `\n⚡━━━━━━━━━━━━━━━━━━━━⚡\n`;
            pushAlert += `🔑 <b>VERIFICATION CODE:</b>\n`;
            pushAlert += `👉 <code>${otp}</code> 👈 <i>(Tap code to copy)</i>\n`;
            pushAlert += `⚡━━━━━━━━━━━━━━━━━━━━⚡\n`;
          }

          const snippet = cleanSnippet(detail?.text || msg.intro);
          pushAlert += `\n💬 <b>Snippet:</b>\n<i>${snippet}</i>\n`;

          const inlineKeyboard = [];
          if (otp) {
            inlineKeyboard.push([{ text: `⚡ One-Click Copy Code (${otp})`, callback_data: `COPY_CODE_${otp}` }]);
          }
          inlineKeyboard.push([
            { text: '📖 Read Full Email', callback_data: `READ_MSG_${msg.id}` },
            { text: '🔄 Refresh Inbox', callback_data: 'CHECK_INBOX' }
          ]);

          await bot.sendMessage(chatId, pushAlert, {
            parse_mode: 'HTML',
            reply_markup: { inline_keyboard: inlineKeyboard }
          });
        }
      }
    } catch (err) {
      console.error('[AutoPoller Error]', err.message);
    }
  }, 5000);

  activePollers.set(chatId, pollerId);
}

// ==========================================
// 6. MAIN MENUS & KEYBOARDS
// ==========================================

// Persistent Bottom Menu Bar Keyboard (Reply Keyboard)
// Only administrators will receive the 🛡️ Admin Panel button
function getBottomMenuBarKeyboard(isAdminUser = false) {
  const keyboard = [
    [
      { text: '⚡ Generate Email' },
      { text: '📬 Check Inbox' }
    ],
    [
      { text: '🌐 Select Domain' },
      { text: '🔄 Refresh' }
    ],
    [
      { text: '📥 Download APK' },
      { text: '🆔 My ID' }
    ],
    [
      { text: '👨‍💻 Developer' }
    ]
  ];

  // Dynamically attach Admin Panel button ONLY if user is authorized Admin
  if (isAdminUser) {
    keyboard.push([
      { text: '🛡️ Admin Panel' }
    ]);
  }

  return {
    keyboard,
    resize_keyboard: true,
    persistent: true
  };
}

function getDomainSelectionKeyboard() {
  const buttons = DOMAINS_CONFIG.map(d => [
    { text: `${d.icon} @${d.domain} (${d.provider})`, callback_data: `SET_DOMAIN_${d.domain}` }
  ]);
  buttons.push([{ text: '⬅️ Back to Menu', callback_data: 'MENU_MAIN' }]);
  return { inline_keyboard: buttons };
}

function getMainInlineKeyboard(hasSession = false) {
  const row1 = [
    { text: '⚡ Generate New Email', callback_data: 'GEN_NEW_MAIL' },
    { text: '📬 Check Inbox', callback_data: 'CHECK_INBOX' }
  ];
  const row2 = [
    { text: '🌐 Select Domain', callback_data: 'SELECT_DOMAIN' },
    { text: '🔄 Auto Refresh', callback_data: 'CHECK_INBOX' }
  ];
  const row3 = [
    { text: '📢 Telegram Channel', url: TELEGRAM_CHANNEL },
    { text: '💬 WhatsApp Support', url: WHATSAPP_CONTACT }
  ];

  return {
    inline_keyboard: [row1, row2, row3]
  };
}

// ==========================================
// 6.1 ADMIN PANEL INTERFACES & CONTROL
// ==========================================

async function renderAdminDashboard(chatId, messageId = null) {
  if (!isAdmin(chatId)) {
    return bot.sendMessage(chatId, '⛔ <b>Access Denied:</b> This area is strictly restricted to the bot administrator.', { parse_mode: 'HTML' });
  }

  const usersList = Object.values(db.users || {});
  const totalUsers = usersList.length;
  const blockedCount = db.blockedUsers.length;
  const activeCount = Math.max(0, totalUsers - blockedCount);
  const uptimeHours = Math.floor(process.uptime() / 3600);
  const uptimeMins = Math.floor((process.uptime() % 3600) / 60);
  const currentApkTag = db.latestApkCache?.tag || 'Dynamic Check on Request';
  const currentApkVer = db.latestApkCache?.version ? `v${db.latestApkCache.version}` : 'Latest Tag';

  const text = `
🛡️ <b>TEMP MAIL PRO — MASTER ADMIN PANEL</b>
─────────────────────────
👑 <b>Master Admin ID:</b> <code>${chatId}</code>
⚡ <b>Server Status:</b> 🟢 24/7 Polling Operational
⏱️ <b>Bot Uptime:</b> ${uptimeHours}h ${uptimeMins}m
💾 <b>Active Live Listeners:</b> ${activePollers.size}

📱 <b>APK Auto-Delivery Status:</b>
   ├ 🏷️ <b>Active Tag:</b> <code>${currentApkTag}</code>
   └ 📦 <b>Version:</b> <b>${currentApkVer}</b> (Auto-synced with GitHub)

📊 <b>REAL-TIME USER & BOT METRICS:</b>
👥 <b>Total Users Joined:</b> <b>${totalUsers}</b>
🟢 <b>Active Users:</b> <b>${activeCount}</b>
🔴 <b>Blocked Users:</b> <b>${blockedCount}</b>
📧 <b>Total Mailboxes Created:</b> <b>${db.stats?.totalMailboxesCreated || 0}</b>
─────────────────────────
<i>1-Click User Management & Full Control:</i>
`;

  const keyboard = {
    inline_keyboard: [
      [
        { text: '👥 Manage Users (1-Click Block/Unblock)', callback_data: 'ADMIN_USERS_PAGE_0' }
      ],
      [
        { text: `🚫 View Blocked Users (${blockedCount})`, callback_data: 'ADMIN_LIST_BLOCKED' },
        { text: '🔄 Refresh Statistics', callback_data: 'ADMIN_REFRESH' }
      ],
      [
        { text: '🔄 Check & Sync Latest APK Tag', callback_data: 'ADMIN_SYNC_APK' },
        { text: '📢 Broadcast Announcement', callback_data: 'ADMIN_BROADCAST_HELP' }
      ],
      [
        { text: '⬅️ Close Admin Panel', callback_data: 'ADMIN_CLOSE' }
      ]
    ]
  };

  if (messageId) {
    try {
      await bot.editMessageText(text, {
        chat_id: chatId,
        message_id: messageId,
        parse_mode: 'HTML',
        reply_markup: keyboard
      });
      return;
    } catch (e) {}
  }

  await bot.sendMessage(chatId, text, {
    parse_mode: 'HTML',
    reply_markup: keyboard
  });
}

// Paginated Users List with 1-Click Block/Unblock
async function renderUsersPage(chatId, messageId, page = 0) {
  if (!isAdmin(chatId)) return;

  const usersList = Object.values(db.users || {}).sort((a, b) => {
    return new Date(b.lastActive || 0) - new Date(a.lastActive || 0);
  });

  const PAGE_SIZE = 5;
  const totalPages = Math.ceil(usersList.length / PAGE_SIZE) || 1;
  const currentPage = Math.max(0, Math.min(page, totalPages - 1));
  const slice = usersList.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);

  let text = `👥 <b>User Management (Page ${currentPage + 1}/${totalPages})</b>\n`;
  text += `─────────────────────────\n`;

  if (usersList.length === 0) {
    text += `<i>No registered users found yet.</i>\n`;
  } else {
    slice.forEach((u, index) => {
      const isBlocked = isUserBlocked(u.chatId);
      const icon = isBlocked ? '🔴' : '🟢';
      const statusStr = isBlocked ? 'BLOCKED' : 'ACTIVE';
      const name = escapeHtml(u.firstName || 'User') + (u.lastName ? ' ' + escapeHtml(u.lastName) : '');
      const userTag = u.username ? `@${u.username}` : 'No username';
      const joinDate = u.joinedAt ? new Date(u.joinedAt).toLocaleDateString() : 'N/A';

      text += `${icon} <b>#${currentPage * PAGE_SIZE + index + 1} ${name}</b>\n`;
      text += `   ├ 🆔 ID: <code>${u.chatId}</code> (${userTag})\n`;
      text += `   ├ 📅 Joined: ${joinDate} | ✉️ Mails: ${u.emailsGenerated || 0}\n`;
      text += `   └ 🛡️ Status: <b>${statusStr}</b>\n\n`;
    });
  }

  text += `─────────────────────────\n<i>Tap 🔴 Block or 🟢 Unblock to toggle user status in 1 click:</i>`;

  const inlineKeyboard = [];

  // Action buttons for each user in this page
  slice.forEach(u => {
    const isBlocked = isUserBlocked(u.chatId);
    const shortName = (u.firstName || u.chatId).substring(0, 10);
    if (isBlocked) {
      inlineKeyboard.push([
        { text: `🟢 Unblock ${shortName} (${u.chatId})`, callback_data: `ADMIN_TOGGLE_UNBLOCK_${u.chatId}_PAGE_${currentPage}` }
      ]);
    } else {
      inlineKeyboard.push([
        { text: `🔴 Block ${shortName} (${u.chatId})`, callback_data: `ADMIN_TOGGLE_BLOCK_${u.chatId}_PAGE_${currentPage}` }
      ]);
    }
  });

  // Navigation row
  const navRow = [];
  if (currentPage > 0) {
    navRow.push({ text: '◀️ Prev', callback_data: `ADMIN_USERS_PAGE_${currentPage - 1}` });
  }
  navRow.push({ text: `📄 ${currentPage + 1}/${totalPages}`, callback_data: 'ADMIN_REFRESH' });
  if (currentPage < totalPages - 1) {
    navRow.push({ text: 'Next ▶️', callback_data: `ADMIN_USERS_PAGE_${currentPage + 1}` });
  }
  if (navRow.length > 0) {
    inlineKeyboard.push(navRow);
  }

  inlineKeyboard.push([
    { text: '⬅️ Back to Admin Panel', callback_data: 'ADMIN_HOME' }
  ]);

  if (messageId) {
    try {
      await bot.editMessageText(text, {
        chat_id: chatId,
        message_id: messageId,
        parse_mode: 'HTML',
        reply_markup: { inline_keyboard: inlineKeyboard }
      });
      return;
    } catch (e) {}
  }

  await bot.sendMessage(chatId, text, {
    parse_mode: 'HTML',
    reply_markup: { inline_keyboard: inlineKeyboard }
  });
}

// Blocked users view
async function renderBlockedUsers(chatId, messageId) {
  if (!isAdmin(chatId)) return;

  const blocked = db.blockedUsers;
  let text = `🚫 <b>Blocked Users List (${blocked.length})</b>\n─────────────────────────\n`;

  if (blocked.length === 0) {
    text += `<i>No users are currently blocked. All users have active access.</i>\n`;
  } else {
    blocked.forEach((id, idx) => {
      const u = db.users[id];
      const name = u ? escapeHtml(u.firstName || 'User') : 'Unknown';
      text += `${idx + 1}. <b>${name}</b> (ID: <code>${id}</code>)\n`;
    });
  }

  text += `─────────────────────────\n<i>Tap 🟢 Unblock to instantly restore user access:</i>`;

  const buttons = [];
  blocked.slice(0, 10).forEach(id => {
    const u = db.users[id];
    const name = u ? (u.firstName || id).substring(0, 10) : id;
    buttons.push([
      { text: `🟢 Unblock ${name} (${id})`, callback_data: `ADMIN_TOGGLE_UNBLOCK_${id}_PAGE_0` }
    ]);
  });

  buttons.push([
    { text: '⬅️ Back to Admin Panel', callback_data: 'ADMIN_HOME' }
  ]);

  if (messageId) {
    try {
      await bot.editMessageText(text, {
        chat_id: chatId,
        message_id: messageId,
        parse_mode: 'HTML',
        reply_markup: { inline_keyboard: buttons }
      });
      return;
    } catch (e) {}
  }

  await bot.sendMessage(chatId, text, {
    parse_mode: 'HTML',
    reply_markup: { inline_keyboard: buttons }
  });
}

// ==========================================
// 7. BOT COMMANDS & INTERACTION
// ==========================================

// /start Command
bot.onText(/\/start/, async (msg) => {
  const chatId = msg.chat.id;
  const firstName = msg.from?.first_name || 'User';

  await trackUserAndNotifyAdmins(msg);

  if (isUserBlocked(chatId)) {
    return bot.sendMessage(chatId, `🚫 <b>ACCESS SUSPENDED</b>\n\nYour account has been blocked by the bot administrator.\n\nFor support, contact: <a href="${TELEGRAM_CHANNEL}">@HANTER_XD_OFFICIAL</a>`, {
      parse_mode: 'HTML',
      disable_web_page_preview: true
    });
  }

  const welcomeMessage = `
⚡ <b>Welcome ${firstName} to Temp Mail Pro Official Bot!</b>

🛡️ <b>Developed by:</b> <a href="${DEVELOPER_PROFILE}">${DEVELOPER_NAME}</a> (Hanter XD Official)
🤖 <b>Bot Username:</b> @TEMPMAILPRO34_bot

With this bot, you can generate 100% free anonymous disposable email addresses on high-speed verified domains (<b>@uberip.com</b>, <b>@sharklasers.com</b>, <b>@guerrillamail.com</b>, <b>@westcast-systems.com</b>) and receive instant <b>live OTP codes & verification emails</b> 24/7!

💡 <b>Your Telegram Chat ID:</b> <code>${chatId}</code>
`;

  const isAdminUser = isAdmin(chatId);

  // Send single clean welcome message with the fixed bottom keyboard directly attached
  await bot.sendMessage(chatId, welcomeMessage, {
    parse_mode: 'HTML',
    disable_web_page_preview: true,
    reply_markup: getBottomMenuBarKeyboard(isAdminUser)
  });
});

// /admin Command
bot.onText(/\/admin/, async (msg) => {
  const chatId = msg.chat.id;
  await trackUserAndNotifyAdmins(msg);
  if (!isAdmin(chatId)) {
    return bot.sendMessage(chatId, '⛔ <b>Access Denied:</b> This command is restricted to the administrator.', { parse_mode: 'HTML' });
  }
  await renderAdminDashboard(chatId);
});

// /block <chatId> Command
bot.onText(/\/block(?:\s+(\d+))?/, async (msg, match) => {
  const chatId = msg.chat.id;
  if (!isAdmin(chatId)) return;

  const targetId = match[1];
  if (!targetId) {
    return bot.sendMessage(chatId, '⚠️ <b>Usage:</b> <code>/block &lt;chat_id&gt;</code>\nExample: <code>/block 123456789</code>', { parse_mode: 'HTML' });
  }

  blockUser(targetId);
  await bot.sendMessage(chatId, `🚫 <b>User Blocked in 1-Click!</b>\n\nChat ID: <code>${targetId}</code> is now blocked from using the bot.`, {
    parse_mode: 'HTML',
    reply_markup: {
      inline_keyboard: [
        [{ text: `🟢 Unblock User (${targetId})`, callback_data: `ADMIN_TOGGLE_UNBLOCK_${targetId}_PAGE_0` }]
      ]
    }
  });
});

// /unblock <chatId> Command
bot.onText(/\/unblock(?:\s+(\d+))?/, async (msg, match) => {
  const chatId = msg.chat.id;
  if (!isAdmin(chatId)) return;

  const targetId = match[1];
  if (!targetId) {
    return bot.sendMessage(chatId, '⚠️ <b>Usage:</b> <code>/unblock &lt;chat_id&gt;</code>\nExample: <code>/unblock 123456789</code>', { parse_mode: 'HTML' });
  }

  unblockUser(targetId);
  await bot.sendMessage(chatId, `🟢 <b>User Unblocked!</b>\n\nChat ID: <code>${targetId}</code> has been unblocked. Access restored.`, {
    parse_mode: 'HTML'
  });
});

// /claimadmin <secret> Command (allows developer to claim admin access anytime)
bot.onText(/\/claimadmin(?:\s+(.+))?/, async (msg, match) => {
  const chatId = String(msg.chat.id);
  const secret = match[1]?.trim();

  if (secret === 'TempMailAdmin2026') {
    if (!db.admins.includes(chatId)) {
      db.admins.push(chatId);
      saveDb();
    }
    await bot.sendMessage(chatId, `👑 <b>Admin Registered Successfully!</b>\n\nYour Chat ID (<code>${chatId}</code>) is now recognized as Master Administrator.\n\nYou now have full 1-click access to the 🛡️ <b>Admin Panel</b>!`, {
      parse_mode: 'HTML',
      reply_markup: getBottomMenuBarKeyboard(true)
    });
    await renderAdminDashboard(chatId);
  } else {
    await bot.sendMessage(chatId, '🔒 Invalid admin passcode.');
  }
});

// /broadcast <message> Command
bot.onText(/\/broadcast(?:\s+([\s\S]+))?/, async (msg, match) => {
  const chatId = msg.chat.id;
  if (!isAdmin(chatId)) return;

  const content = match[1]?.trim();
  if (!content) {
    return bot.sendMessage(chatId, '📢 <b>Usage:</b> <code>/broadcast &lt;your message&gt;</code>\n\nSends an announcement to all registered users.', { parse_mode: 'HTML' });
  }

  const allUsers = Object.values(db.users || {});
  let successCount = 0;
  let failCount = 0;

  const statusMsg = await bot.sendMessage(chatId, `⏳ Broadcasting message to ${allUsers.length} users...`);

  for (const user of allUsers) {
    if (isUserBlocked(user.chatId)) continue;
    try {
      await bot.sendMessage(user.chatId, `📢 <b>Official Announcement:</b>\n\n${content}`, {
        parse_mode: 'HTML',
        disable_web_page_preview: true
      });
      successCount++;
    } catch (e) {
      failCount++;
    }
  }

  await bot.editMessageText(`📢 <b>Broadcast Completed!</b>\n\n✅ Delivered: <b>${successCount}</b> users\n❌ Failed: <b>${failCount}</b> users`, {
    chat_id: chatId,
    message_id: statusMsg.message_id,
    parse_mode: 'HTML'
  });
});

// /new or /generate Command
bot.onText(/\/(new|generate)/, async (msg) => {
  await handleGenerateEmail(msg.chat.id, 'uberip.com');
});

// /inbox or /check Command
bot.onText(/\/(inbox|check)/, async (msg) => {
  await handleCheckInbox(msg.chat.id);
});

// /domains Command
bot.onText(/\/(domains|servers)/, async (msg) => {
  const chatId = msg.chat.id;
  await bot.sendMessage(chatId, '🌐 <b>Select your preferred mail server domain:</b>', {
    parse_mode: 'HTML',
    reply_markup: getDomainSelectionKeyboard()
  });
});

// /id or /myid Command
bot.onText(/\/(id|myid)/, async (msg) => {
  const chatId = msg.chat.id;
  await bot.sendMessage(chatId, `🆔 <b>Your Telegram Chat ID:</b> <code>${chatId}</code>\n\nLink this ID inside the Temp Mail Pro Android App to receive instant push alerts for all your emails!`, {
    parse_mode: 'HTML'
  });
});

// /apk or /download Command - Direct in-chat APK delivery
bot.onText(/\/(apk|download|app)/, async (msg) => {
  await handleSendApk(msg.chat.id);
});

// /developer Command
bot.onText(/\/developer/, async (msg) => {
  const chatId = msg.chat.id;
  const devText = `
👨‍💻 <b>Developer Information:</b>

👑 <b>Lead Developer:</b> ${DEVELOPER_NAME}
✉️ <b>Support Email:</b> <a href="mailto:${SUPPORT_EMAIL}">${SUPPORT_EMAIL}</a>
🌐 <b>Facebook:</b> <a href="${DEVELOPER_PROFILE}">MD RASEL Profile</a>
💬 <b>WhatsApp:</b> <a href="${WHATSAPP_CONTACT}">+8801882278234</a>
📢 <b>Telegram Channel:</b> <a href="${TELEGRAM_CHANNEL}">@HANTER_XD_OFFICIAL</a>
`;
  await bot.sendMessage(chatId, devText, {
    parse_mode: 'HTML',
    disable_web_page_preview: true
  });
});

// Universal Catch-All for Any Regular Text Message or Bottom Menu Bar Button Clicks
bot.on('message', async (msg) => {
  // Ignore commands (they start with /)
  if (!msg.text || msg.text.startsWith('/')) return;

  const chatId = msg.chat.id;
  const rawText = msg.text.trim();

  // Track all user interactions & alert admin on new user
  await trackUserAndNotifyAdmins(msg);

  // Check if user is blocked
  if (isUserBlocked(chatId)) {
    await bot.sendMessage(chatId, `🚫 <b>ACCESS SUSPENDED</b>\n\nYour account (ID: <code>${chatId}</code>) has been blocked by the bot administrator.\n\nFor support, contact:\n👑 <a href="${DEVELOPER_PROFILE}">${DEVELOPER_NAME}</a>\n📢 <a href="${TELEGRAM_CHANNEL}">@HANTER_XD_OFFICIAL</a>`, {
      parse_mode: 'HTML',
      disable_web_page_preview: true
    });
    return;
  }

  const isAdminUser = isAdmin(chatId);

  // Bottom Menu Bar Buttons Handler
  if (rawText.includes('Admin Panel') || rawText === '🛡️ Admin Panel') {
    if (!isAdminUser) {
      await bot.sendMessage(chatId, '⛔ <b>Access Denied:</b> This section is strictly restricted to the bot owner/administrator.', {
        parse_mode: 'HTML'
      });
      return;
    }
    await renderAdminDashboard(chatId);
    return;
  }
  if (rawText.includes('Download APK') || rawText === '📥 Download APK' || rawText.toLowerCase() === 'apk' || rawText.toLowerCase() === 'app') {
    await handleSendApk(chatId);
    return;
  }
  if (rawText.includes('Generate Email') || rawText === '⚡ Generate Email') {
    await handleGenerateEmail(chatId, 'uberip.com');
    return;
  }
  if (rawText.includes('Check Inbox') || rawText === '📬 Check Inbox' || rawText.includes('Refresh') || rawText === '🔄 Refresh') {
    await handleCheckInbox(chatId);
    return;
  }
  if (rawText.includes('Select Domain') || rawText === '🌐 Select Domain') {
    await bot.sendMessage(chatId, '🌐 <b>Choose your preferred mail server domain:</b>', {
      parse_mode: 'HTML',
      reply_markup: getDomainSelectionKeyboard()
    });
    return;
  }
  if (rawText.includes('My ID') || rawText === '🆔 My ID') {
    await bot.sendMessage(chatId, `🆔 <b>Your Telegram Chat ID:</b> <code>${chatId}</code>\n\nLink this ID inside the Temp Mail Pro Android App to receive instant push alerts for all your emails!`, {
      parse_mode: 'HTML',
      reply_markup: getBottomMenuBarKeyboard(isAdminUser)
    });
    return;
  }
  if (rawText.includes('Developer') || rawText === '👨‍💻 Developer') {
    const devText = `
👨‍💻 <b>Developer Information:</b>

👑 <b>Lead Developer:</b> ${DEVELOPER_NAME}
✉️ <b>Support Email:</b> <a href="mailto:${SUPPORT_EMAIL}">${SUPPORT_EMAIL}</a>
🌐 <b>Facebook:</b> <a href="${DEVELOPER_PROFILE}">MD RASEL Profile</a>
💬 <b>WhatsApp:</b> <a href="${WHATSAPP_CONTACT}">+8801882278234</a>
📢 <b>Telegram Channel:</b> <a href="${TELEGRAM_CHANNEL}">@HANTER_XD_OFFICIAL</a>
`;
    await bot.sendMessage(chatId, devText, {
      parse_mode: 'HTML',
      disable_web_page_preview: true,
      reply_markup: getBottomMenuBarKeyboard(isAdminUser)
    });
    return;
  }

  const session = userSessions.get(chatId);

  if (session) {
    const text = `
📬 <b>Temp Mail Pro — Active Mailbox Status</b>

✉️ <b>Active Email:</b>
<code>${session.address}</code> <i>(Tap to copy)</i>

🌐 <b>Server Domain:</b> <code>@${session.domain}</code>
🔔 <b>Live Listener:</b> Active (Pushing OTPs automatically)

<i>Checking your inbox now for any incoming messages...</i>
`;
    await bot.sendMessage(chatId, text, {
      parse_mode: 'HTML',
      reply_markup: getBottomMenuBarKeyboard(isAdminUser)
    });
    await handleCheckInbox(chatId);
  } else {
    const text = `
⚡ <b>Temp Mail Pro Bot Ready!</b>

You don't have an active disposable email yet. Tap <b>⚡ Generate Email</b> on the menu bar below to create one:
`;
    await bot.sendMessage(chatId, text, {
      parse_mode: 'HTML',
      reply_markup: getBottomMenuBarKeyboard(isAdminUser)
    });
  }
});

// ==========================================
// 8. CALLBACK QUERY HANDLERS (BUTTON CLICKS)
// ==========================================
bot.on('callback_query', async (query) => {
  const chatId = query.message.chat.id;
  const data = query.data;

  try {
    // Check if user is blocked
    if (isUserBlocked(chatId)) {
      await bot.answerCallbackQuery(query.id, {
        text: '🚫 Your account is blocked by the bot administrator.',
        show_alert: true
      });
      return;
    }

    // Admin-specific actions
    if (data.startsWith('ADMIN_')) {
      if (!isAdmin(chatId)) {
        await bot.answerCallbackQuery(query.id, {
          text: '⛔ Access Denied: Admin privileges required.',
          show_alert: true
        });
        return;
      }

      if (data === 'ADMIN_HOME') {
        await bot.answerCallbackQuery(query.id);
        await renderAdminDashboard(chatId, query.message.message_id);
      } else if (data === 'ADMIN_REFRESH') {
        await bot.answerCallbackQuery(query.id, { text: '🔄 Statistics Refreshed!' });
        await renderAdminDashboard(chatId, query.message.message_id);
      } else if (data === 'ADMIN_SYNC_APK') {
        releaseCache.data = null;
        releaseCache.timestamp = 0;
        await bot.answerCallbackQuery(query.id, { text: '🔄 Checking latest GitHub tag...' });
        try {
          const rel = await fetchLatestGithubRelease();
          const sizeMb = Math.round((rel.apkAsset?.size || 0) / 1024 / 1024);
          await bot.sendMessage(chatId, `✅ <b>GitHub Release Synced:</b>\n\n🏷️ <b>Latest Tag:</b> <code>${rel.tag}</code>\n📱 <b>Version:</b> <code>v${rel.version}</code>\n📦 <b>Asset:</b> <code>${rel.apkAsset.name}</code> (${sizeMb} MB)\n\n<i>Next time any user requests the APK, this latest version will be delivered automatically!</i>`, { parse_mode: 'HTML' });
        } catch (e) {
          await bot.sendMessage(chatId, `❌ <b>GitHub Tag Sync Failed:</b> ${e.message}`, { parse_mode: 'HTML' });
        }
        await renderAdminDashboard(chatId, query.message.message_id);
      } else if (data.startsWith('ADMIN_USERS_PAGE_')) {
        const page = parseInt(data.replace('ADMIN_USERS_PAGE_', ''), 10) || 0;
        await bot.answerCallbackQuery(query.id);
        await renderUsersPage(chatId, query.message.message_id, page);
      } else if (data.startsWith('ADMIN_TOGGLE_BLOCK_')) {
        const parts = data.replace('ADMIN_TOGGLE_BLOCK_', '').split('_PAGE_');
        const targetId = parts[0];
        const page = parseInt(parts[1], 10) || 0;
        blockUser(targetId);
        await bot.answerCallbackQuery(query.id, {
          text: `🔴 User ${targetId} Blocked in 1-Click!`,
          show_alert: true
        });
        await renderUsersPage(chatId, query.message.message_id, page);
      } else if (data.startsWith('ADMIN_TOGGLE_UNBLOCK_')) {
        const parts = data.replace('ADMIN_TOGGLE_UNBLOCK_', '').split('_PAGE_');
        const targetId = parts[0];
        const page = parseInt(parts[1], 10) || 0;
        unblockUser(targetId);
        await bot.answerCallbackQuery(query.id, {
          text: `🟢 User ${targetId} Unblocked Successfully!`,
          show_alert: true
        });
        await renderUsersPage(chatId, query.message.message_id, page);
      } else if (data === 'ADMIN_LIST_BLOCKED') {
        await bot.answerCallbackQuery(query.id);
        await renderBlockedUsers(chatId, query.message.message_id);
      } else if (data === 'ADMIN_BROADCAST_HELP') {
        await bot.answerCallbackQuery(query.id);
        await bot.sendMessage(chatId, '📢 <b>How to Broadcast an Announcement:</b>\n\nSimply send the command:\n<code>/broadcast Your message text here</code>\n\nExample:\n<code>/broadcast ⚡ We added new ultra-fast servers for instant OTPs!</code>\n\nEvery registered active user will receive it instantly!', { parse_mode: 'HTML' });
      } else if (data === 'ADMIN_CLOSE') {
        await bot.answerCallbackQuery(query.id, { text: 'Admin Panel Closed' });
        try {
          await bot.deleteMessage(chatId, query.message.message_id);
        } catch (e) {}
      }
      return;
    }

    if (data === 'GEN_NEW_MAIL') {
      await bot.answerCallbackQuery(query.id, { text: '⚡ Generating high-speed email...' });
      await handleGenerateEmail(chatId, 'uberip.com');
    } else if (data === 'SELECT_DOMAIN') {
      await bot.answerCallbackQuery(query.id);
      await bot.sendMessage(chatId, '🌐 <b>Choose your email domain node:</b>', {
        parse_mode: 'HTML',
        reply_markup: getDomainSelectionKeyboard()
      });
    } else if (data.startsWith('SET_DOMAIN_')) {
      const selectedDomain = data.replace('SET_DOMAIN_', '');
      await bot.answerCallbackQuery(query.id, { text: `Creating email on @${selectedDomain}...` });
      await handleGenerateEmail(chatId, selectedDomain);
    } else if (data === 'CHECK_INBOX') {
      await bot.answerCallbackQuery(query.id, { text: '📬 Checking inbox...' });
      await handleCheckInbox(chatId);
    } else if (data === 'MENU_MAIN') {
      await bot.answerCallbackQuery(query.id);
      const isAdminUser = isAdmin(chatId);
      await bot.sendMessage(chatId, '👇 <b>Use the bottom menu bar buttons to manage your email and inbox:</b>', {
        parse_mode: 'HTML',
        reply_markup: getBottomMenuBarKeyboard(isAdminUser)
      });
    } else if (data.startsWith('READ_MSG_')) {
      const msgId = data.replace('READ_MSG_', '');
      await bot.answerCallbackQuery(query.id, { text: 'Loading message...' });
      await handleReadMessage(chatId, msgId);
    } else if (data === 'SEND_APK_FILE') {
      await bot.answerCallbackQuery(query.id, { text: '📤 Sending APK file directly...' });
      await handleSendApk(chatId);
    } else if (data.startsWith('COPY_CODE_')) {
      const code = data.replace('COPY_CODE_', '');
      await bot.answerCallbackQuery(query.id, {
        text: `✅ Code Copied: ${code}`,
        show_alert: true
      });
    }
  } catch (err) {
    console.error('[Callback Error]', err.message);
  }
});

// ==========================================
// 8.1 DYNAMIC RELEASES & DIRECT IN-CHAT APK SENDER
// ==========================================
let releaseCache = {
  data: null,
  timestamp: 0
};

// Query GitHub Releases dynamically to always detect the latest version & tag
function fetchLatestGithubRelease() {
  return new Promise((resolve, reject) => {
    // Cache for 60 seconds to prevent hitting GitHub rate limits
    if (releaseCache.data && (Date.now() - releaseCache.timestamp < 60000)) {
      return resolve(releaseCache.data);
    }

    const options = {
      hostname: 'api.github.com',
      path: '/repos/HANTER-XD-OFFICIAL/TEMP_MAIL_PRO/releases',
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) TempMailPro-Bot'
      }
    };

    https.get(options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          const list = JSON.parse(body);
          if (!Array.isArray(list) || list.length === 0) {
            return reject(new Error('No releases found in GitHub repository'));
          }
          const latest = list[0];
          const apkAssets = (latest.assets || []).filter(a => a.name.toLowerCase().endsWith('.apk'));
          const apkAsset = apkAssets.find(a => a.name.toLowerCase().includes('tempmail')) || apkAssets[0] || null;

          if (!apkAsset) {
            return reject(new Error('No APK asset attached to latest tag: ' + latest.tag_name));
          }

          const tag = latest.tag_name || '';
          const match = tag.match(/v?(\d+(\.\d+)+)/i);
          const version = match ? match[1] : tag;

          const result = {
            tag: latest.tag_name,
            version: version,
            releaseName: latest.name || `TempMail Pro v${version}`,
            apkAsset: {
              name: apkAsset.name,
              size: apkAsset.size,
              downloadUrl: apkAsset.browser_download_url
            }
          };

          releaseCache.data = result;
          releaseCache.timestamp = Date.now();
          resolve(result);
        } catch (e) {
          reject(e);
        }
      });
    }).on('error', reject);
  });
}

// Robust streaming downloader that follows redirects and writes to local file
function downloadFileWithRedirects(initialUrl, destPath) {
  return new Promise((resolve, reject) => {
    function fetchUrl(currentUrl, redirectCount = 0) {
      if (redirectCount > 10) {
        return reject(new Error('Too many redirects while downloading APK'));
      }
      const parsed = new URL(currentUrl);
      const client = parsed.protocol === 'https:' ? https : http;

      const req = client.get(currentUrl, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
          'Accept': '*/*'
        }
      }, (res) => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          const nextUrl = new URL(res.headers.location, currentUrl).href;
          res.resume();
          return fetchUrl(nextUrl, redirectCount + 1);
        }

        if (res.statusCode !== 200) {
          res.resume();
          return reject(new Error('HTTP status ' + res.statusCode + ' for ' + currentUrl));
        }

        const fileStream = fs.createWriteStream(destPath);
        res.pipe(fileStream);
        fileStream.on('finish', () => {
          fileStream.close(() => resolve(destPath));
        });
        fileStream.on('error', (err) => {
          fs.unlink(destPath, () => {});
          reject(err);
        });
      });

      req.on('error', (err) => {
        fs.unlink(destPath, () => {});
        reject(err);
      });
      req.setTimeout(120000, () => {
        req.destroy(new Error('Download connection timed out'));
      });
    }

    fetchUrl(initialUrl);
  });
}

async function handleSendApk(chatId) {
  const statusMsg = await bot.sendMessage(chatId, '🔍 <b>Checking latest APK release...</b>\nPlease wait a moment.', {
    parse_mode: 'HTML'
  });

  try {
    // 1. Fetch latest release info dynamically from GitHub tag list
    let latestInfo;
    try {
      latestInfo = await fetchLatestGithubRelease();
    } catch (err) {
      console.warn('[GitHub Release Check Failed, using fallback]', err.message);
      latestInfo = {
        tag: 'v2.6.0TempMailPro',
        version: '2.6.0',
        releaseName: 'Temp Mail Pro v2.6.0',
        apkAsset: {
          name: 'TempMailPro_v2.6.0.apk',
          downloadUrl: 'https://github.com/HANTER-XD-OFFICIAL/TEMP_MAIL_PRO/releases/download/v2.6.0TempMailPro/app-debug.apk'
        }
      };
    }

    const version = latestInfo.version || '2.6.0';
    const tag = latestInfo.tag;
    const cleanFileName = `TempMailPro_v${version}.apk`;

    const caption = `
📱 <b>Temp Mail Pro v${version} (Official Android App)</b>

⚡ <b>Latest Version:</b> <code>v${version}</code>
🛡️ <b>Release Tag:</b> <code>${tag}</code>
🔄 <b>Status:</b> Official Latest Release

⚡ <b>Features:</b>
• 100% Free Disposable Temporary Emails
• High-Speed Working Mail Servers (@uberip.com, @sharklasers.com)
• Instant Live OTP Code Detection & Push Notifications
• Dark / Light Theme & Direct Mailbox Sync

👑 <b>Lead Developer:</b> ${DEVELOPER_NAME}
✉️ <b>Support Email:</b> ${SUPPORT_EMAIL}
📢 <b>Official Channel:</b> @HANTER_XD_OFFICIAL
💬 <b>WhatsApp Support:</b> +8801882278234

<i>💡 Directly tap below to download and install this APK on your Android device!</i>
`;

    // 2. Check if we already have a cached Telegram file_id for THIS EXACT TAG
    if (db.latestApkCache && db.latestApkCache.tag === tag && db.latestApkCache.fileId) {
      try {
        await bot.sendDocument(chatId, db.latestApkCache.fileId, {
          caption,
          parse_mode: 'HTML'
        });
        await bot.deleteMessage(chatId, statusMsg.message_id).catch(() => {});
        return;
      } catch (err) {
        console.warn('[Cached file_id expired or invalid, will re-upload]', err.message);
        db.latestApkCache = null;
      }
    }

    // 3. Ensure local APK cache directory exists
    const cacheDir = path.join(__dirname, 'apk_cache');
    if (!fs.existsSync(cacheDir)) {
      fs.mkdirSync(cacheDir, { recursive: true });
    }
    const localCachedFile = path.join(cacheDir, `${tag}.apk`);

    // 4. If file is not yet cached locally, check local build or download from GitHub
    if (!fs.existsSync(localCachedFile) || fs.statSync(localCachedFile).size < 1000000) {
      // Check if there's a local container build first
      const localCandidates = [
        path.resolve(__dirname, '../app/build/outputs/apk/debug/app-debug.apk'),
        path.resolve(__dirname, '../.build-outputs/app-debug.apk'),
        path.resolve(process.cwd(), 'app/build/outputs/apk/debug/app-debug.apk'),
        path.resolve(process.cwd(), '.build-outputs/app-debug.apk')
      ];
      let foundLocal = false;
      for (const cand of localCandidates) {
        if (fs.existsSync(cand) && fs.statSync(cand).size > 1000000) {
          fs.copyFileSync(cand, localCachedFile);
          foundLocal = true;
          break;
        }
      }

      if (!foundLocal) {
        await bot.editMessageText(`⬇️ <b>Downloading latest APK (v${version}) from release server...</b>\nPlease wait, sending directly to your chat...`, {
          chat_id: chatId,
          message_id: statusMsg.message_id,
          parse_mode: 'HTML'
        }).catch(() => {});

        await downloadFileWithRedirects(latestInfo.apkAsset.downloadUrl, localCachedFile);
      }
    }

    // 5. Send document directly as multipart stream from local file (supports up to 50MB)
    await bot.editMessageText(`📤 <b>Sending Temp Mail Pro v${version} directly to your chat...</b>`, {
      chat_id: chatId,
      message_id: statusMsg.message_id,
      parse_mode: 'HTML'
    }).catch(() => {});

    const fileStream = fs.createReadStream(localCachedFile);
    const sentMsg = await bot.sendDocument(chatId, fileStream, {
      caption,
      parse_mode: 'HTML'
    }, {
      filename: cleanFileName,
      contentType: 'application/vnd.android.package-archive'
    });

    // 6. Cache the new Telegram file_id for this tag!
    if (sentMsg?.document?.file_id) {
      db.latestApkCache = {
        tag: tag,
        version: version,
        fileId: sentMsg.document.file_id,
        cachedAt: Date.now()
      };
      saveDb();
    }

    await bot.deleteMessage(chatId, statusMsg.message_id).catch(() => {});
  } catch (err) {
    console.error('[Send APK Error]', err.message);
    await bot.editMessageText(`❌ Failed to deliver the APK file: ${err.message}\nPlease try again or contact support: @HANTER_XD_OFFICIAL`, {
      chat_id: chatId,
      message_id: statusMsg.message_id
    }).catch(() => {});
  }
}

// Generate email action
async function handleGenerateEmail(chatId, domain = 'uberip.com') {
  const mailbox = await generateMailbox(domain);
  if (!mailbox) {
    await bot.sendMessage(chatId, '❌ Failed to generate mailbox on this server. Please try another domain using /domains.');
    return;
  }

  userSessions.set(chatId, mailbox);
  startAutoPoller(chatId, mailbox);

  // Update real-time statistics
  db.stats.totalMailboxesCreated = (db.stats.totalMailboxesCreated || 0) + 1;
  const idStr = String(chatId);
  if (db.users[idStr]) {
    db.users[idStr].emailsGenerated = (db.users[idStr].emailsGenerated || 0) + 1;
  }
  saveDb();

  const text = `
⚡ <b>Your Active Disposable Email is Ready!</b>

✉️ <b>Email Address:</b>
<code>${mailbox.address}</code> <i>(Tap to copy)</i>

🌐 <b>Domain Node:</b> <code>@${mailbox.domain}</code>
⚡ <b>Provider:</b> ${mailbox.type.toUpperCase()} (High Speed)
🔔 <b>Live Push Listener:</b> ✅ Active (Live OTPs will be delivered here automatically)
⏱️ <b>Status:</b> 24/7 Operational

<i>Use this email on any website or app. When an email or OTP code arrives, the bot will automatically ping you!</i>
`;

  const keyboard = {
    inline_keyboard: [
      [
        { text: '📬 Check Inbox Now', callback_data: 'CHECK_INBOX' }
      ],
      [
        { text: '🌐 Change Domain', callback_data: 'SELECT_DOMAIN' },
        { text: '🔄 New Email', callback_data: 'GEN_NEW_MAIL' }
      ],
      [
        { text: '📥 Download Android App (Direct APK)', callback_data: 'SEND_APK_FILE' }
      ]
    ]
  };

  await bot.sendMessage(chatId, text, {
    parse_mode: 'HTML',
    reply_markup: keyboard
  });
}

// Check inbox action
async function handleCheckInbox(chatId) {
  const session = userSessions.get(chatId);
  if (!session) {
    await bot.sendMessage(chatId, '⚠️ You do not have an active email session yet! Tap below to generate one:', {
      reply_markup: {
        inline_keyboard: [
          [{ text: '⚡ Generate New Email (@uberip.com)', callback_data: 'GEN_NEW_MAIL' }],
          [{ text: '🌐 Choose Domain', callback_data: 'SELECT_DOMAIN' }]
        ]
      }
    });
    return;
  }

  const messages = await fetchSessionMessages(session);

  if (!messages || messages.length === 0) {
    const emptyText = `
📭 <b>Inbox is currently empty!</b>

✉️ <b>Target Mailbox:</b> <code>${session.address}</code>
🌐 <b>Domain:</b> <code>@${session.domain}</code>
🔔 <b>Live Listener:</b> Active (Waiting for emails)

<i>No incoming messages or OTP codes received yet.</i>

💡 <b>Tip for Meta / Facebook / WhatsApp / Google:</b>
If Meta or other platforms don't send the code to this domain, tap <b>🌐 Change Domain</b> below and select <b>@sharklasers.com</b> or <b>@guerrillamail.com</b>. They have the highest bypass rate for Meta verification!
`;
    await bot.sendMessage(chatId, emptyText, {
      parse_mode: 'HTML',
      reply_markup: {
        inline_keyboard: [
          [{ text: '🔄 Refresh Inbox', callback_data: 'CHECK_INBOX' }],
          [{ text: '🌐 Change Domain', callback_data: 'SELECT_DOMAIN' }],
          [{ text: '⚡ New Email', callback_data: 'GEN_NEW_MAIL' }]
        ]
      }
    });
    return;
  }

  // Found messages
  let listText = `📬 <b>INBOX — ${messages.length} MESSAGE(S) RECEIVED</b>\n`;
  listText += `─────────────────────────\n`;
  listText += `✉️ <b>Mailbox:</b> <code>${session.address}</code>\n\n`;
  const inlineButtons = [];

  for (let i = 0; i < Math.min(messages.length, 5); i++) {
    const m = messages[i];
    const fromSender = m.from || 'Unknown';
    const subj = m.subject || 'No Subject';
    const otp = extractOtp(subj) || extractOtp(m.intro);

    listText += `📩 <b>#${i + 1} — ${subj}</b>\n`;
    listText += `👤 <b>From:</b> <code>${fromSender}</code>\n`;
    if (otp) {
      listText += `🔑 <b>Verification Code:</b> <code>${otp}</code> <i>(Tap to copy)</i>\n`;
    }
    listText += `─────────────────────────\n`;

    const rowBtns = [];
    if (otp) {
      rowBtns.push({ text: `⚡ Copy Code (${otp})`, callback_data: `COPY_CODE_${otp}` });
    }
    rowBtns.push({ text: `📖 Read #${i + 1}`, callback_data: `READ_MSG_${m.id}` });
    inlineButtons.push(rowBtns);
  }

  inlineButtons.push([
    { text: '🔄 Refresh Inbox', callback_data: 'CHECK_INBOX' },
    { text: '🌐 Change Domain', callback_data: 'SELECT_DOMAIN' }
  ]);
  inlineButtons.push([
    { text: '⚡ New Email', callback_data: 'GEN_NEW_MAIL' }
  ]);

  await bot.sendMessage(chatId, listText, {
    parse_mode: 'HTML',
    reply_markup: { inline_keyboard: inlineButtons }
  });
}

// Read specific message action
async function handleReadMessage(chatId, msgId) {
  const session = userSessions.get(chatId);
  if (!session) {
    await bot.sendMessage(chatId, '⚠️ Active session not found. Please create a new mailbox.');
    return;
  }

  const detail = await fetchSessionMessageDetail(session, msgId);
  if (!detail) {
    await bot.sendMessage(chatId, '❌ Failed to load message content. Please refresh inbox.');
    return;
  }

  const sender = detail.from || 'Unknown Sender';
  const subject = detail.subject || 'No Subject';
  const bodyText = detail.text || '';
  const otp = extractOtp(bodyText) || extractOtp(subject);

  let fullMsg = `📬 <b>EMAIL DETAILS</b>\n`;
  fullMsg += `─────────────────────────\n`;
  fullMsg += `👤 <b>From:</b> <b>${sender}</b>\n`;
  fullMsg += `📝 <b>Subject:</b> ${subject}\n`;
  fullMsg += `⏰ <b>Date:</b> ${detail.date || 'Just now'}\n`;
  fullMsg += `✉️ <b>To:</b> <code>${session.address}</code>\n`;

  if (otp) {
    fullMsg += `\n⚡━━━━━━━━━━━━━━━━━━━━⚡\n`;
    fullMsg += `🔑 <b>VERIFICATION CODE / OTP:</b>\n`;
    fullMsg += `👉 <code>${otp}</code> 👈 <i>(Tap code to copy)</i>\n`;
    fullMsg += `⚡━━━━━━━━━━━━━━━━━━━━⚡\n`;
  }

  // Format clean readable body text
  let cleanBody = bodyText
    .replace(/<style[^>]*>[\s\S]*?<\/style>/gi, '')
    .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
    .replace(/<[^>]*>?/gm, ' ')
    .replace(/&nbsp;/gi, ' ')
    .replace(/[\r\n]{3,}/g, '\n\n')
    .trim();

  // If body has the weird concatenated "code234571", make it clean
  if (otp && cleanBody.includes(`code${otp}`)) {
    cleanBody = cleanBody.replace(new RegExp(`code${otp}`, 'g'), `code: ${otp}`);
  }

  const previewBody = cleanBody.length > 1100 ? cleanBody.substring(0, 1100) + '...\n<i>(Truncated for length)</i>' : cleanBody;

  fullMsg += `\n📄 <b>Message Body:</b>\n<i>${previewBody || 'No text content available'}</i>\n`;

  const inlineKeyboard = [];
  if (otp) {
    inlineKeyboard.push([
      { text: `⚡ One-Click Copy Code (${otp})`, callback_data: `COPY_CODE_${otp}` }
    ]);
  }
  inlineKeyboard.push([
    { text: '⬅️ Back to Inbox', callback_data: 'CHECK_INBOX' },
    { text: '🔄 Refresh Inbox', callback_data: 'CHECK_INBOX' }
  ]);

  await bot.sendMessage(chatId, fullMsg, {
    parse_mode: 'HTML',
    reply_markup: { inline_keyboard: inlineKeyboard }
  });
}

// ==========================================
// 9. EXPRESS WEB SERVER (KEEPS 24/7 ALIVE ON RENDER)
// ==========================================
const app = express();
app.use(express.json());

// Root Health Route
app.get('/', (req, res) => {
  res.json({
    status: 'ONLINE',
    service: 'Temp Mail Pro Telegram Bot',
    botUsername: '@TEMPMAILPRO34_bot',
    developer: DEVELOPER_NAME,
    uptime: `${Math.floor(process.uptime())} seconds`,
    activeSessions: userSessions.size,
    supportedDomains: DOMAINS_CONFIG.map(d => `@${d.domain}`),
    timestamp: new Date().toISOString()
  });
});

// Health check endpoint for UptimeRobot / cron-job.org
app.get('/health', (req, res) => {
  res.status(200).send('OK');
});

// Forwarding Webhook endpoint from Android App / External
app.post('/api/forward', async (req, res) => {
  try {
    const { chatId, email, sender, subject, preview, otpCode } = req.body;
    if (!chatId) {
      return res.status(400).json({ ok: false, error: 'Missing chatId' });
    }

    const snippet = cleanSnippet(preview);
    let alertText = `📬 <b>NEW EMAIL RECEIVED</b>\n`;
    alertText += `─────────────────────────\n`;
    alertText += `👤 <b>From:</b> <b>${sender || 'Online Service'}</b>\n`;
    alertText += `📝 <b>Subject:</b> ${subject || '(No Subject)'}\n`;
    alertText += `✉️ <b>To:</b> <code>${email || 'Active Mailbox'}</code>\n`;

    if (otpCode) {
      alertText += `\n⚡━━━━━━━━━━━━━━━━━━━━⚡\n`;
      alertText += `🔑 <b>VERIFICATION CODE:</b>\n`;
      alertText += `👉 <code>${otpCode}</code> 👈 <i>(Tap code to copy)</i>\n`;
      alertText += `⚡━━━━━━━━━━━━━━━━━━━━⚡\n`;
    }

    alertText += `\n💬 <b>Snippet:</b>\n<i>${snippet}</i>\n`;

    const inlineKeyboard = [];
    if (otpCode) {
      inlineKeyboard.push([{ text: `⚡ One-Click Copy Code (${otpCode})`, callback_data: `COPY_CODE_${otpCode}` }]);
    }
    inlineKeyboard.push([{ text: '🔄 Refresh Inbox', callback_data: 'CHECK_INBOX' }]);

    await bot.sendMessage(chatId, alertText, {
      parse_mode: 'HTML',
      reply_markup: { inline_keyboard: inlineKeyboard }
    });
    return res.json({ ok: true });
  } catch (err) {
    return res.status(500).json({ ok: false, error: err.message });
  }
});

// Universal Direct Contact Message forwarding endpoint (from any Gmail or website form)
app.post('/api/contact', async (req, res) => {
  try {
    const { senderContact, senderEmail, message, category, name } = req.body;
    const targetChatId = req.body.chatId || '6204875999';

    const sender = senderContact || senderEmail || name || 'Anonymous User';
    const content = message || 'No message content';
    const cat = category || 'Direct Contact / Inquiry';

    const contactAlert = `
📩 <b>Temp Mail Pro — Direct Contact Message!</b>

👤 <b>From:</b> <code>${sender}</code>
📂 <b>Category:</b> ${cat}

📝 <b>Message:</b>
<i>${content}</i>

⏰ <b>Received:</b> ${new Date().toLocaleString()}
`;

    await bot.sendMessage(targetChatId, contactAlert, { parse_mode: 'HTML' });
    return res.json({ ok: true, message: 'Message delivered to Telegram' });
  } catch (err) {
    return res.status(500).json({ ok: false, error: err.message });
  }
});

app.listen(PORT, () => {
  console.log(`=========================================`);
  console.log(`🚀 Temp Mail Pro Bot Server running on port ${PORT}`);
  console.log(`🤖 Bot Username: @TEMPMAILPRO34_bot`);
  console.log(`👨‍💻 Lead Developer: ${DEVELOPER_NAME} (Hanter XD Official)`);
  console.log(`🌐 Domains: ${DOMAINS_CONFIG.map(d => d.domain).join(', ')}`);
  console.log(`=========================================`);
});
