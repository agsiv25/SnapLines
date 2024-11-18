package com.example.snaplines.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.snaplines.R;
import com.example.snaplines.domain.BettingLinesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CameraFragment extends Fragment {

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ImageView capturedImageView;
    private Button captureButton;
    private Button retakeButton;
    private Button sendButton;
    private File capturedPhotoFile;

    private ObjectMapper objectMapper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        previewView = view.findViewById(R.id.previewView);
        capturedImageView = view.findViewById(R.id.captured_image_view);
        captureButton = view.findViewById(R.id.capture_button);
        retakeButton = view.findViewById(R.id.retake_button);
        sendButton = view.findViewById(R.id.send_button);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        retakeButton.setOnClickListener(v -> {
            retakePhoto();
        });

        sendButton.setOnClickListener(v -> {
            sendPhoto();
        });

        startCamera();
        return view;
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider, previewView);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }


    private void bindPreview(ProcessCameraProvider cameraProvider, PreviewView previewView) {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder().build();

        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

        Button captureButton = requireView().findViewById(R.id.capture_button);
        captureButton.setOnClickListener(view -> takePhoto());
    }

    private void takePhoto() {
        capturedPhotoFile  = new File(getOutputDirectory(), new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(new Date()) + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(capturedPhotoFile ).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                displayCapturedPhoto(capturedPhotoFile.getAbsolutePath());
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(getContext(), "Error taking photo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayCapturedPhoto(String photoPath) {
        // Load the captured image into the ImageView
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath);
        bitmap = rotateImageIfRequired(bitmap, photoPath);
        capturedImageView.setImageBitmap(bitmap);

        // Show the ImageView and buttons, hide the camera preview and capture button
        capturedImageView.setVisibility(View.VISIBLE);
        retakeButton.setVisibility(View.VISIBLE);
        sendButton.setVisibility(View.VISIBLE);
        previewView.setVisibility(View.GONE);
        captureButton.setVisibility(View.GONE);
    }

    private Bitmap rotateImageIfRequired(Bitmap img, String photoPath) {
        ExifInterface ei;
        try {
            ei = new ExifInterface(photoPath);
        } catch (IOException e) {
            e.printStackTrace();
            return img;
        }

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    private void retakePhoto() {
        // Hide the ImageView and buttons, show the camera preview and capture button
        capturedImageView.setVisibility(View.GONE);
        retakeButton.setVisibility(View.GONE);
        sendButton.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);
        captureButton.setVisibility(View.VISIBLE);
    }

    // TODO
    private void sendPhoto() {
        // Implement the logic to send the photo (e.g., share via email, upload, etc.)
//        Toast.makeText(requireContext(), "Sending photo: " + capturedPhotoFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();

        // Simulate the server response
        String team1 = "Packers";
        String team2 = "Bears";

        // Simulate sports betting API response
        String bettingLines = "";
        try (InputStream is = requireContext().getAssets().open("nfl_lines.json")) {
            BettingLinesResponse bettingLinesResponse = objectMapper.readValue(is, BettingLinesResponse.class);
            bettingLines = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bettingLinesResponse);
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error loading mock response", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        // Navigate to the home fragment and pass the data
        Bundle bundle = new Bundle();
        bundle.putString("team1", team1);
        bundle.putString("team2", team2);
        bundle.putString("bettingLines", bettingLines);

        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.navigation_home, bundle);
    }

    private File getOutputDirectory() {
        File mediaDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (mediaDir != null && mediaDir.exists()) {
            return mediaDir;
        }
        return requireContext().getFilesDir();
    }
}
