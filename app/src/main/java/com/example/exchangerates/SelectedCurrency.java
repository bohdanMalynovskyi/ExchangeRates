package com.example.exchangerates;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectedCurrency extends AppCompatActivity {

    final String ATTR_CUR_NAME = "curName";
    final String BASE_CURRENCY = "USD";
    String curName;

    private String TAG = "myLogs";

    Map<String, Map<String, String>> ratesByTimePeriod;
    TextView tvCurName;
    TextView tvDates;

    long milSecInDay = 1000*60*60*24;

    SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd");

    String yesterdayDate = formatter.format(new Date());
    String weekAgoDate = formatter.format(new Date(System.currentTimeMillis() - milSecInDay*7));


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selected_currency);

        Intent intent = getIntent();
        curName = intent.getStringExtra(ATTR_CUR_NAME);

        tvDates = (TextView) findViewById(R.id.tvDates);
        tvCurName = (TextView) findViewById(R.id.tvCurName);
        tvCurName.setText(curName);

        loadRatesFromWebService();
    }

    private void loadRatesFromWebService() {

        Log.d(TAG, "Data from web service");

        NetworkService.getInstance()
                .getJSONApi()
                .getRateByTimePeriod(weekAgoDate, yesterdayDate, curName, BASE_CURRENCY)
                .enqueue(new Callback<RateByTimePeriod>() {
                    @Override
                    public void onResponse(@NonNull Call<RateByTimePeriod> call, @NonNull Response<RateByTimePeriod> response) {
                        ratesByTimePeriod = response.body().getRates();
                        List<String> timePeriodDates = new ArrayList<>(ratesByTimePeriod.keySet());

                        //try to use enum class or smt else where fields have names (date, rate)

                        String[][] sortedRatesByTimePeriod = sortingOfRatesByTimePeriod(timePeriodDates);

                        graphBuilding(sortedRatesByTimePeriod);
                    }

                    @Override
                    public void onFailure(@NonNull Call<RateByTimePeriod> call, @NonNull Throwable t) {
                        Log.e(TAG, "onFailure");
                    }
                });
    }

    private String[][] sortingOfRatesByTimePeriod(List<String> ratesDates) {
        Date date1 = null;
        Date date2 = null;
        for (int i = ratesDates.size()-1; i >= 1; i--){

            for (int j = 0; j < i; j++){

                try {
                    date1 = formatter.parse(ratesDates.get(j));
                    date2 = formatter.parse(ratesDates.get(j + 1));
                }
                catch (Exception e){
                   Log.d(TAG, e.getMessage());
                }

                if(date1.after(date2)) {
                    String s = ratesDates.get(j);
                    ratesDates.set(j, ratesDates.get(j+1));
                    ratesDates.set(j+1, s);
                }
            }
        }

        String[][] ratesByDates = new String[ratesDates.size()][2];

        for (int i = 0; i < ratesByDates.length; i++) {
            ratesByDates[i][0] = ratesDates.get(i);
            ratesByDates[i][1] = ratesByTimePeriod.get(ratesDates.get(i)).get(curName);
        }

        return ratesByDates;
    }


    private void graphBuilding(String[][] graphData){

        GraphView graph = (GraphView) findViewById(R.id.graph);

        Date[] datesForGraph = new Date[graphData.length];

        try {
            for (int i = 0; i < graphData.length; i++) {
                datesForGraph[i] = formatter.parse(graphData[i][0]);
            }
        }
        catch (Exception e){
            Log.d(TAG, e.getMessage());
        }

        DataPoint[] dataPoints = new DataPoint[graphData.length];

        for (int i = 0; i < graphData.length; i++) {
            dataPoints[i] = new DataPoint(datesForGraph[i], Double.parseDouble(graphData[i][1]));
        }

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
        series.setDrawDataPoints(true);

        graph.addSeries(series);

        // set date label formatter
        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
        graph.getGridLabelRenderer().setNumHorizontalLabels(graphData.length);
        graph.getGridLabelRenderer().setHorizontalLabelsAngle(90);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(datesForGraph[0].getTime());
        graph.getViewport().setMaxX(datesForGraph[datesForGraph.length-1].getTime());

        tvDates.setText("Rates for " + graphData[0][0] + " - " + graphData[graphData.length-1][0]);
    }
}