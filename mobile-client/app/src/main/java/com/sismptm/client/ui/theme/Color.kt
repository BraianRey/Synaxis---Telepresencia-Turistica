package com.sismptm.client.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Synexis Brand Color Palette
 * Consistent color scheme for both Client and Partner apps
 */

// Primary Brand Colors
val PrimaryAccent = Color(0xFF2563EB)      // Vibrant blue - Main buttons
val PrimaryHover = Color(0xFF3B82F6)       // Lighter blue for hover states
val PrimaryDark = Color(0xFF1D4ED8)        // Dark blue for pressed states

// Background Colors
val Background = Color(0xFF121826)         // General background - dark blue-black
val BackgroundElevated = Color(0xFF1A2332) // Slightly elevated background
val Surface = Color(0xFF121826)            // Base surface

// Card/Container Colors
val CardBackground = Color(0xFF1C2533)     // Containers/cards
val CardBackgroundHover = Color(0xFF232D3D)// Cards on hover
val InputBackground = Color(0xFF1C2533)    // Inputs and text fields

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)        // Primary titles - pure white
val TextSecondary = Color(0xFFE5E7EB)      // Body and data - soft white
val TextTertiary = Color(0xFF9CA3AF)       // Secondary labels - medium gray
val TextDisabled = Color(0xFF6B7280)       // Disabled text

// Border & Divider Colors
val BorderSubtle = Color(0xFF374151)       // Subtle borders and separators
val BorderFocus = Color(0xFF2563EB)        // Focus borders
val Divider = Color(0xFF374151)            // Dividers

// Status Colors
val Success = Color(0xFF10B981)            // Success green
val SuccessLight = Color(0xFF34D399)       // Light green
val Error = Color(0xFFEF4444)              // Error red
val ErrorLight = Color(0xFFF87171)         // Light red
val Warning = Color(0xFFF59E0B)            // Warning yellow
val Info = Color(0xFF3B82F6)               // Info blue

// Misc Colors
val StarColor = Color(0xFFFFC107)          // Rating stars
val AvatarBackground = Color(0xFF9CA3AF)   // Generic avatar background
val OnlineIndicator = Color(0xFF10B981)    // Online indicator
val OfflineIndicator = Color(0xFF6B7280)   // Offline indicator

// Legacy UI Components (ServiceDetailScreen, PartnerSearchScreen)
val AvailableBadgeBg = Color(0xFF1B3A1B)
val AvailableBadgeText = Color(0xFF4CAF50)
val DividerBorder = Color(0xFF2A2A2A)
val FilterChipInactiveBg = Color(0xFF1E1E1E)
val FilterChipActiveBg = Color(0xFF2979FF)
val ToggleInactive = Color(0xFF2A2A2A)
val ScheduleBadgeBg = Color(0xFF1A2A3A)
val ScheduleBadgeText = Color(0xFF2979FF)

// Additional legacy colors
val TourPresenceWhite = Color(0xFFFFFFFF)
val TourPresenceBeige = Color(0xFFEFE8DE)
val OnBeigeText = Color(0xFF1C1B1F)
val TourPresenceGray = Color(0xFFAAAAAA)
val TourPresenceGrayDark = Color(0xFF666666)
val TourPresenceMapBg = Color(0xFFE8DCC8)

// Legacy colors (keeping for backwards compatibility)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Legacy custom colors (deprecated, use new palette above)
@Deprecated("Use Background instead", ReplaceWith("Background"))
val DarkBackground = Color(0xFF1A1A1A)
@Deprecated("Use TextTertiary instead", ReplaceWith("TextTertiary"))
val TextSecondaryLegacy = Color(0xFFAAAAAA)  // Renamed to avoid conflict
@Deprecated("Use CardBackground instead", ReplaceWith("CardBackground"))
val TourPresenceSurface = Color(0xFF2C2C2C)
@Deprecated("Use Background instead", ReplaceWith("Background"))
val TourPresenceBg = Color(0xFF1A1A1A)
