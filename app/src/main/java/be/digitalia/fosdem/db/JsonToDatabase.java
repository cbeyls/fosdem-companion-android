package be.digitalia.fosdem.db;

import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import be.digitalia.fosdem.api.FossasiaUrls;
import be.digitalia.fosdem.model.FossasiaEvent;
import be.digitalia.fosdem.model.KeySpeaker;
import be.digitalia.fosdem.utils.VolleySingleton;

/**
 * Created by Abhishek on 17/02/15.
 */
public class JsonToDatabase {

    private final static String TAG = "JSON_TO_DATABASE";

    private Context context;
    private boolean keySpeakerLoaded;
    private boolean scheduleLoaded;
    private boolean speakerEventRelation;
    private boolean tracks;
    private ArrayList<String> queries;

    public JsonToDatabase(Context context) {
        this.context = context;
        this.keySpeakerLoaded = false;
        fetchKeySpeakers(FossasiaUrls.KEY_SPEAKER_URL);
        fetchSchedule(FossasiaUrls.SCHEDULE_URL);
        fetchSpeakerEventRelation(FossasiaUrls.SPEAKER_EVENT_URL);
        fetchTracks(FossasiaUrls.TRACKS_URL);
        queries = new ArrayList<String>();
        keySpeakerLoaded = false;
        scheduleLoaded = false;
        speakerEventRelation = false;
        tracks = false;


    }

    private void fetchTracks(String url) {

        RequestQueue queue = VolleySingleton.getReqQueue(context);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(url, new Listener<String>() {

            @Override
            public void onResponse(String response) {
                JSONArray jsonArray = removePaddingFromString(response);
                Log.d(TAG, jsonArray.toString());
                String trackName;
                String trackInformation;

                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        trackName = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(0)
                                .getString("v");
                        trackInformation = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(1)
                                .getString("v");
                        String query = "INSERT INTO %s VALUES (%d, '%s', '%s');";
                        query = String.format(query, DatabaseHelper.TABLE_NAME_TRACK, i, trackName, trackInformation);
                        Log.d(TAG, query);
                        queries.add(query);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Error: " + e.getMessage() + "\nResponse" + response);
                    }

                }
                tracks = true;
                checkStatus();
            }
        }

                , new ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                tracks = true;
                checkStatus();
            }
        }

        );
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


    private void fetchSpeakerEventRelation(String url) {

        RequestQueue queue = VolleySingleton.getReqQueue(context);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(url, new Listener<String>() {

            @Override
            public void onResponse(String response) {
                JSONArray jsonArray = removePaddingFromString(response);
                Log.d(TAG, jsonArray.toString());
                String speaker;
                String event;

                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        speaker = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(0)
                                .getString("v");
                        event = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(1)
                                .getString("v");
                        String query = "INSERT INTO %s VALUES ('%s', '%s');";
                        query = String.format(query, DatabaseHelper.TABLE_NAME_SPEAKER_EVENT_RELATION, speaker, event);
//                        Log.d(TAG, query);
                        queries.add(query);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Error: " + e.getMessage() + "\nResponse" + response);
                    }

                }
                speakerEventRelation = true;
                checkStatus();
            }
        }

                , new ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                speakerEventRelation = true;
                checkStatus();
            }
        }

        );
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }




    private void fetchSchedule(String url) {

        RequestQueue queue = VolleySingleton.getReqQueue(context);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(url, new Listener<String>() {

            @Override
            public void onResponse(String response) {
                JSONArray jsonArray = removePaddingFromString(response);
                Log.d(TAG, jsonArray.toString());
                int id;
                String title;
                String subTitle;
                String date;
                String day;
                String startTime;
                String endTime;
                String abstractText;
                String description;
                String venue;
                String track;

                for (int i = 1; i < jsonArray.length(); i++) {
                    // Starting from 1 not 0, because 1st row contains columns name and actual data is from second row
                    try {
                        title = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(0)
                                .getString("v");
                        subTitle = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(1)
                                .getString("v");
                        date = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(2)
                                .getString("v");
                        day = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(3)
                                .getString("v");
                        startTime = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(4)
                                .getString("v");
                        endTime = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(5)
                                .getString("v");
                        abstractText = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(6)
                                .getString("v");
                        description = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(7)
                                .getString("v");
                        venue = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(8)
                                .getString("v");
                        track = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(9)
                                .getString("v");
                        id = i - 1;

                        FossasiaEvent temp = new FossasiaEvent(id, title, subTitle, date, day, startTime, endTime, abstractText, description, venue, track);
                        Log.d(TAG, temp.generateSqlQuery());
                        queries.add(temp.generateSqlQuery());
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Error: " + e.getMessage() + "\nResponse" + response);
                    }

                }
                scheduleLoaded = true;
                checkStatus();
            }
        }

                , new ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                scheduleLoaded = true;
                checkStatus();
            }
        }

        );
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
    

    private void fetchKeySpeakers(String url) {

        RequestQueue queue = VolleySingleton.getReqQueue(context);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(url, new Listener<String>() {

            @Override
            public void onResponse(String response) {
                JSONArray jsonArray = removePaddingFromString(response);
                Log.d(TAG, jsonArray.toString());
                String name;
                String designation;
                String profilePicUrl;
                String information;
                String twitterHandle;
                String linkedInUrl;
                for (int i = 0; i < jsonArray.length(); i++) {
                    try {
                        name = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(0)
                                .getString("v");
                        designation = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(1)
                                .getString("v");
                        information = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(2)
                                .getString("v");
                        twitterHandle = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(3)
                                .getString("v");
                        linkedInUrl = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(4)
                                .getString("v");
                        profilePicUrl = jsonArray.getJSONObject(i).getJSONArray("c").getJSONObject(5)
                                .getString("v");
                        KeySpeaker temp = new KeySpeaker(i + 1, name, information, linkedInUrl, twitterHandle, designation, profilePicUrl);
//                        Log.d(TAG, temp.generateSqlQuery());
                        queries.add(temp.generateSqlQuery());
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Error: " + e.getMessage() + "\nResponse: " + response);
                    }

                }
                keySpeakerLoaded = true;
                checkStatus();
            }
        }

                , new ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                keySpeakerLoaded = true;
                checkStatus();
            }
        }

        );
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void checkStatus() {
        if (keySpeakerLoaded && scheduleLoaded && speakerEventRelation && tracks) {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            //Temporary clearing database for testing only
            dbManager.clearDatabase();
            dbManager.performInsertQueries(queries);

            //Implement callbacks
        }
    }

    private JSONArray removePaddingFromString(String response) {
        response = response.substring(response.indexOf("(") + 1, response.length() - 2);
        try {
            JSONObject jObj = new JSONObject(response);
            jObj = jObj.getJSONObject("table");
            JSONArray jArray = jObj.getJSONArray("rows");
//            Log.d(TAG, jArray.toString());
            return jArray;
        } catch (JSONException e) {
            Log.e(TAG, "JSON Error: " + e.getMessage() + "\nResponse" + response);

        }

        return null;

    }


}
