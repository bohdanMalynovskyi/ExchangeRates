package com.example.exchangerates;

import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.LineGraphView;
//import com.jjoe64.graphview.LineGraphSeries;



import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

public class SelectedCurrency extends AppCompatActivity {

    final String ATTR_CUR_NAME = "curName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selected_currency);

        Intent intent = getIntent();
        String curName = intent.getStringExtra(ATTR_CUR_NAME);

        GraphView graphView  = (GraphView) findViewById(R.id.graph);

  //      LineGra

//        GraphViewSeries exampleSeries = new GraphViewSeries(
//                new GraphViewData[] { new GraphViewData(1, 3.0d),
//                        new GraphViewData(2, 1.5d), new GraphViewData(3, 2.5d),
//                        new GraphViewData(4, 1.0d), new GraphViewData(5, 1.3d) });
//
//        //GraphView graphView = new BarGraphView(this, "График каких-то данных");
//        GraphView graphView  = (GraphView) findViewById(R.id.graph);
//        graphView.addSeries(exampleSeries);

//        LinearLayout layout = (LinearLayout) findViewById(R.id.layoutSelCur);
//        layout.addView(graphView);

    }
}