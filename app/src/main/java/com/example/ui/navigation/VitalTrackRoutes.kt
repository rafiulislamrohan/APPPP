package com.example.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose keys covering all 25 VITALTRACK screens
 * and 5 primary bottom navigation sections.
 */
sealed interface VitalTrackRoute {
  // 1. Splash
  @Serializable data object Splash : VitalTrackRoute

  // 2. Onboarding
  @Serializable data object Onboarding : VitalTrackRoute

  // 3. User profile setup
  @Serializable data class ProfileSetup(val fromSettings: Boolean = false) : VitalTrackRoute

  // 4. Permission setup
  @Serializable data class PermissionSetup(val fromSettings: Boolean = false) : VitalTrackRoute

  // 5. Home dashboard (Bottom Tab 1)
  @Serializable data object Home : VitalTrackRoute

  // 6. Step counter details
  @Serializable data object StepDetails : VitalTrackRoute

  // 7. Activity history (Bottom Tab 2)
  @Serializable data object ActivityHistory : VitalTrackRoute

  // 8. Live GPS tracking
  @Serializable data class LiveTracking(val initialMode: String = "WALKING") : VitalTrackRoute

  // 9. Map timeline (Bottom Tab 3)
  @Serializable data object MapTimeline : VitalTrackRoute

  // 10. Activity details & route replay
  @Serializable data class ActivityDetails(val sessionId: String) : VitalTrackRoute

  // 11. Health dashboard (Bottom Tab 4)
  @Serializable data object HealthDashboard : VitalTrackRoute

  // 12. Heart-rate history
  @Serializable data object HeartRate : VitalTrackRoute

  // 13. Sleep dashboard
  @Serializable data object SleepDashboard : VitalTrackRoute

  // 14. Water tracker
  @Serializable data object WaterTracker : VitalTrackRoute

  // 15. Weight tracker
  @Serializable data object WeightTracker : VitalTrackRoute

  // 16. Exercise library
  @Serializable data object ExerciseLibrary : VitalTrackRoute

  // 17. Workout details
  @Serializable data class WorkoutDetails(val workoutId: String) : VitalTrackRoute

  // 18. Active workout
  @Serializable data class ActiveWorkout(val workoutId: String) : VitalTrackRoute

  // 19. Habit tracker
  @Serializable data object HabitTracker : VitalTrackRoute

  // 20. Calendar
  @Serializable data object Calendar : VitalTrackRoute

  // 21. Analytics
  @Serializable data object Analytics : VitalTrackRoute

  // 22. AI wellness coach
  @Serializable data object AiCoach : VitalTrackRoute

  // 23. Notifications & Reminders
  @Serializable data object Notifications : VitalTrackRoute

  // 24. Settings (Bottom Tab 5 Hub -> Settings / Profile Hub)
  @Serializable data object ProfileHub : VitalTrackRoute

  @Serializable data object Settings : VitalTrackRoute

  // 25. Privacy and data management
  @Serializable data object PrivacyData : VitalTrackRoute

  // 26. Scheduled Wellness Tasks & WorkManager Reminders
  @Serializable data object Tasks : VitalTrackRoute

  // 27. Live Smart AI Clothing & Dress Color Combinations
  @Serializable data object StyleAdvisor : VitalTrackRoute

  // 28. Interactive 3D Wellness, Body & GPS Terrain Studio
  @Serializable data object Spatial3D : VitalTrackRoute

  // 29. 3D Guided Breathing & Smart Wellness Calculators
  @Serializable data object WellnessToolbox : VitalTrackRoute

  // 30. 3D Body Morph, Spine Posture, Habit City & Trophy Gallery
  @Serializable data object SpatialGallery : VitalTrackRoute

  // 31. Macro Fueling, Meal Photo AI, Hydration Factors & Fasting
  @Serializable data object NutritionHub : VitalTrackRoute

  // 32. Clinical Vitals, Labs, Medications & Physician Summary PDF
  @Serializable data object VitalsClinical : VitalTrackRoute

  // 33. Sleep Consistency, Biofeedback Soundscapes, NSDR & Stress Recovery
  @Serializable data object RecoveryStudio : VitalTrackRoute

  // 34. Digital Closet, 12-Season Color Palette & AI Dress-Code Checker
  @Serializable data object WardrobeCloset : VitalTrackRoute

  // 35. Platform Accent Palettes, Biometrics, Backups, NFC & Sensor Diagnostics
  @Serializable data object DiagnosticsLab : VitalTrackRoute
}
