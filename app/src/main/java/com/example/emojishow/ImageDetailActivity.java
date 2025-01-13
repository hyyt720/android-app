package com.example.emojishow;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class ImageDetailActivity extends AppCompatActivity {

    private ImageView imageView;
    private String imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_detail);

        // 获取传递的图片路径
        imagePath = getIntent().getStringExtra("imagePath");

        // 获取 ImageView
        imageView = findViewById(R.id.imageViewDetail);

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        imageView.setImageBitmap(bitmap);

        // 设置返回按钮（如果需要）
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());  // 返回上一页
    }
}
