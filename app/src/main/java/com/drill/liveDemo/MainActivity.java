package com.drill.liveDemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GridView grid = (GridView) findViewById(R.id.grid);
        grid.setAdapter(new HoloTilesAdapter());
    }


    public class HoloTilesAdapter extends BaseAdapter {

        private static final int TILES_COUNT = 2;

        private final int[] DRAWABLES = {
                R.drawable.blue_tile,
                R.drawable.green_tile
        };

        @Override
        public int getCount() {
            return TILES_COUNT;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            RelativeLayout v;
            if (convertView == null) {
                v = (RelativeLayout) getLayoutInflater().inflate(R.layout.grid_item, parent, false);
            } else {
                v = (RelativeLayout) convertView;
            }
            v.setBackgroundResource(DRAWABLES[position % 3]);

            TextView textView1 = (TextView) v.findViewById(R.id.textView1);

            String string1 = "", string2 = "";
            if(position == 0) {
                string1 = "扫二维码";
            } else if(position == 1) {
                string1 = "演练直播";
            }
            textView1.setText(string1);

            final int currentPosition = position;
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(currentPosition == 0) {
                        goScan();
                    } else if(currentPosition == 1) {
                        golive();
                    }
                }
            });
            return v;
        }
    }

    private void goScan() {

    }

    private void golive() {
        Intent intent = new Intent(this, LandscapeActivity.class);
        startActivity(intent);
    }

}
