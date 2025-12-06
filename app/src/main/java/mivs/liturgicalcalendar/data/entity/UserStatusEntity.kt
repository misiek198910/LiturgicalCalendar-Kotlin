// UserStatusEntity.kt
package mivs.liturgicalcalendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_status")
data class UserStatusEntity(
    @PrimaryKey val id: Int = 1, // Zawsze 1, bo mamy tylko jednego użytkownika/telefon
    val isPremium: Boolean,      // Czy ma aktywną subskrypcję
    val purchaseToken: String?   // Token zakupu (dla celów weryfikacji)
)