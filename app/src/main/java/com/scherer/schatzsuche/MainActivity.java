package com.scherer.schatzsuche;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements LocationListener, SensorEventListener, View.OnClickListener {

    private LocationManager Location_GPS;
    private Button GPS_Button;
    private TextView position_text;
    private ImageView zielbild;
    private TextView spielstand_text;
    private int spielstand_level;
    private SensorManager sensor_manager;
    private Sensor Magnetfeld_sensor;
    private double aim_angle;
    private double Kompass_angle;
    private double position_distance;
    private SharedPreferences spielstand;

    private class geopoint {
        double longitudinal;
        double latitudinal;
    }

    private Handler warning_handler = new Handler();
    private Runnable warning_toast = new Runnable()
    {
        public void run()
        {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Location_GPS = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        sensor_manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Magnetfeld_sensor = sensor_manager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d("APP", "onCreate: ");

        /* button in view finden und lissener activieren*/
        GPS_Button = (Button) findViewById(R.id.qr_button);
        GPS_Button.setOnClickListener(this);

        position_text = (TextView)findViewById(R.id.dist_text);
        spielstand_text = (TextView)findViewById(R.id.level);
        zielbild = (ImageView)findViewById(R.id.imageView);

        spielstand = PreferenceManager.getDefaultSharedPreferences(this);
        spielstand_level = spielstand.getInt("Spielstand", 0);

        if ( !Location_GPS.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            Toast einToast = Toast.makeText(getApplicationContext(), "Bitte GPS einschalten", Toast.LENGTH_SHORT);
            einToast.show();
            warning_handler.postDelayed(warning_toast, 1000);
        }
    }
    @Override
    protected void onResume(){
        super.onResume();
        try {
            Location_GPS.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        } catch (SecurityException e) {
            Toast einToast = Toast.makeText(getApplicationContext(), "Bitte GPS Berechtigung erteilen", Toast.LENGTH_SHORT);
            einToast.show();
        }
        sensor_manager.registerListener(this, Magnetfeld_sensor, SensorManager.SENSOR_DELAY_GAME);
        spielstand_text.setText("Auf der Suche nach Rätsel: " + Integer.toString(spielstand_level + 1));
    }

    @Override
    protected void onPause(){
        super.onPause();
        SharedPreferences.Editor editor = spielstand.edit();
        editor.putInt("Spielstand", spielstand_level );
        editor.commit();
        Location_GPS.removeUpdates(this);
    }

    public void onClick(View v){
        if (v == GPS_Button){
            try {

                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes

                startActivityForResult(intent, 0);

            } catch (Exception e) {

                Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
                Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
                startActivity(marketIntent);

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {

            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                if(contents.equals("12459") && spielstand_level == 0) {
                    spielstand_level = 1;
                }else if(contents.equals("59874") && spielstand_level == 1) {
                    spielstand_level = 2;
                }else if(contents.equals("21895") && spielstand_level == 2) {
                    spielstand_level = 3;
                }else if(contents.equals("39834") && spielstand_level == 3) {
                    spielstand_level = 4;
                }else if(contents.equals("54386") && spielstand_level == 4) {
                    spielstand_level = 5;
                }else if(contents.equals("RESET")) {
                    spielstand_level = 0;
                }
                else
                {
                    Toast einToast = Toast.makeText(getApplicationContext(), "Falscher QR-Code", Toast.LENGTH_SHORT);
                    einToast.show();
                }
            }
            if(resultCode == RESULT_CANCELED){
                //handle cancel
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onLocationChanged(Location loc) {
        float[] ergebnis = new float[1];
        geopoint actaul_point = new geopoint();
        actaul_point.longitudinal = loc.getLongitude();
        actaul_point.latitudinal = loc.getLatitude();

        geopoint next_point = new geopoint();
        next_point = get_position(spielstand_level);

        loc.distanceBetween(actaul_point.latitudinal,
                actaul_point.longitudinal,
                next_point.latitudinal,
                next_point.longitudinal,
                ergebnis);
        Location loc2 = new Location("");//provider name is unnecessary
        loc2.setLatitude(next_point.latitudinal);//your coords of course
        loc2.setLongitude(next_point.longitudinal);

        aim_angle = loc.bearingTo(loc2);
        Log.d("APP", "angle: " + Double.toString(aim_angle));
        position_distance = ergebnis[0];
        position_text.setText("Dist. zum Ziel: " + String.format("%.2f", position_distance));
        Log.d("APP", "distance: " + position_distance);
        if(position_distance < 10.0){
            change_image_view(spielstand_level);
        }else{
            zielbild.setImageResource(R.drawable.kompass_);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(position_distance < 10.0){
            zielbild.setRotation(0.0f);
        }else {
            Kompass_angle = (360.0-event.values[0] + aim_angle );
            zielbild.setRotation((float) Kompass_angle);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    geopoint get_position(int level){
        geopoint actal_pint = new geopoint();
        if(level == 0){
            actal_pint.latitudinal = 50.092665;
            actal_pint.longitudinal  = 9.237205;
        }else if(level == 1) {
            actal_pint.latitudinal = 50.090359;
            actal_pint.longitudinal = 9.237533;
        }else if(level == 2) {
            actal_pint.latitudinal = 50.091105;
            actal_pint.longitudinal = 9.238221;
        }else if(level == 3) {
            actal_pint.latitudinal = 50.092188;
            actal_pint.longitudinal = 9.240366;
        }else if(level == 4) {
            actal_pint.latitudinal = 50.092716;
            actal_pint.longitudinal = 9.238358;
        }else if(level == 5) {
            actal_pint.latitudinal = 50.092192;
            actal_pint.longitudinal = 9.236830;
        }else{
            actal_pint.latitudinal  = 0.0;
            actal_pint.longitudinal = 0.0;
        }

        return actal_pint;
    }

    void change_image_view(int level)
    {
        if(level == 0){
            zielbild.setImageResource(R.drawable.bild_1);
        }else if(level == 1){
            zielbild.setImageResource(R.drawable.bild_2);
        }else if(level == 2){
            zielbild.setImageResource(R.drawable.bild_3);
        }else if(level == 3){
            zielbild.setImageResource(R.drawable.bild_4);
        }else if(level == 4){
            zielbild.setImageResource(R.drawable.bild_5);
        }else if(level == 5){
            zielbild.setImageResource(R.drawable.bild_6);
        }
    }
}
