package com.ciappinavictor.geopics;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Victor on 28/06/2015.
 */
public class MainActivity extends Activity {
    public final String LAST_IMAGE = "lastImage";
    public UIHandler uihandler;
    public ImageAdapter imgAdapter;
    private ArrayList<ImageContener> imageList;
    private GridView gridview;
    float dh, dw;
    String longitude = "null", latitude="null", cityName, stateName, countryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Initialize();
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location!=null) {
            longitude = String.valueOf(location.getLongitude());
            latitude = String.valueOf(location.getLatitude());
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            cityName = addresses.get(0).getAddressLine(0);
            stateName = addresses.get(0).getAddressLine(1);
            countryName = addresses.get(0).getAddressLine(2);
        }
        LocationListener LL =  new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                String prevlatitude = latitude, prevlongitude = longitude;
                longitude = String.valueOf(location.getLongitude());
                latitude = String.valueOf(location.getLatitude());
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                cityName = addresses.get(0).getAddressLine(0);
                stateName = addresses.get(0).getAddressLine(1);
                countryName = addresses.get(0).getAddressLine(2);
                if(prevlongitude.equals("null")||prevlatitude.equals("null")){
                    setupPics(cityName, stateName, countryName);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, LL);
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, LL);
        uihandler = new UIHandler();
        gridview = (GridView) findViewById(R.id.gridview);
        gridview.setOnItemClickListener(onThumbClickListener);
        if(!longitude.equals("null")&&!latitude.equals("null")) {
            setupPics(cityName, stateName, countryName);
        }
    }

    private void setupPics(String cityName,String stateName,String countryName){
        ProgressBar spinner = (ProgressBar) findViewById(R.id.spinner);
        TextView text = (TextView) findViewById(R.id.text1);
        TextView text2 = (TextView) findViewById(R.id.text2);
        TextView initext = (TextView) findViewById(R.id.initext);
        GridView gridview = (GridView) findViewById(R.id.gridview);
        spinner.setVisibility(View.GONE);
        text.setVisibility(View.GONE);
        text2.setVisibility(View.GONE);
        initext.setText("Showing the first 50 pictures near "+cityName+", "+stateName+", "+countryName+":");
        initext.setVisibility(View.VISIBLE);
        gridview.setVisibility(View.VISIBLE);
        new Thread(getMetadata).start();
    }

    private void Initialize() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        Display currentDisplay = getWindowManager().getDefaultDisplay();
        dw = currentDisplay.getWidth(); //Ancho
        dh = currentDisplay.getHeight(); //Alto
        if (!ViewConfiguration.get(getBaseContext()).hasPermanentMenuKey()){
            dh = (float) ((double)dh*0.92);
        }
    }

    public class ImageAdapter extends BaseAdapter {
        private Context mContext;
        private int defaultItemBackground;
        private ArrayList<ImageContener> imageContener;

        public ArrayList<ImageContener> getImageContener() {
            return imageContener;
        }

        public void setImageContener(ArrayList<ImageContener> imageContener) {
            this.imageContener = imageContener;
        }

        public ImageAdapter(Context c, ArrayList<ImageContener> imageContener) {
            mContext = c;
            this.imageContener = imageContener;
            TypedArray styleAttrs = c.obtainStyledAttributes(R.styleable.PicGallery);
            styleAttrs.getResourceId(R.styleable.PicGallery_android_galleryItemBackground, 0);
            defaultItemBackground = styleAttrs.getResourceId(R.styleable.PicGallery_android_galleryItemBackground, 0);
            styleAttrs.recycle();
        }

        public int getCount() {
            return imageContener.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView i = new ImageView(mContext);
            if (imageContener.get(position).thumb != null) {
                i.setImageBitmap(imageContener.get(position).thumb);

                i.setLayoutParams(new GridView.LayoutParams((int) dw/2, (int) dw/2));
                i.setBackgroundResource(defaultItemBackground);
            } else
                i.setImageDrawable(getResources().getDrawable(android.R.color.black));
            return i;
        }

    }


    class UIHandler extends Handler {
        public static final int ID_METADATA_DOWNLOADED = 0;
        public static final int ID_SHOW_IMAGE = 1;
        public static final int ID_UPDATE_ADAPTER = 2;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ID_METADATA_DOWNLOADED:
                    // Set of information required to download thumbnails is
                    // available now
                    if (msg.obj != null) {
                        imageList = (ArrayList<ImageContener>) msg.obj;
                        imgAdapter = new ImageAdapter(getApplicationContext(), imageList);
                        gridview.setAdapter(imgAdapter);
                        for (int i = 0; i < imgAdapter.getCount(); i++) {
                            new FlickrManager.GetThumbnailsThread(uihandler, imgAdapter.getImageContener().get(i)).start();
                        }
                    }
                    break;
                case ID_SHOW_IMAGE:
                    // Display large image
                    if (msg.obj != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setCancelable(false);
                        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                        View dialogView = inflater.inflate(R.layout.custom_fullimage_dialog, null);
                        builder.setView(dialogView);
                        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        ImageView imagepop = (ImageView) dialogView.findViewById(R.id.fullimage);
                        imagepop.setImageBitmap(((ImageContener) msg.obj).getPhoto());

                        TextView imagetitle = (TextView) dialogView.findViewById(R.id.custom_fullimage_placename);
                        imagetitle.setText(((ImageContener) msg.obj).getTitle());

                        // Create the AlertDialog
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                    break;
                case ID_UPDATE_ADAPTER:
                    // Update adapter with thumnails
                    imgAdapter.notifyDataSetChanged();
                    break;
            }
            super.handleMessage(msg);
        }
    }

    AdapterView.OnItemClickListener onThumbClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
            // Get large image of selected thumnail
            new GetLargePhotoThread(imageList.get(position), uihandler).start();
        }
    };

    public class GetLargePhotoThread extends Thread {
        ImageContener ic;
        UIHandler uih;

        public GetLargePhotoThread(ImageContener ic, UIHandler uih) {
            this.ic = ic;
            this.uih = uih;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            if (ic.getPhoto() == null) {
                ic.setPhoto(FlickrManager.getImage(ic));
            }
            /*Bitmap bmp = ic.getPhoto();*/
            if (ic.getPhoto() != null) {
                Message msg = Message.obtain(uih, UIHandler.ID_SHOW_IMAGE);
                msg.obj = ic;
                uih.sendMessage(msg);
            }
        }
    }

    Runnable getMetadata = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            FlickrManager.searchImagesByLocation(uihandler, getApplicationContext(), latitude, longitude, 5);
        }
    };
    private void loadPhoto(Bitmap bitmap, int width, int height) {

        BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);

        AlertDialog.Builder imageDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);

        View layout = inflater.inflate(R.layout.custom_fullimage_dialog,
                (ViewGroup) findViewById(R.id.layout_root));
        ImageView image = (ImageView) layout.findViewById(R.id.fullimage);
        image.setImageDrawable(bitmapDrawable);
        imageDialog.setView(layout);

        imageDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener(){

            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });


        imageDialog.create();
        imageDialog.show().getWindow().setLayout(width, height);
    }
}
