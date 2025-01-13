package com.example.emojishow;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;

import java.util.List;

public class ImageAdapter extends BaseAdapter {
    private Context context;
    private List<String> imagePaths;
    private LayoutInflater inflater;

    public ImageAdapter(Context context, List<String> imagePaths) {
        this.context = context;
        this.imagePaths = imagePaths;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return imagePaths.size();
    }

    @Override
    public Object getItem(int position) {
        return imagePaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_image, parent, false);
            imageView = convertView.findViewById(R.id.imageView);
            convertView.setTag(imageView);
        } else {
            imageView = (ImageView) convertView.getTag();
        }

        String filePath = imagePaths.get(position);
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        imageView.setImageBitmap(bitmap);

        Button btnDelete = convertView.findViewById(R.id.btnDelete);
        Button btnDetail = convertView.findViewById(R.id.btnDetail);
        btnDelete.setOnClickListener(v -> {
            // 调用 MainActivity 中的方法来删除图片
            ((MainActivity) context).deleteImage(position);
        });

        // 设置查看详情按钮点击事件
        btnDetail.setOnClickListener(v -> {
            // 启动 ImageDetailActivity 展示完整图片
            Intent intent = new Intent(context, ImageDetailActivity.class);
            intent.putExtra("imagePath", filePath);  // 传递图片路径
            context.startActivity(intent);
        });

        return convertView;
    }
}
