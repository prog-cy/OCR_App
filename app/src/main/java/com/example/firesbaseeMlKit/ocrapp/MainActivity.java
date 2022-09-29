package com.example.firesbaseeMlKit.ocrapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    //Widgets
    private ImageView image;
    private TextView textTV;

    //Variables
    private InputImage inputImage;
    private TextRecognizer recognizer;
    private TextToSpeech textToSpeech;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        image = findViewById(R.id.imageView);
        textTV = findViewById(R.id.textView);


        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });
    }

    //This method will open the internal storage of the mobile phone to add the image
    @SuppressLint("IntentReset")
    private void openGallary() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/");

        @SuppressLint("IntentReset")
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/");

        Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
        startActivityForResult(chooserIntent, PICK_IMAGE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            assert data != null;
            Bundle extrass = data.getExtras();
            Bitmap imgBitmap = (Bitmap) extrass.get("data");

            inputImage = InputImage.fromBitmap(imgBitmap, 0);

            //Setting the image using Glide library
            Glide.with(this)
                    .load(imgBitmap)
                    .into(image);
            
            //Processing the image to get the text;
            Task<Text> result =
                    recognizer.process(inputImage)
                            .addOnSuccessListener(new OnSuccessListener<Text>() {
                                @Override
                                public void onSuccess(Text text) {
                                    processTextBlock(text);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, "Failed to detect, Sorry...", Toast.LENGTH_SHORT).show();
                                }
                            });
        }

        if(requestCode == PICK_IMAGE){

            if(data != null){

                try {
                    inputImage = InputImage.fromFilePath(this, data.getData());
                    Bitmap resultUri = inputImage.getBitmapInternal();

                    //Setting the image into the image view using Glide library
                    Glide.with(this)
                            .load(resultUri)
                            .into(image);

                    //Processing the image to get the text;
                    Task<Text> result =
                            recognizer.process(inputImage)
                                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                                        @Override
                                        public void onSuccess(Text text) {
                                                processTextBlock(text);
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(MainActivity.this, "Failed to detect, Sorry...", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //This method will process the text from the image
    private void processTextBlock(Text text) {
        textTV.setText("");
        String result = text.getText();

        for(Text.TextBlock block : text.getTextBlocks()){

            String blockText = block.getText();
            textTV.append("\n");
            Point[] blockCornerPoints = block.getCornerPoints();
            Rect blockFrames = block.getBoundingBox();

            for(Text.Line line : block.getLines()){

                String lineText = line.getText();
                Point[] lineCornerPoints= line.getCornerPoints();
                Rect lineFrame = line.getBoundingBox();

                for(Text.Element element : line.getElements()){
                    textTV.append(" ");
                    String elementText = element.getText();
                    textTV.append(elementText);
                    Point[] elementCornerPoints = element.getCornerPoints();
                    Rect elementFrame = element.getBoundingBox();

                }
            }
        }
    }

    @Override
    protected void onPause() {

        if(!textToSpeech.isSpeaking()){
            super.onPause();
        }

    }

    //Creating option menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_image_load, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

            case R.id.choose_image:
                openGallary();
                break;
            case R.id.read_text:
                textToSpeech.speak(textTV.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
                break;
            case R.id.capture_image:
                if(checkPermission()){
                    captureImage();
                }else{
                    requestPermission();
                }
                break;
            case R.id.share_text:
                if(!TextUtils.isEmpty(textTV.getText().toString())) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, textTV.getText().toString());
                    startActivity(intent);
                }else{
                    Toast.makeText(this, "Text field is empty, Sorry can't share.", Toast.LENGTH_SHORT).show();
                }
                break;


        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkPermission() {

        int cameraPermission = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA_SERVICE);
        return cameraPermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {

        int PERMISSION_CODE = 200;
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA
        }, PERMISSION_CODE);
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void captureImage() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePicture.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePicture, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0) {
            boolean cameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (cameraPermission) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                captureImage();
            } else {
                Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


}