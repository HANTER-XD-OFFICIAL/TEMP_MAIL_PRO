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
⚡ <b>স্বাগতম ${firstName}! Temp Mail Pro অফিসিয়াল বটে!</b>

🛡️ <b>Developed by:</b> <a href="${DEVELOPER_PROFILE}">${DEVELOPER_NAME}</a> (Hanter XD Official)
🤖 <b>বট ইউজারনেম:</b> @TEMPMAILPRO34_bot
📱 <b>Android App v2.6.0:</b> <a href="${APK_DOWNLOAD_URL}">এখানে ডাউনলোড করুন</a>

এই বটের মাধ্যমে আপনি সম্পূর্ণ ফ্রিতে চব্বিশ ঘণ্টা আনলিমিটেড টেম্পোরারি ইমেইল তৈরি করতে পারবেন এবং ফেসবুক, টেলিগ্রাম, টিকটক, জিমেইলসহ যেকোনো সাইটের <b>লাইভ ওটিপি (OTP) ও ভেরিফিকেশন কোড</b> দেখতে পাবেন!

💡 <b>আপনার Telegram Chat ID:</b> <code>${chatId}</code>
<i>(Temp Mail Pro অ্যাপে এই Chat ID লিঙ্ক করে ওটিপি সরাসরি এখানে পেতে পারেন)</i>
`;

  const keyboard = {
    inline_keyboard: [
      [
        { text: '⚡ নতুন ইমেইল তৈরি করুন (Generate)', callback_data: 'GEN_NEW_MAIL' },
        { text: '📬 ইনবক্স চেক করুন (Check Inbox)', callback_data: 'CHECK_INBOX' }
      ],
      [
        { text: '🔄 ওটিপি অটো-রিফ্রেশ (Auto Check)', callback_data: 'AUTO_REFRESH' },
        { text: '🗑️ সেশন রিসেট করুন', callback_data: 'RESET_MAIL' }
      ],
      [
        { text: '📢 টেলিগ্রাম চ্যানেল', url: TELEGRAM_CHANNEL },
        { text: '💬 ডেভেলপার হোয়াটসঅ্যাপ', url: WHATSAPP_CONTACT }
      ],
      [
        { text: '📥 Android APK ডাউনলোড (v2.6.0)', url: APK_DOWNLOAD_URL }
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
  await bot.sendMessage(chatId, `🆔 <b>আপনার Telegram Chat ID:</b> <code>${chatId}</code>\n\nঅ্যাপে ইনবক্স ফরোয়ার্ড চালু করতে এই আইডিটি ব্যবহার করুন।`, {
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
      await bot.answerCallbackQuery(query.id, { text: 'নতুন ইমেইল তৈরি হচ্ছে...' });
      await handleGenerateEmail(chatId);
    } else if (data === 'CHECK_INBOX' || data === 'AUTO_REFRESH') {
      await bot.answerCallbackQuery(query.id, { text: 'ইনবক্স চেক করা হচ্ছে...' });
      await handleCheckInbox(chatId);
    } else if (data === 'RESET_MAIL') {
      userSessions.delete(chatId);
      await bot.answerCallbackQuery(query.id, { text: 'সেশন ক্লিয়ার করা হয়েছে' });
      await bot.sendMessage(chatId, '🗑️ আপনার পূর্বের ইমেইল সেশন মুছে ফেলা হয়েছে। নতুন ইমেইল পেতে /new চাপুন।');
    } else if (data.startsWith('READ_MSG_')) {
      const msgId = data.replace('READ_MSG_', '');
      await bot.answerCallbackQuery(query.id, { text: 'ইমেইল পড়া হচ্ছে...' });
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
⚡ <b>আপনার সক্রিয় অস্থায়ী ইমেইল তৈরি সম্পন্ন হয়েছে!</b>

✉️ <b>ইমেইল এড্রেস:</b>
<code>${mailbox.email}</code> <i>(ট্যাপ করে কপি করুন)</i>

🌐 <b>ডোমেইন নোড:</b> <code>${mailbox.domain}</code>
⏱️ <b>মেয়াদ:</b> সক্রিয় (24/7 Live Node)
🛡️ <b>প্রাইভেসি:</b> 100% অ্যানোনিমাস ও সুরক্ষিত

<i>যেকোনো ওয়েবসাইট বা অ্যাপে এই ইমেইলটি দিন। ওটিপি আসলে নিচের 'ইনবক্স চেক করুন' বাটনে চাপুন।</i>
`;

  const keyboard = {
    inline_keyboard: [
      [
        { text: '📬 ইনবক্স ও ওটিপি চেক করুন', callback_data: 'CHECK_INBOX' }
      ],
      [
        { text: '🔄 অন্য একটি ইমেইল বানান', callback_data: 'GEN_NEW_MAIL' },
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
    await bot.sendMessage(chatId, '⚠️ আপনার কোনো সক্রিয় ইমেইল নেই! প্রথমে নিচের বাটনে চাপ দিয়ে একটি ইমেইল তৈরি করে নিন।', {
      reply_markup: {
        inline_keyboard: [
          [{ text: '⚡ নতুন ইমেইল তৈরি করুন', callback_data: 'GEN_NEW_MAIL' }]
        ]
      }
    });
    return;
  }

  const messages = await getInboxMessages(session.login, session.domain);

  if (!messages || messages.length === 0) {
    const emptyText = `
📭 <b>ইনবক্স বর্তমানে খালি!</b>

✉️ <b>টার্গেট ইমেইল:</b> <code>${session.email}</code>
⏱️ <i>এখনো কোনো নতুন বার্তা বা ওটিপি আসেনি। অনুগ্রহ করে যে সাইটে ইমেইলটি দিয়েছেন সেখান থেকে ভেরিফিকেশন রিকোয়েস্ট পাঠান এবং ১০-১৫ সেকেন্ড পর পুনরায় চেক করুন।</i>
`;
    await bot.sendMessage(chatId, emptyText, {
      parse_mode: 'HTML',
      reply_markup: {
        inline_keyboard: [
          [{ text: '🔄 আবার রিফ্রেশ করুন', callback_data: 'CHECK_INBOX' }],
          [{ text: '⚡ নতুন ইমেইল বানান', callback_data: 'GEN_NEW_MAIL' }]
        ]
      }
    });
    return;
  }

  // Found messages
  let listText = `📬 <b>মোট ${messages.length} টি ইমেইল পাওয়া গেছে!</b>\n✉️ <b>ইমেইল:</b> <code>${session.email}</code>\n\n`;

  const inlineButtons = [];

  for (const m of messages.slice(0, 5)) {
    const fromSender = m.from || 'Unknown Sender';
    const subj = m.subject || 'No Subject';
    const date = m.date || '';

    listText += `🔹 <b>From:</b> ${fromSender}\n📝 <b>Subject:</b> ${subj}\n⏰ <b>Time:</b> ${date}\n\n`;

    inlineButtons.push([
      { text: `📖 পড়ুন: ${subj.substring(0, 25)}`, callback_data: `READ_MSG_${m.id}` }
    ]);
  }

  inlineButtons.push([
    { text: '🔄 রিফ্রেশ ইনবক্স', callback_data: 'CHECK_INBOX' },
    { text: '⚡ নতুন ইমেইল', callback_data: 'GEN_NEW_MAIL' }
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
    await bot.sendMessage(chatId, '⚠️ সেশন পাওয়া যায়নি। অনুগ্রহ করে নতুন ইমেইল তৈরি করুন।');
    return;
  }

  const detail = await getMessageDetails(session.login, session.domain, msgId);
  if (!detail) {
    await bot.sendMessage(chatId, '❌ বার্তা লোড করতে সমস্যা হয়েছে। আবার চেষ্টা করুন।');
    return;
  }

  const sender = detail.from || 'Unknown Sender';
  const subject = detail.subject || 'No Subject';
  const bodyText = detail.textBody || detail.body || '';
  const otp = extractOtp(bodyText) || extractOtp(subject);

  let fullMsg = `
📬 <b>Temp Mail Pro — ইমেইল বিস্তারিত</b>

👤 <b>প্রেরক:</b> <code>${sender}</code>
📝 <b>বিষয়:</b> <b>${subject}</b>
⏰ <b>তারিখ:</b> ${detail.date || 'N/A'}
`;

  if (otp) {
    fullMsg += `
⚡━━━━━━━━━━━━━━━━━━━━⚡
🔑 <b>শনাক্তকৃত ওটিপি / সিকিউরিটি কোড:</b>
👉 <code>${otp}</code> 👈 <i>(ট্যাপ করে কপি করুন)</i>
⚡━━━━━━━━━━━━━━━━━━━━⚡
`;
  }

  // Trim long text for telegram limits
  const cleanBody = bodyText.replace(/<[^>]*>?/gm, '').trim();
  const previewBody = cleanBody.length > 800 ? cleanBody.substring(0, 800) + '...\n<i>(বাকি অংশ বড় হওয়ার কারণে সংক্ষেপ করা হয়েছে)</i>' : cleanBody;

  fullMsg += `\n📄 <b>বার্তার বিষয়বস্তু:</b>\n${previewBody || '<i>কোনো টেক্সট পাওয়া যায়নি</i>'}`;

  const keyboard = {
    inline_keyboard: [
      [
        { text: '⬅️ ইনবক্সে ফিরে যান', callback_data: 'CHECK_INBOX' },
        { text: '🔄 রিফ্রেশ', callback_data: 'CHECK_INBOX' }
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
