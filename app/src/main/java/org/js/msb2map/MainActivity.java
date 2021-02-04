package org.js.msb2map;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    MapView map=null;
    Context ctx;
    String exPath= Environment.getExternalStorageDirectory().getAbsolutePath();
    Double latitude=48.8583;
    Double longitude=2.2944;
    Double zoom=15.0;
    Float width=4.0f;
    Location center=null;
    Marker flyMarker=null;
    GeoPoint geo=null;
    Marker mark=null;
    IMapController mapController=null;
    IntentFilter filter=new IntentFilter("org.js.LOC");
    Boolean listening=false;
    Button bMsb2And;
    TextView vInfo;
    String caller=null;
    GeoPoint prevGeoPt=null;
    LinkedList<Polyline> listLine=new LinkedList<>();
    Boolean Tail=true;
    Boolean Picking=false;
    Boolean pickWpt=false;
    Marker pick=null;
    Boolean mvPick=false;
    int pickNb=0;
    NumberFormat nfe=NumberFormat.getInstance(Locale.ENGLISH);
    class DataMark {
        Integer index;
        Double altitude;
    }
    Map<Marker,DataMark> assocAlt=new HashMap<>();
    Marker prevMark=null;
    Marker frstRteMark=null;


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
        Tail=intent.getBooleanExtra("Tail",Tail);
        int wPix = Resources.getSystem().getDisplayMetrics().widthPixels;
        int hPix = Resources.getSystem().getDisplayMetrics().heightPixels;
        if (wPix>1024 || hPix>1024) width=6.0f;
        else width=4.0f;
        if (caller!=null) this.setTitle(getString(R.string.app_name)+" : "+caller);
        strtMap();
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

    public void onResume(){
        super.onResume();
        if (map!=null) map.onResume();
        if (listening) registerReceiver(mReceiver,filter);
    }

    public void onPause(){
        super.onPause();
        if (map!=null) map.onPause();
        if (listening) unregisterReceiver(mReceiver);
    }

    public void onDestroy(){
        super.onDestroy();
        if (listening){
            unregisterReceiver(mReceiver);
            listening=false;
        }
    }

    public void launchMsb2And(){

        if (map!=null){
            zoom=map.getZoomLevelDouble();
            IGeoPoint cntr=map.getMapCenter();
            latitude=cntr.getLatitude();
            longitude=cntr.getLongitude();
            putPref();
            map.getOverlays().clear();
            map=null;
        }
        if (listening) unregisterReceiver(mReceiver);
        listening=false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            finishAndRemoveTask();
        } else finish();
    }

    public void strtMap(){
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
        map.getOverlays().clear();
        map.getOverlays().add(mRotationGestureOverlay);
        map.getOverlays().add(new CopyrightOverlay(ctx));
        mapController = map.getController();
        mapController.setZoom(zoom);
        mapController.setCenter(new GeoPoint(latitude,longitude));
        ScaleBarOverlay scale=new ScaleBarOverlay(map);
        scale.setUnitsOfMeasure(ScaleBarOverlay.UnitsOfMeasure.metric);
        map.getOverlays().add(scale);
        map.invalidate();

        Intent nt=new Intent();
        nt.setAction("org.js.ACK");
        nt.putExtra("NAME",getResources().getString(R.string.app_name));
        PackageManager pm=getPackageManager();
        int vc=0;
        try {
            vc = pm.getPackageInfo(getPackageName(), 0).versionCode;
            nt.putExtra("VERSION",vc);
        } catch (Exception e){
            vc=0;
        }
        listening=true;
        registerReceiver(mReceiver,filter);
        sendBroadcast(nt);
    }

    void mrkTyp(Marker mark, int typ){
        switch (typ) {
            case 1:
                mark.setIcon(getResources().getDrawable(R.drawable.dot));
                break;
            case 2:
                mark.setIcon(getResources().getDrawable(R.drawable.diabolo2));
                mark.setAlpha(0.5f);
                break;
            case 3:
                mark.setIcon(getResources().getDrawable(R.drawable.butterfly));
                mark.setAlpha(0.5f);
                break;
            case 4:
                mark.setIcon(getResources().getDrawable(R.drawable.target));
                break;
            default:
                mark.setIcon(getResources().getDrawable(R.drawable.diabolo));
                mark.setAlpha(0.5f);
                break;
        }
    }

    public void wpt(Location loc, String name, String bubble, int typ){
        Double lat=loc.getLatitude();
        Double lon=loc.getLongitude();
        geo=new GeoPoint(lat,lon);
        mark=new Marker(map);
        mrkTyp(mark,typ);
        mark.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        mark.setPosition(geo);
        if (name!=null) {
            mark.setTitle(new String(name));
        }
        map.getOverlayManager().add(mark);
        if (bubble!=null) {
            vInfo.setText(bubble);
        }
    }

    public void update(Location loc, Integer color, String bubble,
                       Boolean startPt, Boolean tail){
        Boolean wthInf=false;
        GeoPoint gp=new GeoPoint(loc.getLatitude(),loc.getLongitude());
        if (startPt){
            prevGeoPt=null;
            Tail=tail;
        }
        if (flyMarker!=null){
            wthInf=flyMarker.isInfoWindowShown();
            if (wthInf) flyMarker.closeInfoWindow();
            flyMarker.remove(map);
            flyMarker=null;
        }
        if (Tail) {
            flyMarker = new Marker(map);
            flyMarker.setIcon(getResources().getDrawable(R.drawable.target));
            flyMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            flyMarker.setPosition(gp);
            if (loc.hasAltitude()){
                String alt=String.format(Locale.ENGLISH,"%.1f",loc.getAltitude());
                flyMarker.setTitle(alt);
                if (wthInf) flyMarker.showInfoWindow();
            }
            map.getOverlays().add(flyMarker);
            mapController.setCenter(gp);
        }
        if (prevGeoPt!=null){
            Polyline poly=new Polyline(map);
            poly.addPoint(prevGeoPt);
            poly.addPoint(gp);
            poly.getOutlinePaint().setStrokeWidth(width);
            poly.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
            if (color!=null) poly.getOutlinePaint().setColor(color);
            poly.setOnClickListener(new Polyline.OnClickListener() {
                @Override
                public boolean onClick(Polyline polyline, MapView mapView, GeoPoint geoPoint) {
                    return false;
                }
            });
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
        if (bubble!=null) {
            vInfo.setText(bubble);
        }
    }

    private final BroadcastReceiver mReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!listening) return;
            Location loc=(Location) intent.getParcelableExtra("LOC");
            if (loc!=null) {
                String bubble = intent.getStringExtra("BUBBLE");
                Integer color = intent.getIntExtra("COLOR", Color.BLACK);
                Boolean startPt=intent.getBooleanExtra("START",false);
                Boolean tail=intent.getBooleanExtra("Tail",Tail);
                update(loc, color, bubble,startPt,tail);
            } else {
                loc=(Location) intent.getParcelableExtra("WPT");
                if (loc!=null) {
                    String bubble = intent.getStringExtra("BUBBLE");
                    String name = intent.getStringExtra("WPT_NAME");
                    int typ=intent.getIntExtra("TYPE",0);
                    wpt(loc, name, bubble,typ);
                } else {
                    Picking=intent.getBooleanExtra("PICKING",false);
                    pickWpt=intent.getBooleanExtra("PICKWPT",false);
                    initPick();
                }
            }
            map.invalidate();
        }
    };

    MapListener mapLstnr=new MapListener() {
        @Override
        public boolean onScroll(ScrollEvent scrollEvent) {
            if (pick==null) mkPkMe();
            else {
                BoundingBox bb=map.getBoundingBox();
                if (bb.contains(pick.getPosition())) return false;
                GeoPoint corner=bb.getGeoPointOfRelativePositionWithLinearInterpolation(
                        0.25f,0.75f);
                pick.setPosition(corner);
            }
            return false;
        }

        @Override
        public boolean onZoom(ZoomEvent zoomEvent) {
            if (pick==null) mkPkMe();
            else {
                BoundingBox bb=map.getBoundingBox();
                if (bb.contains(pick.getPosition())) return false;
                GeoPoint corner=bb.getGeoPointOfRelativePositionWithLinearInterpolation(
                        0.25f,0.75f);
                pick.setPosition(corner);
            }
            return false;
        }
    };

    void initPick(){
        if (Picking){
            mkPkMe();
            map.addMapListener(mapLstnr);
        } else {
            map.removeMapListener(mapLstnr);
            if (pick!=null){
                map.getOverlays().remove(pick);
                pick=null;
            }
        }
    }

    void mkPkMe(){
        if (!map.isLayoutOccurred()) return;
        BoundingBox bb=map.getBoundingBox();
        GeoPoint corner=bb.getGeoPointOfRelativePositionWithLinearInterpolation(
                0.25f,0.75f);
        pick=new Marker(map);
        pick.setIcon(getResources().getDrawable(R.drawable.target));
        pick.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_CENTER);
        pick.setPosition(corner);
        pick.setTitle("Pick Me");
        pick.showInfoWindow();
        pick.setDraggable(true);
        pick.setDragOffset(10.0f);
        pick.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(Marker marker) {
                disInfo(marker);
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                BoundingBox bb=map.getBoundingBox();
                GeoPoint corner=bb.getGeoPointOfRelativePositionWithLinearInterpolation(
                     0.25f,0.75f);
                GeoPoint pt=marker.getPosition();
                nwRteWpt(pt);
                marker.setPosition(corner);
                marker.setTitle("Pick me");
                marker.showInfoWindow();
                map.invalidate();
                mvPick=false;
            }

            @Override
            public void onMarkerDragStart(Marker marker) {
                prevMark=getPrevMark(pickNb);
                if (prevMark==null || pickWpt) vInfo.setText("Latitude,Longitude");
                else vInfo.setText("Bearing,Distance");
                mvPick=true;
            }

        });
        map.getOverlays().add(pick);
    }

    Marker getPrevMark(Integer curIx){
        if (curIx<=0) return null;
        Integer toSearch=curIx-1;
        for (Marker m : assocAlt.keySet()){
            DataMark dm=assocAlt.get(m);
            if (dm.index==toSearch) return m;
        }
        return null;
    }

    void disInfo(Marker m){
        String info;
        GeoPoint pt=m.getPosition();
        if (prevMark==null || pickWpt){
            info=String.format(Locale.ENGLISH,
                        "%.6f°,%.6f°", pt.getLatitude(),pt.getLongitude());
            m.showInfoWindow();
            m.setTitle(info);
        } else {
            Double bearing=prevMark.getPosition().bearingTo(pt);
            Double dist=pt.distanceToAsDouble(prevMark.getPosition());
            int index=assocAlt.get(prevMark).index;
            Double bearing0=frstRteMark.getPosition().bearingTo(pt);
            Double dist0=pt.distanceToAsDouble(frstRteMark.getPosition());
            info=String.format(Locale.ENGLISH,"%d: %.2f°,%.1fm",0,bearing0,dist0);
            String infoN=String.format(Locale.ENGLISH,"%d: %.2f°,%.1fm",
                    index,bearing,dist);
            m.showInfoWindow();
            m.setTitle(infoN+" | "+info);
        }
    }

    void nwRteWpt(GeoPoint pt){
        Marker nw=new Marker(map);
        nw.setPosition(pt);
        nw.setIcon(getResources().getDrawable(R.drawable.butterfly));
        nw.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_CENTER);
        nw.setAlpha(0.5f);
        final Integer mkIx=pickNb++;
        if (pickWpt){
//            nw.setTitle("Wpt "+mkIx.toString());
        } else {
            nw.setTitle("Rte Pt " + mkIx.toString());
        }
        DataMark dm=new DataMark();
        dm.index=mkIx;
        dm.altitude=null;
        nw.setDraggable(true);
        nw.setDragOffset(10.0f);
        nw.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(Marker marker) {
                disInfo(marker);
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                marker.setIcon(getResources().getDrawable(R.drawable.butterfly));
                marker.setAlpha(0.5f);
                sendPicked(marker);
                pick.showInfoWindow();
                map.invalidate();
            }

            @Override
            public void onMarkerDragStart(Marker marker) {
                prevMark=getPrevMark(mkIx);
                if (prevMark==null || pickWpt) vInfo.setText("Latitude,Longitude");
                else vInfo.setText("Bearing,Distance");
                marker.setIcon(getResources().getDrawable(R.drawable.target));
                marker.setAlpha(1.0f);
                map.invalidate();
            }
        });
        assocAlt.put(nw,dm);
        map.getOverlays().add(nw);
        sendPicked(nw);
    }

    void sendPicked(Marker m){
        if (pickWpt){
            final Marker curM=m;
            final GeoPoint gPt=curM.getPosition();
            final String wptName=curM.getTitle();
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            builder.setTitle("Waypoint detail");
            View wptDetail=View.inflate(this,R.layout.wpt,null);
            TextView vLatLon=wptDetail.findViewById(R.id.latlon);
            final EditText vAlt=wptDetail.findViewById(R.id.wptalt);
            final EditText vName=wptDetail.findViewById(R.id.wptname);
            String pos=String.format(Locale.ENGLISH,"Latitude, Longitude: %.6f, %.6f",
                    gPt.getLatitude(),gPt.getLongitude());
            final DataMark dm=assocAlt.get(curM);
            vLatLon.setText(pos);
            final String defName="Wpt "+assocAlt.get(curM).index.toString();
            vName.setHint(defName);
            if (wptName!=null && !wptName.isEmpty()) vName.setText(wptName);
            if (dm.altitude!=null) vAlt.setText(String.format(Locale.ENGLISH,
                    "%.2f",dm.altitude));
            builder.setView(wptDetail);
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    frgtpkd(curM);
                            map.getOverlays().remove(curM);
                            map.invalidate();
                }
            });
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String rdName=vName.getText().toString();
                    if (rdName!=null) rdName=rdName.trim();
                    if (rdName==null || rdName.isEmpty()){
                        rdName=defName;
                    }
                    String sAlt=vAlt.getText().toString();
                    if (sAlt!=null) sAlt=sAlt.trim();
                    Double rdAlt=dm.altitude;
                    if (sAlt!=null && !sAlt.isEmpty()){
                        try {
                            rdAlt=nfe.parse(sAlt).doubleValue();

                        } catch (ParseException e){
                            rdAlt=dm.altitude;
                        }
                    }
                    curM.setTitle(rdName);
                    GeoPoint rPt=gPt;
                    curM.setPosition(rPt);
                    dm.altitude=rdAlt;
                    assocAlt.put(curM,dm);
                    sendpkd(curM);
                }
            })
                    .setNegativeButton("Forget", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            frgtpkd(curM);
                            map.getOverlays().remove(curM);
                            map.invalidate();
                        }
                    });
            builder.show();
        } else {
            sendpkd(m);
        }
    }



    void sendpkd(Marker m){
        DataMark dm=assocAlt.get(m);
        if (dm.index==0) frstRteMark=m;
        Intent nt=new Intent();
        nt.setAction("org.js.PICKED");
        nt.putExtra("INDEX",dm.index);
        nt.putExtra("NAME",m.getTitle());
        GeoPoint pt=m.getPosition();
        Location loc=new Location("");
        loc.setLatitude(pt.getLatitude());
        loc.setLongitude(pt.getLongitude());
        if (dm.altitude!=null) loc.setAltitude(dm.altitude);
        nt.putExtra("LOC",loc);
        sendBroadcast(nt);
    }

    void frgtpkd(Marker m){
        DataMark dm=assocAlt.get(m);
        Intent nt=new Intent();
        nt.setAction("org.js.PICKED");
        nt.putExtra("INDEX",dm.index);
        nt.putExtra("NAME",m.getTitle());
        Location loc=null;
        nt.putExtra("LOC",loc);
        sendBroadcast(nt);
        assocAlt.remove(m);
    }

}
