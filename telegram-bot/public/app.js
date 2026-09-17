/**
 * Temp Mail Pro — Official Web Application
 * Lead Developer: MD RASEL (Hanter XD Official)
 * Support Email: hanterxdofficial@gmail.com
 * WhatsApp: +8801882278234
 * Telegram Bot: @TEMPMAILPRO34_bot
 */

// ==========================================
// 1. STATE & CONFIGURATION
// ==========================================
const APP_CONFIG = {
  version: '2.6.0',
  defaultTimerSeconds: 600, // 10 minutes
  pollIntervalSeconds: 8,
  developerName: 'MD RASEL',
  developerHandle: 'Hanter XD Official',
  supportEmail: 'hanterxdofficial@gmail.com',
  whatsappNumber: '+8801882278234',
  telegramChannel: 'https://t.me/HANTER_XD_OFFICIAL',
  telegramBotUsername: '@TEMPMAILPRO34_bot'
};

let state = {
  activeAccount: null,
  savedAccounts: [],
  messages: [],
  knownMessageIds: new Set(),
  timerSeconds: APP_CONFIG.defaultTimerSeconds,
  timerInterval: null,
  pollCountdown: APP_CONFIG.pollIntervalSeconds,
  pollInterval: null,
  soundEnabled: true,
  currentLang: 'en',
  currentTheme: 'dark',
  currentDetailMessage: null,
  activeDetailTab: 'html',
  searchQuery: '',
  domains: [
    { domain: 'sharklasers.com', type: 'guerrilla', provider: 'Guerrilla (Recommended for Meta/FB)', icon: '⭐', badge: 'High Reputation' },
    { domain: 'guerrillamail.com', type: 'guerrilla', provider: 'Guerrilla (Official)', icon: '🛡️', badge: 'Verified' },
    { domain: 'grr.la', type: 'guerrilla', provider: 'Guerrilla (Short)', icon: '🚀', badge: 'Ultra Short' },
    { domain: 'guerrillamailblock.com', type: 'guerrilla', provider: 'Guerrilla (Spam-Guard)', icon: '🛡️', badge: 'Spam Guard' },
    { domain: 'guerrillamail.net', type: 'guerrilla', provider: 'Guerrilla Net', icon: '🌐', badge: 'Active' },
    { domain: 'guerrillamail.biz', type: 'guerrilla', provider: 'Guerrilla Biz', icon: '💼', badge: 'Business' },
    { domain: 'guerrillamail.org', type: 'guerrilla', provider: 'Guerrilla Org', icon: '🏛️', badge: 'Community' },
    { domain: 'pokemail.net', type: 'guerrilla', provider: 'Guerrilla Poke', icon: '⚡', badge: 'Instant OTP' },
    { domain: 'spam4.me', type: 'guerrilla', provider: 'Guerrilla Stealth', icon: '🎯', badge: 'Stealth' },
    { domain: 'uberip.com', type: 'mailtm', provider: 'Mail.tm (Fast API)', icon: '⚡', badge: 'Instant OTP' },
    { domain: 'emalupe.com', type: 'mailtm', provider: 'Mail.tm Pro', icon: '🔥', badge: 'High Speed' },
    { domain: 'westcast-systems.com', type: 'mailgw', provider: 'Mail.gw (Stable)', icon: '⚡', badge: 'Enterprise' }
  ],
  telegramSettings: {
    chatId: '',
    autoForward: true
  }
};

