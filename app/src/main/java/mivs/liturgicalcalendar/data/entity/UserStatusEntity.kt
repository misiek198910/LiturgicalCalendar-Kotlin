
package mivs.liturgicalcalendar.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_status")
data class UserStatusEntity(
    @PrimaryKey val id: Int = 1, 
    val isPremium: Boolean,      
    val purchaseToken: String?   
)