package mivs.liturgicalcalendar.billing

enum class SubscriptionStatus {
    CHECKING,    // Jesteśmy w trakcie sprawdzania
    PREMIUM,     // Użytkownik ma aktywną subskrypcję
    NON_PREMIUM  // Użytkownik nie ma subskrypcji
}