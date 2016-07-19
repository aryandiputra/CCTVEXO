package id.ryandzhunter.cctvexo;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import id.ryandzhunter.cctvexo.model.Busway;

public class ListCCTVActivity extends AppCompatActivity {

    private CityAdapter mAdapter;
    private ArrayList<Busway> object;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_cctv);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        try {
            InputStream stream = getAssets().open("json_bus.json");
            object = load(stream, Busway.class);

            mAdapter = new CityAdapter(this);
            mAdapter.setData(object);

            mListView = (ListView) findViewById(R.id.cities_list);
            mListView.setAdapter(mAdapter);
            final List<Busway> finalCities = object;
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent i = new Intent(ListCCTVActivity.this, MainActivity.class);
                    i.putExtra("url", finalCities.get(position).getCctvUrl());
                    startActivity(i);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    public static ArrayList<Busway> load(final InputStream inputStream, final Class clazz) {
        try {
            if (inputStream != null) {
                final Gson gson = new Gson();
                final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//                return gson.fromJson(reader, clazz);
                Type listType = new TypeToken<List<String>>() {}.getType();
                listType = new TypeToken<List<Busway>>(){}.getType();
                return gson.fromJson(reader,
                        listType
//                        new TypeToken<List<Class<T>>>(){}.getType()
                );
            }
        } catch (final Exception e) {
        }
        return null;
    }


}
