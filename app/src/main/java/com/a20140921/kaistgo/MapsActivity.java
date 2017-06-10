package com.a20140921.kaistgo;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaMetadataRetriever;
import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.IntegerRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Scanner;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener{

    private GoogleMap mMap;
    protected LocationManager locationManager;
    protected Location myLocation;
    MarkerOptions markerOptions;
    Marker marker;
    MarkerOptions mo_pokemon;
    Marker mk_pokemon;

    CardView cv_pokemon;
    ImageView iv_pokemon;
    VideoView vv_pokemon;
    TextView tv_pokemon, tv_distance, tv_catch, tv_pokemon_info;



    Double walkedDistance = 0.00;

    ProgressBar progressBar;


    // Caching
    private LruCache<String, Bitmap> mMemoryCacheImage;
    private LruCache<String,File> mMemoryCacheVideo;




    public boolean catching = false;

    public boolean pokemon_around = false;
    public String pokemon_url = "";
    public String pokemon_url_redirected = "";
    public int pokemon_type = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);


        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
        myLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        mapFragment.getMapAsync(this);

        cv_pokemon = (CardView)findViewById(R.id.cv_pokemon);
        iv_pokemon = (ImageView)findViewById(R.id.iv_pokemon);
        vv_pokemon = (VideoView)findViewById(R.id.vv_pokemon);
        tv_pokemon = (TextView)findViewById(R.id.tv_pokemon);

        progressBar = (ProgressBar)findViewById(R.id.pb_pokemon);

        tv_pokemon_info = (TextView)findViewById(R.id.tv_pokemon_info);
        tv_distance = (TextView)findViewById(R.id.tv_distance);
        tv_catch = (TextView)findViewById(R.id.tv_catch);
        tv_catch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tv_catch.getText().equals("Done")){
                    cv_pokemon.setVisibility(View.INVISIBLE);
                    tv_pokemon.setVisibility(View.INVISIBLE);
                    iv_pokemon.setVisibility(View.INVISIBLE);
                    vv_pokemon.setVisibility(View.INVISIBLE);
                    tv_pokemon_info.setText("~No more Pokemon~");
                    tv_catch.setText("Catch It");


                    pokemon_around = false;
                    pokemon_type = 0;
                    pokemon_url = "";
                    pokemon_url_redirected = "";
                    catching = false;
                    progressBar.setProgress(0);
                }
                else {
                    catching = true;
                    tv_pokemon_info.setVisibility(View.INVISIBLE);

                    //Download Pokemon from server

                    if (tv_pokemon_info.getText().equals("~Pokemon Text~")){
                        new PokemonDownloader().execute(pokemon_url);
                        tv_pokemon.setVisibility(View.VISIBLE);
                    }
                    else if (tv_pokemon_info.getText().equals("~Pokemon Image~")){
                        loadBitmap();
                        iv_pokemon.setVisibility(View.VISIBLE);
                    }
                    else if (tv_pokemon_info.getText().equals("~Pokemon Video~")){
                        loadVideo();
                        vv_pokemon.setVisibility(View.VISIBLE);
                        vv_pokemon.start();
                    }


                    tv_catch.setText("Done");

                }

            }
        });


        /**
         * Creating a cache for http requests
         * **/
        File httpCacheDir = new File(MapsActivity.this.getCacheDir(),"http");
        long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
        try {
            HttpResponseCache.install(httpCacheDir,httpCacheSize);
        } catch (IOException e) {
            e.printStackTrace();
        }


        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mMemoryCacheImage = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };

        mMemoryCacheVideo = new LruCache<String, File>(cacheSize) {
            @Override
            protected int sizeOf(String key, File video) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return 10; // video.getByteCount()
            }
        };



    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Your Location and move the camera
        LatLng mylatlng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
        markerOptions = new MarkerOptions().position(mylatlng).title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.kaist_mascot3));
        marker = mMap.addMarker(markerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mylatlng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(19));

        //Just add pokemon marker on the map, but hide it and later we will change its position and visibility
        mo_pokemon = new MarkerOptions().position(new LatLng(myLocation.getLatitude()+(180/Math.PI)*(100/6371000),myLocation.getLongitude()+(180/Math.PI)*(100/6371000)/Math.cos(myLocation.getLongitude())));
        mk_pokemon = mMap.addMarker(mo_pokemon);
        mk_pokemon.setTitle("New Pokemon");
        mk_pokemon.setVisible(false);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (marker.getTitle().equals("New Pokemon")){
                    cv_pokemon.setVisibility(View.VISIBLE);
                    marker.setVisible(false);
                }
                return false;
            }
        });


    }

    @Override
    public void onLocationChanged(Location location) {

        double distanceFromLast = distance(myLocation.getLatitude(),location.getLatitude(), myLocation.getLongitude(),location.getLongitude());
        walkedDistance += distanceFromLast;
        DecimalFormat df = new DecimalFormat("#.##");
        tv_distance.setText(df.format(walkedDistance) + " m");

        myLocation = location;
        LatLng mylatlng = new LatLng(location.getLatitude(), location.getLongitude());
        marker.setPosition(mylatlng);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mylatlng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(19));

        // we have to return if catching pokemon
        if (catching){
            return;
        }

        // Check Server whether there is pokemon at this location (Obviously server checks within 200 meters of pokemon position)
        new CheckServer().execute(mylatlng);

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

    public static double distance(double lat1, double lat2, double lon1,
                                  double lon2) {

        final int R = 6371000; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // convert to meters

        return distance;
    }



    /**
     * This is new approach for accessing Server
     * we will just update global pokemon_infos
     * and then download when user wants with Downloader
     * */

    public class CheckServer extends AsyncTask<LatLng,Void,Void> {


        public LatLng latLng;
        public String str_url;

        @Override
        protected Void doInBackground(LatLng... params) {

            str_url = "http://emma.kaist.ac.kr:1442/go?" + params[0].latitude + "," + params[0].longitude;

            latLng = params[0];

            try {
                URL url = new URL(str_url);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("HEAD");
                httpURLConnection.connect();

                int code = httpURLConnection.getResponseCode();
                if (code != 404) {
                    pokemon_around = true;
                    pokemon_url = str_url;
                    String contentType = httpURLConnection.getContentType();

                    // get redirected url
                    InputStream in = httpURLConnection.getInputStream();
                    pokemon_url_redirected = String.valueOf(httpURLConnection.getURL());

                    if (contentType.equals("text/plain")) {
                        pokemon_type = 1;
                    }
                    else if (contentType.equals("image/png")) {
                        pokemon_type = 2;
                    }
                    else if (contentType.equals("video/mp4")) {
                        pokemon_type = 3;
                    }
                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void o) {
            super.onPostExecute(o);
            if (pokemon_around == true){

                mk_pokemon.setPosition(new LatLng(latLng.latitude + (180 / Math.PI) * (100 / 6371000), latLng.longitude + (180 / Math.PI) * (100 / 6371000) / Math.cos(latLng.longitude)));
                mk_pokemon.setVisible(true);

                mk_pokemon.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.duck));
                tv_pokemon_info.setVisibility(View.VISIBLE);
                if (pokemon_type == 1){
                    tv_pokemon_info.setText("~Pokemon Text~");
                }
                else if (pokemon_type == 2){
                    tv_pokemon_info.setText("~Pokemon Image~");
                }
                else if (pokemon_type == 3){
                    tv_pokemon_info.setText("~Pokemon Video~");

                }
            }
        }
    }


    public class PokemonDownloader extends AsyncTask<String,Void,Void>{
        @Override
        protected Void doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("GET");


                httpURLConnection.setUseCaches(true);

                System.out.println( "orignal url: " + httpURLConnection.getURL() );

                httpURLConnection.connect();

                System.out.println( "connected url: " + httpURLConnection.getURL() );


                // test for caching
                //System.out.println(httpURLConnection.getDate());


                int fileLength = httpURLConnection.getContentLength();

                File rootDirectory = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS),"KAIST GO Data");

                if (!rootDirectory.exists()){
                    rootDirectory.mkdir();
                }


                // Here we create a file according to pokemon type
                String nameOfFile = "not_let_it_be_empty";
                if (pokemon_type == 1){
                    nameOfFile = "text_pokemon.txt";
                }
                else if (pokemon_type == 2){
                    nameOfFile = "image_pokemon.png";
                }
                else if (pokemon_type == 3){
                    nameOfFile = "video_pokemon.mp4";
                }


                File file = new File(rootDirectory,nameOfFile);
                file.createNewFile();

                InputStream in = httpURLConnection.getInputStream();
                FileOutputStream out = new FileOutputStream(file);

                System.out.println( "connected url: " + httpURLConnection.getURL() );


                byte[] buffer = new byte[1024];
                int byteCount = 0;
                long total = 0;

                while ((byteCount = in.read(buffer)) > 0){

                    // here we should show progress bar
                    total += byteCount;
                    updateProgressBar((int)(total*100/fileLength));

                    out.write(buffer,0,byteCount);
                }

                out.close();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/KAIST GO Data";
            switch (pokemon_type){
                case 1:
                    try {
                        tv_pokemon.setText(new Scanner(new File(path,"text_pokemon.txt")).useDelimiter("\\Z").next());
                    }
                    catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;
                case 2:
                    Bitmap bitmap = BitmapFactory.decodeFile(new File(path,"image_pokemon.png").getAbsolutePath());
                    iv_pokemon.setImageBitmap(bitmap);
                    addBitmapToMemoryCache(pokemon_url_redirected,bitmap);
                    break;
                case 3:
                    File videoFile = new File(path,"video_pokemon.mp4");
                    addVideoToMemoryCache(pokemon_url_redirected, videoFile);
                    vv_pokemon.setVideoPath(path+"/video_pokemon.mp4");
                    break;
                default:
                    Toast.makeText(MapsActivity.this,"There ain't any pokemon",Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void updateProgressBar(int i) {
        progressBar.setProgress(i);
    }


    // all below code for caching

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCacheImage.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCacheImage.get(key);
    }

    public void loadBitmap() {

        final Bitmap bitmap = getBitmapFromMemCache(pokemon_url_redirected);
        if (bitmap != null) {
            iv_pokemon.setImageBitmap(bitmap);
        } else {
            iv_pokemon.setImageResource(R.drawable.web_hi_res_512);
            new PokemonDownloader().execute(pokemon_url);
        }
    }



    public void addVideoToMemoryCache(String key, File video) {
        if (getVideoFromMemCache(key) == null) {
            mMemoryCacheVideo.put(key, video);
        }

    }

    public File getVideoFromMemCache(String key) {
        return mMemoryCacheVideo.get(key);
    }

    public void loadVideo() {

        final File video = getVideoFromMemCache(pokemon_url_redirected);
        if (video != null) {
            vv_pokemon.setVideoPath(video.getAbsolutePath());
        } else {
            //iv_pokemon.setImageResource(R.drawable.web_hi_res_512);
            new PokemonDownloader().execute(pokemon_url);
        }
    }

}
