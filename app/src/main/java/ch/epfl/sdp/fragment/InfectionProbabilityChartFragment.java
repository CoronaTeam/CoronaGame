package ch.epfl.sdp.fragment;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import ch.epfl.sdp.R;
import ch.epfl.sdp.location.LocationService;

import static android.content.Context.BIND_AUTO_CREATE;

public class InfectionProbabilityChartFragment extends Fragment implements OnChartValueSelectedListener {

    private View view;
    private LineChart chart;
    private LocationService service;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_infection_probability_chart, container, false);

        chart = view.findViewById(R.id.infectionProbabilityChart);

        initializeChart();

        connectService();

        return view;
    }

    private void connectService() {
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                InfectionProbabilityChartFragment.this.service = ((LocationService.LocationBinder)service).getService();
                updateData();
            }
            @Override
            public void onServiceDisconnected(ComponentName name) { service = null; }
        };
        getActivity().bindService(new Intent(getActivity(), LocationService.class), conn, BIND_AUTO_CREATE);
    }

    private void initializeChart() {
        setChartStyle();
        setAxes();
        setLimitLines();

        chart.animateX(1500);
    }

    private void setChartStyle() {
        chart.setBackgroundColor(Color.WHITE);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setOnChartValueSelectedListener(this);
        chart.setDrawGridBackground(false);

        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);

        chart.setScaleYEnabled(false);

        chart.getLegend().setEnabled(false);
    }

    private void setAxes() {
        ValueFormatter xAxisFormatter = new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis((long)value);
                int month = cal.get(Calendar.MONTH) + 1;
                return cal.get(Calendar.DAY_OF_MONTH) + "/" + (month < 10 ? "0" : "") + month;
            }
        };
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(xAxisFormatter);
        xAxis.setGranularity(1000 * 60 * 60 * 24);
        xAxis.setDrawGridLines(false);

        YAxis yAxis = chart.getAxisLeft();
        chart.getAxisRight().setEnabled(false);
        yAxis.setDrawLabels(false);
        yAxis.setDrawGridLines(false);
        yAxis.setAxisMaximum(1.0f);
        yAxis.setAxisMinimum(0.0f);
    }

    private void setLimitLines() {
        LimitLine ll1 = new LimitLine(1f, "Infected");
        ll1.setLineWidth(2f);
        ll1.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_BOTTOM);
        ll1.setLineColor(Color.RED);
        ll1.setTextSize(10f);

        LimitLine ll2 = new LimitLine(0f, "Not infected");
        ll2.setLineWidth(2f);
        ll2.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_TOP);
        ll2.setLineColor(Color.GREEN);
        ll2.setTextSize(10f);

        XAxis xAxis = chart.getXAxis();
        YAxis yAxis = chart.getAxisLeft();

        xAxis.setDrawLimitLinesBehindData(false);
        yAxis.setDrawLimitLinesBehindData(false);

        yAxis.addLimitLine(ll1);
        yAxis.addLimitLine(ll2);
    }

    private List<Entry> generateData() {
        if (service == null) throw new IllegalStateException();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        Date since = calendar.getTime();
        Map<Date, Float> infectionHistory = service.getAnalyst().getCarrier().getIllnessProbabilityHistory(since);

        ArrayList<Entry> values = new ArrayList<>();

        Drawable drawable = getResources().getDrawable(R.drawable.ic_person, getContext().getTheme());
        for (Map.Entry<Date, Float> entry : infectionHistory.entrySet()) {
            values.add(new Entry(entry.getKey().getTime(), entry.getValue(), drawable));
        }

        return values;
    }

    private void updateData() {

        List<Entry> data = generateData();

        if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
            updateExistingDataSet(data);
        } else {
            createNewDataSet(data);
        }
    }

    private void updateExistingDataSet(List<Entry> data) {
        LineDataSet set1 = (LineDataSet) chart.getData().getDataSetByIndex(0);
        set1.setValues(data);
        set1.notifyDataSetChanged();
        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
    }

    private void createNewDataSet(List<Entry> data) {
        LineDataSet set1 = new LineDataSet(data, "Infection Probability");
        set1.setDrawIcons(false);

        set1.setColor(Color.BLACK);
        set1.setCircleColor(Color.BLACK);
        set1.setDrawCircleHole(false);
        set1.setLineWidth(1f);
        set1.setCircleRadius(3f);
        set1.setFillColor(Color.BLACK);

        set1.setDrawValues(false);

        // smooth out curve
        set1.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

        set1.setDrawFilled(true);
        set1.setFillDrawable(ContextCompat.getDrawable(getContext(), R.drawable.chart_fade));

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);

        chart.setData(new LineData(dataSets));
        chart.invalidate();
    }



    @Override
    public void onValueSelected(Entry e, Highlight h) {

    }

    @Override
    public void onNothingSelected() {

    }
}