// ==========================================
// 2. MULTI-LANGUAGE TRANSLATIONS (8 LANGUAGES)
// ==========================================
const TRANSLATIONS = {
  en: {
    langName: 'English',
    flag: '🇺🇸',
    privacyShield: 'Privacy Shield Active',
    copy: 'Copy',
    copied: '✓ Email Copied to Clipboard!',
    copiedOtp: '✓ Verification Code Copied!',
    expiresIn: 'Expires In',
    random: 'Random',
    custom: 'Custom',
    domains: 'Domains (30+)',
    refresh: 'Refresh',
    saved: 'Vault',
    shred: 'Shred',
    inboxTitle: 'Live Mailbox Inbox',
    emptyTitle: 'Waiting for incoming emails...',
    emptyDesc: 'Send an email or sign up on Facebook, Telegram, WhatsApp, TikTok, or Google using your active address above. Your message & verification code will appear here in real time!',
    testEmail: 'Check Incoming OTP',
    checkOtp: 'Check Incoming OTP',
    domainHubTitle: 'Enterprise Domain Hub',
    domainHubDesc: 'Select any verified high-reputation domain. Powered by GuerrillaMail, Mail.tm, and Mail.gw enterprise mail nodes.',
    customTitle: 'Create Custom Mailbox',
    customDesc: 'Choose your preferred username and domain to create a customized burner email address.',
    usernameLabel: 'Username / Prefix:',
    selectDomainLabel: 'Select Domain:',
    vaultTitle: 'Saved Accounts Vault',
    vaultDesc: 'Access, switch, or backup your saved burner email addresses securely stored in your browser.',
    otpDetected: 'VERIFICATION CODE DETECTED',
    copyOtp: 'One-Tap Copy OTP'
  },
  bn: {
    langName: 'বাংলা',
    flag: '🇧🇩',
    privacyShield: 'প্রাইভেসি শিল্ড সক্রিয়',
    copy: 'কপি',
    copied: '✓ ইমেইল ঠিকানা কপি করা হয়েছে!',
    copiedOtp: '✓ ওটিপি কোড কপি করা হয়েছে!',
    expiresIn: 'মেয়াদ শেষ হবে',
    random: 'নতুন ইমেইল',
    custom: 'কাস্টম',
    domains: 'ডোমেইন (৩০+)',
    refresh: 'রিফ্রেশ',
    saved: 'ভল্ট',
    shred: 'ধ্বংস করুন',
    inboxTitle: 'লাইভ ইনবক্স মেসেজ',
    emptyTitle: 'ইনকামিং ইমেলের অপেক্ষায়...',
    emptyDesc: 'ফেসবুক, টেলিগ্রাম, হোয়াটসঅ্যাপ বা গুগলে উপরের ঠিকানাটি ব্যবহার করুন। ওটিপি এবং ভেরিফিকেশন কোড রিয়েল-টাইমে এখানে চলে আসবে!',
    testEmail: 'আগত ওটিপি চেক করুন',
    checkOtp: 'আগত ওটিপি চেক করুন',
    domainHubTitle: 'এন্টারপ্রাইজ ডোমেন হাব',
    domainHubDesc: 'যে কোনো হাই-রেপুটেশন ডোমেন বেছে নিন। ফেসবুক ও সোশ্যাল মিডিয়ার জন্য সেরা গেরিলা ও মেইল.টিএম নেটওয়ার্ক।',
    customTitle: 'কাস্টম মেইলবক্স তৈরি',
    customDesc: 'আপনার পছন্দের ইউজারনেম এবং ডোমেন দিয়ে নিজস্ব ডিসপোজেবল ঠিকানা তৈরি করুন।',
    usernameLabel: 'ইউজারনেম / নাম:',
    selectDomainLabel: 'ডোমেন নির্বাচন করুন:',
    vaultTitle: 'সংরক্ষিত অ্যাকাউন্ট ভল্ট',
    vaultDesc: 'আপনার সংরক্ষিত সব ডিসপোজেবল ইমেইল ঠিকানা এক ক্লিকে পরিচালনা ও সুইচ করুন।',
    otpDetected: 'ভেরিফিকেশন কোড পাওয়া গেছে',
    copyOtp: 'এক-ট্যাপে ওটিপি কপি'
  },
  es: {
    langName: 'Español',
    flag: '🇪🇸',
    privacyShield: 'Escudo de Privacidad Activo',
    copy: 'Copiar',
    copied: '✓ ¡Correo copiado al portapapeles!',
    copiedOtp: '✓ ¡Código OTP copiado!',
    expiresIn: 'Expira en',
    random: 'Aleatorio',
    custom: 'Personalizado',
    domains: 'Dominios (30+)',
    refresh: 'Actualizar',
    saved: 'Bóveda',
    shred: 'Destruir',
    inboxTitle: 'Bandeja de Entrada en Vivo',
    emptyTitle: 'Esperando correos entrantes...',
    emptyDesc: 'Usa esta dirección para registrarte en Facebook, Google, WhatsApp o Telegram. ¡Los códigos llegarán al instante!',
    testEmail: 'Verificar OTP entrante',
    checkOtp: 'Verificar OTP entrante',
    domainHubTitle: 'Centro de Dominios',
    domainHubDesc: 'Selecciona cualquier dominio verificado con alta entregabilidad.',
    customTitle: 'Crear Correo Personalizado',
    customDesc: 'Elige tu nombre de usuario y dominio favoritos.',
    usernameLabel: 'Usuario / Prefijo:',
    selectDomainLabel: 'Seleccionar Dominio:',
    vaultTitle: 'Bóveda de Cuentas',
    vaultDesc: 'Administra tus direcciones temporales guardadas.',
    otpDetected: 'CÓDIGO DE VERIFICACIÓN DETECTADO',
    copyOtp: 'Copiar Código OTP'
  },
  ar: {
    langName: 'العربية',
    flag: '🇸🇦',
    privacyShield: 'درع الخصوصية نشط',
    copy: 'نسخ',
    copied: '✓ تم نسخ البريد الإلكتروني!',
    copiedOtp: '✓ تم نسخ رمز التحقق!',
    expiresIn: 'ينتهي في',
    random: 'عشوائي',
    custom: 'مخصص',
    domains: 'النطاقات (30+)',
    refresh: 'تحديث',
    saved: 'الخزنة',
    shred: 'إتلاف',
    inboxTitle: 'صندوق البريد المباشر',
    emptyTitle: 'في انتظار الرسائل الواردة...',
    emptyDesc: 'استخدم هذا العنوان للتسجيل في فيسبوك أو تيليجرام أو واتساب. سيصل رمز التحقق فوراً!',
    testEmail: 'فحص رمز OTP الوارد',
    checkOtp: 'فحص رمز OTP الوارد',
    domainHubTitle: 'مركز النطاقات',
    domainHubDesc: 'اختر أي نطاق عالي الموثوقية.',
    customTitle: 'إنشاء بريد مخصص',
    customDesc: 'اختر اسم المستخدم والنطاق المفضل.',
    usernameLabel: 'اسم المستخدم:',
    selectDomainLabel: 'اختر النطاق:',
    vaultTitle: 'خزنة الحسابات المحفوظة',
    vaultDesc: 'إدارة وتبديل حساباتك المؤقتة بسهولة.',
    otpDetected: 'تم اكتشاف رمز التحقق',
    copyOtp: 'نسخ رمز التحقق بنقرة واحدة'
  },
  hi: {
    langName: 'हिन्दी',
    flag: '🇮🇳',
    privacyShield: 'गोपनीयता शील्ड सक्रिय',
    copy: 'कॉपी',
    copied: '✓ ईमेल पता कॉपी किया गया!',
    copiedOtp: '✓ ओटीपी कोड कॉपी किया गया!',
    expiresIn: 'समाप्त होने में समय',
    random: 'नया पता',
    custom: 'कस्टम',
    domains: 'डोमेन (30+)',
    refresh: 'रिफ्रेश',
    saved: 'वॉल्ट',
    shred: 'नष्ट करें',
    inboxTitle: 'लाइव इनबॉक्स संदेश',
    emptyTitle: 'आने वाले ईमेल की प्रतीक्षा है...',
    emptyDesc: 'फेसबुक, व्हाट्सएप, टेलीग्राम या गूगल पर साइन अप करने के लिए इस पते का उपयोग करें।',
    testEmail: 'आने वाला OTP जांचें',
    checkOtp: 'आने वाला OTP जांचें',
    domainHubTitle: 'एंटरप्राइज डोमेन हब',
    domainHubDesc: 'उच्च विश्वसनीयता वाले डोमेन में से चुनें।',
    customTitle: 'कस्टम मेलबॉक्स बनाएं',
    customDesc: 'अपना पसंदीदा यूज़रनेम और डोमेन चुनें।',
    usernameLabel: 'यूज़रनेम:',
    selectDomainLabel: 'डोमेन चुनें:',
    vaultTitle: 'सहेजे गए खाते',
    vaultDesc: 'अपने सहेजे गए पतों को आसानी से प्रबंधित करें।',
    otpDetected: 'सत्यापन कोड पाया गया',
    copyOtp: 'ओटीपी कोड कॉपी करें'
  },
  ru: {
    langName: 'Русский',
    flag: '🇷🇺',
    privacyShield: 'Защита приватности активна',
    copy: 'Копировать',
    copied: '✓ Email скопирован в буфер!',
    copiedOtp: '✓ Код OTP скопирован!',
    expiresIn: 'Истекает через',
    random: 'Случайный',
    custom: 'Свой логин',
    domains: 'Домены (30+)',
    refresh: 'Обновить',
    saved: 'Хранилище',
    shred: 'Уничтожить',
    inboxTitle: 'Входящие сообщения онлайн',
    emptyTitle: 'Ожидание входящих писем...',
    emptyDesc: 'Используйте адрес для регистрации в Telegram, WhatsApp, Facebook или Google.',
    testEmail: 'Проверить входящий OTP',
    checkOtp: 'Проверить входящий OTP',
    domainHubTitle: 'Выбор домена',
    domainHubDesc: 'Надежные домены с мгновенной доставкой.',
    customTitle: 'Создать свой адрес',
    customDesc: 'Выберите желаемый логин и домен.',
    usernameLabel: 'Имя пользователя:',
    selectDomainLabel: 'Выберите домен:',
    vaultTitle: 'Сохраненные адреса',
    vaultDesc: 'Быстрое переключение между адресами.',
    otpDetected: 'ОБНАРУЖЕН КОД ПОДТВЕРЖДЕНИЯ',
    copyOtp: 'Скопировать код'
  },
  pt: {
    langName: 'Português',
    flag: '🇵🇹',
    privacyShield: 'Escudo de Privacidade Ativo',
    copy: 'Copiar',
    copied: '✓ E-mail copiado!',
    copiedOtp: '✓ Código OTP copiado!',
    expiresIn: 'Expira em',
    random: 'Aleatório',
    custom: 'Personalizado',
    domains: 'Domínios (30+)',
    refresh: 'Atualizar',
    saved: 'Cofre',
    shred: 'Destruir',
    inboxTitle: 'Caixa de Entrada em Tempo Real',
    emptyTitle: 'Aguardando e-mails...',
    emptyDesc: 'Use este endereço temporário para receber códigos de confirmação instantaneamente.',
    testEmail: 'Verificar OTP de Entrada',
    checkOtp: 'Verificar OTP de Entrada',
    domainHubTitle: 'Hub de Domínios',
    domainHubDesc: 'Domínios seguros e de alta reputação.',
    customTitle: 'Criar E-mail Personalizado',
    customDesc: 'Escolha seu nome de usuário e domínio preferido.',
    usernameLabel: 'Usuário:',
    selectDomainLabel: 'Selecionar Domínio:',
    vaultTitle: 'Cofre de Contas',
    vaultDesc: 'Gerencie seus endereços temporários.',
    otpDetected: 'CÓDIGO DE VERIFICAÇÃO DETECTADO',
    copyOtp: 'Copiar Código'
  },
  ur: {
    langName: 'اردو',
    flag: '🇵🇰',
    privacyShield: 'پرائیویسی شیلڈ فعال ہے',
    copy: 'کاپی',
    copied: '✓ ای میل کاپی ہو گیا!',
    copiedOtp: '✓ او ٹی پی کوڈ کاپی ہو گیا!',
    expiresIn: 'ختم ہونے میں وقت',
    random: 'نیا ای میل',
    custom: 'اپنی مرضی کا',
    domains: 'ڈومینز (30+)',
    refresh: 'تازہ کریں',
    saved: 'والٹ',
    shred: 'ختم کریں',
    inboxTitle: 'لائیو ان باکس پیغامات',
    emptyTitle: 'نئے پیغامات کا انتظار ہے...',
    emptyDesc: 'فیس بک، ٹیلیگرام، واٹس ایپ یا گوگل پر سائن اپ کے لیے یہ پتہ استعمال کریں۔ کوڈ فوراً پہنچے گا!',
    testEmail: 'موصولہ او ٹی پی چیک کریں',
    checkOtp: 'موصولہ او ٹی پی چیک کریں',
    domainHubTitle: 'ڈومین سلیکشن ہب',
    domainHubDesc: 'ہائی ریپوٹیشن ڈومینز میں سے منتخب کریں۔',
    customTitle: 'کسٹم ای میل بنائیں',
    customDesc: 'اپنا پسندیدہ نام اور ڈومین منتخب کریں۔',
    usernameLabel: 'یوزر نیم:',
    selectDomainLabel: 'ڈومین منتخب کریں:',
    vaultTitle: 'محفوظ شدہ اکاؤنٹس',
    vaultDesc: 'اپنے محفوظ کردہ عارضی پتے آسانی سے تبدیل کریں۔',
    otpDetected: 'تصدیقی کوڈ موصول ہوا',
    copyOtp: 'او ٹی پی کاپی کریں'
  }
};

