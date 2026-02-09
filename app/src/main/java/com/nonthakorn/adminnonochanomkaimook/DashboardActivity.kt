//package com.nonthakorn.adminnonochanomkaimook
//
//import android.content.Intent
//import android.os.Bundle
//import android.util.Log
//import android.widget.ImageView
//import android.widget.LinearLayout
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.lifecycleScope
//import io.github.jan.supabase.postgrest.from
//import kotlinx.coroutines.launch
//
//class DashboardActivity : AppCompatActivity() {
//    private lateinit var tvSalesAmount: TextView
//    private lateinit var tvCustomerCount: TextView
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        initViews()
//        setupClickListeners()
//        loadDashboardData()
//    }
//
//    private fun initViews() {
//        tvSalesAmount = findViewById(R.id.tv_sales_amount)
//        tvCustomerCount = findViewById(R.id.tv_customer_count)
//    }
//
//    private fun setupClickListeners() {
//        // ปุ่ม Analytics (รีเฟรช)
//        findViewById<LinearLayout>(R.id.nav_analytics)?.setOnClickListener {
//            Toast.makeText(this, "กำลังรีเฟรช...", Toast.LENGTH_SHORT).show()
//            loadDashboardData()
//        }
//
//        // ปุ่ม Menu (ไปหน้า Order Status)
//        findViewById<ImageView>(R.id.nav_menu)?.setOnClickListener {
//            startActivity(Intent(this, OrderStatusActivity::class.java))
//        }
//
//        // ปุ่ม Delete/Basket (ไปหน้า Stock)
//        findViewById<ImageView>(R.id.nav_delete)?.setOnClickListener {
//            startActivity(Intent(this, StockActivity::class.java))
//        }
//
//        // คลิกที่ Card Sales
//        findViewById<LinearLayout>(R.id.layout_sales)?.setOnClickListener {
//            startActivity(Intent(this, OrderStatusActivity::class.java))
//        }
//
//        // คลิกที่ Card Customers
//        findViewById<LinearLayout>(R.id.layout_customers)?.setOnClickListener {
//            Toast.makeText(this, "จำนวนลูกค้าทั้งหมด", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun loadDashboardData() {
//        lifecycleScope.launch {
//            try {
//                Log.d("Dashboard", "🔄 เริ่มโหลดข้อมูล Dashboard...")
//
//                // 1. นับจำนวน Orders ทั้งหมด
//                val orders = SupabaseConfig.client.from("orders")
//                    .select()
//                    .decodeList<OrderData>()
//
//                val totalOrders = orders.size
//                Log.d("Dashboard", "📦 จำนวน Orders: $totalOrders")
//
//                // 2. นับจำนวน Customers (users ที่ไม่ซ้ำ)
//                val uniqueUserIds = orders.map { it.userid }.distinct()
//                val totalCustomers = uniqueUserIds.size
//                Log.d("Dashboard", "👥 จำนวน Customers: $totalCustomers")
//
//                // 3. คำนวณยอดขาย (ถ้าต้องการ)
//                val menus = SupabaseConfig.client.from("menus")
//                    .select()
//                    .decodeList<MenuData>()
//
//                var totalRevenue = 0.0
//                orders.forEach { order ->
//                    val menu = menus.find { it.menuid == order.menuid }
//                    if (menu != null) {
//                        var price = menu.pricestart ?: 0.0
//
//                        // ปรับราคาตามขนาด
//                        price += when(order.cupsize?.lowercase()) {
//                            "large" -> 15.0
//                            else -> 0.0
//                        }
//
//                        totalRevenue += price
//                    }
//                }
//
//                Log.d("Dashboard", "💰 ยอดขายรวม: ${totalRevenue.toInt()} บาท")
//
//                // อัปเดต UI
//                runOnUiThread {
//                    // แสดงจำนวนออเดอร์
//                    tvSalesAmount.text = "$totalOrders รายการ"
//
//                    // แสดงจำนวนลูกค้า
//                    tvCustomerCount.text = "$totalCustomers คน"
//
//                    Toast.makeText(
//                        this@MainActivity,
//                        "✅ โหลดข้อมูลสำเร็จ",
//                        Toast.LENGTH_SHORT
//                    ).show()
//
//                    Log.d("Dashboard", "✅ อัปเดต UI เรียบร้อย")
//                }
//
//            } catch (e: Exception) {
//                Log.e("Dashboard", "❌ Error: ${e.message}", e)
//                e.printStackTrace()
//
//                runOnUiThread {
//                    Toast.makeText(
//                        this@MainActivity,
//                        "โหลดข้อมูลล้มเหลว: ${e.message}",
//                        Toast.LENGTH_LONG
//                    ).show()
//
//                    // แสดงค่า 0 เมื่อเกิด error
//                    tvSalesAmount.text = "0 รายการ"
//                    tvCustomerCount.text = "0 คน"
//                }
//            }
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        // รีเฟรชข้อมูลทุกครั้งที่กลับมาหน้านี้
//        loadDashboardData()
//    }
//}