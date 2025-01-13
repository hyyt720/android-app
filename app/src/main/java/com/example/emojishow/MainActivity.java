package com.example.emojishow;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSION = 100;
    private static final int REQUEST_CODE_PICK_IMAGE = 101;
    private static final int REQUEST_MANAGE_EXTERNAL_STORAGE = 102;  // 用于申请 MANAGE_EXTERNAL_STORAGE 权限
    private DatabaseHelper databaseHelper;
    private ListView listView;
    private ImageAdapter adapter;
    private List<String> imagePaths = new ArrayList<>();
    private int currentOffset = 0;
    private int limit = 10;
    private ImageView imageView;
    private static final int REQUEST_PERMISSION_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化数据库助手和ListView
        databaseHelper = new DatabaseHelper(this);
        listView = findViewById(R.id.listView);
        adapter = new ImageAdapter(this, imagePaths);
        listView.setAdapter(adapter);

        Button btnAddImage = findViewById(R.id.btnAddImage);
        btnAddImage.setOnClickListener(view -> {
            if (hasPermissions()) {
                openImagePicker();
            } else {
                requestPermissions();
            }
        });

        if (hasPermissions()) {
            loadMoreImages();
            setupListeners();
        }
    }

    // 检查权限
    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) 需要 READ_MEDIA_IMAGES 权限
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            // 低版本仍然使用 READ_EXTERNAL_STORAGE 权限
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // 请求权限
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 对于 Android 13 及更高版本，申请 READ_MEDIA_IMAGES 权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE_PERMISSION);
        } else {
            // 对于低版本设备，申请 READ_EXTERNAL_STORAGE 权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
        }
    }

    // 权限请求回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，打开图库
                openImagePicker();
            } else {
                Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 打开图库选择图片
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");  // 限定选择图片
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);  // 允许多选

        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to open image picker", Toast.LENGTH_SHORT).show();
        }
    }

    // 处理图库选择结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                // 检查是否选择了多个图片
                if (data.getClipData() != null) {
                    // 多张图片被选中
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        if (imageUri != null) {
                            saveImageToInternalStorage(imageUri);
                        }
                    }
                } else {
                    // 只有一张图片被选中
                    Uri imageUri = data.getData();
                    if (imageUri != null) {
                        saveImageToInternalStorage(imageUri);
                    }
                }
            }
        }
    }

    // 设置监听器，包括点击按钮选择图片
    private void setupListeners() {
        Button btnAddImage = findViewById(R.id.btnAddImage);
        btnAddImage.setOnClickListener(view -> openImagePicker());

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    int lastVisiblePosition = listView.getLastVisiblePosition();
                    if (lastVisiblePosition == adapter.getCount() - 1) {
                        loadMoreImages();
                    }
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
        });
    }




    // 从URI获取真实路径
    private String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex(projection[0]);
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            return filePath;
        }
        return null;
    }

    // 将图片保存到应用内部存储
    private void saveImageToInternalStorage(Uri imageUri) {
        try {
            // 获取 InputStream
            InputStream inputStream = getContentResolver().openInputStream(imageUri);

            // 定义内部存储路径
            File destinationDir = new File(getFilesDir(), "images");
            if (!destinationDir.exists()) {
                destinationDir.mkdirs();
            }

            // 使用原始文件名保存文件
            File destinationFile = new File(destinationDir, "image_" + System.currentTimeMillis() + ".jpg");
            OutputStream outputStream = new FileOutputStream(destinationFile);

            // 将图片流写入到目标文件
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            // 将图片路径保存到数据库
            saveImagePathToDatabase(destinationFile.getAbsolutePath());
            Toast.makeText(this, "Image saved successfully.", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image.", Toast.LENGTH_SHORT).show();
        }
    }

    // 保存图片路径到数据库
    private void saveImagePathToDatabase(String path) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("filePath", path);
        db.insert("images", null, values);
        db.close();
    }

    // 加载图片路径
    private void loadMoreImages() {
        List<String> newImagePaths = databaseHelper.getImageFilePaths(limit, currentOffset);
        if (newImagePaths.isEmpty()) {
            return;
        }

        imagePaths.addAll(newImagePaths);
        currentOffset += newImagePaths.size();
        adapter.notifyDataSetChanged();
    }

    // 删除图片
    public void deleteImage(int position) {
        // 获取要删除的图片路径
        String imagePath = imagePaths.get(position);

        // 从数据库中删除图片路径
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete("images", "filePath = ?", new String[]{imagePath});
        db.close();

        // 删除图片文件
        File imageFile = new File(imagePath);
        if (imageFile.exists()) {
            imageFile.delete();
        }

        // 从列表中移除图片路径
        imagePaths.remove(position);
        adapter.notifyDataSetChanged();
    }


}