// ==========================================
// 3. AUDIO ENGINE (Web Audio API Synthesizer)
// ==========================================
function playNotificationChime() {
  if (!state.soundEnabled) return;
  try {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (!AudioContext) return;
    const ctx = new AudioContext();

    const now = ctx.currentTime;
    const osc1 = ctx.createOscillator();
    const osc2 = ctx.createOscillator();
    const gain = ctx.createGain();

    osc1.type = 'sine';
    osc2.type = 'triangle';

    // Pleasant double chime: 587.33Hz (D5) -> 880Hz (A5)
    osc1.frequency.setValueAtTime(587.33, now);
    osc1.frequency.exponentialRampToValueAtTime(880, now + 0.15);

    osc2.frequency.setValueAtTime(880, now);
    osc2.frequency.exponentialRampToValueAtTime(1174.66, now + 0.15);

    gain.gain.setValueAtTime(0.2, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + 0.5);

    osc1.connect(gain);
    osc2.connect(gain);
    gain.connect(ctx.destination);

    osc1.start(now);
    osc2.start(now);
    osc1.stop(now + 0.5);
    osc2.stop(now + 0.5);
  } catch (e) {
    console.log('Audio chime not supported or muted');
  }
}

// ==========================================
// 4. INITIALIZATION & STORAGE
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
  loadStoredSettings();
  setupTheme();
  setupLanguage(state.currentLang);
  fetchDomainsList();
  
  // Initialize Active Mailbox
  const savedLastAccount = localStorage.getItem('tmp_active_account');
  if (savedLastAccount) {
    try {
      state.activeAccount = JSON.parse(savedLastAccount);
      renderActiveAccount();
      startTimer();
      startPolling();
    } catch (e) {
      generateNewRandomEmail();
    }
  } else {
    generateNewRandomEmail();
  }

  // Setup Custom username preview listener
  const userInp = document.getElementById('custom-username-input');
  const domSel = document.getElementById('custom-domain-select');
  if (userInp && domSel) {
    userInp.addEventListener('input', updateCustomPreview);
    domSel.addEventListener('change', updateCustomPreview);
  }
});

function loadStoredSettings() {
  // Language
  const savedLang = localStorage.getItem('tmp_lang');
  if (savedLang && TRANSLATIONS[savedLang]) {
    state.currentLang = savedLang;
  }
  
  // Theme
  const savedTheme = localStorage.getItem('tmp_theme');
  if (savedTheme) {
    state.currentTheme = savedTheme;
  }

  // Sound
  const savedSound = localStorage.getItem('tmp_sound');
  if (savedSound !== null) {
    state.soundEnabled = savedSound === 'true';
    updateSoundIcon();
  }

  // Ensure inbox contains exclusively real incoming messages
  state.messages = (state.messages || []).filter(m => m && (!m.id || !m.id.startsWith('sim_')));

  // Saved Vault
  const savedVault = localStorage.getItem('tmp_saved_vault');
  if (savedVault) {
    try {
      state.savedAccounts = JSON.parse(savedVault) || [];
      updateSavedCountBadge();
    } catch (e) {}
  }

  // Telegram Settings
  const savedTg = localStorage.getItem('tmp_telegram');
  if (savedTg) {
    try {
      state.telegramSettings = JSON.parse(savedTg);
      const tgChatInp = document.getElementById('telegram-chat-id-input');
      const tgFwdTog = document.getElementById('tg-auto-forward-toggle');
      if (tgChatInp) tgChatInp.value = state.telegramSettings.chatId || '';
      if (tgFwdTog) tgFwdTog.checked = state.telegramSettings.autoForward !== false;
    } catch (e) {}
  }
}

function setupTheme() {
  document.documentElement.setAttribute('data-theme', state.currentTheme);
  const themeIcon = document.getElementById('theme-icon');
  if (themeIcon) {
    themeIcon.textContent = state.currentTheme === 'dark' ? '🌙' : '☀️';
  }
}

function toggleTheme() {
  state.currentTheme = state.currentTheme === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', state.currentTheme);
  localStorage.setItem('tmp_theme', state.currentTheme);
  const themeIcon = document.getElementById('theme-icon');
  if (themeIcon) {
    themeIcon.textContent = state.currentTheme === 'dark' ? '🌙' : '☀️';
  }
}

function toggleSound() {
  state.soundEnabled = !state.soundEnabled;
  localStorage.setItem('tmp_sound', state.soundEnabled.toString());
  updateSoundIcon();
  showToast(state.soundEnabled ? '🔊 Sound Alerts Enabled' : '🔇 Sound Alerts Muted');
}

function updateSoundIcon() {
  const icon = document.getElementById('sound-icon');
  if (icon) {
    icon.textContent = state.soundEnabled ? '🔊' : '🔇';
  }
}

// ==========================================
// 5. LANGUAGE ENGINE
// ==========================================
function toggleLangMenu() {
  const menu = document.getElementById('lang-menu');
  if (menu) menu.classList.toggle('active');
}

document.addEventListener('click', (e) => {
  const wrapper = document.querySelector('.lang-dropdown-wrapper');
  if (wrapper && !wrapper.contains(e.target)) {
    const menu = document.getElementById('lang-menu');
    if (menu) menu.classList.remove('active');
  }
});

function setLanguage(langCode) {
  if (!TRANSLATIONS[langCode]) return;
  state.currentLang = langCode;
  localStorage.setItem('tmp_lang', langCode);
  setupLanguage(langCode);
  const menu = document.getElementById('lang-menu');
  if (menu) menu.classList.remove('active');
}

