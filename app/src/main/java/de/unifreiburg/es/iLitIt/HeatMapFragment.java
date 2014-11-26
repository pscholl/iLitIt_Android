package de.unifreiburg.es.iLitIt;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.util.ArrayList;

/**
 * Created by phil on 11/26/14.
 */
public class HeatMapFragment extends SupportMapFragment {
    private static final String TAG = HeatMapFragment.class.getCanonicalName();
    private HeatmapTileProvider mProvider;
    private TileOverlay mOverlay;
    private static ObservableLinkedList<CigaretteEvent> mModel;
    private static GoogleMap mMap;

    DelayedObserver rUpdateView = new DelayedObserver(500, new Runnable() {
        @Override
        public void run() {
            ArrayList<LatLng> latlng = new ArrayList<LatLng>();
            for (CigaretteEvent e : mModel)
                if (e.where != null)
                    latlng.add(new LatLng(e.where.getLatitude(), e.where.getLongitude()));

            if (latlng.size() == 0)
                return;

            if (mProvider == null) {
                mProvider = new HeatmapTileProvider.Builder().data(latlng).build();
                mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
            } else {
                mProvider.setData(latlng);
            }

            Log.d(TAG, "updateview");
            mOverlay.clearTileCache();
        }
    });

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mModel.unregister(rUpdateView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        if (((MainActivity) getActivity()).getModel() != null)
            mModel = ((MainActivity) getActivity()).getModel();

        if (mModel==null)
            return v;

        mModel.register(rUpdateView);

        if (getMap()!=null)
            mMap = getMap();

        rUpdateView.mAction.run();

        if (mModel.size() > 2) {
            getMap().setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    double max_lat=Double.NEGATIVE_INFINITY, max_lon=Double.NEGATIVE_INFINITY,
                            min_lat=Double.POSITIVE_INFINITY, min_lon=Double.POSITIVE_INFINITY;

                    Log.d(TAG, "onchange");

                    for (CigaretteEvent e: mModel) {
                        double lat = e.where.getLatitude(),
                                lon = e.where.getLongitude();

                        if (lat > max_lat)       max_lat = lat;
                        else if (lat < min_lat)  min_lat = lat;
                        if (lon > max_lon)       max_lon = lon;
                        else if (lon < min_lon)  min_lon = lon;
                    }

                    LatLng sw = new LatLng(min_lat, min_lon),
                            ne = new LatLng(max_lat, max_lon);

                    mMap.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(new LatLngBounds(sw,ne), 150));
                    mMap.setOnCameraChangeListener(null);
                }
            });
        }


        return v;
    }
}
