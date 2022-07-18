package com.example.artbookjava;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.example.artbookjava.databinding.ActivityArtBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;


public class ArtActivity extends AppCompatActivity {

    private ActivityArtBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    Bitmap selectedImage;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

        Intent intent = getIntent();
        String info =intent.getStringExtra("info");

        if (info.equals("new")){
        //Yeni Resim
            binding.nameText.setText("");
            binding.artistText.setText("");
            binding.yearText.setText("");
            binding.button.setVisibility(View.VISIBLE);
            binding.imageView.setImageResource(R.drawable.addphoto);
        }else{
            int artId = intent.getIntExtra("artId",0);
            binding.button.setVisibility(View.INVISIBLE);

            try {

                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[] {String.valueOf(artId)});
                int artNameIx = cursor.getColumnIndex("artname");
                int painterNameIx = cursor.getColumnIndex("paintername");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){
                    binding.nameText.setText(cursor.getString(artNameIx));
                    binding.artistText.setText(cursor.getString(painterNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }
                cursor.close();
            }catch (Exception e){
                e.printStackTrace();

            }

        }

    }
    public void save(View view){

        String name = binding.nameText.getText().toString();
        String artistName = binding.artistText.getText().toString();
        String year= binding.yearText.getText().toString();

        Bitmap smallImage= makeSmallerImage(selectedImage,300);

        //SQL İÇERİSİNE KOYABİLMEK İÇİN ALINAN GORSELLERİ VERİYE ÇEVİRİRİZ YANİ 1'LERE 0'LARA BYTE'A ÇEVİRİRİZ
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {

            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB)");

           String sqlString = "INSERT INTO arts (artname, paintername, year, image) VALUES(?, ?, ?, ?)";
           //yukarıdaki stringi alıp databasede çalıştıran kod
           SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
           //artname sql değerini name stringine bağla
           sqLiteStatement.bindString(1,name);
           sqLiteStatement.bindString(2,artistName);
           sqLiteStatement.bindString(3,year);
           sqLiteStatement.bindBlob(4,byteArray);
           sqLiteStatement.execute();


        }catch (Exception e){
            e.printStackTrace();
        }
        //Kayır olduktan sonra mainactivitye geri donmek için
        Intent intent = new Intent(ArtActivity.this,MainActivity.class);
        //BUNDAN ÖNCEKİ BÜTÜN AKTİVİTELERİ KAPAT SADECE GİDECEĞİMİZ YERİ AÇ
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }
    //BOYUTU DAHA KÜÇĞK BİR GÖRSEL YAP
    public Bitmap makeSmallerImage(Bitmap image ,int maximumSize){
        int width= image.getWidth();
        int height= image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio >1){
            //YATAY BİR GÖRSEL
            width = maximumSize;
            height = (int) (width /bitmapRatio);
        }else {
            //DİKEY BİR GÖRSEL
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }

        return image.createScaledBitmap(image,width,height,true);

    }

    public void selectImage(View view){

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Galeriye gitmek için izin gerekiyor",Snackbar.LENGTH_INDEFINITE).setAction("İzin ver", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //izin isteme
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();

            }else{
                //izin isteme
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }


        }else{
            //gallery
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            //Galeriye bağlama kodu
            activityResultLauncher.launch(intentToGallery);
        }

    }

    private void registerLauncher() {
        //KULLANICI GALERİYE GİDİYOR
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            //KULANICI GALERİDEN GERİ GELDİ Mİ
            public void onActivityResult(ActivityResult result) {
                //GERİ GELİRKEN SEÇİM YAPTI MI?
                if(result.getResultCode()== RESULT_OK){
                    Intent intentFromResult = result.getData();
                    if(intentFromResult != null){
                        //SEÇİM YAPTIYSA NERDE KAYITLI OLDUĞUNU ALDIK
                        Uri imageData = intentFromResult.getData();
                       // binding.imageView.setImageURI(imageData);
                        try{
                            if(Build.VERSION.SDK_INT >=28) {
                                //ALDIĞIMIZ KAYDI BİTMAP'E ÇEVİRDİM
                                ImageDecoder.Source source = ImageDecoder.createSource(ArtActivity.this.getContentResolver(), imageData);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                                //KULLANICIYA BUNU GÖSTERDİM
                                binding.imageView.setImageBitmap(selectedImage);
                            }else{
                                selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(),imageData);
                                binding.imageView.setImageBitmap(selectedImage);
                            }
                        } catch (Exception e){
                            e.printStackTrace();

                        }
                    }
                }
            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result){
                    //izin verildi
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);
                }else{
                    //izin verilmedi
                    Toast.makeText(ArtActivity.this,"İzin Lazım",Toast.LENGTH_LONG).show();

                }


            }
        });

    }
    
}