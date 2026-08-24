package com.example.util

import com.example.model.AppLanguage

object LocalizationHelper {

    fun getString(key: String, language: AppLanguage): String {
        val isFa = language == AppLanguage.PERSIAN
        return when (key) {
            "app_name" -> if (isFa) "ShadowRay فیلترشکن" else "ShadowRay VPN"
            "nav_home" -> if (isFa) "اتصال" else "Connect"
            "nav_configs" -> if (isFa) "کانفیگ‌ها" else "Configs"
            "nav_subscriptions" -> if (isFa) "اشتراک‌ها" else "Subscriptions"
            "nav_settings" -> if (isFa) "تنظیمات" else "Settings"
            "nav_logs" -> if (isFa) "گزارشات" else "Logs"

            "status_connected" -> if (isFa) "متصل شد" else "Connected"
            "status_disconnected" -> if (isFa) "قطع شده" else "Disconnected"
            "status_connecting" -> if (isFa) "در حال اتصال…" else "Connecting…"
            "status_disconnecting" -> if (isFa) "در حال قطع…" else "Disconnecting…"

            "tap_to_connect" -> if (isFa) "برای اتصال لمس کنید" else "Tap to Connect"
            "tap_to_disconnect" -> if (isFa) "برای قطع اتصال لمس کنید" else "Tap to Disconnect"
            "no_config_selected" -> if (isFa) "هیچ کانفیگی انتخاب نشده" else "No config selected"
            "select_server" -> if (isFa) "انتخاب سرور" else "Select Server"
            "current_server" -> if (isFa) "سرور فعال" else "Active Server"

            "upload_speed" -> if (isFa) "آپلود" else "Upload"
            "download_speed" -> if (isFa) "دانلود" else "Download"
            "duration" -> if (isFa) "مدت زمان" else "Duration"
            "ping" -> if (isFa) "پینگ" else "Ping"
            "traffic_used" -> if (isFa) "حجم مصرفی" else "Data Used"
            "ip_address" -> if (isFa) "آدرس آی‌پی" else "IP Address"

            "speed_chart_title" -> if (isFa) "نمودار زنده ترافیک" else "Live Traffic Graph"
            "speed_chart_desc" -> if (isFa) "سرعت لحظه‌ای دانلود و آپلود" else "Real-time download & upload throughput"

            "btn_test_all_pings" -> if (isFa) "تست پینگ همه" else "Test All Pings"
            "btn_add_config" -> if (isFa) "افزودن کانفیگ" else "Add Config"
            "btn_sort_fastest" -> if (isFa) "مرتب‌سازی بر اساس پینگ" else "Sort by Lowest Ping"
            "search_placeholder" -> if (isFa) "جستجوی کانفیگ یا لوکیشن…" else "Search configs or host…"

            "chip_all" -> if (isFa) "همه" else "All"
            "chip_vless" -> "VLESS"
            "chip_vmess" -> "VMess"
            "chip_trojan" -> "Trojan"
            "chip_shadowsocks" -> "Shadowsocks"
            "chip_favorites" -> if (isFa) "نشان‌شده" else "Favorites"

            "tab_clipboard" -> if (isFa) "از کلیپ‌بورد / متن" else "From Clipboard"
            "tab_qr" -> if (isFa) "اسکن بارکد QR" else "QR Scanner"
            "tab_manual" -> if (isFa) "ساخت دستی" else "Manual Builder"
            "tab_subscription" -> if (isFa) "لینک اشتراک" else "Subscription"

            "import_success" -> if (isFa) "کانفیگ با موفقیت اضافه شد" else "Config imported successfully"
            "import_fail" -> if (isFa) "فرمت کانفیگ معتبر نیست" else "Invalid config format"
            "qr_code_title" -> if (isFa) "بارکد QR کانفیگ" else "Config QR Code"
            "copy_config" -> if (isFa) "کپی لینک" else "Copy Link"
            "copied_toast" -> if (isFa) "در کلیپ‌بورد کپی شد" else "Copied to clipboard"
            "share_config" -> if (isFa) "اشتراک‌گذاری" else "Share"
            "delete" -> if (isFa) "حذف" else "Delete"
            "edit" -> if (isFa) "ویرایش" else "Edit"

            "subs_title" -> if (isFa) "مدیریت لینک‌های اشتراک" else "Subscription Management"
            "subs_update_all" -> if (isFa) "بروزرسانی همه" else "Update All"
            "subs_add_btn" -> if (isFa) "افزودن اشتراک جدید" else "Add Subscription"
            "sub_url_label" -> if (isFa) "آدرس لینک اشتراک (Sub URL)" else "Subscription URL"
            "sub_name_label" -> if (isFa) "نام اشتراک" else "Subscription Name"
            "sub_update_success" -> if (isFa) "اشتراک با موفقیت بروزرسانی شد" else "Subscription updated"

            "settings_routing_title" -> if (isFa) "حالت مسیریابی و ترافیک" else "Routing & Traffic Rules"
            "routing_global" -> if (isFa) "ترافیک کل دستگاه (Global)" else "All Device Traffic (Global)"
            "routing_bypass_iran" -> if (isFa) "دور زدن سایت‌های ایرانی و LAN" else "Bypass Iranian & LAN sites"
            "routing_custom_apps" -> if (isFa) "فیلتر برنامه‌ها (Split Tunneling)" else "Per-App Filter (Split Tunneling)"

            "settings_dns_title" -> if (isFa) "تنظیمات سرور DNS" else "DNS Server Configuration"
            "settings_kill_switch" -> if (isFa) "کیل سوئیچ (Kill Switch)" else "Kill Switch"
            "settings_kill_switch_desc" -> if (isFa) "قطع کامل اینترنت در صورت قطع اتصال فیلترشکن" else "Block all traffic if VPN disconnects"

            "settings_language" -> if (isFa) "زبان برنامه" else "App Language"
            "settings_theme" -> if (isFa) "پوسته ظاهری" else "App Theme"
            "settings_app_filter" -> if (isFa) "انتخاب برنامه‌های عبوری (Split Tunneling)" else "Select Apps to Bypass VPN"

            "logs_title" -> if (isFa) "گزارشات و رخدادها" else "Event & Connection Logs"
            "clear_logs" -> if (isFa) "پاک کردن گزارشات" else "Clear Logs"
            "diagnostics_ping_tool" -> if (isFa) "ابزار تست شبکه و DNS" else "Network & DNS Diagnostics"

            "manual_name" -> if (isFa) "نام سرور / ریمارک" else "Config Name / Remark"
            "manual_server" -> if (isFa) "آدرس سرور (IP یا Domain)" else "Server Address (IP / Domain)"
            "manual_port" -> if (isFa) "پورت (Port)" else "Port"
            "manual_uuid" -> if (isFa) "شناسه UUID یا پسورد" else "UUID or Password"
            "manual_sni" -> if (isFa) "دامنه جعلی (SNI)" else "SNI / Host"
            "manual_path" -> if (isFa) "مسیر وب‌سوکت (Path)" else "WebSocket Path"
            "manual_security" -> if (isFa) "نوع امنیت (Security)" else "Security (TLS / Reality / None)"
            "manual_save" -> if (isFa) "ذخیره و ایجاد کانفیگ" else "Save & Create Config"

            else -> key
        }
    }
}
