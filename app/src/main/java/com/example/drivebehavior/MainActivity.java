package com.example.drivebehavior;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.Provider;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

//public class MainActivity extends AppCompatActivity {
public class MainActivity extends FragmentActivity implements View.OnClickListener, OnMapReadyCallback {
    private static final int TIME = 5000;
    private static final int DISTANCE = 5;  // Check at every i second  (i *1000) if the location changed more than distance, to update location
    private LocationManager lcm;
    private ArrayList latLonList;
    private ArrayList speedList;
    private ArrayList dateList;
    private ArrayList distanceList;
    public DatabaseHandler mydb;
    public TextView tv;
    public TextView distanceTv;
    public EditText editText;
    public Button btnOn;
    public Button btnOff;
    public Boolean recordFlag;
    private GoogleMap mMap;
    public LatLng currentPosition;
    public LatLng previousPosition;
    public Location lastLocation=null;
    private double calculatedSpeed=0;
    private double lastDistance=0;
    private GraphView graph;
    private LineGraphSeries<DataPoint> series;
    private ArrayList xyValueArray;
    private BarChart chart;
    public double sumDistance=0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        tv=(TextView) findViewById(R.id.txtLatLong);
        editText=(EditText)  findViewById(R.id.editText);
        distanceTv=(TextView)findViewById(R.id.distanceView);
        recordFlag=false;
        btnOn=(Button) findViewById(R.id.btnOn);
        btnOff=(Button) findViewById(R.id.btnOff);
        btnOn.setOnClickListener(this);

        chart=(BarChart)findViewById(R.id.bar_chart);


        SupportMapFragment mapFragment=(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        mydb=new DatabaseHandler(this);
        Log.d("Path"," path : "+this.getDatabasePath("driveBehavior_v1.db").getPath());
        //mydb.clearDatabase(mydb.TABLE_NAME);
        int totalRecord=mydb.getRecordCount();
        Log.d("TotalRow","Total Row : "+totalRecord);
        long result;
        result=mydb.addRecord("preset","1","1","1","1","1");
        totalRecord=mydb.getRecordCount();
        Log.d("TotalRow","Total Row 2  : "+totalRecord);


        lcm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        latLonList= new ArrayList<String>();
        speedList=new ArrayList<Float>();
        dateList=new ArrayList<String>();
        distanceList=new ArrayList<String>();


        final boolean gpsEnabled = lcm.isProviderEnabled(
                LocationManager.GPS_PROVIDER);
        if (!gpsEnabled) {
            new EnableGpsDialogFragment().show(getSupportFragmentManager(), "enableGpsDialog");
        } // if


        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);

        // Add your initialization code here
        Parse.initialize(new Parse.Configuration.Builder(getApplicationContext())
                .applicationId("")
                .clientKey("")
                .server("")
                .build()
        );

