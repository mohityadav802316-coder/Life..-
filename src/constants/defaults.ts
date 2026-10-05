import {
  MeditationTypeInfo,
  ExpenseCategory,
  ExpenseQuickChip,
  Song,
  Playlist,
  UserSettings,
  DashboardSectionKey
} from '../types';

export const MEDITATION_TYPES: MeditationTypeInfo[] = [
  {
    id: 'breathing',
    englishTitle: 'Breathing',
    hindiTitle: 'श्वास ध्यान',
    description: 'Box & rhythmic breathing: Inhale, Hold, Exhale with clear voice & soft bell cues',
    inhaleSec: 4,
    holdSec: 4,
    exhaleSec: 4,
    restSec: 2,
    iconTag: 'air'
  },
  {
    id: 'mindfulness',
    englishTitle: 'Mindfulness',
    hindiTitle: 'सचेतन ध्यान',
    description: 'Present-moment awareness, body grounding, observing thoughts without judgment',
    inhaleSec: 5,
    holdSec: 0,
    exhaleSec: 5,
    restSec: 0,
    iconTag: 'mind'
  },
  {
    id: 'focus',
    englishTitle: 'Focus',
    hindiTitle: 'एकाग्रता ध्यान',
    description: 'Laser concentration, single-pointed awareness and breath counting',
    inhaleSec: 4,
    holdSec: 2,
    exhaleSec: 4,
    restSec: 2,
    iconTag: 'focus'
  },
  {
    id: 'relaxation',
    englishTitle: 'Relaxation',
    hindiTitle: 'गहरा विश्राम',
    description: 'Progressive muscle relaxation, releasing physical tension from crown to toes',
    inhaleSec: 6,
    holdSec: 2,
    exhaleSec: 6,
    restSec: 2,
    iconTag: 'relax'
  },
  {
    id: 'sleep',
    englishTitle: 'Sleep',
    hindiTitle: 'गहरी नींद',
    description: '4-7-8 calming delta frequency flow for deep, restorative sleep induction',
    inhaleSec: 4,
    holdSec: 7,
    exhaleSec: 8,
    restSec: 2,
    iconTag: 'sleep'
  },
  {
    id: 'morning',
    englishTitle: 'Morning',
    hindiTitle: 'सवेरे की ऊर्जा',
    description: 'Energizing morning breath, oxygenation and positive intention setting',
    inhaleSec: 4,
    holdSec: 2,
    exhaleSec: 4,
    restSec: 1,
    iconTag: 'morning'
  },
  {
    id: 'visualization',
    englishTitle: 'Visualization',
    hindiTitle: 'सकारात्मक कल्पना',
    description: 'Mental sanctuary of golden healing light, calm strength and empowerment',
    inhaleSec: 5,
    holdSec: 2,
    exhaleSec: 5,
    restSec: 2,
    iconTag: 'vision'
  },
  {
    id: 'sound',
    englishTitle: 'Sound',
    hindiTitle: 'नाद ध्यान',
    description: 'Tibetan singing bowl harmonics, resonant frequency and profound inner silence',
    inhaleSec: 6,
    holdSec: 2,
    exhaleSec: 6,
    restSec: 2,
    iconTag: 'sound'
  },
  {
    id: 'gratitude',
    englishTitle: 'Gratitude',
    hindiTitle: 'कृतज्ञता भाव',
    description: 'Heart-centered awareness, blessing reflection and deep contentment',
    inhaleSec: 5,
    holdSec: 2,
    exhaleSec: 5,
    restSec: 2,
    iconTag: 'heart'
  },
  {
    id: 'walking',
    englishTitle: 'Walking',
    hindiTitle: 'सजग गति',
    description: 'Mindful pacing, grounded presence and synchronous rhythmic movement',
    inhaleSec: 4,
    holdSec: 0,
    exhaleSec: 4,
    restSec: 0,
    iconTag: 'walk'
  }
];

