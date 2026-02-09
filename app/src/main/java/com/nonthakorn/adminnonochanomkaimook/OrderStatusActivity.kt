package com.nonthakorn.adminnonochanomkaimook

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class OrderData(
    val orderid: Int? = null,
    val userid: Int? = null,
    val menuid: Int? = null,
    val cupsize: String? = null,
    val topping: String? = null,
    val status: String? = null,
    val orderdatecreate: String? = null,
    val detailtext: String? = null
)

@Serializable
data class UserData(
    val userid: Int,
    val username: String,
    val phone: String? = null
)

enum class OrderStatus(val thaiName: String, val dbValue: String) {
    PENDING("รอดำเนินการ", "pending"),
    PROCESSING("กำลังทำ", "processing"),
    READY("พร้อมรับ", "ready"),
    COMPLETED("เสร็จสิ้น", "success"),
    CANCELLED("ยกเลิก", "cancel")
}

data class OrderItem(
    val id: String,
    val userName: String,
    val userPhone: String,
    val menuName: String,
    val cupSize: String,
    val toppings: String,
    val orderTime: String,
    var status: OrderStatus,
    val imageResource: Int,
    val detailText: String? = null
)

class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val imageOrder: ImageView = itemView.findViewById(R.id.imageOrder)
    val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
    val tvOrderTime: TextView = itemView.findViewById(R.id.tvOrderTime)
    val tvMenuName: TextView = itemView.findViewById(R.id.tvMenuName)
    val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    val tvCupSize: TextView = itemView.findViewById(R.id.tvCupSize)
    val tvToppings: TextView = itemView.findViewById(R.id.tvToppings)
    val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
    val tvUserPhone: TextView = itemView.findViewById(R.id.tvUserPhone)
    val cardDetailText: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.cardDetailText)
    val tvDetailText: TextView = itemView.findViewById(R.id.tvDetailText)

    val btnProcessing: Button = itemView.findViewById(R.id.btnProcessing)
    val btnReady: Button = itemView.findViewById(R.id.btnReady)
    val btnComplete: Button = itemView.findViewById(R.id.btnComplete)
    val btnCancel: Button = itemView.findViewById(R.id.btnCancel)
    val btnDelete: Button = itemView.findViewById(R.id.btnDelete) // ⭐ เพิ่มปุ่มลบ

    fun bind(item: OrderItem, onStatusChanged: (OrderItem, OrderStatus) -> Unit, onDelete: (OrderItem) -> Unit) { // ⭐ เพิ่ม parameter
        imageOrder.setImageResource(item.imageResource)
        tvOrderId.text = "Order #${item.id}"
        tvOrderTime.text = item.orderTime
        tvMenuName.text = item.menuName
        tvCupSize.text = "ขนาด: ${item.cupSize}"
        tvToppings.text = "ท็อปปิ้ง: ${if (item.toppings.isNullOrEmpty()) "ไม่มี" else item.toppings}"

        tvUserName.text = "ลูกค้า: ${item.userName}"
        tvUserPhone.text = "เบอร์: ${item.userPhone}"

        // แสดงโน้ต
        if (!item.detailText.isNullOrEmpty()) {
            tvDetailText.text = "หมายเหตุ: ${item.detailText}"
            cardDetailText.visibility = View.VISIBLE
        } else {
            cardDetailText.visibility = View.GONE
        }

        tvStatus.text = item.status.thaiName

        val (bgColor, textColor) = when (item.status) {
            OrderStatus.PENDING -> Pair(android.R.color.darker_gray, android.R.color.white)
            OrderStatus.PROCESSING -> Pair(android.R.color.holo_orange_light, android.R.color.white)
            OrderStatus.READY -> Pair(android.R.color.holo_blue_light, android.R.color.white)
            OrderStatus.COMPLETED -> Pair(android.R.color.holo_green_light, android.R.color.white)
            OrderStatus.CANCELLED -> Pair(android.R.color.holo_red_light, android.R.color.white)
        }

        tvStatus.setBackgroundColor(ContextCompat.getColor(itemView.context, bgColor))
        tvStatus.setTextColor(ContextCompat.getColor(itemView.context, textColor))

        btnProcessing.isEnabled = true
        btnReady.isEnabled = true
        btnComplete.isEnabled = true
        btnCancel.isEnabled = true

        when (item.status) {
            OrderStatus.PENDING -> {
                btnProcessing.alpha = 0.5f
                btnReady.alpha = 0.5f
                btnComplete.alpha = 0.5f
                btnCancel.alpha = 1.0f
            }
            OrderStatus.PROCESSING -> {
                btnProcessing.alpha = 1.0f
                btnReady.alpha = 0.5f
                btnComplete.alpha = 0.5f
                btnCancel.alpha = 1.0f
            }
            OrderStatus.READY -> {
                btnProcessing.alpha = 0.5f
                btnReady.alpha = 1.0f
                btnComplete.alpha = 0.5f
                btnCancel.alpha = 1.0f
            }
            OrderStatus.COMPLETED -> {
                btnProcessing.alpha = 0.5f
                btnReady.alpha = 0.5f
                btnComplete.alpha = 1.0f
                btnCancel.alpha = 0.5f
            }
            OrderStatus.CANCELLED -> {
                btnProcessing.alpha = 0.5f
                btnReady.alpha = 0.5f
                btnComplete.alpha = 0.5f
                btnCancel.alpha = 1.0f
            }
        }

        btnProcessing.setOnClickListener {
            onStatusChanged(item, OrderStatus.PROCESSING)
        }

        btnReady.setOnClickListener {
            onStatusChanged(item, OrderStatus.READY)
        }

        btnComplete.setOnClickListener {
            onStatusChanged(item, OrderStatus.COMPLETED)
        }

        btnCancel.setOnClickListener {
            if (item.status == OrderStatus.CANCELLED) {
                showRestoreConfirmation(itemView, item, onStatusChanged)
            } else {
                showCancelConfirmation(itemView, item, onStatusChanged)
            }
        }

        // ⭐ ปุ่มลบ
        btnDelete.setOnClickListener {
            showDeleteConfirmation(itemView, item, onDelete)
        }
    }

    private fun showCancelConfirmation(view: View, item: OrderItem, onStatusChanged: (OrderItem, OrderStatus) -> Unit) {
        android.app.AlertDialog.Builder(view.context)
            .setTitle("ยืนยันการยกเลิก")
            .setMessage("ต้องการยกเลิก Order #${item.id} ใช่หรือไม่?")
            .setPositiveButton("ยืนยัน") { _, _ ->
                onStatusChanged(item, OrderStatus.CANCELLED)
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun showRestoreConfirmation(view: View, item: OrderItem, onStatusChanged: (OrderItem, OrderStatus) -> Unit) {
        android.app.AlertDialog.Builder(view.context)
            .setTitle("กู้คืนออเดอร์")
            .setMessage("ต้องการกู้คืน Order #${item.id} กลับเป็น 'รอดำเนินการ' ใช่หรือไม่?")
            .setPositiveButton("ยืนยัน") { _, _ ->
                onStatusChanged(item, OrderStatus.PENDING)
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    // ⭐ ยืนยันการลบ
    private fun showDeleteConfirmation(view: View, item: OrderItem, onDelete: (OrderItem) -> Unit) {
        android.app.AlertDialog.Builder(view.context)
            .setTitle("⚠️ ยืนยันการลบ")
            .setMessage("ต้องการลบ Order #${item.id} ออกจากระบบถาวรใช่หรือไม่?\n\n⚠️ การลบจะไม่สามารถกู้คืนได้")
            .setPositiveButton("ลบ") { _, _ ->
                onDelete(item)
            }
            .setNegativeButton("ยกเลิก", null)
            .setCancelable(true)
            .show()
    }
}

class OrderAdapter(
    private var list: List<OrderItem>,
    val onStatusChanged: (OrderItem, OrderStatus) -> Unit,
    val onDelete: (OrderItem) -> Unit
) : RecyclerView.Adapter<OrderViewHolder>() {

    override fun onCreateViewHolder(p: ViewGroup, p1: Int) =
        OrderViewHolder(LayoutInflater.from(p.context).inflate(R.layout.item_order, p, false))

    override fun onBindViewHolder(h: OrderViewHolder, p: Int) =
        h.bind(list[p], onStatusChanged, onDelete)

    override fun getItemCount() = list.size

    fun updateOrderItems(newList: List<OrderItem>) {
        list = newList
        notifyDataSetChanged()
    }

    // ⭐ เพิ่มฟังก์ชันนี้
    fun getItems(): List<OrderItem> = list
}

class OrderStatusActivity : AppCompatActivity() {
    private lateinit var rv: RecyclerView
    private lateinit var adapter: OrderAdapter
    private val menuCache = mutableMapOf<Int, Pair<String, Int>>()
    private val userCache = mutableMapOf<Int, Pair<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_order_status)

        rv = findViewById(R.id.recyclerViewOrders)

        // ⭐ ส่งทั้ง updateOrderStatus และ deleteOrder
        adapter = OrderAdapter(
            emptyList(),
            onStatusChanged = { item, status -> updateOrderStatus(item, status) },
            onDelete = { item -> deleteOrder(item) } // ⭐ เพิ่มฟังก์ชันลบ
        )

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        loadMenusAndOrders()

        findViewById<LinearLayout>(R.id.nav_menu)?.setOnClickListener {
            Toast.makeText(this, "กำลังรีเฟรช...", Toast.LENGTH_SHORT).show()
            loadMenusAndOrders()
        }

        findViewById<LinearLayout>(R.id.nav_analytics)?.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        findViewById<LinearLayout>(R.id.nav_delete)?.setOnClickListener { startActivity(Intent(this, StockActivity::class.java)) }
    }

    private fun loadMenusAndOrders() {
        lifecycleScope.launch {
            try {
                Log.d("Supabase", "กำลังโหลดข้อมูลเมนู...")
                val menus = SupabaseConfig.client.from("menus")
                    .select()
                    .decodeList<MenuData>()

                menus.forEach { menu ->
                    val imageName = menu.menupicture?.trim() ?: "product01"
                    val imageResId = resources.getIdentifier(imageName, "drawable", packageName)
                    val finalImageRes = if (imageResId != 0) imageResId else R.drawable.product01

                    menuCache[menu.menuid] = Pair(menu.namemenu, finalImageRes)
                }

                Log.d("Supabase", "กำลังโหลดข้อมูล users...")
                val users = SupabaseConfig.client.from("profiles")
                    .select()
                    .decodeList<UserData>()

                users.forEach { user ->
                    userCache[user.userid] = Pair(user.username, user.phone ?: "ไม่ระบุ")
                }

                fetchOrders()

            } catch (e: Exception) {
                Log.e("Supabase", "โหลดข้อมูลล้มเหลว: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@OrderStatusActivity, "โหลดข้อมูลล้มเหลว", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchOrders() {
        lifecycleScope.launch {
            try {
                Log.d("Supabase", "เริ่มดึงข้อมูลออเดอร์...")

                val results = SupabaseConfig.client.from("orders")
                    .select {
                        order("orderdatecreate", order = Order.DESCENDING)
                    }
                    .decodeList<OrderData>()

                val uiItems = results.map { data ->
                    val menuInfo = menuCache[data.menuid] ?: Pair("เมนู #${data.menuid}", R.drawable.product01)
                    val userInfo = userCache[data.userid] ?: Pair("ลูกค้า #${data.userid}", "ไม่ทราบ")

                    val orderTime = try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        inputFormat.timeZone = TimeZone.getTimeZone("UTC")

                        val date = inputFormat.parse(data.orderdatecreate?.substringBefore(".") ?: "")

                        if (date != null) {
                            val outputFormat = SimpleDateFormat("d MMM yyyy HH:mm", Locale("th", "TH"))
                            outputFormat.timeZone = TimeZone.getTimeZone("Asia/Bangkok")
                            outputFormat.format(date)
                        } else {
                            data.orderdatecreate?.substringBefore(".")?.replace("T", " ") ?: ""
                        }
                    } catch (e: Exception) {
                        Log.e("Supabase", "แปลงเวลาไม่ได้", e)
                        data.orderdatecreate?.substringBefore(".")?.replace("T", " ") ?: ""
                    }

                    OrderItem(
                        id = data.orderid.toString(),
                        userName = userInfo.first,
                        userPhone = userInfo.second,
                        menuName = menuInfo.first,
                        cupSize = when(data.cupsize) {
                            "small" -> "เล็ก"
                            "medium" -> "กลาง"
                            "large" -> "ใหญ่"
                            else -> data.cupsize ?: "ปกติ"
                        },
                        toppings = when(data.topping) {
                            "none" -> "ไม่มี"
                            "bubble", "pearl" -> "ไข่มุก"
                            "jelly" -> "เยลลี่"
                            else -> data.topping ?: "ไม่มี"
                        },
                        orderTime = orderTime,
                        status = try {
                            val statusStr = data.status?.trim()?.lowercase() ?: "pending"
                            when (statusStr) {
                                "pending" -> OrderStatus.PENDING
                                "processing" -> OrderStatus.PROCESSING
                                "ready" -> OrderStatus.READY
                                "success", "completed" -> OrderStatus.COMPLETED
                                "cancel", "cancelled" -> OrderStatus.CANCELLED
                                else -> OrderStatus.PENDING
                            }
                        } catch(e: Exception) {
                            OrderStatus.PENDING
                        },
                        imageResource = menuInfo.second,
                        detailText = data.detailtext
                    )
                }

                runOnUiThread {
                    adapter.updateOrderItems(uiItems)
                    Toast.makeText(this@OrderStatusActivity, "โหลด ${uiItems.size} รายการ", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("Supabase", "Fetch Error: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this@OrderStatusActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateOrderStatus(item: OrderItem, newStatus: OrderStatus) {
        lifecycleScope.launch {
            try {
                val orderIdInt = item.id.toInt()
                val statusValue = newStatus.dbValue

                Log.d("Supabase", "=== เริ่มอัปเดตสถานะ ===")
                Log.d("Supabase", "Order ID: $orderIdInt")
                Log.d("Supabase", "Status เก่า: ${item.status.dbValue}")
                Log.d("Supabase", "Status ใหม่: $statusValue")

                val updateData = mapOf("status" to statusValue)
                SupabaseConfig.client.from("orders").update(updateData) {
                    filter {
                        eq("orderid", orderIdInt)
                    }
                }

                Log.d("Supabase", "✅ อัปเดตสำเร็จ")

                runOnUiThread {
                    Toast.makeText(
                        this@OrderStatusActivity,
                        "✅ Order #${item.id} → ${newStatus.thaiName}",
                        Toast.LENGTH_SHORT
                    ).show()
                    loadMenusAndOrders()
                }
            } catch (e: Exception) {
                Log.e("Supabase", "❌ Update Error: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(
                        this@OrderStatusActivity,
                        "❌ ข้อผิดพลาด: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ⭐ ฟังก์ชันลบออเดอร์
    // ⭐ ฟังก์ชันลบออเดอร์ (แก้ไขใหม่)
    private fun deleteOrder(item: OrderItem) {
        lifecycleScope.launch {
            try {
                val orderIdInt = item.id.toInt()

                Log.d("Supabase", "=== เริ่มลบออเดอร์ ===")
                Log.d("Supabase", "Order ID: $orderIdInt")

                // ลบออเดอร์จาก Supabase
                SupabaseConfig.client.from("orders").delete {
                    filter {
                        eq("orderid", orderIdInt)
                    }
                }

                Log.d("Supabase", "✅ ลบออกจาก Database สำเร็จ!")

                runOnUiThread {
                    // ⭐ ลบออกจาก Adapter ทันที (ไม่ต้องรอโหลดใหม่)
                    val currentList = adapter.getItems().toMutableList()
                    val removedItem = currentList.find { it.id == item.id }

                    if (removedItem != null) {
                        currentList.remove(removedItem)
                        adapter.updateOrderItems(currentList)
                        Log.d("Supabase", "✅ ลบออกจาก UI สำเร็จ!")
                    }

                    Toast.makeText(
                        this@OrderStatusActivity,
                        "✅ ลบ Order #${item.id} สำเร็จ",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("Supabase", "❌ Delete Error: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(
                        this@OrderStatusActivity,
                        "❌ ลบไม่สำเร็จ: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}