//******************************************************************************
// SCICHART® Copyright SciChart Ltd. 2011-2017. All rights reserved.
//
// Web: http://www.scichart.com
// Support: support@scichart.com
// Sales:   sales@scichart.com
//
// FifoChartsFragment.java is part of the SCICHART® Examples. Permission is hereby granted
// to modify, create derivative works, distribute and publish any part of this source
// code whether for commercial, private or personal use.
//
// The SCICHART® examples are distributed in the hope that they will be useful, but
// without any warranty. It is provided "AS IS" without warranty of any kind, either
// expressed or implied.
//******************************************************************************

package com.scichart.scichartonraspberry.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.scichart.charting.model.dataSeries.IXyDataSeries;
import com.scichart.charting.visuals.SciChartSurface;
import com.scichart.charting.visuals.axes.AutoRange;
import com.scichart.charting.visuals.axes.NumericAxis;
import com.scichart.charting.visuals.axes.DateAxis;
import com.scichart.charting.visuals.renderableSeries.IRenderableSeries;
import com.scichart.core.framework.UpdateSuspender;
import com.scichart.data.model.ISciList;
import com.scichart.scichartonraspberry.R;
import com.scichart.scichartonraspberry.fragments.base.ExampleBaseFragment;
import com.scichart.scichartonraspberry.utils.widgetgeneration.ImageViewWidget;
import com.scichart.scichartonraspberry.utils.widgetgeneration.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;

public class FifoChartsFragment extends ExampleBaseFragment {

    private static boolean fakeHardwareSensor = false;

    private final static String TAG = "scichart-wiringPi";

    private final static int FIFO_CAPACITY = 500;
    private final static long TIME_INTERVAL = 500;
    private final static double ONE_OVER_TIME_INTERVAL = 1.0 / TIME_INTERVAL;
    private final static double VISIBLE_RANGE_MAX = FIFO_CAPACITY * ONE_OVER_TIME_INTERVAL;
    private final static double GROW_BY = VISIBLE_RANGE_MAX * 0.1;

    private Random random = new Random();

    private final IXyDataSeries<Date, Double> ds1 = sciChartBuilder.newXyDataSeries(Date.class, Double.class).withFifoCapacity(FIFO_CAPACITY).build();

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> schedule;

    @BindView(R.id.chart)
    SciChartSurface surface;

//    private final DateRange xVisibleRange = new DateRange(new Date(),  VISIBLE_RANGE_MAX + GROW_BY);

    private volatile boolean isRunning = true;

    @Override
    public List<Widget> getToolbarItems() {
        return new ArrayList<Widget>() {
            {
                add(new ImageViewWidget.Builder().setId(R.drawable.example_toolbar_play).setListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isRunning = true;
                    }
                }).build());
                add(new ImageViewWidget.Builder().setId(R.drawable.example_toolbar_pause).setListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isRunning = false;
                    }
                }).build());
                add(new ImageViewWidget.Builder().setId(R.drawable.example_toolbar_stop).setListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isRunning = false;
                        resetChart();
                    }
                }).build());
            }
        };
    }

    @Override
    protected int getLayoutId() {
        return R.layout.example_single_chart_fragment;
    }

    @Override
    protected void initExample() {
        if (!fakeHardwareSensor  && bme280_begin() == -1) {
            Log.e(TAG, "Unable to connect BME 280. Entering fail-safe presentation mode...");
            fakeHardwareSensor = true;
        }

        final DateAxis xAxis = sciChartBuilder.newDateAxis()/*.withVisibleRange(xVisibleRange)*/.withAutoRangeMode(AutoRange.Never).build();
        final NumericAxis yAxis = sciChartBuilder.newNumericAxis().withGrowBy(0.1d, 0.1d).withAutoRangeMode(AutoRange.Always).build();

        final IRenderableSeries rs = sciChartBuilder.newLineSeries().withDataSeries(ds1).withStrokeStyle(0xFF4083B7, 2f, true).build();

        UpdateSuspender.using(surface, new Runnable() {
            @Override
            public void run() {
                Collections.addAll(surface.getXAxes(), xAxis);
                Collections.addAll(surface.getYAxes(), yAxis);
                Collections.addAll(surface.getRenderableSeries(), rs);
            }
        });

        schedule = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    return;
                }
                UpdateSuspender.using(surface, insertRunnable);
            }
        }, 0, TIME_INTERVAL, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        isRunning = false;

        outState.putDouble("time", t);
        outState.putParcelable("xValues1", ds1.getXValues());
        outState.putParcelable("yValues1", ds1.getYValues());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            t = savedInstanceState.getDouble("time");
            final ISciList<Date> xValues1 = savedInstanceState.getParcelable("xValues1");
            final ISciList<Double> yValues1 = savedInstanceState.getParcelable("yValues1");
            ds1.append(xValues1, yValues1);
        }
    }

    double t = 0;
    private final Runnable insertRunnable = new Runnable() {
        @Override
        public void run() {
            double y1;

            if (fakeHardwareSensor) {
                y1 = 3.0 * Math.sin(((2 * Math.PI) * 1.4) * t) + random.nextDouble() * 0.5;
            } else {
                readyData();
                y1 = getTemperature() / 100.0;
                // getHumidity() / 1024.0;
                // getPressure() / 100.0;
                // getAltitude();
            }

            Date dateNow = new Date();
            ds1.append(dateNow, y1);

            t += ONE_OVER_TIME_INTERVAL;

//            if (t > VISIBLE_RANGE_MAX) {
//                xVisibleRange.setMinMax(xVisibleRange.getMin() + ONE_OVER_TIME_INTERVAL, xVisibleRange.getMax() + ONE_OVER_TIME_INTERVAL);
//            }
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (schedule != null) {
            schedule.cancel(true);
        }

        if (!fakeHardwareSensor) {
            bme280_end();
        }
    }

    private void resetChart() {
        UpdateSuspender.using(surface, new Runnable() {
            @Override
            public void run() {
                ds1.clear();
            }
        });
    }

    public native int bme280_begin();
    public native void bme280_end();
    public native void readyData();
    public native int getTemperature();
    public native int getPressure();
    public native int getHumidity();
    public native int getAltitude();

    static {
        if (!fakeHardwareSensor) {
            System.loadLibrary("wpi_android");
        }
    }
}