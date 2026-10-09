package com.example.books;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfViewerActivity extends Activity {
    public static final String EXTRA_PATH = "pdf_path";
    public static final String EXTRA_TITLE = "pdf_title";
    private ParcelFileDescriptor descriptor;
    private PdfRenderer renderer;
    private final List<ZoomPageView> pageViews = new ArrayList<>();
    private TextView pageLabel;
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String path = getIntent().getStringExtra(EXTRA_PATH);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        setTitle(title == null ? "PDF Reader" : title);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xffeef1f5);

        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.setPadding(dp(16), dp(10), dp(12), dp(10));
        heading.setBackgroundColor(Color.WHITE);
        heading.setElevation(dp(2));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView brand = text("BOOKS  ·  PDF READER", 10, true, 0xff64748b);
        TextView name = text(title == null ? "Your document" : title, 17, true, 0xff172033);
        titles.addView(brand);
        titles.addView(name);
        heading.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        pageLabel = text("Pinch to zoom", 11, false, 0xff64748b);
        heading.addView(pageLabel);
        root.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout tools = new LinearLayout(this);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(dp(12), dp(6), dp(12), dp(6));
        tools.setBackgroundColor(Color.WHITE);
        Button minus = toolButton("−");
        Button reset = toolButton("Fit page");
        Button plus = toolButton("+");
        minus.setOnClickListener(v -> changeZoom(0.8f));
        plus.setOnClickListener(v -> changeZoom(1.25f));
        reset.setOnClickListener(v -> { for (ZoomPageView p : pageViews) p.setZoom(1f); pageLabel.setText("Fit page"); });
        tools.addView(minus, new LinearLayout.LayoutParams(dp(48), dp(42)));
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(0, dp(42), 1);
        resetParams.leftMargin = dp(8); resetParams.rightMargin = dp(8);
        tools.addView(reset, resetParams);
        tools.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(42)));
        root.addView(tools, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout pages = new LinearLayout(this);
        pages.setOrientation(LinearLayout.VERTICAL);
        pages.setPadding(dp(8), dp(10), dp(8), dp(18));
        ScrollView vertical = new ScrollView(this);
        vertical.setFillViewport(false);
        vertical.addView(pages, new ScrollView.LayoutParams(-1, -2));
        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setFillViewport(true);
        horizontal.setHorizontalScrollBarEnabled(true);
        horizontal.addView(vertical, new HorizontalScrollView.LayoutParams(-1, -1));
        root.addView(horizontal, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        try {
            if (path == null || !new File(path).isFile()) throw new IOException("The saved PDF file could not be found. Import it again.");
            descriptor = ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(descriptor);
            int availableWidth = Math.max(1, getResources().getDisplayMetrics().widthPixels - dp(40));
            int count = renderer.getPageCount();
            for (int index = 0; index < count; index++) {
                PdfRenderer.Page page = renderer.openPage(index);
                int width = availableWidth;
                int height = Math.max(1, (int)(width * (page.getHeight() / (float)page.getWidth()));
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                ZoomPageView image = new ZoomPageView(bitmap, width, height);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
                params.gravity = Gravity.CENTER_HORIZONTAL;
                params.bottomMargin = dp(12);
                pages.addView(image, params);
                pageViews.add(image);
            }
            pageLabel.setText(count + (count == 1 ? " page" : " pages") + " · pinch or use + / −");
            if (count == 0) showError(pages, "This PDF has no pages.");
        } catch (Exception e) {
            showError(pages, "Could not open this PDF. The file may be damaged or unsupported.\n" + e.getMessage());
            Toast.makeText(this, "PDF could not be opened", Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(String s, int size, boolean bold, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null, 1); return t;
    }
    private Button toolButton(String label) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(16);
        return b;
    }
    private void changeZoom(float factor) {
        for (ZoomPageView p : pageViews) p.setZoom(p.getZoom() * factor);
        pageLabel.setText("Zoom " + Math.round((pageViews.isEmpty() ? 1f : pageViews.get(0).getZoom()) * 100) + "%");
    }
    private void showError(LinearLayout parent, String message) {
        TextView error = text(message, 15, false, 0xff374151);
        error.setGravity(Gravity.CENTER); error.setPadding(dp(24), dp(32), dp(24), dp(32));
        parent.addView(error, new LinearLayout.LayoutParams(-1, -2));
    }

    private class ZoomPageView extends View {
        private final Bitmap bitmap;
        private final int baseWidth, baseHeight;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final ScaleGestureDetector detector;
        private float zoom = 1f;
        ZoomPageView(Bitmap bitmap, int width, int height) {
            super(PdfViewerActivity.this);
            this.bitmap = bitmap; baseWidth = width; baseHeight = height;
            setBackgroundColor(Color.WHITE);
            detector = new ScaleGestureDetector(PdfViewerActivity.this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector d) {
                    setZoom(zoom * d.getScaleFactor()); return true;
                }
            });
        }
        float getZoom() { return zoom; }
        void setZoom(float value) {
            zoom = Math.max(1f, Math.min(4f, value));
            ViewGroup.LayoutParams lp = getLayoutParams();
            if (lp != null) {
                lp.width = Math.max(baseWidth, (int)(baseWidth * zoom));
                lp.height = Math.max(baseHeight, (int)(baseHeight * zoom));
                setLayoutParams(lp);
            }
            invalidate();
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(bitmap, null, new RectF(0, 0, getWidth(), getHeight()), paint);
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getPointerCount() > 1 || detector.isInProgress()) {
                detector.onTouchEvent(event); return true;
            }
            detector.onTouchEvent(event);
            return true;
        }
        @Override public boolean performClick() { super.performClick(); return true; }
    }

    @Override protected void onDestroy() {
        for (ZoomPageView p : pageViews) {
            // Bitmaps are released with their views when the reader closes.
        }
        if (renderer != null) renderer.close();
        if (descriptor != null) { try { descriptor.close(); } catch (IOException ignored) {} }
        super.onDestroy();
    }
}