export type TaskStatus = 'COMPLETE' | 'PARTIAL' | 'MISSED';

export type TaskPriority = 'NORMAL' | 'IMPORTANT' | 'HIGH';

export type ReflectionCategory = 'MISTAKE' | 'GOOD_DEED' | 'OBSERVATION';

export type DailyMood = 'GREAT' | 'GOOD' | 'NORMAL' | 'LOW' | 'BAD';

export type EnergyMode = 'LOW' | 'NORMAL' | 'HIGH';

export type MusicMood = 'CALM' | 'ENERGETIC' | 'HAPPY' | 'FOCUS' | 'RELAX' | 'SLEEP' | 'SAD' | 'DEVOTIONAL' | 'CUSTOM';

export type JournalistCategory = 'GOOD' | 'BAD' | 'OBSERVATION';

export type MainTab =
  | 'TODAY'
  | 'CALENDAR'
  | 'REPORT'
  | 'SETTINGS'
  | 'ALARM_CENTER'
  | 'MEDITATION'
  | 'MUSIC'
  | 'WEEK'
  | 'REFLECTION'
  | 'TIMELINE'
  | 'SHORT_CONTENT_TRACKER'
  | 'EXPENSE_DIARY';

export interface DayTask {
  id: number;
  date: string; // YYYY-MM-DD
  templateId?: number | null;
  name: string;
  timeMinutes: number; // Minutes from midnight
  category: string;
  status: TaskStatus;
  notes: string;
  isExtra: boolean;
  orderIndex: number;
  priority: TaskPriority;
}

export interface RoutineTemplate {
  id: number;
  activityKey: string;
  name: string;
  timeMinutes: number;
  category: string;
  notes: string;
  orderIndex: number;
  daysMask: number; // Bitmask for 7 days (1..7 all active = 127)
  isActive: boolean;
  priority: TaskPriority;
}

export interface Reflection {
  id: number;
  title: string;
  description: string;
  date: string; // YYYY-MM-DD
  timestamp: number;
  category: ReflectionCategory;
  tags: string;
}

export interface Goal {
  id: number;
  title: string;
  description: string;
  progress: number; // 0..100
  deadline?: string | null;
  isCompleted: boolean;
  createdAt: number;
}

export interface DailyNote {
  date: string;
  note: string;
  mood: DailyMood;
  updatedAt: number;
}

export interface UserSettings {
  id: number;
  anchorDate: string; // Date of Day 1 of 7-day Cycle (YYYY-MM-DD)
  initializedDefaultRoutine: boolean;
  wakeUpMinutes: number; // e.g. 300 = 5:00 AM
  isAlarmEnabled: boolean;
  snoozeMinutes: number;
  alarmSoundType: 'DEFAULT' | 'CUSTOM' | 'CHIME' | 'RADAR';
  customSoundUri?: string | null;
  customSoundTitle: string;
  alarmVolume: number;
  isVibrationEnabled: boolean;
  alarmRepeatMode: 'DAILY' | 'WEEKDAYS' | 'CUSTOM';
  alarmCustomDaysMask: number;
  isHapticFeedbackEnabled: boolean;
  smartReminderMinutes: number;
  userName: string;
  meditationChimeEnabled: boolean;
  meditationVoiceLanguage: 'HI' | 'EN';
  enableQuickAdd: boolean;
  enableMorningBrief: boolean;
  enableLifeTimeline: boolean;
  enablePersonalInsights: boolean;
  enableRecoveryMode: boolean;
  dailyEnergyMode: EnergyMode;
  dailyEnergyModeDate: string;
  dailyChallengeEnabled: boolean;
  musicAutoRoutineEnabled: boolean;
  shortDailyLimit: number;
  shortWarning50Enabled: boolean;
  shortWarning80Enabled: boolean;
  shortWarning100Enabled: boolean;
  shortFocusLockIntegration: boolean;
  shortTrackingEnabled: boolean;
  dashboardPreset: string;
  dashboardSectionsOrder: string;
  dashboardDisabledSections: string;
  isFocusModeActive: boolean;
  isFocusScheduleEnabled: boolean;
  focusStartTimeMinutes: number;
  focusEndTimeMinutes: number;
  focusSessionEndTimestamp: number;
  isBlackScreenEnabled: boolean;
  isBlackScreenOverlayActive: boolean;
  isFloatingDotEnabled: boolean;
  blackScreenDotX: number;
  blackScreenDotY: number;
  blackScreenDotSize: number;
  blackScreenDotOpacity: number;
  blackScreenActivationMethod: string;
  blackScreenExitGesture: string;
  blackScreenGestureSensitivity: number;
  blackScreenShowClock: boolean;
  blackScreenClockFormat24: boolean;
  blackScreenRestoreAfterUnlock: boolean;
  blackScreenRestoreAfterReboot: boolean;
  currencySymbol: string;
  monthlyBudget: number;
}

