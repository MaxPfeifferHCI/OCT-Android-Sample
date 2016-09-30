package com.example.pfeiffer.xmasmarket;

import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class xmasMarketMapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    String APIKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcHBfbmFtZSI6IlRlc3RfMTcuOCIsImlhdCI6MTQ3MTQyNjQwOH0.BiUS3cEKLzE-y2c2ci6eonVya7kCvDVU9z00Lkzk5bI";
    String myQuery ="Social";
    String urlJason = "http://giv-oct.uni-muenster.de:8081/api/query/"+myQuery+"?authorization="+APIKey;

//some test code


    // just a test message
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xmas_market_maps);
        EditText search = (EditText) findViewById(R.id.queryTF);
        search.setText(myQuery);


        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
        }
    }


    // Search for query
    public void onSearch(View view) {
        EditText queryTF = (EditText) findViewById(R.id.queryTF);
        String myQuery = queryTF.getText().toString();

        urlJason = "http://giv-oct.uni-muenster.de:8081/api/query/" + myQuery + "?authorization="+APIKey;
        System.out.println(" urlJason " +urlJason);
        final RequestQueue reQue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlJason,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Pars the response
                        parseJson(response);
                        reQue.stop();

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                reQue.stop();

            }
        });
        reQue.add(stringRequest);
    }

    public void parseJson(String jsonString)
    {

        try {
            JSONObject reader = new JSONObject(jsonString);
            JSONObject tagQuary  = reader.getJSONObject("search_tag");
            JSONObject resultsData = reader.getJSONObject("data").getJSONObject("couchDB");
            JSONArray dataJSONArray = resultsData.getJSONArray("data");
            JSONObject dataJsonObject = dataJSONArray.getJSONObject(0);
            JSONObject previewObject =  dataJsonObject.getJSONObject("preview");
            JSONArray featuresArray =  previewObject.getJSONArray("features");

            for(int i=0; i < featuresArray.length(); i++) {

                JSONObject featuresObject = featuresArray.getJSONObject(i);
                JSONObject propertiesObject = featuresObject.getJSONObject("properties");
                String stand_nr = propertiesObject.getString("stand_nr");
                String warenangeb = propertiesObject.getString("warenangeb");
                JSONObject geometryObject = featuresObject.getJSONObject("geometry");

                if (geometryObject.getString("type").equalsIgnoreCase("Polygon")){
                    JSONArray coordinatesA = geometryObject.getJSONArray("coordinates");
                    JSONArray coordinatesAB = coordinatesA.getJSONArray(0);

                    ArrayList<LatLng> polyPoints = new ArrayList<LatLng>();
                    PolygonOptions standPolygons =new PolygonOptions();

                    for(int j=0; j < coordinatesAB.length(); j++) {
                        JSONArray coodinateA = coordinatesAB.getJSONArray(j);
                        String lat = coodinateA.getString(1);
                        String log = coodinateA.getString(0);

                        polyPoints.add(new LatLng(Double.parseDouble(lat), Double.parseDouble(log)));
                        standPolygons.add(new LatLng(Double.parseDouble(lat), Double.parseDouble(log)));

                    }

                    // Create the overly polygons
                    if(polyPoints.size() >=3) {
                        Polygon polygon = mMap.addPolygon(
                                standPolygons

                                        .strokeColor(Color.BLUE).strokeWidth(0.1f)
                                        .fillColor(0x22FF0000));

                        LatLng centroid = centroid(polyPoints);

                        //Add a marker on polygon with description
                        if (warenangeb!=null && warenangeb !="null") {
                            mMap.addMarker(new MarkerOptions().position(centroid).title("Stand " + stand_nr).snippet(warenangeb));
                        }else {
                            mMap.addMarker(new MarkerOptions().position(centroid).title("Stand " + stand_nr));
                        }

                        // Mover the camera to the position of the polygons
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(polyPoints.get(0), 20.0f));

                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //Calculate the center of a polygon
    public static LatLng centroid(List<LatLng> points) {
        double[] centroid = { 0.0, 0.0 };
        for (int i = 0; i < points.size(); i++) {
            centroid[0] += points.get(i).latitude;
            centroid[1] += points.get(i).longitude;
        }
        int totalPoints = points.size();
        return new LatLng(centroid[0] / totalPoints, centroid[1] / totalPoints);
    }

}