function setupLanguage(langCode) {
  const t = TRANSLATIONS[langCode] || TRANSLATIONS.en;
  
  // RTL Support for Arabic & Urdu
  if (langCode === 'ar' || langCode === 'ur') {
    document.documentElement.setAttribute('dir', 'rtl');
  } else {
    document.documentElement.removeAttribute('dir');
  }

  // Update top button flag
  document.getElementById('current-lang-flag').textContent = t.flag;
  document.getElementById('current-lang-code').textContent = langCode.toUpperCase();

  // Update Texts
  setElemText('txt-privacy-shield', t.privacyShield);
  setElemText('txt-copy', t.copy);
  setElemText('txt-expires-in', t.expiresIn);
  setElemText('txt-random', t.random);
  setElemText('txt-custom', t.custom);
  setElemText('txt-domains', t.domains);
  setElemText('txt-refresh', t.refresh);
  setElemText('txt-shred', t.shred);
  setElemText('txt-inbox-title', t.inboxTitle);
  setElemText('txt-empty-title', t.emptyTitle);
  setElemText('txt-empty-desc', t.emptyDesc);
  setElemText('txt-test-email', t.checkOtp || t.testEmail);
  setElemText('txt-check-otp', t.checkOtp || t.testEmail);
  setElemText('txt-domain-hub-title', t.domainHubTitle);
  setElemText('txt-domain-hub-desc', t.domainHubDesc);
  setElemText('txt-custom-title', t.customTitle);
  setElemText('txt-custom-desc', t.customDesc);
  setElemText('txt-username-label', t.usernameLabel);
  setElemText('txt-select-domain-label', t.selectDomainLabel);
  setElemText('txt-vault-title', t.vaultTitle);
  setElemText('txt-vault-desc', t.vaultDesc);
  setElemText('txt-otp-detected', t.otpDetected);
  setElemText('txt-copy-otp', t.copyOtp);

  updateSavedCountBadge();
}

function setElemText(id, text) {
  const el = document.getElementById(id);
  if (el) el.textContent = text;
}

// ==========================================
// 6. BACKEND ROUTING & STANDALONE DETECTOR
// ==========================================
function getBackendBaseUrl() {
  const custom = localStorage.getItem('tmp_backend_url');
  if (custom && custom.trim()) {
    return custom.trim().replace(/\/+$/, '');
  }
  // Detect if running on GitHub Pages (github.io) or static file
  const isStaticHost = window.location.hostname.includes('github.io') ||
                       window.location.hostname.includes('pages.dev') ||
                       window.location.protocol === 'file:';
  if (isStaticHost) {
    return null; // Pure standalone client-side mode
  }
  return '';
}

// ==========================================
// 6. MAILBOX GENERATION & DOMAINS
// ==========================================
async function fetchDomainsList() {
  const backend = getBackendBaseUrl();
  if (backend !== null) {
    try {
      const res = await fetch(`${backend}/api/domains`);
      if (res.ok) {
        const data = await res.json();
        if (data.ok && Array.isArray(data.domains) && data.domains.length > 0) {
          state.domains = data.domains;
        }
      }
    } catch (e) {
      console.log('Using default client domain list');
    }
  }
  populateDomainSelects();
  renderDomainsModalList();
}

function populateDomainSelects() {
  const sel = document.getElementById('custom-domain-select');
  if (!sel) return;
  sel.innerHTML = '';
  state.domains.forEach(d => {
    const opt = document.createElement('option');
    opt.value = d.domain;
    opt.textContent = `@${d.domain} (${d.provider})`;
    sel.appendChild(opt);
  });
  updateCustomPreview();
}

async function generateNewRandomEmail(selectedDomain = null) {
  const domainToUse = selectedDomain || (state.activeAccount ? state.activeAccount.domain : 'sharklasers.com');
  const btn = document.getElementById('btn-change');
  if (btn) btn.classList.add('loading');

  const emailInput = document.getElementById('active-email-input');
  if (emailInput) emailInput.value = 'Generating secure identity...';

  const backend = getBackendBaseUrl();
  if (backend !== null) {
    try {
      const res = await fetch(`${backend}/api/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ domain: domainToUse })
      });

      if (res.ok) {
        const data = await res.json();
        if (data.ok && data.mailbox) {
          setNewActiveAccount(data.mailbox);
          showToast('✓ Fresh Email Generated!');
          if (btn) btn.classList.remove('loading');
          return;
        }
      }
    } catch (err) {
      // Fall through to client-side generation
    }
  }

  // Client-side direct browser generation (GuerrillaMail CORS API)
  await generateClientFallbackMailbox(domainToUse);
  if (btn) btn.classList.remove('loading');
}

async function generateClientFallbackMailbox(domain = 'sharklasers.com') {
  try {
    const res = await fetch('https://api.guerrillamail.com/ajax.php?f=get_email_address');
    const data = await res.json();
    const user = 'u' + Math.random().toString(36).substring(2, 10);
    const sid = data.sid_token;
    
    await fetch(`https://api.guerrillamail.com/ajax.php?f=set_email_user&email_user=${user}&domain=${domain}&sid_token=${sid}`);
    
    const mailbox = {
      address: `${user}@${domain}`,
      username: user,
      domain: domain,
      type: 'guerrilla',
      sidToken: sid,
      createdAt: Date.now()
    };
    setNewActiveAccount(mailbox);
    showToast('✓ Fresh Email Generated!');
  } catch (e) {
    showToast('❌ Connection error. Please retry.');
  }
}

function setNewActiveAccount(mailbox) {
  state.activeAccount = mailbox;
  state.messages = [];
  state.knownMessageIds.clear();
  state.timerSeconds = APP_CONFIG.defaultTimerSeconds;
  
  localStorage.setItem('tmp_active_account', JSON.stringify(mailbox));
  renderActiveAccount();
  renderMessagesList();
  startTimer();
  startPolling();
}

function renderActiveAccount() {
  if (!state.activeAccount) return;
  const input = document.getElementById('active-email-input');
  if (input) input.value = state.activeAccount.address;

  const domText = document.getElementById('current-domain-text');
  const domIcon = document.getElementById('current-domain-icon');
  if (domText) domText.textContent = `@${state.activeAccount.domain}`;
  
  const dObj = state.domains.find(d => d.domain === state.activeAccount.domain);
  if (domIcon) domIcon.textContent = dObj ? dObj.icon : '⭐';
}

// ==========================================
// 7. INBOX POLLING & MESSAGE FETCHING
// ==========================================
function startPolling() {
  if (state.pollInterval) clearInterval(state.pollInterval);
  state.pollCountdown = APP_CONFIG.pollIntervalSeconds;
  updatePollTicker();

  state.pollInterval = setInterval(() => {
    state.pollCountdown--;
    if (state.pollCountdown <= 0) {
      state.pollCountdown = APP_CONFIG.pollIntervalSeconds;
      fetchInboxMessages(true);
    }
    updatePollTicker();
  }, 1000);

  // Initial fetch immediately
  fetchInboxMessages(false);
}

function updatePollTicker() {
  const cd = document.getElementById('refresh-countdown');
  if (cd) cd.textContent = `${state.pollCountdown}s`;
}

async function manualRefreshInbox() {
  const refreshIcon = document.getElementById('refresh-icon');
  if (refreshIcon) refreshIcon.classList.add('spinning');

  state.pollCountdown = APP_CONFIG.pollIntervalSeconds;
  updatePollTicker();
  await fetchInboxMessages(false);

  setTimeout(() => {
    if (refreshIcon) refreshIcon.classList.remove('spinning');
  }, 600);
}

async function fetchInboxMessages(isAuto = false) {
  if (!state.activeAccount) return;

  try {
    let newMessages = [];
    const backend = getBackendBaseUrl();

    if (backend !== null) {
      try {
        const res = await fetch(`${backend}/api/inbox`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            type: state.activeAccount.type,
            token: state.activeAccount.token,
            sidToken: state.activeAccount.sidToken,
            baseUrl: state.activeAccount.baseUrl,
            address: state.activeAccount.address,
            domain: state.activeAccount.domain
          })
        });

        if (res.ok) {
          const data = await res.json();
          if (data.ok && Array.isArray(data.messages)) {
            newMessages = data.messages;
          }
        }
      } catch (e) {
        // Backend unavailable, fallback to client-side
      }
    }

    // Direct browser Guerrilla fallback
    if (newMessages.length === 0 && state.activeAccount.type === 'guerrilla' && state.activeAccount.sidToken) {
      try {
        const gRes = await fetch(`https://api.guerrillamail.com/ajax.php?f=check_email&seq=0&sid_token=${state.activeAccount.sidToken}`);
        const gData = await gRes.json();
        const list = gData.list || [];
        newMessages = list.map(m => {
          const combined = `${m.mail_subject || ''} ${m.mail_excerpt || ''}`;
          return {
            id: m.mail_id.toString(),
            from: m.mail_from || 'Unknown Sender',
            subject: m.mail_subject || 'No Subject',
            intro: m.mail_excerpt || '',
            date: m.mail_date || 'Just now',
            otpCode: extractOtp(combined),
            snippet: m.mail_excerpt || ''
          };
        });
      } catch (gErr) {
        console.log('Guerrilla browser fetch failed:', gErr);
      }
    }

    // Filter to ensure only genuine incoming emails are accepted
    newMessages = (newMessages || []).filter(m => m && (!m.id || !m.id.startsWith('sim_')));

    // Check for newly arrived messages
    let hasNewlyArrived = false;
    newMessages.forEach(msg => {
      if (!state.knownMessageIds.has(msg.id)) {
        state.knownMessageIds.add(msg.id);
        hasNewlyArrived = true;

        // Auto-forward to Telegram if enabled
        if (state.telegramSettings.autoForward && state.telegramSettings.chatId) {
          forwardEmailToTelegram(state.telegramSettings.chatId, msg);
        }
      }
    });

    state.messages = newMessages;
    renderMessagesList();

    if (hasNewlyArrived) {
      playNotificationChime();
      showToast('📬 New email received!');
    }
  } catch (err) {
    console.log('Error checking inbox:', err.message);
  }
}

