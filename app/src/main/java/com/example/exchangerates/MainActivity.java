package com.example.exchangerates;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // с зависимостями в build.gradle все норм???

    final String ATTR_CUR_NAME = "curName";
    final String ATTR_EX_RATE = "exRate";
    final String TIMESTAMP = "timestamp";

    private String TAG = "myLogs";

    ProgressDialog pDialog;
    ListView lv;
    DBHelper dbHelper;
    SharedPreferences sPref;

    private static String url = "https://api.exchangeratesapi.io/latest?base=USD";

    private static String[] currencyNames = {"CAD", "HKD", "ISK", "PHP", "DKK", "HUF", "CZK", "GBP", "RON", "SEK", "IDR", "INR",
            "BRL", "RUB", "HRK", "JPY", "THB", "CHF", "EUR", "MYR", "BGN", "TRY",
            "CNY", "NOK", "NZD", "ZAR", "USD", "MXN", "SGD", "AUD", "ILS", "KRW", "PLN"};

    ArrayList<Map<String, String>> exchangeData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // if preference doesnt exist

        sPref = getPreferences(MODE_PRIVATE);
        if(!sPref.contains(TIMESTAMP)){
            SharedPreferences.Editor ed = sPref.edit();
            ed.putString(TIMESTAMP, "0");
            ed.commit();
        }

        lv = (ListView) findViewById(R.id.lv);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, SelectedCurrency.class);
                intent.putExtra(ATTR_CUR_NAME, currencyNames[position]);
                startActivity(intent);
            }
        });

        new GetRates().execute();

        dbHelper = new DBHelper(this);
    }

    private class GetRates extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            ContentValues cv;

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            String jsonStr = sh.makeServiceCall(url);
            Map<String, String> map;

            sPref = getPreferences(MODE_PRIVATE);
            long oldTimestamp = Long.parseLong(sPref.getString(TIMESTAMP, ""));

            long newTimestamp = System.currentTimeMillis();
            long diff = (newTimestamp - oldTimestamp) / (60 * 1000);

            Log.d(TAG, "Time dif in min = " + diff);

            if (diff >= 10) {

                Log.d(TAG, "Data from web service");

                if (jsonStr != null) {
                    try {
                        JSONObject jsonObj = (new JSONObject(jsonStr)).getJSONObject("rates");

                        String[] rate = new String[currencyNames.length];

                        for (int j = 0; j < currencyNames.length; j++) {
                            rate[j] = jsonObj.getString(currencyNames[j]);
                        }

                        for (int j = 0; j < rate.length; j++) {
                            map = new HashMap<>();
                            map.put(ATTR_CUR_NAME, currencyNames[j]);
                            map.put(ATTR_EX_RATE, rate[j]);
                            exchangeData.add(map);

                            cv = new ContentValues();
                            cv.put(ATTR_CUR_NAME, currencyNames[j]);
                            cv.put(ATTR_EX_RATE, rate[j]);
                            db.insert("mytable", null, cv);

                            sPref = getPreferences(MODE_PRIVATE);
                            SharedPreferences.Editor ed = sPref.edit();
                            ed.putString(TIMESTAMP, Long.toString(System.currentTimeMillis()));
                            ed.commit();
                        }

                        //}
                    } catch (final JSONException e) {
                        Log.e(TAG, "Json parsing error: " + e.getMessage());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),
                                        "Json parsing error: " + e.getMessage(),
                                        Toast.LENGTH_LONG)
                                        .show();
                            }
                        });

                    }
                } else {
                    Log.e(TAG, "Couldn't get json from server.");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Couldn't get json from server. Check LogCat for possible errors!",
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                    });

                }
            } else {

                Log.d(TAG, "Data from local DB");

                Cursor c = db.query("mytable", null, null, null, null, null, null);

                if (c.moveToFirst()) {

                    int curColIndex = c.getColumnIndex(ATTR_CUR_NAME);
                    int rateColIndex = c.getColumnIndex(ATTR_EX_RATE);

                    do {
                        map = new HashMap<>();
                        map.put(ATTR_CUR_NAME, c.getString(curColIndex));
                        map.put(ATTR_EX_RATE, c.getString(rateColIndex));
                        exchangeData.add(map);
                    } while (c.moveToNext());
                }
                else
                    Log.d(TAG, "0 rows");
                c.close();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (pDialog.isShowing())
                pDialog.dismiss();

            ListAdapter adapter = new SimpleAdapter(
                    MainActivity.this, exchangeData,
                    R.layout.item, new String[]{ATTR_CUR_NAME, ATTR_EX_RATE},
                    new int[]{R.id.tvCurName, R.id.tvExRate});

            lv.setAdapter(adapter);
        }

    }

    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            // конструктор суперкласса
            super(context, "myDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "--- onCreate database ---");
            // создаем таблицу с полями
            db.execSQL("create table mytable ("
                    + "id integer primary key autoincrement,"
                    + ATTR_CUR_NAME + " text,"
                    + ATTR_EX_RATE + " float" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}