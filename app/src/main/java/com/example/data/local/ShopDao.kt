package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {

    // Products
    @Query("SELECT * FROM products ORDER BY updatedAtMillis DESC, name ASC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    // Lookups
    @Query("SELECT * FROM lookup_entries ORDER BY sortOrder ASC, label ASC")
    fun observeLookups(): Flow<List<LookupEntity>>

    @Query("SELECT COUNT(*) FROM lookup_entries")
    suspend fun countLookups(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLookup(lookup: LookupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLookups(lookups: List<LookupEntity>)

    @Query("DELETE FROM lookup_entries WHERE id = :id")
    suspend fun deleteLookupById(id: String)

    // Customers
    @Query("SELECT * FROM customers ORDER BY updatedAtMillis DESC, name ASC")
    fun observeCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE UPPER(loyaltyCode) = UPPER(:code) LIMIT 1")
    suspend fun findCustomerByLoyaltyCode(code: String): CustomerEntity?

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun countCustomers(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomers(customers: List<CustomerEntity>)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: String)

    // Credit Logs
    @Query("SELECT * FROM credit_logs ORDER BY createdAtMillis DESC")
    fun observeAllCreditLogs(): Flow<List<CreditLogEntity>>

    @Query("SELECT * FROM credit_logs WHERE customerId = :customerId ORDER BY createdAtMillis DESC")
    fun observeCreditLogsForCustomer(customerId: String): Flow<List<CreditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCreditLog(log: CreditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCreditLogs(logs: List<CreditLogEntity>)

    // Sales & Sale Items
    @Transaction
    @Query("SELECT * FROM sales ORDER BY createdAtMillis DESC")
    fun observeSalesWithItems(): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    suspend fun getSaleWithItems(saleId: String): SaleWithItems?

    @Query("SELECT COUNT(*) FROM sales")
    suspend fun countSales(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSaleItems(items: List<SaleItemEntity>)

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    suspend fun deleteItemsForSale(saleId: String)

    @Transaction
    suspend fun saveSaleWithItems(sale: SaleEntity, items: List<SaleItemEntity>) {
        upsertSale(sale)
        deleteItemsForSale(sale.id)
        if (items.isNotEmpty()) {
            upsertSaleItems(items)
        }
    }

    // Shop Settings
    @Query("SELECT * FROM shop_settings WHERE id = 'shop' LIMIT 1")
    fun observeShopSettings(): Flow<ShopSettingsEntity?>

    @Query("SELECT * FROM shop_settings WHERE id = 'shop' LIMIT 1")
    suspend fun getShopSettings(): ShopSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertShopSettings(settings: ShopSettingsEntity)
}