// ==========================================
// 8. RENDER MESSAGES LIST & OTP CARDS
// ==========================================
function renderMessagesList() {
  const container = document.getElementById('messages-list');
  const countPill = document.getElementById('inbox-count-pill');
  if (!container) return;

  // Apply search query filter if exists
  let displayList = state.messages;
  if (state.searchQuery.trim()) {
    const q = state.searchQuery.toLowerCase();
    displayList = displayList.filter(m => 
      (m.from && m.from.toLowerCase().includes(q)) ||
      (m.subject && m.subject.toLowerCase().includes(q)) ||
      (m.intro && m.intro.toLowerCase().includes(q)) ||
      (m.otpCode && m.otpCode.includes(q))
    );
  }

  if (countPill) countPill.textContent = displayList.length;

  if (displayList.length === 0) {
    container.innerHTML = `
      <div class="empty-inbox-state" id="empty-state">
        <div class="empty-illustration">
          <div class="radar-circle"></div>
          <span class="empty-icon">📬</span>
        </div>
        <h3 class="empty-title">${TRANSLATIONS[state.currentLang].emptyTitle}</h3>
        <p class="empty-desc">${TRANSLATIONS[state.currentLang].emptyDesc}</p>
        <div class="empty-action-box">
          <button class="btn btn-outline" onclick="checkIncomingOtpNow()">
            🔄 ${TRANSLATIONS[state.currentLang].checkOtp || 'Check Incoming OTP'}
          </button>
        </div>
      </div>
    `;
    return;
  }

  container.innerHTML = '';
  displayList.forEach(msg => {
    const card = document.createElement('div');
    card.className = 'message-item-card';
    card.onclick = () => openEmailDetailModal(msg);

    const initial = (msg.from || 'U').charAt(0).toUpperCase();

    let otpHtml = '';
    if (msg.otpCode) {
      otpHtml = `
        <div class="msg-otp-badge-card" onclick="event.stopPropagation()">
          <div class="msg-otp-content">
            <span class="msg-otp-key-icon">🔑</span>
            <span class="msg-otp-digits">${msg.otpCode}</span>
          </div>
          <button class="msg-otp-copy-btn" onclick="copyOtpFromCard('${msg.otpCode}')">
            Copy Code
          </button>
        </div>
      `;
    }

    card.innerHTML = `
      <div class="msg-header-row">
        <div class="msg-sender-group">
          <div class="msg-avatar">${initial}</div>
          <div>
            <div class="msg-sender-name">${escapeHtml(msg.from)}</div>
            <div class="msg-sender-email">${escapeHtml(state.activeAccount.address)}</div>
          </div>
        </div>
        <div class="msg-timestamp">${escapeHtml(msg.date || 'Just now')}</div>
      </div>
      <div class="msg-subject-text">${escapeHtml(msg.subject || '(No Subject)')}</div>
      <div class="msg-snippet-text">${escapeHtml(msg.snippet || msg.intro || '')}</div>
      ${otpHtml}
    `;

    container.appendChild(card);
  });
}

function handleSearch(val) {
  state.searchQuery = val;
  renderMessagesList();
}

