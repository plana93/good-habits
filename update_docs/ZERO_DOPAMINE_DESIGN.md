# 🧘 Zero Dopamine Design System
## Good Habits - 2026 Wellness App Guidelines

---

## 📐 Design Philosophy

Good Habits follows **Zero Dopamine Design** principles to create an "invisible" wellness tool that supports habits without demanding excessive screen time or creating addictive patterns.

### Core Principles
1. **Minimalist UI** - Reduce cognitive load by 20% through whitespace and clarity
2. **Predictable Feedback** - No variable rewards or flashy animations
3. **Intentional Utility** - Focus on time-to-value, not engagement metrics
4. **Calm Technology** - The app should recede into the background

---

## 🎨 Color Palette

### Earthy Terracotta & Sage Theme
The app uses **muted, natural tones** to prevent overstimulation and create a grounding experience.

#### Primary Colors
- **Terracotta** `#B76E5D` - Warm, grounding primary color
- **Terracotta Light** `#D4A59A` - Containers and highlights
- **Sage** `#8A9A8E` - Calm, natural secondary color
- **Sage Light** `#B8C4BC` - Backgrounds and secondary containers

#### Neutral Gray Scale
- **Warm Gray 900** `#2D2926` - High contrast text
- **Warm Gray 700** `#5A564F` - Secondary text
- **Warm Gray 400** `#9B9589` - Disabled states, dividers
- **Warm Gray 200** `#D9D3C8` - Light dividers
- **Warm Gray 50** `#F5F3EF` - Background (softer than pure white)

#### Accent Colors (Minimal Use)
- **Calm Blue** `#7A9AAA` - Links, informational elements
- **Muted Red** `#C17A74` - Errors (non-alarming, desaturated)

### Color Usage Rules
✅ **Do:**
- Use terracotta for primary actions and completed states
- Use sage for supportive, calm elements
- Use warm grays for most text and UI elements
- Keep backgrounds off-white (`#F5F3EF`) instead of pure white

❌ **Don't:**
- Use bright, saturated colors (too stimulating)
- Use pure black or pure white (harsh contrast)
- Use variable color rewards (green for good, red for bad)
- Create color-based gamification

---

## 🔤 Typography

### Readability First
All text uses **16sp minimum** for body content to ensure comfortable reading without strain.

| Style | Size | Weight | Use Case |
|-------|------|--------|----------|
| Display | 32sp | Bold | Rare, only critical numbers (e.g., streak count) |
| Title | 24sp | Semibold | Section headers |
| Headline | 20sp | Medium | Card titles |
| Body | 16sp | Regular | Main content (default) |
| Caption | 14sp | Regular | Secondary info, timestamps |
| Label | 12sp | Medium | Button labels (minimal use) |

### Typography Rules
✅ **Do:**
- Use 16sp as default for all body text
- Maintain generous line spacing (1.5x)
- Use medium gray for secondary text, not light gray
- Limit font hierarchy to 3 levels per screen

❌ **Don't:**
- Use text smaller than 12sp
- Use all-caps for large blocks of text
- Mix multiple font weights in the same paragraph
- Create complex visual hierarchies

---

## 📏 Spacing & Layout

### The 20% Rule
Every screen should have **20% more whitespace** than a typical app to reduce cognitive load.

#### Spacing Scale
```kotlin
Extra Small: 4dp   // Icon padding
Small:       8dp   // Minimal spacing
Medium:      16dp  // Default content padding
Large:       24dp  // Section spacing
Extra Large: 32dp  // Major sections
Huge:        48dp  // Screen-level spacing
```

#### Layout Guidelines
- **Content Padding**: 16dp minimum on all sides
- **Between Sections**: 24dp minimum vertical spacing
- **Card Spacing**: 16dp between cards, 16dp internal padding
- **List Items**: 56dp minimum height for touch targets

---

## 🎬 Animations

### Slow, Predictable, Calming
No sudden movements or bouncy effects. All animations are **linear or ease-out** only.

#### Animation Durations
```kotlin
Quick:    200ms  // Button press, micro-interactions
Standard: 400ms  // Standard transitions (default)
Slow:     600ms  // Page transitions, major state changes
None:     0ms    // Focus mode (distraction-free)
```

