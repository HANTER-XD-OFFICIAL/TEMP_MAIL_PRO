const express = require('express');
const TelegramBot = require('node-telegram-bot-api');
const axios = require('axios');
require('dotenv').config();

// ==========================================
// 1. CONFIGURATION & CONSTANTS
// ==========================================
const BOT_TOKEN = process.env.BOT_TOKEN || '8659662216:AAHfxsv6-XG3k2K75lMWfidI10T2KeGEXwI';
const PORT = process.env.PORT || 3000;
const DEVELOPER_NAME = 'MD RASEL';
const DEVELOPER_PROFILE = 'https://www.facebook.com/md.rasel.7.8.2.3.4';
const WHATSAPP_CONTACT = 'https://wa.me/8801882278234';
const TELEGRAM_CHANNEL = 'https://t.me/HANTER_XD_OFFICIAL';
const GITHUB_REPO = 'https://github.com/HANTER-XD-OFFICIAL/TEMP_MAIL_PRO';
const APK_DOWNLOAD_URL = 'https://github.com/HANTER-XD-OFFICIAL/TEMP_MAIL_PRO/releases/tag/v2.6.0TempMailPro';

// In-Memory Storage for Active User Sessions (ChatId -> Mail Data)
const userSessions = new Map();

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

// Suppress and handle unhandled polling errors gracefully
bot.on('polling_error', (error) => {
  console.error('[Polling Error]', error.code, error.message);
});

// ==========================================
// 3. SECMAIL API HELPER FUNCTIONS
// ==========================================
// Domains supported by SecMail API
const AVAILABLE_DOMAINS = [
  '1secmail.com',
  '1secmail.org',
  '1secmail.net',
  'kzccv.com',
  'qiott.com',
  'wuuvo.com',
  'icznn.com'
];

