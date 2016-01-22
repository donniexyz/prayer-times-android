package com.metinkale.prayerapp.vakit;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.metinkale.prayer.R;
import com.metinkale.prayerapp.BaseActivity;
import com.metinkale.prayerapp.PermissionUtils;
import com.metinkale.prayerapp.vakit.times.CalcTimes;
import com.metinkale.prayerapp.vakit.times.Cities;
import com.metinkale.prayerapp.vakit.times.Cities.Item;
import com.metinkale.prayerapp.vakit.times.Times;
import com.metinkale.prayerapp.vakit.times.WebTimes;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AddCity extends BaseActivity implements OnItemClickListener, OnQueryTextListener, LocationListener {
    private Executor mExecutor = Executors.newSingleThreadExecutor();
    private MyAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.vakit_addcity);

        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setFastScrollEnabled(true);
        listView.setOnItemClickListener(this);
        mAdapter = new MyAdapter(this);
        listView.setAdapter(mAdapter);

        TextView legacy = (TextView) findViewById(R.id.legacySwitch);
        legacy.setText(R.string.oldAddCity);
        legacy.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                AddCity.this.finish();
                startActivity(new Intent(AddCity.this, AddCityLegacy.class));

            }

        });

        checkLocation();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (PermissionUtils.get(this).pLocation) checkLocation();
    }

    public void checkLocation() {
        if (PermissionUtils.get(this).pLocation) {
            LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            Location loc = null;
            List<String> providers = lm.getProviders(true);
            for (String provider : providers) {
                Location last = lm.getLastKnownLocation(provider);
                // one hour==1meter in accuracy
                if (last != null && (loc == null || last.getAccuracy() - last.getTime() / (1000 * 60 * 60) < loc.getAccuracy() - loc.getTime() / (1000 * 60 * 60))) {
                    loc = last;
                }
            }

            if (loc != null)
                onQueryTextSubmit(loc.getLatitude() + ";" + loc.getLongitude());

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_MEDIUM);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(false);
            criteria.setSpeedRequired(false);
            String provider = lm.getBestProvider(criteria, true);
            if (provider != null)
                lm.requestSingleUpdate(provider, this, null);

        } else {
            PermissionUtils.get(this).needLocation(this);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search, menu);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_search));

        searchView.performClick();
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long index) {
        final Cities.Item i = mAdapter.getItem(pos);
        switch (i.source) {
            case Calc:
                Bundle bdl = new Bundle();
                bdl.putString("city", i.city);
                bdl.putDouble("lat", i.lat);
                bdl.putDouble("lng", i.lng);
                CalcTimes.add(this, bdl);
                break;
            case NVC:
            case Diyanet:
            case Fazilet:
            case Semerkand:
            case IGMG:
                WebTimes.add(i.source, i.city, i.id, i.lat, i.lng);
                finish();
                break;
        }


    }

    @Override
    public boolean onQueryTextSubmit(final String query) {
        if (query.contains(";") && mAdapter.getCount() <= 1) {
            mAdapter.clear();
            Item item = new Item();
            item.city = "GPS";
            item.country = query;
            item.source = Times.Source.Calc;
            item.lat = Double.parseDouble(query.substring(0, query.indexOf(";")));
            item.lng = Double.parseDouble(query.substring(1 + query.indexOf(";")));
            mAdapter.add(item);
            mAdapter.notifyDataSetChanged();
        }
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final List<Cities.Item> items = Cities.search(query);
                AddCity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (items != null && !items.isEmpty()) {
                            mAdapter.clear();
                            mAdapter.addAll(items);
                        }
                        mAdapter.notifyDataSetChanged();

                    }
                });

            }
        });

        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        return false;
    }

    @Override
    public boolean setNavBar() {
        this.setNavBarColor(0xffeeeeee);
        return true;
    }

    @Override
    public void onLocationChanged(Location loc) {
        onQueryTextSubmit(loc.getLatitude() + ";" + loc.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    class MyAdapter extends ArrayAdapter<Cities.Item> {

        public MyAdapter(Context context) {
            super(context, 0, 0);

        }

        @Override
        public void addAll(Collection<? extends Item> collection) {
            super.addAll(collection);
            sort(new Comparator<Item>() {

                @Override
                public int compare(Item i0, Item i1) {
                    return i0.source.ordinal() - i1.source.ordinal();
                }

            });
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh;
            if (convertView == null) {
                convertView = View.inflate(parent.getContext(), R.layout.vakit_addcity_row, null);
                vh = new ViewHolder();
                vh.city = (TextView) convertView.findViewById(R.id.city);
                vh.country = (TextView) convertView.findViewById(R.id.country);
                vh.sourcetxt = (TextView) convertView.findViewById(R.id.sourcetext);
                vh.source = (ImageView) convertView.findViewById(R.id.source);
                // vh.internetdsc = (TextView) convertView
                // .findViewById(R.id.internet_dsc);

                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            Cities.Item i = getItem(position);
            vh.city.setText(i.city);
            vh.country.setText(i.country);

            vh.sourcetxt.setText(i.source.text);
            if (i.source.resId != 0) {
                vh.source.setImageResource(i.source.resId);
                vh.source.setVisibility(View.VISIBLE);
            } else {
                vh.source.setVisibility(View.GONE);
            }
            return convertView;
        }

        class ViewHolder {
            TextView country, city, sourcetxt, internetdsc;
            ImageView source, internet;
        }

    }

}