### Animation Rules
✅ **Do:**
- Use slow, steady transitions (400ms default)
- Fade elements in/out with linear easing
- Cross-fade between screens
- Use haptic feedback instead of visual celebration

❌ **Don't:**
- Use spring, bounce, or elastic animations
- Create unpredictable "delight" animations
- Animate multiple elements simultaneously
- Use confetti, fireworks, or celebratory effects

---

## 🔔 Notifications & Haptics

### Gentle Nudges, Not Interruptions

#### Smart Notifications
- **Max Daily Nudges**: 2 per day
- **Timing**: Only when user is likely to act (not arbitrary times)
- **Quiet Hours**: 9 PM - 8 AM (no notifications)
- **Delay**: 5 minutes after ideal time (not instant)

#### Haptic Feedback
- **Success**: Light tap (50ms) for task completion
- **Error**: Slightly longer (100ms) for mistakes
- **Usage**: Confirmations only, not celebrations
- **Sound**: None (silent by default, haptic only)

### Notification Rules
✅ **Do:**
- Use haptic feedback instead of sounds
- Show notifications only when actionable
- Allow one-tap snooze for 1 hour
- Respect system Do Not Disturb

❌ **Don't:**
- Send notifications at fixed times daily
- Use loud, high-pitched sounds
- Create urgency or FOMO language
- Require immediate action

---

## 📊 Progress Visualization

### Internal Motivation, Not Competition

#### What to Show
✅ **Personal Trends**:
- Simple day chain (streak) without levels
- Heat map of activity over time
- Trend lines for personal progress
- "Habit strength" score (internal metric)

#### What NOT to Show
❌ **Gamification Elements**:
- No leaderboards or social comparison
- No XP points or level-up systems
- No badges or achievement unlocks
- No variable rewards or surprise bonuses

### Progress Display Rules
✅ **Do:**
- Show simple numerical counts (e.g., "7 days")
- Use heat maps for long-term patterns
- Display trend arrows (↑ ↓ →) for direction
- Keep graphs minimal and monochrome

❌ **Don't:**
- Create complex scoring systems
- Show "you're falling behind" comparisons
- Use progress bars that create urgency
- Celebrate milestones with flashy animations

---

## 🧩 Component Guidelines

### Buttons
- **Height**: 48dp minimum (comfortable tap target)
- **Corner Radius**: 12dp (soft, approachable)
- **Colors**: Terracotta for primary, Sage for secondary
- **States**: Subtle opacity change (0.7) on press, no animations

### Cards
- **Corner Radius**: 16dp
- **Elevation**: 2dp maximum (subtle shadow)
- **Padding**: 16dp internal
- **Spacing**: 16dp between cards
- **Colors**: White surface on warm gray background

### Text Fields
- **Height**: 56dp
- **Corner Radius**: 12dp
- **Border**: 1dp warm gray outline
- **Focus**: Terracotta border (no glow)
- **Label**: Always visible above field (not floating)

### Bottom Navigation
- **Height**: 80dp (generous tap targets)
- **Icons**: 24dp, outline style only
- **Labels**: Always visible
- **Selection**: Terracotta icon + label
- **Unselected**: Warm gray 700

---

## 🎯 Focus Mode

### Distraction-Free Workout/Meditation
When user enters AI Detection or meditation session:

#### Enabled Features
- ✅ Large, readable timer/counter
- ✅ Essential controls only (pause/stop)
- ✅ Dimmed background (90% opacity)
- ✅ Haptic feedback on rep completion

#### Disabled Features
- ❌ All notifications
- ❌ Statistics and charts
- ❌ Navigation bar
- ❌ Social features

---

## 📱 Screen-Specific Guidelines

### Dashboard
- **KPI Cards**: 2x2 grid, large numbers (32sp)
- **Charts**: Minimal, monochrome line graphs
- **Spacing**: 24dp between sections
- **Actions**: Single FAB for "Add Workout" (bottom right)

