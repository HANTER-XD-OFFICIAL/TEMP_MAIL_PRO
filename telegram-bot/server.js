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

// Supported High-Reliability Working Domains (Matches Temp Mail Pro App)
const DOMAINS_CONFIG = [
  { domain: 'uberip.com', type: 'mailtm', provider: 'Mail.tm', icon: '⚡' },
  { domain: 'westcast-systems.com', type: 'mailgw', provider: 'Mail.gw', icon: '⚡' },
  { domain: 'sharklasers.com', type: 'guerrilla', provider: 'Guerrilla', icon: '🛡️' },
  { domain: 'guerrillamail.com', type: 'guerrilla', provider: 'Guerrilla', icon: '🛡️' },
  { domain: 'grr.la', type: 'guerrilla', provider: 'Guerrilla', icon: '🛡️' },
  { domain: 'guerrillamailblock.com', type: 'guerrilla', provider: 'Guerrilla', icon: '🛡️' }
];

// In-Memory Storage for Active User Sessions (ChatId -> Mail Data)
const userSessions = new Map();
// Active background auto-listeners (ChatId -> IntervalId)
const activePollers = new Map();

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

function extractOtp(text) {
  if (!text) return null;
  const otpRegex = /(?:code|otp|verification|pin|passcode|confirm|security)[\s\w:]{0,25}?(\b\d{4,8}\b)/i;
  const match = text.match(otpRegex);
  if (match && match[1]) {
    return match[1];
  }
  const standalone = text.match(/\b\d{6}\b/);
  return standalone ? standalone[0] : null;
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

          let pushAlert = `🔔 <b>NEW EMAIL RECEIVED!</b>\n\n`;
          if (otp) {
            pushAlert += `⚡━━━━━━━━━━━━━━━━━━━━⚡\n`;
            pushAlert += `🔑 <b>DETECTED OTP / SECURITY CODE:</b>\n`;
            pushAlert += `👉 <code>${otp}</code> 👈 <i>(Tap code to copy)</i>\n`;
            pushAlert += `⚡━━━━━━━━━━━━━━━━━━━━⚡\n\n`;
          }

          pushAlert += `📬 <b>Mailbox:</b> <code>${currentSession.address}</code>\n`;
          pushAlert += `👤 <b>From:</b> <code>${msg.from}</code>\n`;
          pushAlert += `📝 <b>Subject:</b> <b>${msg.subject}</b>\n`;
          if (detail?.text) {
            const preview = detail.text.replace(/<[^>]*>?/gm, '').trim().substring(0, 300);
            pushAlert += `\n📄 <b>Preview:</b> <i>${preview}</i>\n`;
          }

          const inlineKeyboard = [
            [{ text: '📖 Read Full Email', callback_data: `READ_MSG_${msg.id}` }],
            [{ text: '🔄 Refresh Inbox', callback_data: 'CHECK_INBOX' }]
          ];

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
  const row4 = [
    { text: '📥 Download Android APK (v2.6.0)', url: APK_DOWNLOAD_URL }
  ];

  return {
    inline_keyboard: [row1, row2, row3, row4]
  };
}

// ==========================================
// 7. BOT COMMANDS & INTERACTION
// ==========================================

