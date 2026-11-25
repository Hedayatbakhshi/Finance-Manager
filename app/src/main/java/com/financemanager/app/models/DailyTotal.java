package com.financemanager.app.models;

import androidx.room.ColumnInfo;

/** Total for a single day. */
public class DailyTotal {

    /** Local midnight timestamp of the day. */
    @ColumnInfo(name = "day_millis")
    public long dayMillis;

    @ColumnInfo(name = "total_minor")
    public long totalMinor;
}
