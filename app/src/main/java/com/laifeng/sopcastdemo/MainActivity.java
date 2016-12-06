package com.laifeng.sopcastdemo;

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

        private static final int TILES_COUNT = 4;

        private final int[] DRAWABLES = {
                R.drawable.blue_tile,
                R.drawable.green_tile,
                R.drawable.purple_tile,
                R.drawable.yellow_tile
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
            v.setBackgroundResource(DRAWABLES[position % 5]);

            TextView textView1 = (TextView) v.findViewById(R.id.textView1);
            TextView textView2 = (TextView) v.findViewById(R.id.textView2);

            String string1 = "", string2 = "";
            if(position == 0) {
                string1 = "Portrait";
                string2 = "Flv + Local";
            } else if(position == 1) {
                string1 = "Landscape";
                string2 = "Rtmp";
            } else if(position == 2) {
                string1 = "Portrait";
                string2 = "Part";
            } else if(position == 3) {
                string1 = "Portrait";
                string2 = "Screen + Rtmp";
            }
            textView1.setText(string1);
            textView2.setText(string2);

            final int currentPosition = position;
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(currentPosition == 0) {
                        goPortraitAndLocal();
                    } else if(currentPosition == 1) {
                        goLandscapeAndRtmp();
                    } else if(currentPosition == 2) {
                        goPart();
                    } else if(currentPosition == 3) {
                        goScreen();
                    }
                }
            });
            return v;
        }
    }

    private void goPortraitAndLocal() {
        Intent intent = new Intent(this, PortraitActivity.class);
        startActivity(intent);
    }

    private void goLandscapeAndRtmp() {
        Intent intent = new Intent(this, LandscapeActivity.class);
        startActivity(intent);
    }

    private void goPart() {
        Intent intent = new Intent(this, PartActivity.class);
        startActivity(intent);
    }

    private void goScreen() {
        Intent intent = new Intent(this, ScreenActivity.class);
        startActivity(intent);
    }
}
