package com.example.snaplines.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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

import com.example.snaplines.ApiService;
import com.example.snaplines.R;
import com.example.snaplines.domain.BettingLinesResponse;
import com.example.snaplines.domain.UploadResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class CameraFragment extends Fragment {

    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ImageView capturedImageView;
    private ImageButton captureButton;
    private Button retakeButton;
    private Button sendButton;
    private File capturedPhotoFile;

    private ObjectMapper objectMapper;
    private Retrofit retrofit;
    private ApiService apiService;

    private final String URL = "https://6q09pnnl-8000.use.devtunnels.ms/";

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

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                // Increase timeout settings for MLLM to processs the image
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(URL)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build();

        apiService = retrofit.create(ApiService.class);

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

        ImageButton captureButton = requireView().findViewById(R.id.capture_button);
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

        // Image sometimes doesn't save orientated correctly, so fix it if needed
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

    private void sendPhoto() {

        // Decode and compress the image
        Bitmap bitmap = BitmapFactory.decodeFile(capturedPhotoFile.getAbsolutePath());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Compress the image to 75% size so its not too big for the MLLM
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
        byte[] compressedImageBytes = outputStream.toByteArray();

        // Create RequestBody for the compressed image data
        RequestBody requestBody = RequestBody.create(compressedImageBytes, MediaType.parse("image/jpeg"));
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", capturedPhotoFile.getName(), requestBody);

        apiService.uploadImage(part).enqueue(new Callback<UploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<UploadResponse> call, @NonNull Response<UploadResponse> response) {
                UploadResponse uploadResponse = response.body();

                if (response.isSuccessful() && uploadResponse != null) {
                    String[] teams = uploadResponse.getResponse().replace("(", "").replace(")", "").split(",");

                    if (teams.length != 2) {
                        throw new IllegalArgumentException("Invalid response format");
                    }

                    // Navigate to the home fragment and pass the 2 teams
                    Bundle bundle = new Bundle();
                    bundle.putString("team1", teams[0].trim());
                    bundle.putString("team2", teams[1].trim());

                    NavController navController = NavHostFragment.findNavController(requireParentFragment());
                    navController.navigate(R.id.navigation_home, bundle);
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(requireContext(), "Failed to use photo. Please try again.", Toast.LENGTH_LONG).show()
                    );
                }
            }

            @Override
            public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(requireContext(), "Failed to use photo. Please try again.", Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private File getOutputDirectory() {
        File mediaDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (mediaDir != null && mediaDir.exists()) {
            return mediaDir;
        }
        return requireContext().getFilesDir();
    }
}