// ==========================================
// 9. EMAIL DETAIL READER MODAL (GMAIL-STYLE)
// ==========================================
async function openEmailDetailModal(msg) {
  state.currentDetailMessage = msg;
  state.activeDetailTab = 'html';

  const modal = document.getElementById('modal-email-detail');
  const subj = document.getElementById('detail-subject');
  const sName = document.getElementById('detail-sender-name');
  const sAddr = document.getElementById('detail-sender-address');
  const avatar = document.getElementById('detail-avatar');
  const dDate = document.getElementById('detail-date');
  const otpHero = document.getElementById('detail-otp-card');
  const otpDigits = document.getElementById('detail-otp-code');
  const htmlBox = document.getElementById('detail-html-container');
  const textBox = document.getElementById('detail-text-container');

  subj.textContent = msg.subject || '(No Subject)';
  sName.textContent = msg.from || 'Unknown Sender';
  sAddr.textContent = `To: ${state.activeAccount ? state.activeAccount.address : ''}`;
  avatar.textContent = (msg.from || 'S').charAt(0).toUpperCase();
  dDate.textContent = msg.date || 'Just now';

  // OTP Hero Banner
  if (msg.otpCode) {
    otpHero.style.display = 'flex';
    otpDigits.textContent = msg.otpCode;
  } else {
    otpHero.style.display = 'none';
  }

  // Pre-fill text
  htmlBox.innerHTML = '<div style="padding: 20px; color: #888;">Loading full email body...</div>';
  textBox.textContent = msg.text || msg.intro || '';

  modal.classList.add('active');

  // Fetch full detail if not simulation
  if (msg.id && msg.id.startsWith('sim_')) {
    htmlBox.innerHTML = msg.html || msg.text || '';
    textBox.textContent = msg.text || msg.intro || '';
    return;
  }

  const backend = getBackendBaseUrl();
  if (backend !== null) {
    try {
      const res = await fetch(`${backend}/api/message-detail`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          id: msg.id,
          type: state.activeAccount.type,
          token: state.activeAccount.token,
          sidToken: state.activeAccount.sidToken,
          baseUrl: state.activeAccount.baseUrl
        })
      });

      if (res.ok) {
        const data = await res.json();
        if (data.ok && data.message) {
          state.currentDetailMessage = { ...msg, ...data.message };
          htmlBox.innerHTML = data.message.html || `<div style="padding: 16px; white-space: pre-wrap;">${escapeHtml(data.message.text || '')}</div>`;
          textBox.textContent = data.message.text || '';

          if (data.message.otpCode) {
            otpHero.style.display = 'flex';
            otpDigits.textContent = data.message.otpCode;
          }
          return;
        }
      }
    } catch (e) {}
  }

  // Direct Guerrilla fetch_email fallback
  if (state.activeAccount && state.activeAccount.type === 'guerrilla' && state.activeAccount.sidToken) {
    try {
      const gRes = await fetch(`https://api.guerrillamail.com/ajax.php?f=fetch_email&email_id=${msg.id}&sid_token=${state.activeAccount.sidToken}`);
      const gData = await gRes.json();
      if (gData && (gData.mail_body || gData.mail_excerpt)) {
        const fullContent = gData.mail_body || `<div style="padding: 16px; white-space: pre-wrap;">${escapeHtml(gData.mail_excerpt || '')}</div>`;
        const rawOtp = extractOtp((gData.mail_subject || '') + ' ' + (gData.mail_body || '') + ' ' + (gData.mail_excerpt || ''));
        state.currentDetailMessage = {
          ...msg,
          ...gData,
          html: fullContent,
          text: gData.mail_excerpt || '',
          otpCode: rawOtp || msg.otpCode
        };
        htmlBox.innerHTML = fullContent;
        textBox.textContent = gData.mail_excerpt || '';
        if (rawOtp) {
          otpHero.style.display = 'flex';
          otpDigits.textContent = rawOtp;
        }
        return;
      }
    } catch (gErr) {
      console.log('Error fetching email body:', gErr);
    }
  }

  htmlBox.innerHTML = `<div style="padding: 16px; white-space: pre-wrap;">${escapeHtml(msg.intro || 'No body preview available')}</div>`;
}

function switchDetailTab(tab) {
  state.activeDetailTab = tab;
  const tabHtml = document.getElementById('tab-html');
  const tabText = document.getElementById('tab-text');
  const htmlBox = document.getElementById('detail-html-container');
  const textBox = document.getElementById('detail-text-container');

  if (tab === 'html') {
    tabHtml.classList.add('active');
    tabText.classList.remove('active');
    htmlBox.style.display = 'block';
    textBox.style.display = 'none';
  } else {
    tabText.classList.add('active');
    tabHtml.classList.remove('active');
    htmlBox.style.display = 'none';
    textBox.style.display = 'block';
  }
}

function copyDetailOtp() {
  if (state.currentDetailMessage && state.currentDetailMessage.otpCode) {
    copyToClipboard(state.currentDetailMessage.otpCode);
    showToast(TRANSLATIONS[state.currentLang].copiedOtp);
  }
}

function copyOtpFromCard(code) {
  copyToClipboard(code);
  showToast(TRANSLATIONS[state.currentLang].copiedOtp);
}

function copyDetailBody() {
  if (state.currentDetailMessage) {
    const text = state.currentDetailMessage.text || state.currentDetailMessage.intro || '';
    copyToClipboard(text);
    showToast('✓ Email body copied!');
  }
}

async function deleteCurrentEmail() {
  if (!state.currentDetailMessage) return;
  const id = state.currentDetailMessage.id;

  const backend = getBackendBaseUrl();
  if (backend !== null) {
    try {
      await fetch(`${backend}/api/delete-message`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          id: id,
          type: state.activeAccount.type,
          token: state.activeAccount.token,
          sidToken: state.activeAccount.sidToken,
          baseUrl: state.activeAccount.baseUrl
        })
      });
    } catch (e) {}
  }

  // Direct Guerrilla delete call
  if (state.activeAccount && state.activeAccount.type === 'guerrilla' && state.activeAccount.sidToken) {
    try {
      await fetch(`https://api.guerrillamail.com/ajax.php?f=del_email&email_ids[]=${id}&sid_token=${state.activeAccount.sidToken}`);
    } catch (gErr) {}
  }

  state.messages = state.messages.filter(m => m.id !== id);
  closeModal('modal-email-detail');
  renderMessagesList();
  showToast('✓ Email deleted');
}

// ==========================================
// 10. TIMER & EXTENSION ENGINE
// ==========================================
function startTimer() {
  if (state.timerInterval) clearInterval(state.timerInterval);
  updateTimerDisplay();

  state.timerInterval = setInterval(() => {
    state.timerSeconds--;
    if (state.timerSeconds <= 0) {
      state.timerSeconds = 0;
      updateTimerDisplay();
      clearInterval(state.timerInterval);
      showToast('⚠️ Mailbox timer expired. Extend or refresh.');
    } else {
      updateTimerDisplay();
    }
  }, 1000);
}

function updateTimerDisplay() {
  const digits = document.getElementById('timer-digits');
  if (!digits) return;

  const mins = Math.floor(state.timerSeconds / 60);
  const secs = state.timerSeconds % 60;
  digits.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

  digits.classList.remove('warning', 'danger');
  if (state.timerSeconds <= 60) {
    digits.classList.add('danger');
  } else if (state.timerSeconds <= 180) {
    digits.classList.add('warning');
  }
}

function extendTimer(secondsToAdd) {
  state.timerSeconds += secondsToAdd;
  updateTimerDisplay();
  startTimer();
  showToast(`⏳ Extended by +${Math.floor(secondsToAdd / 60)} minutes!`);
}

function resetTimer() {
  state.timerSeconds = APP_CONFIG.defaultTimerSeconds;
  updateTimerDisplay();
  startTimer();
  showToast('🔄 Timer reset to 10 minutes!');
}

// ==========================================
// 11. MODAL CONTROLLERS & ACTIONS
// ==========================================
function openModal(modalId) {
  const m = document.getElementById(modalId);
  if (m) m.classList.add('active');
}

function closeModal(modalId) {
  const m = document.getElementById(modalId);
  if (m) m.classList.remove('active');
}

// Close modal when clicking backdrop
document.addEventListener('click', (e) => {
  if (e.target.classList.contains('modal-backdrop')) {
    e.target.classList.remove('active');
  }
});

function copyEmailAddress() {
  if (!state.activeAccount) return;
  copyToClipboard(state.activeAccount.address);
  showToast(TRANSLATIONS[state.currentLang].copied);
}

// Domains Modal
function openDomainModal() {
  renderDomainsModalList();
  openModal('modal-domains');
}

function renderDomainsModalList(filter = '') {
  const container = document.getElementById('domains-list-container');
  if (!container) return;

  const q = filter.toLowerCase();
  const list = state.domains.filter(d => 
    d.domain.toLowerCase().includes(q) || 
    (d.provider && d.provider.toLowerCase().includes(q))
  );

  container.innerHTML = '';
  list.forEach(d => {
    const isCurrent = state.activeAccount && state.activeAccount.domain === d.domain;
    const card = document.createElement('div');
    card.className = `domain-item-card ${isCurrent ? 'active' : ''}`;
    card.onclick = () => {
      closeModal('modal-domains');
      generateNewRandomEmail(d.domain);
    };

    card.innerHTML = `
      <div class="domain-item-info">
        <div class="domain-name-text">${d.icon || '🌐'} @${d.domain}</div>
        <div class="domain-provider-text">${d.provider || 'High Speed Engine'}</div>
      </div>
      <span class="domain-badge-pill">${d.badge || 'Verified'}</span>
    `;

    container.appendChild(card);
  });
}

