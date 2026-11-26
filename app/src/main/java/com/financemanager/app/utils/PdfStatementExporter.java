package com.financemanager.app.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.text.TextPaint;
import android.text.TextUtils;

import com.financemanager.app.R;
import com.financemanager.app.data.FinanceDatabase;
import com.financemanager.app.data.entity.Category;
import com.financemanager.app.data.entity.TransactionEntity;
import com.financemanager.app.models.CategoryTotal;
import com.financemanager.app.models.TransactionType;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a printable A4 statement for the current month: summary, expense
 * breakdown and the full transaction list, paginated as needed.
 *
 * <p>Uses the platform {@link PdfDocument} so no extra dependency is required.</p>
 */
public class PdfStatementExporter {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final float MARGIN = 46f;
    private static final float CONTENT_RIGHT = PAGE_WIDTH - MARGIN;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2;
    private static final float CONTENT_BOTTOM = PAGE_HEIGHT - 62f;
    private static final float ROW_HEIGHT = 16f;

    private static final int INK = Color.rgb(23, 28, 36);
    private static final int MUTED = Color.rgb(107, 114, 128);
    private static final int LINE = Color.rgb(229, 231, 235);
    private static final int ACCENT = Color.rgb(22, 163, 106);
    private static final int EXPENSE = Color.rgb(214, 69, 69);

    private final Context context;
    private final FinanceDatabase database;
    private final Paint paint = new Paint();

    public PdfStatementExporter(Context context, FinanceDatabase database) {
        this.context = context.getApplicationContext();
        this.database = database;
    }

    public static String suggestedFileName() {
        return "finance-manager-statement-" + LocalDate.now() + ".pdf";
    }

    /** Writes the statement and returns how many transactions it contains. */
    public int export(Uri uri, ContentResolver resolver, String currency) throws IOException {
        LocalDate today = LocalDate.now();
        long from = DateUtils.startOfMonth(today);
        long to = DateUtils.endOfMonth(today);

        long income = database.transactionDao()
                .totalForType(TransactionType.INCOME, from, to, currency);
        long expense = database.transactionDao()
                .totalForType(TransactionType.EXPENSE, from, to, currency);
        List<CategoryTotal> categories = database.transactionDao()
                .categoryTotals(TransactionType.EXPENSE, from, to, currency);
        List<TransactionEntity> transactions = database.transactionDao()
                .getForStatement(from, to, currency);

        Map<Long, String> categoryNames = new HashMap<>();
        for (Category category : database.categoryDao().getAllSync()) {
            categoryNames.put(category.id, category.name);
        }

        PdfDocument document = new PdfDocument();
        try {
            int pageCount = 1;
            Sheet sheet = new Sheet(document, pageCount);
            drawHeader(sheet, today, currency);

            drawSummary(sheet, income, expense, currency);
            sheet.y += 28f;

            drawText(sheet.canvas, context.getString(R.string.expenses_by_category),
                    MARGIN, sheet.y, 12.5f, Typeface.BOLD, INK);
            sheet.y += 20f;
            if (categories.isEmpty()) {
                drawText(sheet.canvas, context.getString(R.string.pdf_no_expenses),
                        MARGIN, sheet.y, 10f, Typeface.NORMAL, MUTED);
                sheet.y += ROW_HEIGHT;
            } else {
                for (CategoryTotal category : categories) {
                    if (!sheet.hasRoom(ROW_HEIGHT)) {
                        sheet.finish();
                        sheet = newPage(document, ++pageCount, today, currency);
                    }
                    String name = category.categoryName == null
                            ? context.getString(R.string.category_uncategorized)
                            : category.categoryName;
                    drawText(sheet.canvas, fit(name, CONTENT_WIDTH - 110f, 10f, Typeface.NORMAL),
                            MARGIN, sheet.y, 10f, Typeface.NORMAL, INK);
                    drawText(sheet.canvas, Money.format(category.totalMinor, currency),
                            CONTENT_RIGHT, sheet.y, 10f, Typeface.NORMAL, INK, Paint.Align.RIGHT);
                    sheet.y += ROW_HEIGHT;
                }
            }
            sheet.y += 22f;

            if (!sheet.hasRoom(60f)) {
                sheet.finish();
                sheet = newPage(document, ++pageCount, today, currency);
            }
            drawText(sheet.canvas, context.getString(R.string.pdf_transactions),
                    MARGIN, sheet.y, 12.5f, Typeface.BOLD, INK);
            sheet.y += 20f;

            if (transactions.isEmpty()) {
                drawText(sheet.canvas, context.getString(R.string.pdf_no_transactions),
                        MARGIN, sheet.y, 10f, Typeface.NORMAL, MUTED);
                sheet.y += ROW_HEIGHT;
            } else {
                drawTableHeader(sheet);
                for (TransactionEntity transaction : transactions) {
                    if (!sheet.hasRoom(ROW_HEIGHT + 2f)) {
                        sheet.finish();
                        sheet = new Sheet(document, ++pageCount);
                        drawContinuation(sheet, today, currency);
                        drawTableHeader(sheet);
                    }
                    String description = transaction.title == null
                            || transaction.title.trim().isEmpty()
                            ? categoryNames.getOrDefault(transaction.categoryId,
                            context.getString(R.string.category_uncategorized))
                            : transaction.title;
                    drawText(sheet.canvas, DateUtils.formatShortDay(transaction.date),
                            MARGIN, sheet.y, 9.5f, Typeface.NORMAL, MUTED);
                    drawText(sheet.canvas,
                            fit(description, CONTENT_WIDTH - 56f - 104f, 9.5f, Typeface.NORMAL),
                            MARGIN + 56f, sheet.y, 9.5f, Typeface.NORMAL, INK);
                    boolean isIncome = transaction.type == TransactionType.INCOME;
                    long signed = isIncome ? transaction.amountMinor : -transaction.amountMinor;
                    drawText(sheet.canvas, Money.formatSigned(signed, currency),
                            CONTENT_RIGHT, sheet.y, 9.5f, Typeface.NORMAL,
                            isIncome ? ACCENT : EXPENSE, Paint.Align.RIGHT);
                    sheet.y += ROW_HEIGHT;
                }
            }

            sheet.finish();
            try (OutputStream stream = resolver.openOutputStream(uri, "w")) {
                if (stream == null) {
                    throw new IOException("Could not open the selected file");
                }
                document.writeTo(stream);
                stream.flush();
            }
            return transactions.size();
        } finally {
            document.close();
        }
    }

