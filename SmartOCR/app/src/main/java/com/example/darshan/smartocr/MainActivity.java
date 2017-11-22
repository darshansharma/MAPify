package com.example.darshan.smartocr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static java.io.File.createTempFile;

public class MainActivity extends AppCompatActivity {

    Calendar now = Calendar.getInstance();
    Uri photoURI;
    private static final String LOG_TAG = "Text API";
    ImageView imageView;
    final int CAMERA_REQUEST_CODE = 1;
    TextView scanResults;
    private TextRecognizer detector;
    String address;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button btn = (Button)findViewById(R.id.btn);
        scanResults = (TextView)findViewById(R.id.textView);
        imageView = (ImageView)findViewById(R.id.imagev);
        detector = new TextRecognizer.Builder(getApplicationContext()).build();

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {

                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        Toast.makeText(MainActivity.this, "IOException", Toast.LENGTH_SHORT).show(); // Test 2
                    }
                    if (photoFile != null) {

                        photoURI = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider", photoFile);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
                        btn.setVisibility(View.GONE);
                        btn.setEnabled(false);


                    }else{
                        Toast.makeText(MainActivity.this, "Sorry! App will not work on this phone for now", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }


    private void launchMediaScanIntent() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(photoURI);
        this.sendBroadcast(mediaScanIntent);
    }

    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir =(Environment.getExternalStorageDirectory());
        storageDir = new File(storageDir.getAbsolutePath()+"/temp/");

        if(!storageDir.exists())
        {
            storageDir.mkdirs();
        }
        File image = createTempFile(imageFileName,".jpg",storageDir);


        return image;
    }


    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver()
                .openInputStream(uri), null, bmOptions);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case CAMERA_REQUEST_CODE:
                if(resultCode == Activity.RESULT_OK){
                    launchMediaScanIntent();
                    Toast.makeText(this,"Image Captured Successfully", Toast.LENGTH_LONG).show();


                    try {
                        Bitmap bitmap = decodeBitmapUri(this, photoURI);
                        if (detector.isOperational() && bitmap != null) {
                            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                            SparseArray<TextBlock> textBlocks = detector.detect(frame);
                            String blocks = "";
                            String lines = "";
                            String words = "";
                            for (int index = 0; index < textBlocks.size(); index++) {
                                //extract scanned text blocks here
                                TextBlock tBlock = textBlocks.valueAt(index);
                                blocks = blocks + tBlock.getValue() + "\n" + "\n";
                                for (Text line : tBlock.getComponents()) {
                                    //extract scanned text lines here
                                    lines = lines + line.getValue() + "\n";
                                    for (Text element : line.getComponents()) {
                                        //extract scanned text words here
                                        words = words + element.getValue() + ", ";
                                    }
                                }
                            }
                            if (textBlocks.size() == 0) {
                                scanResults.setText("Scan Failed: Found nothing to scan");
                            } else {

                                scanResults.setText(scanResults.getText() + "***************" + "\n");
                                scanResults.setText(scanResults.getText() + lines + "\n");
                                scanResults.setText(scanResults.getText() + "***************" + "\n");
                                address = lines;

                            }
                        } else {
                            scanResults.setText("Could not set up the detector!");
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT)
                                .show();
                        Log.e(LOG_TAG, e.toString());
                    }


                }
                else if (resultCode == Activity.RESULT_CANCELED){
                    Toast.makeText(this, "Image is not captured", Toast.LENGTH_LONG).show();
                }
        }


    }


}
