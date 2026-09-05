# Temp Mail Pro — 24/7 Telegram Bot Deployment Guide on Render.com
**বিকাশকারী:** MD RASEL (Hanter XD Official)  
**বট ইউজারনেম:** `@TEMPMAILPRO34_bot`  
**টোকেন:** `8659662216:AAHfxsv6-XG3k2K75lMWfidI10T2KeGEXwI`

---

## 📌 কেন Render-এ ডিপ্লয় করবেন?
অন্যান্য ফ্রি হোস্টিংয়ে কোটা লিমিট বা স্লিপ মোডের কারণে বট কিছু সময় পর বন্ধ হয়ে যায়। এই ফোল্ডারে এমনভাবে কোড লেখা হয়েছে যাতে একটি লাইভ Express Web Server এর সাথে বটটি চালু থাকে। এর ফলে **UptimeRobot** বা **cron-job.org** দিয়ে বিনামূল্যে ২৪ ঘণ্টা ৭ দিন বট সচল রাখা যায়।

---

## 🚀 সম্পূর্ণ ডিপ্লয়মেন্ট প্রক্রিয়া (ধাপে ধাপে বাংলায়):

### ধাপ ১: GitHub-এ কোড পুশ নিশ্চিত করুন
আপনার সম্পূর্ণ রিপোজিটরিটি (`TEMP_MAIL_PRO`) GitHub-এ পুশ করা থাকতে হবে:
- রিপোজিটরি লিংক: `https://github.com/HANTER-XD-OFFICIAL/TEMP_MAIL_PRO`

### ধাপ ২: Render.com-এ একাউন্ট খুলুন ও লগইন করুন
1. ব্রাউজারে যান: [https://render.com](https://render.com)
2. **GET STARTED FOR FREE** বা **Sign in with GitHub**-এ ক্লিক করে আপনার GitHub একাউন্ট দিয়ে লগইন করুন।

### ধাপ ৩: New Web Service তৈরি করুন
1. Render ড্যাশবোর্ডে গিয়ে উপরে ডানপাশে **New +** বাটনে ক্লিক করে **Web Service** সিলেক্ট করুন।
2. **Build and deploy from a Git repository** অপশন সিলেক্ট করে **Next** দিন।
3. আপনার `TEMP_MAIL_PRO` রিপোজিটরি সিলেক্ট করুন (অথবা Public Git repository URL দিয়ে `https://github.com/HANTER-XD-OFFICIAL/TEMP_MAIL_PRO` বসিয়ে দিন)।

### ধাপ ৪: সেটিংস কনফিগারেশন করুন
নিচের তথ্যগুলো যথাযথভাবে পূরণ করুন:
- **Name:** `temp-mail-pro-bot` (বা যেকোনো নাম)
- **Region:** `Singapore` (বাংলাদেশ থেকে দ্রুত রেসপন্সের জন্য সবচেয়ে ভালো)
- **Branch:** `main`
- **Root Directory:** `telegram-bot` ⚠️ *(এটি অবশ্যই `telegram-bot` লিখবেন)*
- **Runtime:** `Node`
- **Build Command:** `npm install`
- **Start Command:** `npm start`
- **Instance Type:** `Free` ($0/month)

### ধাপ ৫: Environment Variables (সিক্রেট টোকেন) যোগ করুন
নিচে **Environment Variables** সেকশনে যান এবং **Add Environment Variable** এ ক্লিক করুন:
1. **Key:** `BOT_TOKEN`  
   **Value:** `8659662216:AAHfxsv6-XG3k2K75lMWfidI10T2KeGEXwI`
2. **Key:** `PORT`  
   **Value:** `3000`

### ধাপ ৬: ডিপ্লয় শুরু করুন
- নিচে স্ক্রোল করে **Deploy Web Service** বাটনে ক্লিক করুন।
- ২-৩ মিনিটের মধ্যে ডিপ্লয় সম্পন্ন হবে এবং লগ ফাইলে দেখতে পাবেন:
  `🚀 Temp Mail Pro Bot Server running on port 3000`  
  `🤖 Bot Username: @TEMPMAILPRO34_bot`
- Render আপনাকে একটি ফ্রি লাইভ URL দেবে (যেমন: `https://temp-mail-pro-bot.onrender.com`)।

---

## 🕒 ধাপ ৭: বটকে ২৪ ঘণ্টা কখনো স্লিপ না হতে দেওয়ার ট্রিক (100% Free 24/7 Uptime)
Render-এর ফ্রি প্ল্যানে ১৫ মিনিট কোনো ট্রাফিক না থাকলে সার্ভার সাময়িক স্লিপ মোডে চলে যায়। এটিকে সবসময় ১০০% জেগে রাখার সহজ উপায়:

1. [https://uptimerobot.com](https://uptimerobot.com) অথবা [https://cron-job.org](https://cron-job.org) এ যান এবং একটি ফ্রি একাউন্ট খুলুন।
2. **Add New Monitor** এ ক্লিক করুন।
3. **Monitor Type:** `HTTP(s)` সিলেক্ট করুন।
4. **Friendly Name:** `Temp Mail Telegram Bot`
5. **URL (or IP):** Render থেকে পাওয়া আপনার বটের URL দিন (যেমন: `https://temp-mail-pro-bot.onrender.com/health`)
6. **Monitoring Interval:** `Every 5 minutes` সিলেক্ট করুন।
7. **Create Monitor** এ চাপুন।

🎯 **অভিনন্দন!** এখন প্রতি ৫ মিনিট পর পর UptimeRobot আপনার Render সার্ভারকে একটি পিং পাঠাবে, যার ফলে আপনার টেলিগ্রাম বটটি Render-এ কখনোই বন্ধ বা স্লিপ হবে না এবং মাসব্যাপী ২৪ ঘণ্টা সম্পূর্ণ ফ্রিতে সচল থাকবে!
