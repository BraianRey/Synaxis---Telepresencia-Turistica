package com.sismptm.client.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Synexis Brand Color Palette
 * Consistent color scheme for both Client and Partner apps
 */

// Hex Color Constants
private const val HEX_PRIMARY_ACCENT = 0xFF2563EB
private const val HEX_PRIMARY_HOVER = 0xFF3B82F6
private const val HEX_PRIMARY_DARK = 0xFF1D4ED8
private const val HEX_BACKGROUND = 0xFF121826
private const val HEX_BACKGROUND_ELEVATED = 0xFF1A2332
private const val HEX_CARD_BACKGROUND = 0xFF1C2533
private const val HEX_CARD_BACKGROUND_HOVER = 0xFF232D3D
private const val HEX_TEXT_PRIMARY = 0xFFFFFFFF
private const val HEX_TEXT_SECONDARY = 0xFFE5E7EB
private const val HEX_TEXT_TERTIARY = 0xFF9CA3AF
private const val HEX_TEXT_DISABLED = 0xFF6B7280
private const val HEX_BORDER_SUBTLE = 0xFF374151
private const val HEX_SUCCESS = 0xFF10B981
private const val HEX_SUCCESS_LIGHT = 0xFF34D399
private const val HEX_ERROR = 0xFFEF4444
private const val HEX_ERROR_LIGHT = 0xFFF87171
private const val HEX_WARNING = 0xFFF59E0B
private const val HEX_STAR_COLOR = 0xFFFFC107
private const val HEX_AVATAR_BACKGROUND = 0xFF9CA3AF
private const val HEX_ONLINE_INDICATOR = 0xFF10B981
private const val HEX_OFFLINE_INDICATOR = 0xFF6B7280
private const val HEX_AVAILABLE_BADGE_BG = 0xFF1B3A1B
private const val HEX_AVAILABLE_BADGE_TEXT = 0xFF4CAF50
private const val HEX_DIVIDER_BORDER = 0xFF2A2A2A
private const val HEX_FILTER_CHIP_INACTIVE = 0xFF1E1E1E
private const val HEX_FILTER_CHIP_ACTIVE = 0xFF2979FF
private const val HEX_SCHEDULE_BADGE_BG = 0xFF1A2A3A
private const val HEX_SCHEDULE_BADGE_TEXT = 0xFF2979FF
private const val HEX_TOUR_PRESENCE_WHITE = 0xFFFFFFFF
private const val HEX_TOUR_PRESENCE_BEIGE = 0xFFEFE8DE
private const val HEX_ON_BEIGE_TEXT = 0xFF1C1B1F
private const val HEX_TOUR_PRESENCE_GRAY = 0xFFAAAAAA
private const val HEX_TOUR_PRESENCE_GRAY_DARK = 0xFF666666
private const val HEX_TOUR_PRESENCE_MAP_BG = 0xFFE8DCC8
private const val HEX_PURPLE_80 = 0xFFD0BCFF
private const val HEX_PURPLE_GREY_80 = 0xFFCCC2DC
private const val HEX_PINK_80 = 0xFFEFB8C8
private const val HEX_PURPLE_40 = 0xFF6650a4
private const val HEX_PURPLE_GREY_40 = 0xFF625b71
private const val HEX_PINK_40 = 0xFF7D5260

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
val OfflineStatusColor = Color(0xFFFF5722) // Offline/disconnected status indicator
val SuccessBackground = Color(0xFFE8F5E9)  // Light green background for success states

// Action & Accent Variants
val PurpleAccent = Color(0xFF7C3AED)       // Purple accent for alternative actions
val BlueSecondary = Color(0xFF2196F3)      // Secondary blue for specific features
val ErrorDark = Color(0xFFC62828)          // Dark error red

// Background Variants
val CardBackgroundDark = Color(0xFF1E2430) // Dark card variant
val CardBackgroundLight = Color(0xFF2D3748)// Light card variant
val InputBorderFocused = Color(0xFF444444) // Input border when focused
val InputBackgroundFocused = Color(0xFF333333) // Input background when focused
val InputTextActive = Color(0xFFDDDDDD)    // Active input text color

// Legacy UI Components (ServiceDetailScreen, PartnerSearchScreen)
val AvailableBadgeBg = Color(HEX_AVAILABLE_BADGE_BG)
val AvailableBadgeText = Color(HEX_AVAILABLE_BADGE_TEXT)
val DividerBorder = Color(HEX_DIVIDER_BORDER)
val FilterChipInactiveBg = Color(HEX_FILTER_CHIP_INACTIVE)
val FilterChipActiveBg = Color(HEX_FILTER_CHIP_ACTIVE)
val ToggleInactive = Color(HEX_DIVIDER_BORDER)
val ScheduleBadgeBg = Color(HEX_SCHEDULE_BADGE_BG)
val ScheduleBadgeText = Color(HEX_SCHEDULE_BADGE_TEXT)

// Additional legacy colors
val TourPresenceWhite = Color(HEX_TOUR_PRESENCE_WHITE)
val TourPresenceBeige = Color(HEX_TOUR_PRESENCE_BEIGE)
val OnBeigeText = Color(HEX_ON_BEIGE_TEXT)
val TourPresenceGray = Color(HEX_TOUR_PRESENCE_GRAY)
val TourPresenceGrayDark = Color(HEX_TOUR_PRESENCE_GRAY_DARK)
val TourPresenceMapBg = Color(HEX_TOUR_PRESENCE_MAP_BG)

// Legacy colors (keeping for backwards compatibility)
val Purple80 = Color(HEX_PURPLE_80)
val PurpleGrey80 = Color(HEX_PURPLE_GREY_80)
val Pink80 = Color(HEX_PINK_80)
val Purple40 = Color(HEX_PURPLE_40)
val PurpleGrey40 = Color(HEX_PURPLE_GREY_40)
val Pink40 = Color(HEX_PINK_40)

// Legacy custom colors (deprecated, use new palette above)
private const val HEX_DARK_BACKGROUND = 0xFF1A1A1A
private const val HEX_TEXT_SECONDARY_LEGACY = 0xFFAAAAAA
private const val HEX_TOUR_PRESENCE_SURFACE = 0xFF2C2C2C

@Deprecated("Use Background instead", ReplaceWith("Background"))
val DarkBackground = Color(HEX_DARK_BACKGROUND)
@Deprecated("Use TextTertiary instead", ReplaceWith("TextTertiary"))
val TextSecondaryLegacy = Color(HEX_TEXT_SECONDARY_LEGACY)  // Renamed to avoid conflict
@Deprecated("Use CardBackground instead", ReplaceWith("CardBackground"))
val TourPresenceSurface = Color(HEX_TOUR_PRESENCE_SURFACE)
@Deprecated("Use Background instead", ReplaceWith("Background"))
val TourPresenceBg = Color(HEX_DARK_BACKGROUND)
