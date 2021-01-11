package com.example.exchangerates;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

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

    private static final String ATTR_RATE_DATE = "date";

    String curName;

    Map<String, Map<String, String>> ratesByTimePeriod;
    TextView tvCurName;
    TextView tvDates;

    long milSecInDay = 1000 * 60 * 60 * 24;

    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

    String yesterdayDate = formatter.format(new Date());
    String weekAgoDate = formatter.format(new Date(System.currentTimeMillis() - milSecInDay * 7));


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selected_currency);

        Intent intent = getIntent();
        curName = intent.getStringExtra(GlobalVariables.ATTR_CUR_NAME);

        tvDates = findViewById(R.id.tvDates);
        tvCurName = findViewById(R.id.tvCurName);
        tvCurName.setText(curName);

        loadRatesFromWebService();
    }

    private void loadRatesFromWebService() {
        NetworkService.getInstance()
                .getJSONApi()
                .getRateByTimePeriod(weekAgoDate, yesterdayDate, curName, GlobalVariables.BASE_CURRENCY)
                .enqueue(new Callback<RateByTimePeriod>() {
                    @Override
                    public void onResponse(@NonNull Call<RateByTimePeriod> call, @NonNull Response<RateByTimePeriod> response) {
                        ratesByTimePeriod = response.body().getRates();
                        List<String> timePeriodDates = new ArrayList<>(ratesByTimePeriod.keySet());

                        ArrayList<Map<String, String>> sortedRatesByTimePeriod = sortingOfRatesByTimePeriod(timePeriodDates);

                        graphBuilding(sortedRatesByTimePeriod);
                    }

                    @Override
                    public void onFailure(@NonNull Call<RateByTimePeriod> call, @NonNull Throwable t) {
                        Log.e(GlobalVariables.TAG, "onFailure");
                    }
                });
    }

    private ArrayList<Map<String, String>> sortingOfRatesByTimePeriod(List<String> ratesDates) {
        Date comparedDate1 = null;
        Date comparedDate2 = null;
        for (int i = ratesDates.size() - 1; i >= 1; i--) {

            for (int j = 0; j < i; j++) {

                try {
                    comparedDate1 = formatter.parse(ratesDates.get(j));
                    comparedDate2 = formatter.parse(ratesDates.get(j + 1));
                } catch (Exception e) {
                    Log.e(GlobalVariables.TAG, e.getMessage());
                }

                if (comparedDate1.after(comparedDate2)) {
                    String s = ratesDates.get(j);
                    ratesDates.set(j, ratesDates.get(j + 1));
                    ratesDates.set(j + 1, s);
                }
            }
        }

        ArrayList<Map<String, String>> sortedRatesByDates = new ArrayList<>(ratesDates.size());
        Map map;
        for (int i = 0; i < ratesDates.size(); i++) {
            map = new HashMap();
            map.put(ATTR_RATE_DATE, ratesDates.get(i));
            map.put(GlobalVariables.ATTR_EX_RATE, ratesByTimePeriod.get(ratesDates.get(i)).get(curName));
            sortedRatesByDates.add(map);
        }

        return sortedRatesByDates;
    }

    private void graphBuilding(ArrayList<Map<String, String>> graphData) {

        GraphView graph = findViewById(R.id.graph);

        Date[] datesForGraph = new Date[graphData.size()];

        try {
            for (int i = 0; i < graphData.size(); i++) {
                datesForGraph[i] = formatter.parse(graphData.get(i).get(ATTR_RATE_DATE));
            }
        } catch (Exception e) {
            Log.e(GlobalVariables.TAG, e.getMessage());
        }

        DataPoint[] dataPoints = new DataPoint[graphData.size()];

        for (int i = 0; i < graphData.size(); i++) {
            dataPoints[i] = new DataPoint(datesForGraph[i], Double.parseDouble(graphData.get(i).get(GlobalVariables.ATTR_EX_RATE)));
        }

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoints);
        series.setDrawDataPoints(true);

        graph.addSeries(series);

        graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
        graph.getGridLabelRenderer().setNumHorizontalLabels(graphData.size());
        graph.getGridLabelRenderer().setHorizontalLabelsAngle(90);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(datesForGraph[0].getTime());
        graph.getViewport().setMaxX(datesForGraph[datesForGraph.length - 1].getTime());

        tvDates.setText("Rates for " + graphData.get(0).get(ATTR_RATE_DATE) + " - " + graphData.get(graphData.size() - 1).get(ATTR_RATE_DATE));
    }
}