        ParseObject object = new ParseObject("GeoTracker");




    } // onCreate\

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onResume() {
        super.onResume();

        //lcm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions((Activity) this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);

            return;
        }

        Criteria criteria=new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        String provider = lcm.getBestProvider(criteria, true);

        Log.d("Location"," best provider "+ provider);

        //lcm.requestLocationUpdates(LocationManager.GPS_PROVIDER,TIME, DISTANCE, listener);

        lcm.requestLocationUpdates(provider,
                TIME, DISTANCE, listener);
    } //onResume

    private final LocationListener listener=new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            currentPosition= new LatLng(location.getLatitude(),location.getLongitude());
            //if(previousPosition!=null) {
            //    Log.d("Position", " Current " + currentPosition + ", Previous " + previousPosition);
            //}


            //Log.d("Position", " Current "+currentPosition+", Previous "+previousPosition);
            if(recordFlag){
                updateMap();
                previousPosition=currentPosition;
                updateWithNewLocation(location);
                Log.d("Flag", "(" + location.getLatitude() + "," + location.getLongitude() + ")");}else{
                Log.d("Flag", " Flag "+recordFlag);
            }

        } // onLocationChanged

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }; // end listener


    @Override
    public void onStop(){
        super.onStop();
        lcm.removeUpdates(listener);
    } // onStop


    private void updateWithNewLocation(Location location){
        //TextView tv=new TextView(this);
        //TextView tv=(TextView) findViewById(R.id.txtLatLong);
        //setContentView(tv);
        //tv.setTextSize(20);

        String latLongString="";
        if(location!=null){
            double lat=location.getLatitude();
            double lng=location.getLongitude();
            latLongString="Lat: "+lat+" ::  Long : "+lng;

            if(lastLocation!=null){
                double elaspedTime=(location.getTime()-lastLocation.getTime())/1000;
                lastDistance=lastLocation.distanceTo(location);
                sumDistance=sumDistance+(lastDistance/1000);
                calculatedSpeed=lastLocation.distanceTo(location)/elaspedTime;
            }
            this.lastLocation=location;
            double speed=location.hasSpeed() ? location.getSpeed() : calculatedSpeed;
            Log.d("Speed","CurrentSpeed "+speed+"  calSpeed "+calculatedSpeed+ " distance "+lastDistance+" sumDist "+sumDistance);


        } else {
            latLongString= " No location found ";
        } // if
        tv.setText("Current Lat Lon :\n "+latLongString);
        //tv.append("\n")

        SimpleDateFormat sdf=new SimpleDateFormat("dd-MM-YYYY HH:mm:ss");
        String currentDatetime=sdf.format(new Date());


        String out=" [ " +currentDatetime+ ","+ location.getLatitude() + "," + location.getLongitude() + " ] ";
        latLonList.add(out);

        speedList.add((float) calculatedSpeed);

        DrawGraph(speedList);

        Log.d("List Out", latLonList.toString() );

        Log.d("DateTime","location date :" + sdf.format(location.getTime()));

        ParseObject object = new ParseObject("GeoTracker");
        SaveToParseServer(object, editText.getText().toString(), currentDatetime, location.getLatitude(), location.getLongitude(), calculatedSpeed, lastDistance);


        //WriteFile(out);

        String outText=OutputList(latLonList);
        tv.setText(outText);
        DecimalFormat df = new DecimalFormat("#.#");
        String dummy = df.format(sumDistance);

        Log.i("text"," text out : "+dummy);
        distanceTv.setText(dummy+" km");
        //WriteFile(latLongString);

        int totalRecord=mydb.getRecordCount();
        Log.d("TotalRow","Total Row 33  : "+totalRecord);
        //long result;
        //result=mydb.addRecord(editText.getText().toString(),""+location.getLatitude(),""+location.getLongitude(),currentDatetime,""+calculatedSpeed,""+lastDistance);
        totalRecord=mydb.getRecordCount();
        tv.append(" \n\n Total Record after updated "+totalRecord);

    } //updateWithNewLocation






    public String OutputList(ArrayList input){
        Integer listLen=input.size();
        //Log.d("Try","len : "+listLen);
        String outString="";
        for(int i=0; i<listLen;i++){
            //Log.d("text1","out :"+input.get(i));
            outString=outString+" "+input.get(i)+"\n";
        }
        //Log.d("Outstring"," - "+outString);
        return outString;
    } // outputstring


    public void WriteFile(String output){
        String FILENAME="latlon1.txt";
        FileOutputStream fos;

        try{

            //File extmem= Environment.getExternalStorageDirectory();
            //File directory=new File(extmem.getAbsolutePath()+"/myFiles");
            //directory.mkdirs();
            //String directoryLocation=directory.getAbsolutePath();
            //Log.d("EXT Fileout",directoryLocation);
            //File file=new File(directory, FILENAME);
            //fos=new FileOutputStream(file);

            // Write to Internal Storage
            File file=getFilesDir();
            String filedir=file.getAbsolutePath();
            Log.d("Fileout",filedir);
            fos=openFileOutput(FILENAME, Context.MODE_PRIVATE);

            fos.write(output.getBytes());
            fos.close();

            Toast.makeText(getBaseContext(),"Text saved",Toast.LENGTH_SHORT).show();


        } catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }


    }//WriteFile

    @Override
    public void onClick(View v) {
        recordFlag=true;
        Toast.makeText(getApplicationContext()," Pressed "+recordFlag,Toast.LENGTH_SHORT).show();
    }

    public void TurnOffRecord(View v){
        recordFlag=false;
        Toast.makeText(getApplicationContext()," Pressed "+recordFlag,Toast.LENGTH_SHORT).show();
        mydb.close();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap=googleMap;
        //LatLng sydney=new LatLng(-34,151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker is Sydneu"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        Log.d("currentPosition"," ==> "+currentPosition);
        if(currentPosition!=null) {
            mMap.addMarker(new MarkerOptions().position(currentPosition).title("Marker is NOW"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));
        }

    }

    public void updateMap() {
        Log.d("currentPosition", " ==> " + currentPosition);
        if (currentPosition != null) {
            mMap.addMarker(new MarkerOptions().position(currentPosition).title("Marker is NOW"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition,15));
        }
        if(previousPosition!=null) {
            mMap.addPolyline(new PolylineOptions().geodesic(true)
                    .add(previousPosition)
                    .add(currentPosition));
        }
    }

    public ArrayList getSampleStudentData(int size) {
        ArrayList student = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            student.add(i, (float) Math.random() * 100);
        }
        return student;
    }


    public void DrawGraph(ArrayList input){
        try{
            Log.d("Draw", "Graph");
            Integer listLen=input.size();
            ArrayList<BarEntry> entries=new ArrayList<>();
            for( int index=0; index<listLen;index++){
                entries.add(new BarEntry(index, (float) input.get(index)));
            }// for

            BarDataSet dataset= new BarDataSet(entries,"#");
            dataset.setValueTextSize(8);
            ArrayList<IBarDataSet> dataSets=new ArrayList<IBarDataSet>();
            dataSets.add(dataset);

            BarData data=new BarData(dataSets);

            chart.setData(data);
            YAxis RightAxis=chart.getAxisRight();
            YAxis LeftAxis=chart.getAxisLeft();
            RightAxis.setEnabled(false);
            LeftAxis.setEnabled(false);

            chart.animateY(1000);

        }catch (IllegalArgumentException e){
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

    }  // draw graph

    public void SaveToParseServer(ParseObject object, String name, String date, double lat, double lon, double speed, double distance){
        Log.i("Parse"," => "+name+" "+date+" : "+lat+","+lon+" "+speed+" "+distance);
        object.put("Name", name);
        object.put("Date", date);
        object.put("lat", lat);
        object.put("lon", lon);
        object.put("Speed", speed);
        object.put("Distance", distance);

        object.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException ex) {
                if (ex == null) {
                    Log.i("Parse Result", "Successful!");
                } else {
                    Log.i("Parse Result", "Failed  1 " + ex.toString());
                }
            }
        });


        ParseUser.enableAutomaticUser();

        ParseACL defaultACL = new ParseACL();
        defaultACL.setPublicReadAccess(true);
        defaultACL.setPublicWriteAccess(true);
        ParseACL.setDefaultACL(defaultACL, true);


    }



    public static class EnableGpsDialogFragment extends DialogFragment{

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState){
            return new AlertDialog.Builder(getActivity())
                    .setTitle("GPS System")
                    .setMessage(" Enable GPS to use Tracker ")
                    .setPositiveButton("Setting ", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent settingsIntent=new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(settingsIntent);
                        }
                    })
                    .create();
        } //onCreateDialog

    } // EnableGpsDialogFragment



} // MainActivity
