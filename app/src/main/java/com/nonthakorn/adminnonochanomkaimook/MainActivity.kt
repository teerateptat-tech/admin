package com.nonthakorn.adminnonochanomkaimook

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var tvSalesAmount: TextView
    private lateinit var tvCustomerCount: TextView
    private lateinit var tvMenuItem1: TextView
    private lateinit var tvMenuItem2: TextView
    private lateinit var tvMenuItem3: TextView
    private lateinit var tvStockItem1: TextView
    private lateinit var tvStockItem2: TextView
    private lateinit var tvStockItem3: TextView
    private lateinit var tvStockItem4: TextView
    private lateinit var tvStockItem5: TextView
    private lateinit var tvStockItem6: TextView
    private lateinit var tvStockItem7: TextView
    private lateinit var tvStockItem8: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupClickListeners()
        loadDashboardData()
    }

    private fun initViews() {
        tvSalesAmount = findViewById(R.id.tv_sales_amount)
        tvCustomerCount = findViewById(R.id.tv_customer_count)

        // เมนูยอดนิยม
        tvMenuItem1 = findViewById(R.id.tv_menu_item_1)
        tvMenuItem2 = findViewById(R.id.tv_menu_item_2)
        tvMenuItem3 = findViewById(R.id.tv_menu_item_3)

        // วัตถุดิบใกล้หมด
        tvStockItem1 = findViewById(R.id.tv_stock_item_1)
        tvStockItem2 = findViewById(R.id.tv_stock_item_2)
        tvStockItem3 = findViewById(R.id.tv_stock_item_3)
        tvStockItem4 = findViewById(R.id.tv_stock_item_4)
        tvStockItem5 = findViewById(R.id.tv_stock_item_5)
        tvStockItem6 = findViewById(R.id.tv_stock_item_6)
        tvStockItem7 = findViewById(R.id.tv_stock_item_7)
        tvStockItem8 = findViewById(R.id.tv_stock_item_8)
    }

    private fun setupClickListeners() {
        // ปุ่ม Analytics (รีเฟรช)
        findViewById<LinearLayout>(R.id.nav_analytics)?.setOnClickListener {
            Toast.makeText(this, "กำลังรีเฟรช...", Toast.LENGTH_SHORT).show()
            loadDashboardData()
        }

        // ปุ่ม Menu (ไปหน้า Order Status)
        findViewById<ImageView>(R.id.nav_menu)?.setOnClickListener {
            startActivity(Intent(this, OrderStatusActivity::class.java))
        }

        // ปุ่ม Delete/Basket (ไปหน้า Stock)
        findViewById<ImageView>(R.id.nav_delete)?.setOnClickListener {
            startActivity(Intent(this, StockActivity::class.java))
        }

        // คลิกที่ Card Sales
        findViewById<LinearLayout>(R.id.layout_sales)?.setOnClickListener {
            startActivity(Intent(this, OrderStatusActivity::class.java))
        }

        // คลิกที่ Card Customers
        findViewById<LinearLayout>(R.id.layout_customers)?.setOnClickListener {
            Toast.makeText(this, "จำนวนลูกค้าทั้งหมด", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                Log.d("Dashboard", "🔄 เริ่มโหลดข้อมูล Dashboard...")

                // 1. นับจำนวน Orders ทั้งหมด
                val orders = SupabaseConfig.client.from("orders")
                    .select()
                    .decodeList<OrderData>()

                val totalOrders = orders.size
                Log.d("Dashboard", "📦 จำนวน Orders: $totalOrders")

                // 2. นับจำนวน Customers (users ที่ไม่ซ้ำ)
                val uniqueUserIds = orders.map { it.userid }.distinct()
                val totalCustomers = uniqueUserIds.size
                Log.d("Dashboard", "👥 จำนวน Customers: $totalCustomers")

                // 3. คำนวณยอดขาย (ถ้าต้องการ)
                val menus = SupabaseConfig.client.from("menus")
                    .select()
                    .decodeList<MenuData>()

                var totalRevenue = 0.0
                orders.forEach { order ->
                    val menu = menus.find { it.menuid == order.menuid }
                    if (menu != null) {
                        var price = menu.pricestart ?: 0.0

                        // ปรับราคาตามขนาด
                        price += when(order.cupsize?.lowercase()) {
                            "large" -> 15.0
                            else -> 0.0
                        }

                        totalRevenue += price
                    }
                }

                Log.d("Dashboard", "💰 ยอดขายรวม: ${totalRevenue.toInt()} บาท")

                // 4. หาเมนูยอดนิยม (Top 3)
                val menuOrderCount = orders.groupBy { it.menuid }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(3)

                val topMenus = menuOrderCount.map { (menuid, count) ->
                    val menu = menus.find { it.menuid == menuid }
                    "${menu?.namemenu ?: "เมนู #$menuid"} - $count รายการ"
                }

                Log.d("Dashboard", "🏆 เมนูยอดนิยม: $topMenus")

                // 5. หาวัตถุดิบใกล้หมด (น้อยกว่า 10)
                val stocks = try {
                    SupabaseConfig.client.from("stocks")
                        .select()
                        .decodeList<StockData>()
                } catch (e: Exception) {
                    Log.w("Dashboard", "ไม่สามารถดึงข้อมูล stocks: ${e.message}")
                    emptyList()
                }

                val lowStocks = stocks.filter { (it.stockamount ?: 0) < 10 }
                    .sortedBy { it.stockamount }
                    .take(8)

                Log.d("Dashboard", "⚠️ วัตถุดิบใกล้หมด: ${lowStocks.size} รายการ")

                // อัปเดต UI
                runOnUiThread {
                    // ยอดขายและลูกค้า
                    tvSalesAmount.text = "$totalOrders รายการ"
                    tvCustomerCount.text = "$totalCustomers คน"

                    // เมนูยอดนิยม
                    if (topMenus.isNotEmpty()) {
                        tvMenuItem1.text = topMenus.getOrNull(0) ?: "-"
                        tvMenuItem2.text = topMenus.getOrNull(1) ?: "-"
                        tvMenuItem3.text = topMenus.getOrNull(2) ?: "-"
                    } else {
                        tvMenuItem1.text = "ยังไม่มีข้อมูล"
                        tvMenuItem2.text = "-"
                        tvMenuItem3.text = "-"
                    }

                    // วัตถุดิบใกล้หมด
                    val stockTexts = lowStocks.map {
                        "${it.stockname}: ${it.stockamount}" // ⭐ เอาหน่วยออก แสดงแค่ชื่อกับจำนวน
                    }

                    tvStockItem1.text = stockTexts.getOrNull(0) ?: "-"
                    tvStockItem2.text = stockTexts.getOrNull(1) ?: "-"
                    tvStockItem3.text = stockTexts.getOrNull(2) ?: "-"
                    tvStockItem4.text = stockTexts.getOrNull(3) ?: "-"
                    tvStockItem5.text = stockTexts.getOrNull(4) ?: "-"
                    tvStockItem6.text = stockTexts.getOrNull(5) ?: "-"
                    tvStockItem7.text = stockTexts.getOrNull(6) ?: "-"
                    tvStockItem8.text = stockTexts.getOrNull(7) ?: "-"

                    Toast.makeText(
                        this@MainActivity,
                        "✅ โหลดข้อมูลสำเร็จ",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d("Dashboard", "✅ อัปเดต UI เรียบร้อย")
                }

            } catch (e: Exception) {
                Log.e("Dashboard", "❌ Error: ${e.message}", e)
                e.printStackTrace()

                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "โหลดข้อมูลล้มเหลว: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    // แสดงค่า 0 เมื่อเกิด error
                    tvSalesAmount.text = "0 รายการ"
                    tvCustomerCount.text = "0 คน"
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // รีเฟรชข้อมูลทุกครั้งที่กลับมาหน้านี้
        loadDashboardData()
    }
}