// /start Command
bot.onText(/\/start/, async (msg) => {
  const chatId = msg.chat.id;
  const firstName = msg.from?.first_name || 'User';

  const welcomeMessage = `
⚡ <b>Welcome ${firstName} to Temp Mail Pro Official Bot!</b>

🛡️ <b>Developed by:</b> <a href="${DEVELOPER_PROFILE}">${DEVELOPER_NAME}</a> (Hanter XD Official)
🤖 <b>Bot Username:</b> @TEMPMAILPRO34_bot
📱 <b>Android App v2.6.0:</b> <a href="${APK_DOWNLOAD_URL}">Download APK Here</a>

With this bot, you can generate 100% free anonymous disposable email addresses on high-speed verified domains (<b>@uberip.com</b>, <b>@sharklasers.com</b>, <b>@guerrillamail.com</b>, <b>@westcast-systems.com</b>) and receive instant <b>live OTP codes & verification emails</b> 24/7!

💡 <b>Your Telegram Chat ID:</b> <code>${chatId}</code>
<i>(Link this Chat ID in the Android App for auto-forwarding)</i>
`;

  await bot.sendMessage(chatId, welcomeMessage, {
    parse_mode: 'HTML',
    disable_web_page_preview: true,
    reply_markup: getMainInlineKeyboard()
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

// Universal Catch-All for Any Regular Text Message
bot.on('message', async (msg) => {
  // Ignore commands (they start with /)
  if (!msg.text || msg.text.startsWith('/')) return;

  const chatId = msg.chat.id;
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
    await bot.sendMessage(chatId, text, { parse_mode: 'HTML' });
    await handleCheckInbox(chatId);
  } else {
    const text = `
⚡ <b>Temp Mail Pro Bot Ready!</b>

You don't have an active disposable email yet. Tap below to generate one with <b>@uberip.com</b> or select your preferred domain:
`;
    await bot.sendMessage(chatId, text, {
      parse_mode: 'HTML',
      reply_markup: getMainInlineKeyboard()
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
      await bot.sendMessage(chatId, '⚡ <b>Temp Mail Pro Main Menu</b>', {
        parse_mode: 'HTML',
        reply_markup: getMainInlineKeyboard()
      });
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
async function handleGenerateEmail(chatId, domain = 'uberip.com') {
  const mailbox = await generateMailbox(domain);
  if (!mailbox) {
    await bot.sendMessage(chatId, '❌ Failed to generate mailbox on this server. Please try another domain using /domains.');
    return;
  }

  userSessions.set(chatId, mailbox);
  startAutoPoller(chatId, mailbox);

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
        { text: '📥 Android App v2.6.0', url: APK_DOWNLOAD_URL }
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

<i>No incoming messages or OTP codes received yet. Please submit the verification code request on your website/app.</i>
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
  let listText = `📬 <b>Found ${messages.length} Message(s)!</b>\n✉️ <b>Mailbox:</b> <code>${session.address}</code>\n\n`;
  const inlineButtons = [];

  for (const m of messages.slice(0, 5)) {
    const fromSender = m.from || 'Unknown';
    const subj = m.subject || 'No Subject';
    const otp = extractOtp(subj) || extractOtp(m.intro);

    listText += `🔹 <b>From:</b> ${fromSender}\n📝 <b>Subject:</b> ${subj}\n`;
    if (otp) {
      listText += `🔑 <b>OTP:</b> <code>${otp}</code> <i>(Tap to copy)</i>\n`;
    }
    listText += `\n`;

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

  let fullMsg = `
📬 <b>Temp Mail Pro — Message Details</b>

👤 <b>From:</b> <code>${sender}</code>
📝 <b>Subject:</b> <b>${subject}</b>
⏰ <b>Date:</b> ${detail.date || 'Just now'}
`;

  if (otp) {
    fullMsg += `
⚡━━━━━━━━━━━━━━━━━━━━⚡
🔑 <b>DETECTED OTP / SECURITY CODE:</b>
👉 <code>${otp}</code> 👈 <i>(Tap code to copy)</i>
⚡━━━━━━━━━━━━━━━━━━━━⚡
`;
  }

  const cleanBody = bodyText.replace(/<[^>]*>?/gm, '').trim();
  const previewBody = cleanBody.length > 900 ? cleanBody.substring(0, 900) + '...\n<i>(Truncated)</i>' : cleanBody;

  fullMsg += `\n📄 <b>Message Content:</b>\n${previewBody || '<i>No text body available</i>'}`;

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

    let alertText = '';
    if (otpCode) {
      alertText = `
🔑 <b>Temp Mail Pro — Live OTP Security Alert!</b>

⚡ <b>Verification Code:</b>
👉 <code>${otpCode}</code> 👈 <i>(Tap code to copy)</i>

📬 <b>Mailbox:</b> <code>${email || 'Active'}</code>
👤 <b>From:</b> ${sender || 'Online Service'}
📝 <b>Subject:</b> ${subject || 'Verification'}
`;
    } else {
      alertText = `
📬 <b>Temp Mail Pro — New Incoming Email!</b>

✉️ <b>Mailbox:</b> <code>${email || 'Active'}</code>
👤 <b>From:</b> ${sender || 'Unknown'}
📝 <b>Subject:</b> ${subject || 'No Subject'}
📄 <b>Preview:</b> <i>${(preview || '').substring(0, 300)}</i>
`;
    }

    await bot.sendMessage(chatId, alertText, { parse_mode: 'HTML' });
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
