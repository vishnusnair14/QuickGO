package com.vishnu.quickgoorder.ui.search;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.databinding.FragmentImageSearchBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageSearchFragment extends Fragment {

    private ImageView imageView;
    View scanningLine;
    ImageView imageView1;
    Animation scanningAnimation;
    File AppImageDir, imageFile;
    private final String LOG_TAG = "ImageSearchFragment: ";
    private ActivityResultLauncher<Intent> cameraLauncher;

    public ImageSearchFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        com.vishnu.quickgoorder.databinding.FragmentImageSearchBinding binding = FragmentImageSearchBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Button captureButton = root.findViewById(R.id.orderByImage_button);
        Button scanImageButton = root.findViewById(R.id.scanImage_button);
        imageView = root.findViewById(R.id.orderByImage_imageView);

        scanningAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_animation);
        scanningLine = root.findViewById(R.id.scanningLine);
        imageView1 = root.findViewById(R.id.scanLinescope_imageView);

        File appExternalDirectory = Environment.getExternalStoragePublicDirectory("Android/vishnu/" + requireContext().getPackageName() + "/files");
        AppImageDir = new File(appExternalDirectory, "image_dir");
        imageFile = new File(AppImageDir, "my_image.png");

        scanningLine.setVisibility(View.GONE);

        // camera launcher init.
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleCameraResult
        );

        captureButton.setOnClickListener(view -> startCameraActivity());
        scanImageButton.setOnClickListener(view -> {
            imageView1.startAnimation(scanningAnimation);
            scanningLine.startAnimation(scanningAnimation);
        });

        scanningAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                scanningLine.setVisibility(View.VISIBLE);
                scanImageButton.setText(R.string.scanning);
                scanImageButton.setEnabled(false);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                scanImageButton.setText(R.string.scan_image);
                scanningLine.setVisibility(View.GONE);
                scanImageButton.setEnabled(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        loadImageViewOnCreate(imageFile);

        return root;
    }

    private void startCameraActivity() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    private void handleCameraResult(ActivityResult result) {
        if (result.getResultCode() == FragmentActivity.RESULT_OK) {
            Intent data = result.getData();
            if (data != null && data.getExtras() != null) {
                Bitmap capturedBitmap = (Bitmap) data.getExtras().get("data");

                if (capturedBitmap != null) {
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(capturedBitmap);
                    saveBitmap(capturedBitmap);
//                    // Optionally, you can round the corners of the image
//                    RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), capturedBitmap);
//                    roundedBitmapDrawable.setCircular(true);
//                    imageView.setImageDrawable(roundedBitmapDrawable);
                }
            } else {
                // The image capture was canceled or failed
                Toast.makeText(requireContext(), "Unable to capture image!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveBitmap(Bitmap bitmap) {
        if (!AppImageDir.exists()) {
            if (AppImageDir.mkdirs()) {
                Log.d(LOG_TAG, "AppImageDir: " + AppImageDir + " successfully created");
            } else {
                Log.d(LOG_TAG, "AppImageDir: " + AppImageDir + " failed to created!");
            }
        }

        try {
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();
            Toast.makeText(requireContext(), "Image saved to internal storage", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
            Log.d(LOG_TAG, String.valueOf(e));
        }
    }

    private void loadImageViewOnCreate(File imgFile) {
        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            imageView.setImageBitmap(myBitmap);
        }
    }
}
