package com.sismptm.client.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Synexis Brand Color Palette
 * Consistent color scheme for both Client and Partner apps
 */

// Primary Brand Colors
val PrimaryAccent = Color(0xFF2563EB)      // Azul vibrante - Botones principales
val PrimaryHover = Color(0xFF3B82F6)       // Azul más claro para estados hover
val PrimaryDark = Color(0xFF1D4ED8)        // Azul oscuro para pressed states

// Background Colors
val Background = Color(0xFF121826)         // Fondo general - casi negro azulado
val BackgroundElevated = Color(0xFF1A2332) // Fondo ligeramente elevado
val Surface = Color(0xFF121826)            // Superficie base

// Card/Container Colors
val CardBackground = Color(0xFF1C2533)     // Contenedores/cards
val CardBackgroundHover = Color(0xFF232D3D)// Cards en hover
val InputBackground = Color(0xFF1C2533)    // Inputs y campos de texto

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)        // Títulos principales - blanco puro
val TextSecondary = Color(0xFFE5E7EB)      // Cuerpo y datos - blanco suave
val TextTertiary = Color(0xFF9CA3AF)       // Etiquetas secundarias - gris medio
val TextDisabled = Color(0xFF6B7280)       // Texto deshabilitado

// Border & Divider Colors
val BorderSubtle = Color(0xFF374151)       // Bordes y separadores sutiles
val BorderFocus = Color(0xFF2563EB)        // Bordes en focus
val Divider = Color(0xFF374151)            // Divisores

// Status Colors
val Success = Color(0xFF10B981)            // Verde éxito
val SuccessLight = Color(0xFF34D399)       // Verde claro
val Error = Color(0xFFEF4444)              // Rojo error
val ErrorLight = Color(0xFFF87171)         // Rojo claro
val Warning = Color(0xFFF59E0B)            // Amarillo advertencia
val Info = Color(0xFF3B82F6)               // Azul información

// Misc Colors
val StarColor = Color(0xFFFFC107)          // Estrellas de rating
val AvatarBackground = Color(0xFF9CA3AF)   // Fondo avatar genérico
val OnlineIndicator = Color(0xFF10B981)    // Indicador online
val OfflineIndicator = Color(0xFF6B7280)   // Indicador offline

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