export const DEFAULT_EXPENSE_CATEGORIES: ExpenseCategory[] = [
  {
    id: 'food',
    nameHi: 'खाना-पीना',
    nameEn: 'Food',
    emoji: '🍔',
    colorHex: '#FF9800',
    keywords: 'चाय,chai,tea,coffee,खाना,khana,lunch,dinner,nashta,breakfast,roti,sabzi,pizza,burger,biryani,samosa,maggi,swiggy,zomato,cafe,restaurant,dhaba,hotel,snacks,biscuit,mithai,sweet,juice,lassi,cold drink',
    orderIndex: 0,
    monthlyBudget: 8000
  },
  {
    id: 'travel',
    nameHi: 'यात्रा / पेट्रोल',
    nameEn: 'Travel',
    emoji: '🚗',
    colorHex: '#C9A15B',
    keywords: 'ऑटो,auto,rickshaw,riksha,cab,taxi,uber,ola,rapido,bus,metro,train,flight,ticket,fare,kiraya,petrol,diesel,fuel,cng,toll,parking,puncture,bike,car,scooty',
    orderIndex: 1,
    monthlyBudget: 3500
  },
  {
    id: 'grocery',
    nameHi: 'किराना / राशन',
    nameEn: 'Grocery',
    emoji: '🛒',
    colorHex: '#7D9B76',
    keywords: 'दूध,milk,doodh,curd,dahi,paneer,ration,kirana,dmart,bazaar,bazar,sabji,sabzi,fruit,fal,vegetable,aalu,pyaaz,tamatar,tel,oil,ghee,atta,rice,chawal,daal,masala,soap,surf',
    orderIndex: 2,
    monthlyBudget: 5000
  },
  {
    id: 'bills',
    nameHi: 'बिल व रिचार्ज',
    nameEn: 'Bills',
    emoji: '💡',
    colorHex: '#96789C',
    keywords: 'recharge,mobile,phone bill,wifi,net,broadband,electricity,bijli,current,rent,room rent,water,paani,cylinder,gas,lpg,maintenance,ott,subscription,netflix,prime,spotify',
    orderIndex: 3,
    monthlyBudget: 4000
  },
  {
    id: 'shopping',
    nameHi: 'खरीदारी',
    nameEn: 'Shopping',
    emoji: '🛍️',
    colorHex: '#EC4899',
    keywords: 'कपड़े,kapde,clothes,shoes,joota,chappal,shopping,amazon,flipkart,myntra,meesho,shirt,tshirt,jeans,pant,kurta,watch,bag,electronics,mobile cover,perfume',
    orderIndex: 4,
    monthlyBudget: 3000
  },
  {
    id: 'health',
    nameHi: 'स्वास्थ्य व दवा',
    nameEn: 'Health',
    emoji: '💊',
    colorHex: '#7AA2BA',
    keywords: 'दवा,dawa,medicine,tablet,syrup,doctor,fees,clinic,medical,chemist,pharmacy,test,blood test,lab,hospital,injection,bandage,eye drop,vitamins',
    orderIndex: 5,
    monthlyBudget: 2000
  },
  {
    id: 'entertainment',
    nameHi: 'मनोरंजन',
    nameEn: 'Entertainment',
    emoji: '🎬',
    colorHex: '#F59E0B',
    keywords: 'movie,cinema,pvr,inox,popcorn,film,game,gaming,match,cricket,party,club,outing,picnic,trip,fun,fair,mela',
    orderIndex: 6,
    monthlyBudget: 1500
  },
  {
    id: 'education',
    nameHi: 'शिक्षा व पढ़ाई',
    nameEn: 'Education',
    emoji: '📚',
    colorHex: '#3B82F6',
    keywords: 'book,kitaab,pen,copy,register,notebook,stationary,pencil,fees,school,college,course,tuition,xerox,printout,form,exam',
    orderIndex: 7,
    monthlyBudget: 2000
  },
  {
    id: 'other',
    nameHi: 'अन्य',
    nameEn: 'Other',
    emoji: '📦',
    colorHex: '#857D6C',
    keywords: 'other,extra,misc,khracha,dost,borrow,loan,gift,puja,donation,daan',
    orderIndex: 8,
    monthlyBudget: 1000
  }
];

export const DEFAULT_QUICK_CHIPS: ExpenseQuickChip[] = [
  { id: 1, label: 'चाय ₹10', amount: 10, note: 'चाय', category: 'Food', emoji: '☕', usageCount: 10, orderIndex: 0 },
  { id: 2, label: 'चाय ₹20', amount: 20, note: 'चाय', category: 'Food', emoji: '☕', usageCount: 9, orderIndex: 1 },
  { id: 3, label: 'ऑटो ₹30', amount: 30, note: 'ऑटो', category: 'Travel', emoji: '🛺', usageCount: 8, orderIndex: 2 },
  { id: 4, label: 'ऑटो ₹50', amount: 50, note: 'ऑटो', category: 'Travel', emoji: '🛺', usageCount: 7, orderIndex: 3 },
  { id: 5, label: 'नाश्ता ₹50', amount: 50, note: 'नाश्ता', category: 'Food', emoji: '🥪', usageCount: 6, orderIndex: 4 },
  { id: 6, label: 'लंच ₹120', amount: 120, note: 'लंच', category: 'Food', emoji: '🍱', usageCount: 5, orderIndex: 5 },
  { id: 7, label: 'दूध ₹35', amount: 35, note: 'दूध', category: 'Grocery', emoji: '🥛', usageCount: 4, orderIndex: 6 },
  { id: 8, label: 'पेट्रोल ₹100', amount: 100, note: 'पेट्रोल', category: 'Travel', emoji: '⛽', usageCount: 3, orderIndex: 7 }
];

