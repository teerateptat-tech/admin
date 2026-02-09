package com.nonthakorn.adminnonochanomkaimook

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

// --- 1. ViewHolder ---
class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageProduct: ImageView = itemView.findViewById(R.id.imageProduct)
    private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
    private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
    private val btnDecrease: ImageView = itemView.findViewById(R.id.btnDecrease)
    private val btnIncrease: ImageView = itemView.findViewById(R.id.btnIncrease)
    private val btnConfirm: Button = itemView.findViewById(R.id.btnConfirm)

    fun bind(stockItem: StockItem, onConfirmClicked: (StockItem) -> Unit) {
        tvProductName.text = stockItem.name
        updateQuantityDisplay(stockItem)
        updateConfirmButtonState(stockItem)
        imageProduct.setImageResource(stockItem.imageResource)

        btnIncrease.setOnClickListener {
            if (stockItem.quantity < stockItem.maxStock) {
                stockItem.quantity++
                updateQuantityDisplay(stockItem)
                updateConfirmButtonState(stockItem)
            }
        }
        btnDecrease.setOnClickListener {
            if (stockItem.quantity > 0) {
                stockItem.quantity--
                updateQuantityDisplay(stockItem)
                updateConfirmButtonState(stockItem)
            }
        }
        btnConfirm.setOnClickListener { onConfirmClicked(stockItem) }
    }

    private fun updateQuantityDisplay(stockItem: StockItem) {
        tvQuantity.text = stockItem.quantity.toString()
        val color = when {
            stockItem.isOutOfStock() -> ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
            stockItem.isLowStock() -> ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark)
            else -> ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
        }
        tvQuantity.setTextColor(color)
    }

    private fun updateConfirmButtonState(stockItem: StockItem) {
        btnConfirm.visibility = if (stockItem.hasChanged()) View.VISIBLE else View.GONE
    }
}

// --- 2. Adapter ---
class StockAdapter(
    private var stockItems: List<StockItem>,
    private val onConfirmClicked: (StockItem) -> Unit
) : RecyclerView.Adapter<StockViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stock, parent, false)
        return StockViewHolder(view)
    }
    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        holder.bind(stockItems[position], onConfirmClicked)
    }
    override fun getItemCount(): Int = stockItems.size
    fun updateStockItems(newItems: List<StockItem>) {
        stockItems = newItems.sortedBy { it.name }
        notifyDataSetChanged()
    }
}

