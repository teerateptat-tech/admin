package com.nonthakorn.adminnonochanomkaimook

import java.io.Serializable

// แยกออกมาไว้ไฟล์นี้ เพื่อให้ Compiler ทำงานง่ายขึ้น
@kotlinx.serialization.Serializable
data class Stock(
    val stockid: String? = null,
    val stockname: String,
    val stockamount: Int
)

data class StockItem(
    val id: String,
    val name: String,
    var quantity: Int,
    var originalQuantity: Int = quantity,
    val imageResource: Int,
    val minStock: Int = 5,
    val maxStock: Int = 100
) : Serializable {
    fun isLowStock(): Boolean = quantity > 0 && quantity <= minStock
    fun isOutOfStock(): Boolean = quantity <= 0
    fun hasChanged(): Boolean = quantity != originalQuantity
}