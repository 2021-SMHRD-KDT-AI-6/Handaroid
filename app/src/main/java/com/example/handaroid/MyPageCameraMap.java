package com.example.handaroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MyPageCameraMap extends AppCompatActivity{

    private ImageView img;
    private TextView tvPillname;
    private Button btn_capture, btn_gallery, btn_send;
    private ProgressDialog progress;

    private RequestQueue queue;
    private String currentPhotoPath;
    private Bitmap bitmap;
    private Bitmap bitmap1;
    private String pill_name;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int GET_GALLERY_IMAGE = 2;
    String id1;
    private String uri;
    private String bitmapChange;

    static final String TAG = "?????????";
    private String imageString;
    ImageView btn_back;
    private String id;


    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after__mycamera_map);
        btn_back = findViewById(R.id.btn_back5);
        id = getIntent().getStringExtra("id");

        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),SufLoginActivity.class);
                intent.putExtra("id",id);
                startActivity(intent);
                finish();
            }
        });
        gallery_open_intent();
        init();

        btn_capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera_open_intent();
            }
        });

        btn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gallery_open_intent();
            }
        });

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progress = new ProgressDialog(MyPageCameraMap.this);
                progress.setMessage("?????? ?????? ?????????...");
                progress.show();

                sendImage();
                Intent intent = new Intent(getApplicationContext(),f2.class);
                intent.putExtra("id",id);
                startActivity(intent);
            }
        });
    }

    //????????? ??????????????? ??????
    private void sendImage() {

        //????????? ???????????? byte??? ?????? -> base64????????? ??????
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);





        //base64????????? ????????? ????????? ???????????? ???????????? ????????? ??????
        String flask_url = "http://172.30.1.14:5000/sendFrame";
        StringRequest request = new StringRequest(Request.Method.POST, flask_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progress.dismiss();

                        // response ????????? ????????? ???????????? split ?????? ???????????????
                        String send_result = response.split("?????????")[0];
                        pill_name = response.split("?????????")[1];

                        if(send_result.equals("true")){
                            Toast.makeText(MyPageCameraMap.this, "Uploaded Successful", Toast.LENGTH_LONG).show();
                        }
                        else{
                            Toast.makeText(MyPageCameraMap.this, "Some error occurred!", Toast.LENGTH_LONG).show();
                        }

                        tvPillname.setText("??? ????????? : " + pill_name);
                        Intent intent = new Intent(getApplicationContext(),ShowitemActivity.class);
                        intent.putExtra("id",id);
                        intent.putExtra("pill",pill_name);





                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progress.dismiss();
                        Toast.makeText(MyPageCameraMap.this, "Some error occurred -> "+error, Toast.LENGTH_LONG).show();
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("image", imageString);

                return params;
            }
        };
        queue.add(request);



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Uri picturePhotoURI = Uri.fromFile(new File(currentPhotoPath));

            getBitmap(picturePhotoURI);
            img.setImageBitmap(bitmap);

            //???????????? ?????? ??????
            saveFile(currentPhotoPath);

        } else if (requestCode == GET_GALLERY_IMAGE && resultCode == RESULT_OK) {
            Uri galleryURI = data.getData();
            //img.setImageURI(galleryURI);

            getBitmap(galleryURI);
            img.setImageBitmap(bitmap);




        }

    }

    //Uri?????? bisap
    private void getBitmap(Uri picturePhotoURI) {
        try {
            //????????? ???????????? ???????????? ?????? ????????? ????????????
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), picturePhotoURI);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //xml??? ????????? view ?????????
    private void init() {
        img = findViewById(R.id.imgChange);
        btn_capture = findViewById(R.id.btn_capture);
        btn_gallery = findViewById(R.id.btn_gallery);
        btn_send = findViewById(R.id.btn_send);


        queue = Volley.newRequestQueue(MyPageCameraMap.this);

        requestPermission();
    }

    //?????????, ??????, ?????? ?????? ??????/??????
    private void requestPermission() {
        //????????? ?????? ??????????????? ????????????
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { // ???????????? ???????????? ?????? ????????? ???????????? ????????????~

            ActivityCompat.requestPermissions(MyPageCameraMap.this, new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    //????????? ?????????
    private void gallery_open_intent() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GET_GALLERY_IMAGE);
    }

    //????????? ?????? ?????? ??????
    private void saveFile(String currentPhotoPath) {

        Bitmap bitmap = BitmapFactory.decodeFile( currentPhotoPath );

        ContentValues values = new ContentValues( );

        //?????? ????????? ????????? ???????????????
        values.put( MediaStore.Images.Media.DISPLAY_NAME, new SimpleDateFormat( "yyyyMMdd_HHmmss", Locale.US ).format( new Date( ) ) + ".jpg" );
        values.put( MediaStore.Images.Media.MIME_TYPE, "image/*" );

        //????????? ?????? -> /?????? ?????????/DCIM/ ??? 'AndroidQ' ????????? ??????
        values.put( MediaStore.Images.Media.RELATIVE_PATH, "DCIM/AndroidQ" );

        Uri u = MediaStore.Images.Media.getContentUri( MediaStore.VOLUME_EXTERNAL );
        Uri uri = getContentResolver( ).insert( u, values ); //????????? Uri??? MediaStore.Images??? ??????

        try {
            /*
             ParcelFileDescriptor: ?????? ?????? ?????? ??????
             ContentResolver: ???????????????????????? ????????? ???????????? ?????? ?????? ??? ?????? ????????? ??????(?????? ??????????????????)
                            ex) ??????????????? ?????? ???????????? ?????????????????? ?????? ????????? ???????????? ?????? ??????

            getContentResolver(): ContentResolver?????? ??????
            */

            ParcelFileDescriptor parcelFileDescriptor = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                parcelFileDescriptor = getContentResolver( ).openFileDescriptor( uri, "w", null ); //????????? ?????? ??????
            }
            if ( parcelFileDescriptor == null ) return;

            //??????????????????????????? ???????????? JPEG????????? ?????????????????? ?????? ??? ??????
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream( );

            //????????? ?????? ????????? ?????? ??????
            bitmap.compress( Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream );
            byte[] b = byteArrayOutputStream.toByteArray( );
            InputStream inputStream = new ByteArrayInputStream( b );

            ByteArrayOutputStream buffer = new ByteArrayOutputStream( );
            int bufferSize = 1024;
            byte[] buffers = new byte[ bufferSize ];

            int len = 0;
            while ( ( len = inputStream.read( buffers ) ) != -1 ) {
                buffer.write( buffers, 0, len );
            }

            byte[] bs = buffer.toByteArray( );
            FileOutputStream fileOutputStream = new FileOutputStream( parcelFileDescriptor.getFileDescriptor( ) );
            fileOutputStream.write( bs );
            fileOutputStream.close( );
            inputStream.close( );
            parcelFileDescriptor.close( );

            getContentResolver( ).update( uri, values, null, null ); //MediaStore.Images ???????????? ????????? ??? ?????? ??? ????????????

        } catch ( Exception e ) {
            e.printStackTrace( );
        }

        values.clear( );
        values.put( MediaStore.Images.Media.IS_PENDING, 0 ); //???????????? ???????????? ?????? IS_PENDING ?????? 1??? ???????????? ?????? ????????? ?????? ??????
        getContentResolver( ).update( uri, values, null, null );

    }

    //????????? ??????
    private void camera_open_intent() {
        Log.d("Camera", "???????????????!");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            File photoFile = null;

            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.d(TAG, "????????????!!");
            }

            if (photoFile != null) {
                Uri providerURI = FileProvider.getUriForFile(this, "com.example.camera.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }

        }
    }

    //????????? ?????? ??? ????????? ????????? ???????????? ??????????????? ?????? Uri ????????? ???????????? ?????????
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        Log.d(TAG, "????????????>> "+storageDir.toString());

        currentPhotoPath = image.getAbsolutePath();

        return image;
    }
    public Bitmap byteArrayToBitmap( byte[] arr ) {
        Bitmap bitmap1 = BitmapFactory.decodeByteArray( arr, 0, arr.length ) ;
        return bitmap1 ; }

    public static Bitmap StringToBitmap(String uri) {
        try {
            byte[] encodeByte = Base64.decode(uri, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

}