package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

object LookupTypes {
    const val CATEGORY = "CATEGORY"
    const val UNIT = "UNIT"
    const val QUICK_SIZE = "QUICK_SIZE"
}

object PaymentMethods {
    const val CASH = "CASH"
    const val CREDIT = "CREDIT"
}

object LoyaltyModes {
    const val STAMPS = "STAMPS"
    const val POINTS = "POINTS"
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subtitle: String? = null,
    val barcode: String? = null,
    val category: String,
    val unitLabel: String,
    val buyPrice: Double,
    val sellPrice: Double,
    val currentStock: Double,
    val minStockAlert: Double,
    val imageKey: String = "royal_oud",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "lookup_entries")
data class LookupEntity(
    @PrimaryKey val id: String,
    val lookupType: String,
    val label: String,
    val sortOrder: Int = 0,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val loyaltyCode: String,
    val name: String,
    val phone: String? = null,
    val creditBalance: Double = 0.0,
    val stamps: Int = 0,
    val points: Int = 0,
    val loyaltyMode: String = LoyaltyModes.STAMPS,
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "credit_logs",
    indices = [Index("customerId")]
)
data class CreditLogEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val amountDelta: Double,
    val note: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String,
    val invoiceNumber: String,
    val customerId: String? = null,
    val customerNameSnapshot: String? = null,
    val paymentMethod: String = PaymentMethods.CASH,
    val subtotal: Double,
    val discount: Double = 0.0,
    val totalAmount: Double,
    val estimatedProfit: Double,
    val stampAwarded: Boolean = false,
    val returned: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId"), Index("productId")]
)
data class SaleItemEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val productId: String? = null,
    val itemName: String,
    val unitLabel: String,
    val quantity: Double,
    val costUnitPrice: Double,
    val actualUnitPrice: Double,
    val lineDiscount: Double = 0.0,
    val totalPrice: Double,
    val soldByAmount: Boolean = false
)

data class SaleWithItems(
    @Embedded val sale: SaleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val items: List<SaleItemEntity>
)

@Entity(tableName = "shop_settings")
data class ShopSettingsEntity(
    @PrimaryKey val id: String = "shop",
    val shopName: String = "حيدر للعطور",
    val currencySymbol: String = "د.ج",
    val defaultLoyaltyMode: String = LoyaltyModes.STAMPS,
    val stampTarget: Int = 10,
    val rewardText: String = "بعد إكمال 10 ختمات .. تحصل على عطر هدية",
    val updatedAtMillis: Long = System.currentTimeMillis()
)
