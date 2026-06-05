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
val PrimaryAccent = Color(HEX_PRIMARY_ACCENT)      // Azul vibrante - Botones principales
val PrimaryHover = Color(HEX_PRIMARY_HOVER)       // Azul más claro para estados hover
val PrimaryDark = Color(HEX_PRIMARY_DARK)        // Azul oscuro para pressed states

// Background Colors
val Background = Color(HEX_BACKGROUND)         // Fondo general - casi negro azulado
val BackgroundElevated = Color(HEX_BACKGROUND_ELEVATED) // Fondo ligeramente elevado
val Surface = Color(HEX_BACKGROUND)            // Superficie base

// Card/Container Colors
val CardBackground = Color(HEX_CARD_BACKGROUND)     // Contenedores/cards
val CardBackgroundHover = Color(HEX_CARD_BACKGROUND_HOVER)// Cards en hover
val InputBackground = Color(HEX_CARD_BACKGROUND)    // Inputs y campos de texto

// Text Colors
val TextPrimary = Color(HEX_TEXT_PRIMARY)        // Títulos principales - blanco puro
val TextSecondary = Color(HEX_TEXT_SECONDARY)      // Cuerpo y datos - blanco suave
val TextTertiary = Color(HEX_TEXT_TERTIARY)       // Etiquetas secundarias - gris medio
val TextDisabled = Color(HEX_TEXT_DISABLED)       // Texto deshabilitado

// Border & Divider Colors
val BorderSubtle = Color(HEX_BORDER_SUBTLE)       // Bordes y separadores sutiles
val BorderFocus = Color(HEX_PRIMARY_ACCENT)        // Bordes en focus
val Divider = Color(HEX_BORDER_SUBTLE)            // Divisores

// Status Colors
val Success = Color(HEX_SUCCESS)            // Verde éxito
val SuccessLight = Color(HEX_SUCCESS_LIGHT)       // Verde claro
val Error = Color(HEX_ERROR)              // Rojo error
val ErrorLight = Color(HEX_ERROR_LIGHT)         // Rojo claro
val Warning = Color(HEX_WARNING)            // Amarillo advertencia
val Info = Color(HEX_PRIMARY_HOVER)               // Azul información

// Misc Colors
val StarColor = Color(HEX_STAR_COLOR)          // Estrellas de rating
val AvatarBackground = Color(HEX_AVATAR_BACKGROUND)   // Fondo avatar genérico
val OnlineIndicator = Color(HEX_ONLINE_INDICATOR)    // Indicador online
val OfflineIndicator = Color(HEX_OFFLINE_INDICATOR)   // Indicador offline

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