function generateRandomString(length = 8) {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

// Extract 4-8 digit OTP codes automatically
function extractOtp(text) {
  if (!text) return null;
  const otpRegex = /(?:code|otp|verification|pin|passcode|confirm)[\s\w:]{0,20}?(\b\d{4,8}\b)/i;
  const match = text.match(otpRegex);
  if (match && match[1]) {
    return match[1];
  }
  // Fallback standalone 6-digit number
  const standAloneMatch = text.match(/\b\d{6}\b/);
  return standAloneMatch ? standAloneMatch[0] : null;
}

// Generate a new temporary mailbox
async function generateMailbox(customLogin = null, customDomain = null) {
  const login = customLogin || generateRandomString(9);
  const domain = customDomain || AVAILABLE_DOMAINS[Math.floor(Math.random() * AVAILABLE_DOMAINS.length)];
  const email = `${login}@${domain}`;
  return { login, domain, email };
}

// Fetch messages from 1secmail
async function getInboxMessages(login, domain) {
  try {
    const url = `https://www.1secmail.com/api/v1/?action=getMessages&login=${encodeURIComponent(login)}&domain=${encodeURIComponent(domain)}`;
    const res = await axios.get(url, { timeout: 10000 });
    return Array.isArray(res.data) ? res.data : [];
  } catch (err) {
    console.error(`[SecMail Fetch Error]`, err.message);
    return [];
  }
}

// Fetch message details
async function getMessageDetails(login, domain, id) {
  try {
    const url = `https://www.1secmail.com/api/v1/?action=readMessage&login=${encodeURIComponent(login)}&domain=${encodeURIComponent(domain)}&id=${id}`;
    const res = await axios.get(url, { timeout: 10000 });
    return res.data;
  } catch (err) {
    console.error(`[SecMail Read Error]`, err.message);
    return null;
  }
}

// ==========================================
// 4. TELEGRAM BOT COMMANDS & INTERACTION
// ==========================================

// /start Command
bot.onText(/\/start/, async (msg) => {
  const chatId = msg.chat.id;
  const firstName = msg.from.first_name || 'User';

  const welcomeMessage = `
⚡ <b>Welcome ${firstName} to Temp Mail Pro Official Bot!</b>

🛡️ <b>Developed by:</b> <a href="${DEVELOPER_PROFILE}">${DEVELOPER_NAME}</a> (Hanter XD Official)
🤖 <b>Bot Username:</b> @TEMPMAILPRO34_bot
📱 <b>Android App v2.6.0:</b> <a href="${APK_DOWNLOAD_URL}">Download APK Here</a>

With this bot, you can generate 100% free anonymous disposable email addresses 24/7, and receive instant <b>live OTP codes & verification emails</b> for Facebook, Telegram, TikTok, Google, and any online service!

💡 <b>Your Telegram Chat ID:</b> <code>${chatId}</code>
<i>(Link this Chat ID in Temp Mail Pro Android App to forward OTPs automatically)</i>
`;

  const keyboard = {
    inline_keyboard: [
      [
        { text: '⚡ Generate New Email', callback_data: 'GEN_NEW_MAIL' },
        { text: '📬 Check Inbox', callback_data: 'CHECK_INBOX' }
      ],
      [
        { text: '🔄 Auto Refresh (OTP)', callback_data: 'AUTO_REFRESH' },
        { text: '🗑️ Reset Session', callback_data: 'RESET_MAIL' }
      ],
      [
        { text: '📢 Telegram Channel', url: TELEGRAM_CHANNEL },
        { text: '💬 Developer WhatsApp', url: WHATSAPP_CONTACT }
      ],
      [
        { text: '📥 Download Android APK (v2.6.0)', url: APK_DOWNLOAD_URL }
      ]
    ]
  };

  await bot.sendMessage(chatId, welcomeMessage, {
    parse_mode: 'HTML',
    disable_web_page_preview: true,
    reply_markup: keyboard
  });
});

// /new or /generate Command
bot.onText(/\/(new|generate)/, async (msg) => {
  await handleGenerateEmail(msg.chat.id);
});

// /inbox or /check Command
bot.onText(/\/(inbox|check)/, async (msg) => {
  await handleCheckInbox(msg.chat.id);
});

// /id or /myid Command
bot.onText(/\/(id|myid)/, async (msg) => {
  const chatId = msg.chat.id;
  await bot.sendMessage(chatId, `🆔 <b>Your Telegram Chat ID:</b> <code>${chatId}</code>\n\nUse this ID in the Temp Mail Pro app to enable instant OTP forwarding.`, {
    parse_mode: 'HTML'
  });
});

// /developer Command
bot.onText(/\/developer/, async (msg) => {
  const chatId = msg.chat.id;
  const devText = `
👨‍💻 <b>Developer Information:</b>

👑 <b>Lead Developer:</b> ${DEVELOPER_NAME}
🌐 <b>Facebook:</b> <a href="${DEVELOPER_PROFILE}">MD RASEL Profile</a>
💬 <b>WhatsApp:</b> <a href="${WHATSAPP_CONTACT}">+8801882278234</a>
📢 <b>Telegram Channel:</b> <a href="${TELEGRAM_CHANNEL}">@HANTER_XD_OFFICIAL</a>
📂 <b>GitHub:</b> <a href="${GITHUB_REPO}">Hanter XD Repositories</a>
📱 <b>Temp Mail Pro APK:</b> <a href="${APK_DOWNLOAD_URL}">Download v2.6.0 APK</a>
`;
  await bot.sendMessage(chatId, devText, {
    parse_mode: 'HTML',
    disable_web_page_preview: true
  });
});

// ==========================================
// 5. CALLBACK QUERY HANDLERS (BUTTON CLICKS)
// ==========================================
bot.on('callback_query', async (query) => {
  const chatId = query.message.chat.id;
  const data = query.data;

  try {
    if (data === 'GEN_NEW_MAIL') {
      await bot.answerCallbackQuery(query.id, { text: 'Generating new email...' });
      await handleGenerateEmail(chatId);
    } else if (data === 'CHECK_INBOX' || data === 'AUTO_REFRESH') {
      await bot.answerCallbackQuery(query.id, { text: 'Checking inbox...' });
      await handleCheckInbox(chatId);
    } else if (data === 'RESET_MAIL') {
      userSessions.delete(chatId);
      await bot.answerCallbackQuery(query.id, { text: 'Session cleared' });
      await bot.sendMessage(chatId, '🗑️ Your previous email session has been cleared. Send /new or tap Generate to create a fresh inbox.');
    } else if (data.startsWith('READ_MSG_')) {
      const msgId = data.replace('READ_MSG_', '');
      await bot.answerCallbackQuery(query.id, { text: 'Loading message...' });
      await handleReadMessage(chatId, msgId);
    }
  } catch (err) {
    console.error('[Callback Error]', err.message);
  }
});

// Generate email action
async function handleGenerateEmail(chatId) {
  const mailbox = await generateMailbox();
  userSessions.set(chatId, mailbox);

  const text = `
⚡ <b>Your Active Disposable Email is Ready!</b>

✉️ <b>Email Address:</b>
<code>${mailbox.email}</code> <i>(Tap to copy)</i>

🌐 <b>Domain Node:</b> <code>${mailbox.domain}</code>
⏱️ <b>Uptime:</b> Active (24/7 Live Node)
🛡️ <b>Privacy:</b> 100% Anonymous & Secure

<i>Paste this email into any website or app. When an OTP or email arrives, tap 'Check Inbox & OTP' below.</i>
`;

  const keyboard = {
    inline_keyboard: [
      [
        { text: '📬 Check Inbox & OTP', callback_data: 'CHECK_INBOX' }
      ],
      [
        { text: '🔄 Generate Another Email', callback_data: 'GEN_NEW_MAIL' },
        { text: '📥 Android App', url: APK_DOWNLOAD_URL }
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
    await bot.sendMessage(chatId, '⚠️ You do not have an active email session yet! Tap the button below to generate one.', {
      reply_markup: {
        inline_keyboard: [
          [{ text: '⚡ Generate New Email', callback_data: 'GEN_NEW_MAIL' }]
        ]
      }
    });
    return;
  }

  const messages = await getInboxMessages(session.login, session.domain);

  if (!messages || messages.length === 0) {
    const emptyText = `
📭 <b>Inbox is currently empty!</b>

✉️ <b>Target Mailbox:</b> <code>${session.email}</code>
⏱️ <i>No incoming messages or OTP codes received yet. Please trigger the verification from the target website or app and check again in 10-15 seconds.</i>
`;
    await bot.sendMessage(chatId, emptyText, {
      parse_mode: 'HTML',
      reply_markup: {
        inline_keyboard: [
          [{ text: '🔄 Refresh Inbox', callback_data: 'CHECK_INBOX' }],
          [{ text: '⚡ Generate New Email', callback_data: 'GEN_NEW_MAIL' }]
        ]
      }
    });
    return;
  }

  // Found messages
  let listText = `📬 <b>Found ${messages.length} Message(s)!</b>\n✉️ <b>Mailbox:</b> <code>${session.email}</code>\n\n`;

  const inlineButtons = [];

  for (const m of messages.slice(0, 5)) {
    const fromSender = m.from || 'Unknown Sender';
    const subj = m.subject || 'No Subject';
    const date = m.date || '';

    listText += `🔹 <b>From:</b> ${fromSender}\n📝 <b>Subject:</b> ${subj}\n⏰ <b>Time:</b> ${date}\n\n`;

    inlineButtons.push([
      { text: `📖 Read: ${subj.substring(0, 25)}`, callback_data: `READ_MSG_${m.id}` }
    ]);
  }

  inlineButtons.push([
    { text: '🔄 Refresh Inbox', callback_data: 'CHECK_INBOX' },
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
    await bot.sendMessage(chatId, '⚠️ Active session not found. Please create a new temporary mailbox.');
    return;
  }

  const detail = await getMessageDetails(session.login, session.domain, msgId);
  if (!detail) {
    await bot.sendMessage(chatId, '❌ Failed to load message content. Please try again.');
    return;
  }

  const sender = detail.from || 'Unknown Sender';
  const subject = detail.subject || 'No Subject';
  const bodyText = detail.textBody || detail.body || '';
  const otp = extractOtp(bodyText) || extractOtp(subject);

  let fullMsg = `
📬 <b>Temp Mail Pro — Message Details</b>

👤 <b>From:</b> <code>${sender}</code>
📝 <b>Subject:</b> <b>${subject}</b>
⏰ <b>Date:</b> ${detail.date || 'N/A'}
`;

  if (otp) {
    fullMsg += `
⚡━━━━━━━━━━━━━━━━━━━━⚡
🔑 <b>DETECTED OTP / SECURITY CODE:</b>
👉 <code>${otp}</code> 👈 <i>(Tap code to copy)</i>
⚡━━━━━━━━━━━━━━━━━━━━⚡
`;
  }

  // Trim long text for telegram limits
  const cleanBody = bodyText.replace(/<[^>]*>?/gm, '').trim();
  const previewBody = cleanBody.length > 800 ? cleanBody.substring(0, 800) + '...\n<i>(Truncated for length)</i>' : cleanBody;

  fullMsg += `\n📄 <b>Message Content:</b>\n${previewBody || '<i>No text content found</i>'}`;

  const keyboard = {
    inline_keyboard: [
      [
        { text: '⬅️ Back to Inbox', callback_data: 'CHECK_INBOX' },
        { text: '🔄 Refresh', callback_data: 'CHECK_INBOX' }
      ]
    ]
  };

  await bot.sendMessage(chatId, fullMsg, {
    parse_mode: 'HTML',
    reply_markup: keyboard
  });
}

// ==========================================
// 6. EXPRESS WEB SERVER (KEEPS 24/7 ALIVE ON RENDER)
// ==========================================
const app = express();
app.use(express.json());

// Root Health Check Route
app.get('/', (req, res) => {
  res.json({
    status: 'ONLINE',
    service: 'Temp Mail Pro Telegram Bot',
    botUsername: '@TEMPMAILPRO34_bot',
    developer: DEVELOPER_NAME,
    uptime: `${Math.floor(process.uptime())} seconds`,
    timestamp: new Date().toISOString(),
    message: '24/7 Render Active Service by Hanter XD Official'
  });
});

// Health check endpoint for Cron / UptimeRobot / Cron-Job.org
app.get('/health', (req, res) => {
  res.status(200).send('OK');
});

// Optional Webhook endpoint for Android app to trigger Telegram alerts
app.post('/api/send-alert', async (req, res) => {
  try {
    const { chatId, message, otpCode } = req.body;
    if (!chatId || !message) {
      return res.status(400).json({ ok: false, error: 'Missing chatId or message' });
    }

    let alertText = message;
    if (otpCode) {
      alertText += `\n\n🔑 <b>OTP Code:</b> <code>${otpCode}</code>`;
    }

    await bot.sendMessage(chatId, alertText, { parse_mode: 'HTML' });
    return res.json({ ok: true });
  } catch (err) {
    return res.status(500).json({ ok: false, error: err.message });
  }
});

// Start listening
app.listen(PORT, () => {
  console.log(`=========================================`);
  console.log(`🚀 Temp Mail Pro Bot Server running on port ${PORT}`);
  console.log(`🤖 Bot Username: @TEMPMAILPRO34_bot`);
  console.log(`👨‍💻 Developer: ${DEVELOPER_NAME} (Hanter XD Official)`);
  console.log(`=========================================`);
});
