package com.nonthakorn.adminnonochanomkaimook

import kotlinx.serialization.Serializable

@Serializable
data class MenuData(
    val menuid: Int,
    val namemenu: String,
    val menupicture: String? = null,
    val pricestart: Double? = null
)
@Serializable
data class StockData(
    val stockid: String,
    val stockname: String,
    val stockamount: Int? = 0
)