export const DEFAULT_SONGS: Song[] = [
  {
    id: 'synth_deep_focus',
    title: 'Alpha Wave Focus Flow',
    artist: 'Chrono Ambient Labs',
    album: 'Deep Work Frequencies',
    durationMs: 600000, // 10 mins
    contentUri: 'synth:alpha_focus',
    isFavorite: true,
    lastPlayedAt: Date.now(),
    playCount: 12,
    isBuiltIn: true,
    mood: 'FOCUS'
  },
  {
    id: 'synth_tibetan_calm',
    title: 'Tibetan Sing Harmonic Resonator',
    artist: 'Monastery Soundscapes',
    album: 'Inner Silence 432Hz',
    durationMs: 900000, // 15 mins
    contentUri: 'synth:tibetan_bowl',
    isFavorite: true,
    lastPlayedAt: 0,
    playCount: 8,
    isBuiltIn: true,
    mood: 'CALM'
  },
  {
    id: 'synth_morning_energy',
    title: 'Surya Prana Sunrise Wave',
    artist: 'Vitality Pulse',
    album: 'Dawn Awakening',
    durationMs: 480000,
    contentUri: 'synth:morning_pulse',
    isFavorite: false,
    lastPlayedAt: 0,
    playCount: 5,
    isBuiltIn: true,
    mood: 'ENERGETIC'
  },
  {
    id: 'synth_delta_sleep',
    title: 'Delta Deep Sleep Lullaby',
    artist: 'Night Sanctuary',
    album: 'Restorative Sleep Cycle',
    durationMs: 1200000,
    contentUri: 'synth:delta_sleep',
    isFavorite: false,
    lastPlayedAt: 0,
    playCount: 14,
    isBuiltIn: true,
    mood: 'SLEEP'
  },
  {
    id: 'synth_binaural_relax',
    title: 'Rainforest Rainfall & Forest Wind',
    artist: 'Nature Instruments',
    album: 'Zen Forest Atmosphere',
    durationMs: 600000,
    contentUri: 'synth:rain_nature',
    isFavorite: false,
    lastPlayedAt: 0,
    playCount: 7,
    isBuiltIn: true,
    mood: 'RELAX'
  },
  {
    id: 'synth_devotional_om',
    title: 'Sacred Pranava Om Resonator',
    artist: 'Ancient Vibrations',
    album: 'Cosmic Chants',
    durationMs: 720000,
    contentUri: 'synth:devotional_om',
    isFavorite: true,
    lastPlayedAt: 0,
    playCount: 9,
    isBuiltIn: true,
    mood: 'DEVOTIONAL'
  }
];

export const DEFAULT_PLAYLISTS: Playlist[] = [
  {
    id: 1,
    name: 'प्रातः साधना (Morning Routine)',
    description: 'ऊर्जावान और शांत तरंगें सवेरे के लिए',
    colorHex: '#C9A15B',
    icon: 'sunrise',
    createdAt: Date.now()
  },
  {
    id: 2,
    name: 'डीप वर्क (Deep Focus Study)',
    description: 'एकाग्रता और फ्लो स्टेट के लिए साउंड',
    colorHex: '#7AA2BA',
    icon: 'target',
    createdAt: Date.now()
  },
  {
    id: 3,
    name: 'संध्या विश्राम (Evening Relax)',
    description: 'दिन भर की थकान उतारने और ध्यान के लिए',
    colorHex: '#96789C',
    icon: 'coffee',
    createdAt: Date.now()
  },
  {
    id: 4,
    name: 'गहरी नींद (Sleep Sanctuary)',
    description: 'रात को सुखद नींद लाने वाला शांत संगीत',
    colorHex: '#7D9B76',
    icon: 'moon',
    createdAt: Date.now()
  }
];