export interface DailySnapshot {
  date: string;
  dayOfCycle: number;
  totalTasks: number;
  completedCount: number;
  partialCount: number;
  missedCount: number;
  totalScore: number;
  completionPercentage: number;
  lastUpdated: number;
}

export interface StreakInfo {
  currentStreak: number;
  longestStreak: number;
  todayStatus: string;
  isMaintainedToday: boolean;
  activeDaysCount: number;
  consistencyScore: number;
}

export interface JournalistPerson {
  id: string;
  name: string;
  emoji: string;
  createdAt: number;
}

export interface JournalistEntry {
  id: string;
  personId: string;
  personName: string;
  category: JournalistCategory;
  text: string;
  timestamp: string;
  date: string;
  time: string;
  context: string;
  intensity: string;
  createdAt: number;
  updatedAt: number;
}

export interface MeditationTypeInfo {
  id: string;
  englishTitle: string;
  hindiTitle: string;
  description: string;
  inhaleSec: number;
  holdSec: number;
  exhaleSec: number;
  restSec: number;
  iconTag: string;
}

export interface MeditationSession {
  id: number;
  date: string;
  type: string;
  durationMinutes: number;
  completedSeconds: number;
  isCompleted: boolean;
  timestamp: number;
}

export interface Song {
  id: string;
  title: string;
  artist: string;
  album: string;
  durationMs: number;
  contentUri: string;
  albumArtUri?: string | null;
  isFavorite: boolean;
  lastPlayedAt: number;
  playCount: number;
  isBuiltIn: boolean;
  mood: MusicMood;
}

export interface Playlist {
  id: number;
  name: string;
  description: string;
  colorHex: string;
  icon: string;
  createdAt: number;
  songs?: Song[];
}

export interface PlaylistRule {
  id: number;
  playlistId: number;
  startMinutes: number;
  endMinutes: number;
  mood: MusicMood;
  situation: string;
  linkedCategory?: string | null;
  autoPlay: boolean;
}

export interface DailyChallenge {
  date: string;
  category: string;
  title: string;
  description: string;
  targetMinutes: number;
  isCompleted: boolean;
  completedAt: number;
  isSkipped: boolean;
}

export interface ShortContentRecord {
  id: number;
  date: string;
  appPackage: string;
  appName: string;
  videoSignature: string;
  timestamp: number;
  durationSeconds: number;
}

export interface ShortContentDailySummary {
  date: string;
  instagramCount: number;
  youtubeCount: number;
  facebookCount: number;
  otherCount: number;
  totalCount: number;
  totalTimeSeconds: number;
  dailyLimit: number;
  lastUpdated: number;
}

export interface Expense {
  id: number;
  amount: number;
  note: string;
  category: string;
  categoryEmoji: string;
  categoryColorHex: string;
  date: string;
  time: string;
  timestamp: number;
  mood?: string | null;
}

export interface ExpenseCategory {
  id: string;
  nameHi: string;
  nameEn: string;
  emoji: string;
  colorHex: string;
  keywords: string;
  orderIndex: number;
  monthlyBudget: number;
}

export interface ExpenseQuickChip {
  id: number;
  label: string;
  amount: number;
  note: string;
  category: string;
  emoji: string;
  usageCount: number;
  orderIndex: number;
}

export interface RecurringExpense {
  id: number;
  title: string;
  amount: number;
  category: string;
  categoryEmoji: string;
  categoryColorHex: string;
  dayOfMonth: number;
  isEnabled: boolean;
  lastAddedMonth: string;
}

export interface StrictLockEvent {
  id: number;
  timestamp: number;
  date: string;
  time: string;
  reason: string;
  triggerWord: string;
  packageName: string;
  durationMinutes: number;
  epochEndTimestamp: number;
  isCompleted: boolean;
}

export type DashboardSectionKey =
  | 'NEXT_ACTIVITY'
  | 'PRIORITY_TASK'
  | 'TODAYS_ROUTINE'
  | 'DAILY_PROGRESS'
  | 'STREAK'
  | 'ENERGY_MODE'
  | 'SHORT_CONTENT_TRACKER'
  | 'MEDITATION'
  | 'MUSIC'
  | 'DAILY_CHALLENGE'
  | 'NEXT_ALARM'
  | 'RECOVERY_DAY'
  | 'WEEKLY_SUMMARY';
