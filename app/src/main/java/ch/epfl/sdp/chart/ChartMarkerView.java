package ch.epfl.sdp.chart;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;

import ch.epfl.sdp.R;

public class ChartMarkerView extends MarkerView {

    private TextView textView;

    public ChartMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        textView = findViewById(R.id.markerText);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e instanceof CandleEntry) {
            CandleEntry ce = (CandleEntry) e;
            textView.setText(Utils.formatNumber(ce.getHigh(), 0, true));
        } else {
            textView.setText(Utils.formatNumber(e.getY(), 0, true));
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }
}