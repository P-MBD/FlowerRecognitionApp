package com.example.flowerrecognitionapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final String TAG = "FlowerRecognitionApp"; // برای لاگ‌گذاری
    private TextView textViewResult;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonSelectImage = findViewById(R.id.buttonSelectImage);
        textViewResult = findViewById(R.id.textViewResult);

        buttonSelectImage.setOnClickListener(v -> openGallery());
    }

    private void openGallery() {
        Log.d(TAG, "Opening gallery to select image");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            Log.d(TAG, "Image selected: " + imageUri.toString());
            if (imageUri != null) {
                // ارسال تصویر به Imagga
                sendImageToImagga(imageUri);
            }
        } else {
            Log.e(TAG, "Image selection failed");
        }
    }

    private void sendImageToImagga(Uri imageUri) {
        String apiKey = "acc_834184012740a39"; // کلید API خود را اینجا قرار دهید
        String apiSecret = "6739bc6194f1f625e1deb30f45a2b49a"; // کلید مخفی API خود را اینجا قرار دهید

        Log.d(TAG, "Sending image to Imagga: " + imageUri.toString());
        OkHttpClient client = new OkHttpClient();
        String credentials = Base64.encodeToString((apiKey + ":" + apiSecret).getBytes(), Base64.NO_WRAP);

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Log.e(TAG, "InputStream is null for URI: " + imageUri.toString());
                return;
            }

            // استفاده از ByteArrayOutputStream برای خواندن داده‌های تصویر
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, length);
            }

            byte[] imageBytes = byteBuffer.toByteArray();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "image.jpg",
                            RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.imagga.com/v2/tags")
                    .addHeader("Authorization", "Basic " + credentials)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Error sending image to Imagga", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String jsonResponse = response.body().string();
                        Log.d(TAG, "Response received: " + jsonResponse);
                        runOnUiThread(() -> parseAndDisplayResult(jsonResponse));
                    } else {
                        Log.e(TAG, "Request not successful: " + response.code());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error while reading image input stream", e);
        }
    }

    private void parseAndDisplayResult(String jsonResponse) {
        Gson gson = new Gson();
        ImaggaResponse imaggaResponse = gson.fromJson(jsonResponse, ImaggaResponse.class);

        if (imaggaResponse != null && imaggaResponse.getResult() != null) {
            StringBuilder result = new StringBuilder();
            result.append("Identified Plants:\n");

            boolean plantFound = false;

            for (int i = 0; i < imaggaResponse.getResult().getTags().size(); i++) {
                ImaggaResponse.Tag tag = imaggaResponse.getResult().getTags().get(i);
                // اگر درصد اطمینان بیشتر از 50 باشد، نام آن برچسب را نمایش دهید
                if (tag.getConfidence() > 50) {
                    plantFound = true; // حداقل یک برچسب با اطمینان کافی پیدا شده است

                    // اضافه کردن اطلاعات گیاه به خروجی
                    result.append((i + 1) + ". **" + tag.getTag().getEn() + "** (Confidence: " + tag.getConfidence() + "%)\n");
                    result.append("   - Description: " + getPlantDescription(tag.getTag().getEn()) + "\n");
                    result.append("   - Uses: " + getPlantUses(tag.getTag().getEn()) + "\n\n");
                }
            }

            if (plantFound) {
                textViewResult.setText(result.toString());
            } else {
                textViewResult.setText("هیچ برچسبی با اطمینان کافی شناسایی نشد.");
            }
        } else {
            Log.e(TAG, "No tags found in the response");
            textViewResult.setText("خطا در پردازش پاسخ.");
        }
    }

    // متدهای جدید برای توصیف و استفاده از گیاهان
    private String getPlantDescription(String plantName) {
        switch (plantName.toLowerCase()) {
            case "arum":
                return "A genus of flowering plants in the family Araceae. Known for its unique spathes and arrow-shaped flower structure.";
            case "herb":
                return "Generally refers to a plant that is valued for flavoring, medicine, or fragrance.";
            // می‌توانید موارد بیشتری اضافه کنید
            default:
                return "Description not available.";
        }
    }

    private String getPlantUses(String plantName) {
        switch (plantName.toLowerCase()) {
            case "arum":
                return "Often used in ornamental gardening, but some species can be toxic.";
            case "herb":
                return "Commonly used in cooking and traditional medicine.";
            // می‌توانید موارد بیشتری اضافه کنید
            default:
                return "Uses not available.";
        }
    }
}