function filterDomainsList(val) {
  renderDomainsModalList(val);
}

// Custom Modal
function openCustomModal() {
  populateDomainSelects();
  updateCustomPreview();
  openModal('modal-custom');
}

function updateCustomPreview() {
  const user = document.getElementById('custom-username-input').value.trim() || 'username';
  const dom = document.getElementById('custom-domain-select').value || 'sharklasers.com';
  const cleanUser = user.toLowerCase().replace(/[^a-z0-9._-]/g, '');
  document.getElementById('custom-address-preview').textContent = `${cleanUser}@${dom}`;
}

async function submitCustomMailbox() {
  const userInp = document.getElementById('custom-username-input');
  const domSel = document.getElementById('custom-domain-select');
  const user = userInp.value.trim().toLowerCase().replace(/[^a-z0-9._-]/g, '');
  const domain = domSel.value;

  if (user.length < 3) {
    showToast('⚠️ Username must be at least 3 characters');
    return;
  }

  const btn = document.getElementById('btn-create-custom');
  if (btn) btn.textContent = 'Creating...';

  try {
    const res = await fetch('/api/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ domain: domain, customUser: user })
    });

    if (res.ok) {
      const data = await res.json();
      if (data.ok && data.mailbox) {
        setNewActiveAccount(data.mailbox);
        closeModal('modal-custom');
        showToast('✓ Custom Mailbox Created!');
        return;
      }
    }
    throw new Error('API failed');
  } catch (e) {
    // Guerrilla direct fallback
    try {
      const gRes = await fetch('https://api.guerrillamail.com/ajax.php?f=get_email_address');
      const gData = await gRes.json();
      await fetch(`https://api.guerrillamail.com/ajax.php?f=set_email_user&email_user=${user}&domain=${domain}&sid_token=${gData.sid_token}`);
      
      const mailbox = {
        address: `${user}@${domain}`,
        username: user,
        domain: domain,
        type: 'guerrilla',
        sidToken: gData.sid_token,
        createdAt: Date.now()
      };
      setNewActiveAccount(mailbox);
      closeModal('modal-custom');
      showToast('✓ Custom Mailbox Created!');
    } catch (err) {
      showToast('❌ Error creating custom mailbox');
    }
  } finally {
    if (btn) btn.textContent = '⚡ Create Mailbox';
  }
}

// Vault Modal
function openVaultModal() {
  renderVaultList();
  openModal('modal-vault');
}

function updateSavedCountBadge() {
  const badge = document.getElementById('saved-count-badge');
  if (badge) badge.textContent = state.savedAccounts.length;
}

function saveCurrentAccountToVault() {
  if (!state.activeAccount) return;
  const exists = state.savedAccounts.some(a => a.address === state.activeAccount.address);
  if (exists) {
    showToast('Address is already saved in your vault!');
    return;
  }

  state.savedAccounts.push({
    ...state.activeAccount,
    savedAt: new Date().toLocaleDateString()
  });

  localStorage.setItem('tmp_saved_vault', JSON.stringify(state.savedAccounts));
  updateSavedCountBadge();
  renderVaultList();
  showToast('💾 Address saved to Vault!');
}

function renderVaultList() {
  const container = document.getElementById('vault-items-list');
  if (!container) return;

  if (state.savedAccounts.length === 0) {
    container.innerHTML = '<div style="text-align: center; padding: 24px; color: var(--text-dim);">No saved accounts yet. Click "Save Current Address" to store your burner addresses here.</div>';
    return;
  }

  container.innerHTML = '';
  state.savedAccounts.forEach((acc, idx) => {
    const isCurrent = state.activeAccount && state.activeAccount.address === acc.address;
    const item = document.createElement('div');
    item.className = 'vault-item-card';

    item.innerHTML = `
      <div>
        <div class="vault-address-text">${escapeHtml(acc.address)}</div>
        <small style="color: var(--text-dim);">${acc.savedAt || 'Saved'} ${isCurrent ? '• (Active)' : ''}</small>
      </div>
      <div class="vault-actions-group">
        <button class="btn btn-outline" style="padding: 4px 8px; font-size: 0.75rem;" onclick="switchVaultAccount(${idx})">
          ${isCurrent ? 'Active' : 'Switch'}
        </button>
        <button class="btn btn-outline" style="padding: 4px 8px; font-size: 0.75rem;" onclick="copyToClipboard('${acc.address}'); showToast('✓ Address copied!');">
          Copy
        </button>
        <button class="btn btn-danger" style="padding: 4px 8px; font-size: 0.75rem;" onclick="deleteVaultAccount(${idx})">
          ✕
        </button>
      </div>
    `;

    container.appendChild(item);
  });
}

function switchVaultAccount(index) {
  const target = state.savedAccounts[index];
  if (!target) return;
  setNewActiveAccount(target);
  closeModal('modal-vault');
  showToast(`✓ Switched to ${target.address}`);
}

function deleteVaultAccount(index) {
  state.savedAccounts.splice(index, 1);
  localStorage.setItem('tmp_saved_vault', JSON.stringify(state.savedAccounts));
  updateSavedCountBadge();
  renderVaultList();
  showToast('✓ Address removed from vault');
}

function exportVaultAccounts() {
  if (state.savedAccounts.length === 0) {
    showToast('Vault is empty!');
    return;
  }
  const lines = state.savedAccounts.map(a => `${a.address} (Created: ${a.savedAt || 'N/A'})`).join('\n');
  const blob = new Blob([lines], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `temp_mail_pro_vault_${Date.now()}.txt`;
  a.click();
  URL.revokeObjectURL(url);
  showToast('📥 Vault exported!');
}

// Telegram Modal
function openTelegramModal() {
  const customInp = document.getElementById('custom-backend-url-input');
  if (customInp) {
    customInp.value = localStorage.getItem('tmp_backend_url') || '';
  }
  const chatIdInput = document.getElementById('telegram-chat-id-input');
  if (chatIdInput && state.telegramSettings.chatId) {
    chatIdInput.value = state.telegramSettings.chatId;
  }
  const autoToggle = document.getElementById('tg-auto-forward-toggle');
  if (autoToggle) {
    autoToggle.checked = state.telegramSettings.autoForward !== false;
  }
  openModal('modal-telegram');
}

function saveTelegramSettings() {
  const input = document.getElementById('telegram-chat-id-input');
  const toggle = document.getElementById('tg-auto-forward-toggle');
  const customInp = document.getElementById('custom-backend-url-input');

  state.telegramSettings.chatId = input ? input.value.trim() : '';
  state.telegramSettings.autoForward = toggle ? toggle.checked : true;

  localStorage.setItem('tmp_telegram', JSON.stringify(state.telegramSettings));

  if (customInp) {
    const val = customInp.value.trim();
    if (val) {
      localStorage.setItem('tmp_backend_url', val);
    } else {
      localStorage.removeItem('tmp_backend_url');
    }
  }

  closeModal('modal-telegram');
  showToast('✓ Settings Saved!');
}

async function sendTestTelegramAlert() {
  const input = document.getElementById('telegram-chat-id-input');
  const chatId = input ? input.value.trim() : '';
  if (!chatId) {
    showToast('⚠️ Please enter your Telegram Chat ID first');
    return;
  }

  const backend = getBackendBaseUrl();
  if (backend !== null) {
    try {
      const res = await fetch(`${backend}/api/forward`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          chatId: chatId,
          email: state.activeAccount ? state.activeAccount.address : 'user@sharklasers.com',
          sender: 'Temp Mail Pro Web Verification',
          subject: '🚀 Telegram Test Notification',
          preview: 'This is a test notification from Temp Mail Pro. Your Telegram connection is working successfully!',
          otpCode: '849201'
        })
      });

      if (res.ok) {
        showToast('✓ Test message sent to your Telegram!');
        return;
      }
    } catch (e) {}
  }

  showToast('💡 Connect your Render backend URL in settings to forward via Telegram Bot!');
}

