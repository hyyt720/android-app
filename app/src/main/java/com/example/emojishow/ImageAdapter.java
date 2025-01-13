package com.example.emojishow;

import android.app.Activity;
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

import androidx.recyclerview.widget.RecyclerView;

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
        // 异步加载图片
        // 设置一个tag来标记当前的position，避免错位问题
        convertView.setTag(R.id.imageView, position);

        // 使用final修饰convertView，以便在Runnable中安全访问
        final View finalConvertView = convertView;

        // 异步加载图片
        // 启动新的线程异步加载图片
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = BitmapFactory.decodeFile(filePath);

                // 确保UI线程更新，且检查convertView的tag与当前position一致
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // 通过convertView的tag来确保只更新对应位置的图片
                        int currentPosition = (int) finalConvertView.getTag(R.id.imageView);
                        if (currentPosition == position) {
                            imageView.setImageBitmap(bitmap);  // 只在当前位置一致时更新图片
                        }
                    }
                });
            }
        }).start();

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
