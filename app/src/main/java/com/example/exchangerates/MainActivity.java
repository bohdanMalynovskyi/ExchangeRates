package com.example.exchangerates;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

    private static final String ATTR_TIMESTAMP = "timestamp";

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

        checkTimePreferenceExisting();

        lv = findViewById(R.id.lv);
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
        intent.putExtra(GlobalVariables.ATTR_CUR_NAME, exchangeRates.get(position).get(GlobalVariables.ATTR_CUR_NAME));
        startActivity(intent);
    }

    private void checkTimePreferenceExisting() {
        sPref = getPreferences(MODE_PRIVATE);
        if (!sPref.contains(ATTR_TIMESTAMP)) {
            SharedPreferences.Editor ed = sPref.edit();
            ed.putString(ATTR_TIMESTAMP, "0");
            ed.commit();
        }
    }

    private void loadRatesFromWebService() {
        NetworkService.getInstance()
                .getJSONApi()
                .getRateByBaseCurrency(GlobalVariables.BASE_CURRENCY)
                .enqueue(new Callback<RateByBaseCurrency>() {
                    @Override
                    public void onResponse(@NonNull Call<RateByBaseCurrency> call, @NonNull Response<RateByBaseCurrency> response) {
                        Map<String, String> map = response.body().getRates();
                        List<String> mapKeysList = new ArrayList<String>(map.keySet());
                        List<String> mapValuesList = new ArrayList<String>(map.values());

                        for (int i = 0; i < mapKeysList.size(); i++) {
                            map = new HashMap<>();
                            map.put(GlobalVariables.ATTR_CUR_NAME, mapKeysList.get(i));
                            map.put(GlobalVariables.ATTR_EX_RATE, mapValuesList.get(i));
                            exchangeRates.add(map);
                        }

                        adapter = new SimpleAdapter(
                                MainActivity.this, exchangeRates,
                                R.layout.item, new String[]{GlobalVariables.ATTR_CUR_NAME, GlobalVariables.ATTR_EX_RATE},
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
        Cursor cursor = db.query("mytable", null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            int curColIndex = cursor.getColumnIndex(GlobalVariables.ATTR_CUR_NAME);
            int rateColIndex = cursor.getColumnIndex(GlobalVariables.ATTR_EX_RATE);

            do {
                map = new HashMap<>();
                map.put(GlobalVariables.ATTR_CUR_NAME, cursor.getString(curColIndex));
                map.put(GlobalVariables.ATTR_EX_RATE, cursor.getString(rateColIndex));
                exchangeRates.add(map);
            } while (cursor.moveToNext());

            adapter = new SimpleAdapter(
                    MainActivity.this, exchangeRates,
                    R.layout.item, new String[]{GlobalVariables.ATTR_CUR_NAME, GlobalVariables.ATTR_EX_RATE},
                    new int[]{R.id.tvCurName, R.id.tvExRate});
            lv.setAdapter(adapter);
        } else
            Log.d(GlobalVariables.TAG, "0 rows");
        cursor.close();
    }

    private void saveRatesToDB(ArrayList<Map<String, String>> exchangeRates) {
        ContentValues cv;

        for (int i = 0; i < exchangeRates.size(); i++) {
            cv = new ContentValues();
            cv.put(GlobalVariables.ATTR_CUR_NAME, exchangeRates.get(i).get(GlobalVariables.ATTR_CUR_NAME));
            cv.put(GlobalVariables.ATTR_EX_RATE, exchangeRates.get(i).get(GlobalVariables.ATTR_EX_RATE));
            db.insert("mytable", null, cv);
        }
    }

    private void saveTimestamp() {
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString(ATTR_TIMESTAMP, Long.toString(System.currentTimeMillis()));
        ed.commit();

    }

    private long timeAfterLastRequest() {
        sPref = getPreferences(MODE_PRIVATE);
        int milsecInMin = 60 * 1000;

        long oldTimestamp = Long.parseLong(sPref.getString(ATTR_TIMESTAMP, ""));
        long newTimestamp = System.currentTimeMillis();

        return (newTimestamp - oldTimestamp) / milsecInMin;
    }

    private void connectDB() {
        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();
    }
}