    private void drawHeader(Sheet sheet, LocalDate today, String currency) {
        drawText(sheet.canvas, context.getString(R.string.app_name),
                MARGIN, 52f, 10f, Typeface.BOLD, INK);
        drawText(sheet.canvas, context.getString(R.string.pdf_statement_title),
                MARGIN, 86f, 22f, Typeface.BOLD, INK);
        drawText(sheet.canvas, context.getString(R.string.pdf_period,
                        DateUtils.formatMonth(DateUtils.startOfMonth(today)), currency),
                MARGIN, 106f, 10f, Typeface.NORMAL, MUTED);
        drawText(sheet.canvas, context.getString(R.string.pdf_generated,
                        DateUtils.formatDay(System.currentTimeMillis())),
                CONTENT_RIGHT, 106f, 9f, Typeface.NORMAL, MUTED, Paint.Align.RIGHT);
        drawLine(sheet.canvas, 120f);
        sheet.y = 146f;
    }

    private void drawContinuation(Sheet sheet, LocalDate today, String currency) {
        drawText(sheet.canvas, context.getString(R.string.app_name),
                MARGIN, 52f, 10f, Typeface.BOLD, INK);
        drawText(sheet.canvas, context.getString(R.string.pdf_period,
                        DateUtils.formatMonth(DateUtils.startOfMonth(today)), currency),
                CONTENT_RIGHT, 52f, 9f, Typeface.NORMAL, MUTED, Paint.Align.RIGHT);
        drawLine(sheet.canvas, 64f);
        sheet.y = 92f;
    }

