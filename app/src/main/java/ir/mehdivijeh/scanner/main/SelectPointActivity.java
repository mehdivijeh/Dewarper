package ir.mehdivijeh.scanner.main;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import ir.mehdivijeh.scanner.R;

public class SelectPointActivity extends AppCompatActivity {

    private static final String TAG = "SelectPointActivity";
    private String imagePath;
    private static final String IMAGE_PATH = "IMAGE_PATH";
    public static final String POINT_ARRAY_LIST = "POINT_ARRAY_LIST";
    public static final String IMAGE_WIDTH = "IMAGE_WIDTH";
    public static final String IMAGE_HEIGHT = "IMAGE_HEIGHT";

    public static Intent SelectPointActivityIntent(Context context, String imagePath) {
        Intent intent = new Intent(context, SelectPointActivity.class);
        intent.putExtra(IMAGE_PATH, imagePath);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_point);

        imagePath = getIntent().getStringExtra(IMAGE_PATH);

        FloatingActionButton fab_finish = findViewById(R.id.fab_finish);
        PointSelector point_selector = findViewById(R.id.point_selector);
        ImageView image_paper = findViewById(R.id.image_paper);

        image_paper.setImageDrawable(Drawable.createFromPath(imagePath));


        fab_finish.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: " + image_paper.getWidth() + "  " + image_paper.getHeight());

            ArrayList<CircleArea> circles = point_selector.getCircles();
            Intent intent = getIntent();
            intent.putParcelableArrayListExtra(POINT_ARRAY_LIST, circles);
            intent.putExtra(IMAGE_WIDTH, image_paper.getWidth());
            intent.putExtra(IMAGE_HEIGHT, image_paper.getHeight());
            setResult(RESULT_OK, intent);
            finish();
        });

        /*final DragRectView view = findViewById(R.id.dragRect);

        if (null != view) {
            view.setOnUpCallback(new DragRectView.OnUpCallback() {
                @Override
                public void onRectFinished(final Rect rect) {
                    Toast.makeText(getApplicationContext(), "Rect is (" + rect.left + ", " + rect.top + ", " + rect.right + ", " + rect.bottom + ")",
                            Toast.LENGTH_LONG).show();
                }
            });
        }*/
    }
}
