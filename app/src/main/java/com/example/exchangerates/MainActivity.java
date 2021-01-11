package com.example.exchangerates;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    final String ATTR_CUR_NAME = "curName";
    final String ATTR_EX_RATE = "exRate";
    final String TIMESTAMP = "timestamp";
    final String BASE_CURRENCY = "USD";

    private String TAG = "myLogs";

    ListView lv;
    SharedPreferences sPref;
    DBHelper dbHelper;
    SQLiteDatabase db;
    ListAdapter adapter;
    ArrayList<Map<String, String>> exchangeRates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // COMMIT after completing
        //
        //

        checkTimePreferenceExisting();

        lv = (ListView) findViewById(R.id.lv);
        lv.setOnItemClickListener(this);

        connectDB();

        if (timeAfterLastRequest() >= 10) {
            loadRatesFromWebService();
        } else {
            loadRatesFromDB();
        }

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(MainActivity.this, SelectedCurrency.class);
        intent.putExtra(ATTR_CUR_NAME, exchangeRates.get(position).get(ATTR_CUR_NAME));
        startActivity(intent);
    }

    private void checkTimePreferenceExisting() {
        sPref = getPreferences(MODE_PRIVATE);
        if (!sPref.contains(TIMESTAMP)) {
            SharedPreferences.Editor ed = sPref.edit();
            ed.putString(TIMESTAMP, "0");
            ed.commit();
        }
    }

    private void loadRatesFromWebService() {

        Log.d(TAG, "Data from web service");

        NetworkService.getInstance()
                .getJSONApi()
                .getRateByBaseCurrency(BASE_CURRENCY)
                .enqueue(new Callback<RateByBaseCurrency>() {
                    @Override
                    public void onResponse(@NonNull Call<RateByBaseCurrency> call, @NonNull Response<RateByBaseCurrency> response) {
                        Map<String, String> map = response.body().getRates();
                        List<String> list1 = new ArrayList<String>(map.keySet());
                        List<String> list2 = new ArrayList<String>(map.values());

                        for (int i = 0; i < list1.size(); i++) {
                            map = new HashMap<>();
                            map.put(ATTR_CUR_NAME, list1.get(i));
                            map.put(ATTR_EX_RATE, list2.get(i));
                            exchangeRates.add(map);
                        }

                        adapter = new SimpleAdapter(
                                MainActivity.this, exchangeRates,
                                R.layout.item, new String[]{ATTR_CUR_NAME, ATTR_EX_RATE},
                                new int[]{R.id.tvCurName, R.id.tvExRate});
                        lv.setAdapter(adapter);

                        saveRatesToDB(exchangeRates);
                        saveTimestamp();
                    }

                    @Override
                    public void onFailure(@NonNull Call<RateByBaseCurrency> call, @NonNull Throwable t) {

                    }
                });
    }

    private void loadRatesFromDB() {
        Map<String, String> map;
        Cursor c = db.query("mytable", null, null, null, null, null, null);

        Log.d(TAG, "Data from local DB");

        if (c.moveToFirst()) {

            int curColIndex = c.getColumnIndex(ATTR_CUR_NAME);
            int rateColIndex = c.getColumnIndex(ATTR_EX_RATE);

            do {
                map = new HashMap<>();
                map.put(ATTR_CUR_NAME, c.getString(curColIndex));
                map.put(ATTR_EX_RATE, c.getString(rateColIndex));
                exchangeRates.add(map);
            } while (c.moveToNext());

            adapter = new SimpleAdapter(
                    MainActivity.this, exchangeRates,
                    R.layout.item, new String[]{ATTR_CUR_NAME, ATTR_EX_RATE},
                    new int[]{R.id.tvCurName, R.id.tvExRate});
            lv.setAdapter(adapter);
        } else
            Log.d(TAG, "0 rows");
        c.close();
    }

    private void saveRatesToDB(ArrayList<Map<String, String>> exchangeRates) {
        ContentValues cv;

        for (int i = 0; i < exchangeRates.size(); i++) {
            cv = new ContentValues();
            cv.put(ATTR_CUR_NAME, exchangeRates.get(i).get(ATTR_CUR_NAME));
            cv.put(ATTR_EX_RATE, exchangeRates.get(i).get(ATTR_EX_RATE));
            db.insert("mytable", null, cv);
        }
    }

    private void saveTimestamp() {
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString(TIMESTAMP, Long.toString(System.currentTimeMillis()));
        ed.commit();

    }

    private long timeAfterLastRequest() {
        sPref = getPreferences(MODE_PRIVATE);
        int milsecInMin = 60 * 1000;

        long oldTimestamp = Long.parseLong(sPref.getString(TIMESTAMP, ""));
        long newTimestamp = System.currentTimeMillis();

        return (newTimestamp - oldTimestamp) / milsecInMin;
    }

    private void connectDB(){
        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();
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