export const DEFAULT_USER_SETTINGS: UserSettings = {
  id: 1,
  anchorDate: new Date().toISOString().split('T')[0],
  initializedDefaultRoutine: true,
  wakeUpMinutes: 300, // 5:00 AM
  isAlarmEnabled: true,
  snoozeMinutes: 10,
  alarmSoundType: 'DEFAULT',
  customSoundTitle: 'High-Tone Digital Alarm',
  alarmVolume: 1.0,
  isVibrationEnabled: true,
  alarmRepeatMode: 'DAILY',
  alarmCustomDaysMask: 127,
  isHapticFeedbackEnabled: true,
  smartReminderMinutes: 10,
  userName: 'मोहित',
  meditationChimeEnabled: true,
  meditationVoiceLanguage: 'HI',
  enableQuickAdd: true,
  enableMorningBrief: true,
  enableLifeTimeline: true,
  enablePersonalInsights: true,
  enableRecoveryMode: true,
  dailyEnergyMode: 'NORMAL',
  dailyEnergyModeDate: '',
  dailyChallengeEnabled: true,
  musicAutoRoutineEnabled: true,
  shortDailyLimit: 20,
  shortWarning50Enabled: true,
  shortWarning80Enabled: true,
  shortWarning100Enabled: true,
  shortFocusLockIntegration: true,
  shortTrackingEnabled: true,
  dashboardPreset: 'CUSTOM',
  dashboardSectionsOrder: 'NEXT_ACTIVITY,PRIORITY_TASK,TODAYS_ROUTINE,DAILY_PROGRESS,STREAK,ENERGY_MODE,SHORT_CONTENT_TRACKER,MEDITATION,MUSIC,DAILY_CHALLENGE,NEXT_ALARM,RECOVERY_DAY,WEEKLY_SUMMARY',
  dashboardDisabledSections: '',
  isFocusModeActive: false,
  isFocusScheduleEnabled: false,
  focusStartTimeMinutes: 1260, // 9:00 PM
  focusEndTimeMinutes: 360,    // 6:00 AM
  focusSessionEndTimestamp: 0,
  isBlackScreenEnabled: false,
  isBlackScreenOverlayActive: false,
  isFloatingDotEnabled: true,
  blackScreenDotX: 80,
  blackScreenDotY: 260,
  blackScreenDotSize: 36,
  blackScreenDotOpacity: 0.45,
  blackScreenActivationMethod: 'DOUBLE_TAP',
  blackScreenExitGesture: 'THREE_FINGER_TAP',
  blackScreenGestureSensitivity: 1.0,
  blackScreenShowClock: true,
  blackScreenClockFormat24: false,
  blackScreenRestoreAfterUnlock: true,
  blackScreenRestoreAfterReboot: true,
  currencySymbol: '₹',
  monthlyBudget: 30000
};

export const DAILY_CHALLENGES_POOL = [
  { category: 'Meditation', title: '10 मिनट मौन प्राणायाम', description: 'बिना किसी फोन या शोर के 10 मिनट सिर्फ श्वास पर ध्यान दें।', targetMinutes: 10 },
  { category: 'Reading', title: '15 पृष्ठ ज्ञानवर्धक पुस्तक', description: 'किसी अच्छी किताब या विषय के 15 पन्ने बिना रुके पढ़ें और मुख्य बिंदु लिखें।', targetMinutes: 20 },
  { category: 'Productivity', title: 'दिन का सबसे कठिन काम पहले', description: 'सुबह के पहले 90 मिनट सबसे जरूरी और भारी कार्य पर समर्पित करें।', targetMinutes: 90 },
  { category: 'Organization', title: 'कमरा और स्टडी टेबल क्लीनअप', description: 'अपने कार्यक्षेत्र से अनावश्यक चीजें हटाकर पूर्ण व्यवस्था बनाएं।', targetMinutes: 15 },
  { category: 'Fitness', title: '100 सूर्य नमस्कार / 5000 कदम वॉक', description: 'खुली हवा में शरीर को स्ट्रेच और ऊर्जावान बनाएं।', targetMinutes: 30 },
  { category: 'Self-Improvement', title: 'डिजिटल डिटॉक्स संध्या', description: 'रात 8:00 बजे के बाद सभी सोशल मीडिया और स्क्रीन बंद रखें।', targetMinutes: 120 },
  { category: 'Reflection', title: '3 कृतज्ञता बिंदु लिखें', description: 'आज के दिन की तीन बातें लिखें जिनके लिए आप वास्तव में आभारी हैं।', targetMinutes: 10 }
];