    private void drawSummary(Sheet sheet, long income, long expense, String currency) {
        float gap = 10f;
        float width = (CONTENT_WIDTH - gap * 2f) / 3f;
        float height = 62f;
        float top = sheet.y;
        long net = income - expense;

        drawSummaryBox(sheet.canvas, MARGIN, top, width, height,
                context.getString(R.string.income), Money.format(income, currency), INK);
        drawSummaryBox(sheet.canvas, MARGIN + width + gap, top, width, height,
                context.getString(R.string.expenses), Money.format(expense, currency), EXPENSE);
        drawSummaryBox(sheet.canvas, MARGIN + (width + gap) * 2f, top, width, height,
                context.getString(R.string.net_label), Money.formatSigned(net, currency),
                net >= 0 ? ACCENT : EXPENSE);
        sheet.y = top + height;
    }

    private void drawSummaryBox(Canvas canvas, float x, float top, float width, float height,
                                String label, String value, int valueColor) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(Color.rgb(247, 248, 250));
        canvas.drawRoundRect(x, top, x + width, top + height, 8f, 8f, paint);
        drawText(canvas, label, x + 10f, top + 20f, 8.5f, Typeface.NORMAL, MUTED);
        drawText(canvas, fit(value, width - 20f, 13f, Typeface.BOLD),
                x + 10f, top + 43f, 13f, Typeface.BOLD, valueColor);
    }

    private void drawTableHeader(Sheet sheet) {
        drawText(sheet.canvas, context.getString(R.string.date),
                MARGIN, sheet.y, 8.5f, Typeface.BOLD, MUTED);
        drawText(sheet.canvas, context.getString(R.string.description),
                MARGIN + 56f, sheet.y, 8.5f, Typeface.BOLD, MUTED);
        drawText(sheet.canvas, context.getString(R.string.amount),
                CONTENT_RIGHT, sheet.y, 8.5f, Typeface.BOLD, MUTED, Paint.Align.RIGHT);
        sheet.y += 6f;
        drawLine(sheet.canvas, sheet.y);
        sheet.y += 14f;
    }

    private void drawLine(Canvas canvas, float y) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(LINE);
        paint.setStrokeWidth(1f);
        canvas.drawLine(MARGIN, y, CONTENT_RIGHT, y, paint);
    }

    private void drawText(Canvas canvas, String text, float x, float y, float size,
                          int style, int color) {
        drawText(canvas, text, x, y, size, style, color, Paint.Align.LEFT);
    }

    private void drawText(Canvas canvas, String text, float x, float y, float size,
                          int style, int color, Paint.Align align) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, style));
        paint.setTextAlign(align);
        canvas.drawText(text, x, y, paint);
    }

    private String fit(String text, float width, float size, int style) {
        TextPaint textPaint = new TextPaint(paint);
        textPaint.setTextSize(size);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, style));
        return TextUtils.ellipsize(text, textPaint, width, TextUtils.TruncateAt.END).toString();
    }

    private Sheet newPage(PdfDocument document, int number, LocalDate today, String currency) {
        Sheet sheet = new Sheet(document, number);
        drawContinuation(sheet, today, currency);
        return sheet;
    }

    /** One A4 page plus the current vertical position while drawing. */
    private final class Sheet {
        final PdfDocument document;
        final PdfDocument.Page page;
        final Canvas canvas;
        final int number;
        float y;

        Sheet(PdfDocument document, int number) {
            this.document = document;
            this.page = document.startPage(new PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH, PAGE_HEIGHT, number).create());
            this.canvas = page.getCanvas();
            this.number = number;
            canvas.drawColor(Color.WHITE);
        }

        boolean hasRoom(float height) {
            return y + height <= CONTENT_BOTTOM;
        }

        void finish() {
            drawLine(canvas, PAGE_HEIGHT - 46f);
            drawText(canvas, context.getString(R.string.app_name),
                    MARGIN, PAGE_HEIGHT - 30f, 8f, Typeface.NORMAL, MUTED);
            drawText(canvas, context.getString(R.string.pdf_page, number),
                    CONTENT_RIGHT, PAGE_HEIGHT - 30f, 8f, Typeface.NORMAL, MUTED,
                    Paint.Align.RIGHT);
            document.finishPage(page);
        }
    }
}