// --- 3. Activity ---
class StockActivity : AppCompatActivity() {
    private lateinit var recyclerViewStock: RecyclerView
    private lateinit var stockAdapter: StockAdapter
    private lateinit var navAnalytics: ImageView
    private lateinit var navMenu: ImageView
    private lateinit var navDelete: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupRecyclerView()
        fetchStockFromSupabase()
        setupNavigationListeners()
    }

    private fun initViews() {
        recyclerViewStock = findViewById(R.id.recyclerViewStock)
        navAnalytics = findViewById(R.id.nav_analytics)
        navMenu = findViewById(R.id.nav_menu)
        navDelete = findViewById(R.id.nav_delete)
    }

    private fun setupRecyclerView() {
        stockAdapter = StockAdapter(emptyList()) { stockItem ->
            updateStockToSupabase(stockItem)
        }
        recyclerViewStock.apply {
            adapter = stockAdapter
            layoutManager = LinearLayoutManager(this@StockActivity)
        }
    }

    private fun fetchStockFromSupabase() {
        lifecycleScope.launch {
            try {
                Log.d("Stock", "🔄 เริ่มโหลดข้อมูล Stock...")

                val results = SupabaseConfig.client.from("stocks").select().decodeList<Stock>()

                Log.d("Stock", "📦 โหลดได้ ${results.size} รายการ")

                val uiItems = results.map { dbStock ->
                    val imageRes = when (dbStock.stockname) {
                        "โกโก้" -> R.drawable.stock1
                        "ครีมเทียม" -> R.drawable.stock2
                        "สตอร์เบอร์รี่" -> R.drawable.stock3
                        "โยเกิร์ด" -> R.drawable.stock4
                        "โคล่า" -> R.drawable.stock5
                        "น้ำแข็ง" -> R.drawable.stock6
                        "น้ำตาล" -> R.drawable.stock7
                        "น้ำเชื่อม" -> R.drawable.stock8
                        "นมข้น" -> R.drawable.stock9
                        "นมสด" -> R.drawable.stock10
                        "ผงนมสด" -> R.drawable.stock11
                        "ผงชาเย็น" -> R.drawable.stock12
                        "ผงแคนตาลูป" -> R.drawable.stock13
                        "ผงชาเขียว" -> R.drawable.stock14
                        "ผงเผือก" -> R.drawable.stock15
                        "ไข่มุก" -> R.drawable.stock16
                        "เยลลี่" -> R.drawable.stock17
                        else -> R.drawable.stock1
                    }

                    StockItem(
                        id = dbStock.stockid ?: "",
                        name = dbStock.stockname,
                        quantity = dbStock.stockamount,
                        imageResource = imageRes
                    )
                }

                runOnUiThread {
                    stockAdapter.updateStockItems(uiItems)
                    Log.d("Stock", "✅ อัปเดต UI เรียบร้อย")
                }

            } catch (e: Exception) {
                Log.e("Stock", "❌ Error fetching: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@StockActivity, "โหลดข้อมูลล้มเหลว", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateStockToSupabase(item: StockItem) {
        lifecycleScope.launch {
            try {
                Log.d("Stock", "=== เริ่มอัปเดต Stock ===")
                Log.d("Stock", "Stock ID: ${item.id}")
                Log.d("Stock", "ชื่อสินค้า: ${item.name}")
                Log.d("Stock", "จำนวนเดิม: ${item.originalQuantity}")
                Log.d("Stock", "จำนวนใหม่: ${item.quantity}")

                // อัปเดตข้อมูล
                SupabaseConfig.client.from("stocks").update(
                    mapOf("stockamount" to item.quantity)
                ) {
                    filter { eq("stockid", item.id) }
                }

                Log.d("Stock", "✅ ส่งคำสั่ง UPDATE สำเร็จ")

                // เช็คว่าอัปเดตจริงหรือไม่
                val result = SupabaseConfig.client.from("stocks")
                    .select {
                        filter { eq("stockid", item.id) }
                    }
                    .decodeSingle<Stock>()

                Log.d("Stock", "🔍 ค่าหลังอัปเดต: ${result.stockamount}")
                Log.d("Stock", "=== จบการอัปเดต ===")

                runOnUiThread {
                    if (result.stockamount == item.quantity) {
                        // อัปเดตสำเร็จ
                        item.originalQuantity = item.quantity
                        stockAdapter.notifyDataSetChanged()
                        Toast.makeText(
                            this@StockActivity,
                            "✅ บันทึกสำเร็จ: ${item.name} = ${item.quantity}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // อัปเดตไม่สำเร็จ
                        Toast.makeText(
                            this@StockActivity,
                            "⚠️ อัปเดตไม่สำเร็จ: ยังเป็น ${result.stockamount}",
                            Toast.LENGTH_LONG
                        ).show()
                        // รีเฟรชข้อมูลใหม่
                        fetchStockFromSupabase()
                    }
                }

            } catch (e: Exception) {
                Log.e("Stock", "❌ Update Error: ${e.message}", e)
                e.printStackTrace()

                runOnUiThread {
                    Toast.makeText(
                        this@StockActivity,
                        "ข้อผิดพลาด: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupNavigationListeners() {
        navAnalytics.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        navMenu.setOnClickListener {
            startActivity(Intent(this, OrderStatusActivity::class.java))
        }
        navDelete.setOnClickListener {
            Toast.makeText(this, "กำลังรีเฟรช...", Toast.LENGTH_SHORT).show()
            fetchStockFromSupabase()
        }
    }
}