#!/usr/bin/env python3
"""Download Material Symbols (rounded) SVGs and convert them to Android vector drawables."""
import os
import re
import urllib.request

BASE = "https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsrounded/{name}/default/24px.svg"
OUT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                       "app", "src", "main", "res", "drawable")

ICONS = {
    "nav_home": "home",
    "nav_transactions": "receipt_long",
    "nav_budgets": "request_quote",
    "nav_reports": "analytics",
    "nav_more": "more_horiz",
    "nav_home_filled": "home",
    "nav_transactions_filled": "receipt_long",
    "nav_budgets_filled": "request_quote",
    "nav_reports_filled": "analytics",
    "action_add": "add",
    "action_search": "search",
    "action_filter": "tune",
    "action_settings": "settings",
    "action_notifications": "notifications",
    "action_visibility": "visibility",
    "action_visibility_off": "visibility_off",
    "arrow_up": "arrow_upward",
    "arrow_down": "arrow_downward",
    "chevron_right": "chevron_right",
    "arrow_back": "arrow_back",
    "action_delete": "delete",
    "action_edit": "edit",
    "action_close": "close",
    "action_download": "download",
    "action_upload": "upload",
    "action_share": "share",
    "action_check": "check",
    "action_check_circle": "check_circle",
    "action_warning": "warning",
    "action_error": "error",
    "action_info": "info",
    "action_sort": "sort",
    "action_more_vert": "more_vert",
    "action_logout": "logout",
    "action_add_circle": "add_circle",
    "action_remove_circle": "remove_circle",
    "action_flag": "flag",
    "action_trending_up": "trending_up",
    "action_trending_down": "trending_down",
    "action_swap": "swap_horiz",
    "action_repeat": "repeat",
    "action_calendar": "calendar_month",
    "action_schedule": "schedule",
    "action_pie_chart": "pie_chart",
    "action_bar_chart": "bar_chart",
    "action_line_chart": "show_chart",
    "action_receipt": "receipt_long",
    "action_transfer": "swap_horiz",
    "action_target": "track_changes",
    "action_theme": "palette",
    "action_language": "language",
    "action_security": "security",
    "action_lock": "lock",
    "action_fingerprint": "fingerprint",
    "action_backup": "backup",
    "action_restore": "restore",
    "action_export": "file_upload",
    "action_person": "person",
    "action_about": "info",
    "action_wallet": "account_balance_wallet",
    "action_notifications_active": "notifications_active",
    "action_category": "category",
    "action_account": "account_balance_wallet",
    "action_data": "folder_open",
    "cat_food": "restaurant",
    "cat_transport": "directions_car",
    "cat_shopping": "shopping_bag",
    "cat_entertainment": "movie",
    "cat_health": "medical_services",
    "cat_education": "school",
    "cat_housing": "home",
    "cat_utilities": "bolt",
    "cat_travel": "flight",
    "cat_subscriptions": "subscriptions",
    "cat_personal_care": "spa",
    "cat_other": "category",
    "cat_salary": "payments",
    "cat_freelance": "work",
    "cat_business": "business_center",
    "cat_investment": "trending_up",
    "cat_gifts": "card_giftcard",
    "account_cash": "payments",
    "account_bank": "account_balance",
    "account_savings": "savings",
    "account_credit": "credit_card",
    "account_wallet": "account_balance_wallet",
    "goal_target": "track_changes",
}


def fetch(name):
    req = urllib.request.Request(BASE.format(name=name), headers={"User-Agent": "icon-fetch"})
    with urllib.request.urlopen(req, timeout=30) as response:
        return response.read().decode("utf-8")


def convert(svg, drawable_name):
    paths = re.findall(r'<path[^>]*?d="([^"]+)"[^>]*/?>', svg)
    if not paths:
        raise ValueError("no path found in " + drawable_name)
    body = []
    for d in paths:
        body.append('        <path\n            android:fillColor="#FF000000"\n'
                    '            android:pathData="%s" />' % d)
    return ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            "    android:width=\"24dp\"\n"
            "    android:height=\"24dp\"\n"
            "    android:viewportWidth=\"960\"\n"
            "    android:viewportHeight=\"960\">\n"
            "    <group android:translateY=\"960\">\n"
            + "\n".join(body) +
            "\n    </group>\n</vector>\n")


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for drawable, symbol in ICONS.items():
        svg = fetch(symbol)
        content = convert(svg, drawable)
        target = os.path.join(OUT_DIR, "ic_" + drawable + ".xml")
        with open(target, "w", encoding="utf-8") as handle:
            handle.write(content)
        print("wrote", os.path.basename(target))


if __name__ == "__main__":
    main()