### Today Screen
- **Session List**: Vertical, generous spacing (24dp)
- **Add Button**: Subtle "+" with text label
- **Navigation**: Date arrows at top (not swipe)
- **Wellness**: Separate card, muted sage color

### Calendar
- **View**: Month view only (no week/day toggle)
- **Day Cells**: 48dp minimum
- **Completed Days**: Terracotta dot (subtle)
- **Streak**: Large number at top, no flames or icons
- **Missed Days**: No color (neutral gray)

### AI Detection
- **Camera**: Full screen, minimal UI
- **Counter**: Large, centered (48sp)
- **Controls**: Bottom overlay, auto-hide after 3s
- **Feedback**: Haptic only (no sounds)

---

## 🔐 Privacy & Offline

### Calm Technology Principles
- **Data Storage**: All data local (no cloud sync by default)
- **Offline Mode**: Full functionality without internet
- **No Tracking**: No analytics or user behavior tracking
- **Export**: User owns data (CSV/JSON export)

---

## 📈 Metrics & Goals

### Micro-Goals Over Grand Ambitions
- **Default Workout**: 10 minutes (achievable)
- **Daily Reps**: 20 reps (realistic)
- **Messaging**: "Just 1 minute is enough" (encouragement)
- **Flexibility**: "Life happens" - easy to reschedule

### Success Metrics (Internal)
- **Time-to-Value**: How fast user can log and leave
- **Completion Rate**: % of started sessions finished
- **Return Rate**: Users returning next day (not engagement time)
- **Stress Reduction**: Self-reported mood improvements

---

## ✅ Implementation Checklist

### Phase 1: Color Migration ✅
- [x] Update Theme.kt with Zero Dopamine palette
- [x] Create ZeroDopamineConfig.kt
- [ ] Migrate all screens to new colors
- [ ] Remove gradients and vibrant colors

### Phase 2: Typography & Spacing
- [ ] Update all body text to 16sp minimum
- [ ] Add generous padding to all screens (20% more whitespace)
- [ ] Simplify font hierarchy to 3 levels max

### Phase 3: Animations
- [ ] Replace all spring/bounce animations with linear
- [ ] Slow down all transitions to 400ms
- [ ] Add focus mode (animations disabled)

### Phase 4: Haptics & Notifications
- [ ] Implement gentle haptic feedback
- [ ] Remove all notification sounds
- [ ] Add smart notification timing logic
- [ ] Respect quiet hours (9 PM - 8 AM)

### Phase 5: Progress Visualization
- [ ] Remove gamification elements
- [ ] Simplify streak display (no flames, just number)
- [ ] Add personal trend graphs (heat map)
- [ ] Remove leaderboards and social features

### Phase 6: Focus Mode
- [ ] Implement distraction-free AI Detection mode
- [ ] Hide non-essential UI during workouts
- [ ] Add dim background overlay
- [ ] Auto-hide controls after 3 seconds

---

## 📚 References

### 2026 Wellness Design Trends
- **Minimalism**: 20% cognitive load reduction through whitespace
- **Calm Technology**: Invisible tools that support habits
- **Zero UI**: Integration with wearables for automatic logging
- **Privacy First**: Local-only data storage as differentiator

### Key Principles Summary
1. **Remove Variable Rewards**: Steady, predictable feedback
2. **Reduced Visual Stimuli**: Monochrome, muted palettes
3. **High Utility**: Time-to-value over engagement
4. **Sustainable Focus**: Internal motivation, not competition

---

## 🎨 Color Reference Card

```kotlin
// Light Theme
background = WarmGray50 (#F5F3EF)
surface = White (#FFFFFF)
primary = Terracotta (#B76E5D)
secondary = Sage (#8A9A8E)
onSurface = WarmGray900 (#2D2926)

// Dark Theme  
background = DeepWarmBlack (#1A1816)
surface = WarmCharcoal (#241F1C)
primary = TerracottaLight (#D4A59A)
secondary = SageLight (#B8C4BC)
onSurface = WarmWhite (#E8E3DC)
```

---

**Last Updated**: January 18, 2026  
**Version**: 1.0 - Initial Zero Dopamine Design System
