package org.js.msb2map;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.util.LinkedList;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity {

    MapView map=null;
    Context ctx;
    String exPath= Environment.getExternalStorageDirectory().getAbsolutePath();
    Marker startMarker;
    Double latitude=48.8583;
    Double longitude=2.2944;
    Double zoom=15.0;
    Float width=4.0f;
    Location center=null;
    String pathStartGPS=exPath+"/MSBlog/StartGPS.gpx";
    Marker flyMarker=null;
    IMapController mapController=null;
    IntentFilter filter=new IntentFilter("org.js.LOC");
    Boolean listening=false;
    Button bMsb2And;
    TextView vInfo;
    String caller=null;
    GeoPoint prevGeoPt=null;
    LinkedList<Polyline> listLine=new LinkedList<>();
    Boolean withStart=true;
    Boolean Tail=true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);
        fetchPref();
        Intent intent=getIntent();
        caller=intent.getStringExtra("CALLER");
        center=(Location) intent.getParcelableExtra("CENTER");
        if (center!=null){
            latitude=center.getLatitude();
            longitude=center.getLongitude();
        }
        zoom=intent.getDoubleExtra("ZOOM",zoom);
        withStart=intent.getBooleanExtra("StartGPS",withStart);
        Tail=intent.getBooleanExtra("Tail",Tail);
        int wPix = Resources.getSystem().getDisplayMetrics().widthPixels;
        int hPix = Resources.getSystem().getDisplayMetrics().heightPixels;
        if (wPix>1024 || hPix>1024) width=6.0f;
        else width=4.0f;
        if (withStart) {
            checkStorage();
        } else {
            strtMap();
        }
    }

    void fetchPref(){
        SharedPreferences pref=ctx.getSharedPreferences(ctx.getString(R.string.PrefName),0);
        Float fZoom=pref.getFloat("ZOOM",zoom.floatValue());
        zoom=fZoom.doubleValue();
        Float fLat=pref.getFloat("LATITUDE",latitude.floatValue());
        latitude=fLat.doubleValue();
        Float fLon=pref.getFloat("LONGITUDE",longitude.floatValue());
        longitude=fLon.doubleValue();
    }

    void putPref(){
        SharedPreferences pref=ctx.getSharedPreferences(ctx.getString(R.string.PrefName),0);
        SharedPreferences.Editor edit=pref.edit();
        edit.putFloat("ZOOM",zoom.floatValue());
        edit.putFloat("LATITUDE",latitude.floatValue());
        edit.putFloat("LONGITUDE",longitude.floatValue());
        edit.apply();
    }

    void checkStorage(){
        String state=Environment.getExternalStorageState();
        Boolean montedSD=state.contains(Environment.MEDIA_MOUNTED);
        if (!montedSD){
            Toast.makeText(ctx,exPath+" not mounted!",Toast.LENGTH_LONG).show();
            finish();
        }
        Boolean writeSD=!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
        if (!writeSD){
            Toast.makeText(ctx,exPath+" not writable.",Toast.LENGTH_LONG).show();
        }
        Boolean hasPermission=(ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED);
        if (!hasPermission){
            Toast.makeText(ctx,"This application need to read "+exPath+".",Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        } else strtMap();
    }

    @Override
    public void onRequestPermissionsResult(int reqCode, String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(reqCode,permissions,grantResults);
        if (reqCode==1){
            if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                strtMap();
            } else {
                Toast.makeText(ctx,"Not granted!",Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    public void onResume(){
        super.onResume();
        if (listening) registerReceiver(mReceiver,filter);
    }

    public void onPause(){
        super.onPause();
        if (listening) unregisterReceiver(mReceiver);
    }

    public void launchMsb2And(){
        if (map!=null){
            zoom=map.getZoomLevelDouble();
            IGeoPoint cntr=map.getMapCenter();
            latitude=cntr.getLatitude();
            longitude=cntr.getLongitude();
            putPref();
        }
        finish();
    }

    public void strtMap(){
        StartGPS sg;
        Map<String,Location> startPt;
        map=(MapView) findViewById(R.id.map);
        bMsb2And=(Button) findViewById(R.id.msb2and);
        vInfo=(TextView) findViewById(R.id.vInfo);
        if (caller==null){
            bMsb2And.setText("Exit");
        } else {
            bMsb2And.setText(caller);
        }
        bMsb2And.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchMsb2And();
            }
        });
        map.setTileSource(TileSourceFactory.OpenTopo);
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(map);
        mRotationGestureOverlay.setEnabled(true);
        map.setMultiTouchControls(true);
        map.getOverlays().add(mRotationGestureOverlay);
        map.getOverlays().add(new CopyrightOverlay(ctx));
        mapController = map.getController();
        sg=new StartGPS(pathStartGPS);
        Integer nPt=0;
        if (withStart) {
            startPt = sg.readSG();
            if (!startPt.isEmpty()) {
                SortedSet<String> keys = new TreeSet<>();
                keys.addAll(startPt.keySet());
                for (String here : keys) {
                    if (center == null) {
                        center = startPt.get(here);
                        latitude = center.getLatitude();
                        longitude = center.getLongitude();
                    }
                    Double lat = startPt.get(here).getLatitude();
                    Double lon = startPt.get(here).getLongitude();
                    Marker m = new Marker(map);
                    m.setIcon(getResources().getDrawable(R.drawable.diabolo));
                    m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                    m.setPosition(new GeoPoint(lat, lon));
                    m.setTitle(here);
                    map.getOverlays().add(m);
                    nPt++;
                }
            }
            vInfo.setText("Nb. Location: "+nPt);
        }
        mapController.setZoom(zoom);
        mapController.setCenter(new GeoPoint(latitude,longitude));
        ScaleBarOverlay scale=new ScaleBarOverlay(map);
        scale.setUnitsOfMeasure(ScaleBarOverlay.UnitsOfMeasure.metric);
        map.getOverlays().add(scale);
        map.invalidate();
        Intent nt=new Intent();
        nt.setAction("org.js.ACK");
        nt.putExtra("NAME",getResources().getString(R.string.app_name));
        listening=true;
        registerReceiver(mReceiver,filter);
        sendBroadcast(nt);
    }

    public void wpt(Location loc, String name){
        Double lat=loc.getLatitude();
        Double lon=loc.getLongitude();
        Marker m=new Marker(map);
        if (name!=null) m.setTitle(name);
        m.setIcon(getResources().getDrawable(R.drawable.diabolo));
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        m.setPosition(new GeoPoint(lat, lon));
        map.getOverlays().add(m);
        map.invalidate();
    }

    public void update(Location loc, Integer color, String bubble){
        GeoPoint gp=new GeoPoint(loc.getLatitude(),loc.getLongitude());
        if (Tail) {
            if (flyMarker != null) flyMarker.remove(map);
            flyMarker = new Marker(map);
            flyMarker.setIcon(getResources().getDrawable(R.drawable.target));
            flyMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            flyMarker.setPosition(gp);
            if (flyMarker == null) {
                flyMarker = new Marker(map);
                flyMarker.setIcon(getResources().getDrawable(R.drawable.target));
                flyMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                flyMarker.setPosition(gp);
                map.getOverlays().add(flyMarker);
            }
            flyMarker.setPosition(gp);
            map.getOverlays().add(flyMarker);
            mapController.setCenter(gp);
        }
        if (prevGeoPt!=null){
            Polyline poly=new Polyline(map);
            poly.addPoint(prevGeoPt);
            poly.addPoint(gp);
            poly.setWidth(width);
            poly.getPaint().setStrokeCap(Paint.Cap.ROUND);
            if (color!=null) poly.setColor(color);
            map.getOverlays().add(poly);
            if (Tail) {
                listLine.addFirst(poly);
                if (listLine.size() > 20) {
                    Polyline rm = listLine.removeLast();
                    map.getOverlays().remove(rm);
                }
            }
        }
        prevGeoPt=gp;
        map.invalidate();
        if (bubble!=null) {
            vInfo.setText(bubble);
        }
    }

    private final BroadcastReceiver mReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location loc=(Location) intent.getParcelableExtra("LOC");
            if (loc!=null) {
                String bubble = intent.getStringExtra("BUBBLE");
                Integer color = intent.getIntExtra("COLOR", Color.BLACK);
                update(loc, color, bubble);
            } else {
                loc=(Location) intent.getParcelableExtra("WPT");
                String name=intent.getStringExtra("WPT_NAME");
                if (loc!=null) wpt(loc,name);
            }
        }
    };
}