async function forwardCurrentEmailToTelegram() {
  if (!state.currentDetailMessage) return;
  const chatId = state.telegramSettings.chatId;
  if (!chatId) {
    openTelegramModal();
    showToast('⚠️ Please configure your Telegram Chat ID first');
    return;
  }

  await forwardEmailToTelegram(chatId, state.currentDetailMessage);
  showToast('✓ Forwarding to Telegram...');
}

async function forwardEmailToTelegram(chatId, msg) {
  const backend = getBackendBaseUrl();
  if (backend !== null) {
    try {
      await fetch(`${backend}/api/forward`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          chatId: chatId,
          email: state.activeAccount ? state.activeAccount.address : '',
          sender: msg.from,
          subject: msg.subject,
          preview: msg.intro || msg.snippet || msg.text || '',
          otpCode: msg.otpCode || null
        })
      });
    } catch (e) {}
  }
}

// Developer & Contact Modal
function openDevModal() {
  openModal('modal-dev');
}

async function submitContactMessage() {
  const name = document.getElementById('contact-name').value.trim();
  const email = document.getElementById('contact-email').value.trim();
  const category = document.getElementById('contact-category').value;
  const message = document.getElementById('contact-message').value.trim();

  if (!message) {
    showToast('⚠️ Please enter a message before sending');
    return;
  }

  const btn = document.getElementById('btn-send-contact');
  if (btn) btn.textContent = 'Delivering message...';

  const backend = getBackendBaseUrl();
  if (backend !== null) {
    try {
      const res = await fetch(`${backend}/api/contact`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name,
          senderContact: email,
          category,
          message
        })
      });

      if (res.ok) {
        showToast('✓ Message sent directly to MD RASEL!');
        document.getElementById('contact-message').value = '';
        closeModal('modal-dev');
        if (btn) btn.textContent = '🚀 Send Message to Developer';
        return;
      }
    } catch (e) {}
  }

  // Pure Standalone / GitHub Pages Mode: Direct WhatsApp link to MD RASEL
  const formattedText = `Hi MD RASEL (Hanter XD Official),\n\n*Name:* ${name || 'User'}\n*Contact:* ${email || 'Not specified'}\n*Category:* ${category}\n\n*Message:*\n${message}`;
  const waUrl = `https://wa.me/8801882278234?text=${encodeURIComponent(formattedText)}`;
  window.open(waUrl, '_blank');

  showToast('✓ Opening WhatsApp direct message to MD RASEL!');
  document.getElementById('contact-message').value = '';
  closeModal('modal-dev');
  if (btn) btn.textContent = '🚀 Send Message to Developer';
}

// QR Code Modal
function openQrModal() {
  if (!state.activeAccount) return;
  const qrContainer = document.getElementById('qr-code-container');
  const qrText = document.getElementById('qr-email-text');
  
  qrText.textContent = state.activeAccount.address;
  // Generate QR Code SVG
  const encodedAddress = encodeURIComponent(state.activeAccount.address);
  qrContainer.innerHTML = `<img src="https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=mailto:${encodedAddress}" alt="QR Code" style="border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.2);">`;
  
  openModal('modal-qr');
}

// Panic Shredder
function openPanicModal() {
  openModal('modal-panic');
}

function executePanicShred() {
  localStorage.removeItem('tmp_active_account');
  localStorage.removeItem('tmp_saved_vault');
  state.savedAccounts = [];
  state.messages = [];
  state.knownMessageIds.clear();
  updateSavedCountBadge();
  closeModal('modal-panic');
  closeModal('modal-vault');
  
  generateNewRandomEmail();
  showToast('💥 Session Shredded! Fresh burner identity created.');
}

// APK Download Modal
function openApkModal() {
  openModal('modal-apk');
}

// Check for Incoming Social Media Verification Codes (Real live mail only)
async function checkIncomingOtpNow() {
  const btn = document.querySelector('.btn-check-otp');
  if (btn) btn.style.opacity = '0.6';

  showToast('🔄 Checking server for incoming social media OTP...');
  const countBefore = state.messages.length;
  await fetchInboxMessages(false);

  if (btn) btn.style.opacity = '1';

  const countAfter = state.messages.length;
  if (countAfter > countBefore) {
    playNotificationChime();
    showToast('📬 Social media verification email received!');
  } else if (countAfter === 0) {
    showToast('⏳ No OTP email yet. Sign up on Facebook, Telegram, Google, etc., and your code will arrive here!');
  } else {
    showToast('✅ Mailbox checked — up to date.');
  }
}

// ==========================================
// 12. UTILITIES
// ==========================================
function extractOtp(text) {
  if (!text) return null;
  const clean = text.replace(/<[^>]*>/g, ' ');

  // 1. Google Verification format: G-123456
  const googleMatch = clean.match(/G-(\d{5,7})\b/i);
  if (googleMatch && googleMatch[1]) return googleMatch[1];

  // 2. WhatsApp hyphenated format: 123-456
  const waMatch = clean.match(/\b(\d{3})-(\d{3})\b/);
  if (waMatch && waMatch[1] && waMatch[2]) return `${waMatch[1]}${waMatch[2]}`;

  // 3. Social Media Prefix pattern: "123456 is your Facebook/Instagram/TikTok/Google/Discord/Twitter/Telegram code"
  const socialPrefixMatch = clean.match(/\b(\d{4,8})\b\s+is\s+your\s+(?:[\w\s-]{1,25})?(?:code|confirmation|verification|login)/i);
  if (socialPrefixMatch && socialPrefixMatch[1]) return socialPrefixMatch[1];

  // 4. Meta / Facebook / Telegram: "Confirmation code: 446457", "Telegram code: 12345"
  const metaMatch = clean.match(/(?:confirmation\s*code|security\s*code|verification\s*code|login\s*code|telegram\s*code)\s*[:=-]?\s*(\b\d{4,8}\b)/i);
  if (metaMatch && metaMatch[1]) return metaMatch[1];

  // 5. Standard pattern: "OTP is 123456", "code: 123456", "PIN: 1234"
  const otpMatch = clean.match(/(?:code|otp|pin|passcode)[\s\w:]{0,20}?(\b\d{4,8}\b)/i);
  if (otpMatch && otpMatch[1]) return otpMatch[1];

  // 6. Standalone 6-digit number (common worldwide social media standard)
  const standalone6 = clean.match(/\b\d{6}\b/);
  if (standalone6) return standalone6[0];

  // 7. Standalone 5-digit number (Telegram code standard)
  const standalone5 = clean.match(/\b\d{5}\b/);
  if (standalone5) return standalone5[0];

  return null;
}

function copyToClipboard(text) {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(text).catch(() => fallbackCopy(text));
  } else {
    fallbackCopy(text);
  }
}

function fallbackCopy(text) {
  const ta = document.createElement('textarea');
  ta.value = text;
  ta.style.position = 'fixed';
  ta.style.opacity = '0';
  document.body.appendChild(ta);
  ta.select();
  document.execCommand('copy');
  document.body.removeChild(ta);
}

let toastTimer = null;
function showToast(msg) {
  const toast = document.getElementById('toast-notification');
  if (!toast) return;
  toast.textContent = msg;
  toast.classList.add('show');
  
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toast.classList.remove('show');
  }, 3200);
}

function escapeHtml(str) {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
