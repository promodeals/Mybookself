package com.example.books;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;

public class PdfViewerActivity extends Activity {
    public static final String EXTRA_PATH = "pdf_path";
    public static final String EXTRA_TITLE = "pdf_title";
    private ParcelFileDescriptor descriptor;
    private PdfRenderer renderer;

    private int dp(float n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String path = getIntent().getStringExtra(EXTRA_PATH);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        setTitle(title == null ? "PDF Reader" : title);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xffe9e9e9);
        TextView heading = new TextView(this);
        heading.setText(title == null ? "PDF Reader" : title);
        heading.setTextColor(Color.BLACK);
        heading.setTextSize(18);
        heading.setPadding(dp(16), dp(12), dp(16), dp(12));
        heading.setBackgroundColor(Color.WHITE);
        root.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        ScrollView scroll = new ScrollView(this);
        LinearLayout pages = new LinearLayout(this);
        pages.setOrientation(LinearLayout.VERTICAL);
        pages.setPadding(dp(8), dp(8), dp(8), dp(8));
        scroll.addView(pages);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        try {
            if (path == null || !new File(path).isFile()) throw new IOException("The saved PDF file could not be found.");
            descriptor = ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(descriptor);
            int availableWidth = getResources().getDisplayMetrics().widthPixels - dp(32);
            for (int index = 0; index < renderer.getPageCount(); index++) {
                PdfRenderer.Page page = renderer.openPage(index);
                int width = Math.max(1, availableWidth);
                int height = Math.max(1, (int) (width * (page.getHeight() / (float) page.getWidth())));
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.WHITE);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                ImageView image = new ImageView(this);
                image.setImageBitmap(bitmap);
                image.setAdjustViewBounds(true);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setBackgroundColor(Color.WHITE);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                params.bottomMargin = dp(10);
                pages.addView(image, params);
            }
            if (renderer.getPageCount() == 0) showError(pages, "This PDF has no pages.");
        } catch (Exception e) {
            showError(pages, "Could not open this PDF. The file may be damaged or unsupported.\n" + e.getMessage());
            Toast.makeText(this, "PDF could not be opened", Toast.LENGTH_LONG).show();
        }
    }

    private void showError(LinearLayout parent, String message) {
        TextView error = new TextView(this);
        error.setText(message);
        error.setTextColor(Color.DKGRAY);
        error.setTextSize(16);
        error.setGravity(Gravity.CENTER);
        error.setPadding(dp(24), dp(32), dp(24), dp(32));
        parent.addView(error, new LinearLayout.LayoutParams(-1, -2));
    }

    @Override protected void onDestroy() {
        if (renderer != null) renderer.close();
        if (descriptor != null) {
            try { descriptor.close(); } catch (IOException ignored) {}
        }
        super.onDestroy();
    }
}
