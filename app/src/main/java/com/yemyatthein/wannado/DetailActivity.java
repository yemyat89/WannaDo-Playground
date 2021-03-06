package com.yemyatthein.wannado;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.yemyatthein.wannado.data.DataContract;

import java.text.SimpleDateFormat;


public class DetailActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DetailFragment extends Fragment {

        public DetailFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

            final TextView txtName = (TextView) rootView.findViewById(R.id.txtDetailTitle);
            final TextView txtDescription = (TextView) rootView.findViewById(R.id.txtDetailDescription);
            final TextView txtWarning = (TextView) rootView.findViewById(R.id.txtDetailWarning);
            final TextView txtStatsCreatedOn = (TextView) rootView.findViewById(
                    R.id.txtStatsCreatedOn);
            final TextView txtStatsFocusCount = (TextView) rootView.findViewById(
                    R.id.txtStatsFocusCount);
            final TextView txtStatsFocusingSince = (TextView) rootView.findViewById(
                    R.id.txtStatsFocusingSince);

            Bundle b = getActivity().getIntent().getExtras();

            final String nameString = b.getString("name", "");
            String descriptionString = b.getString("description", "");
            final long thingId = b.getLong("id", -1L);
            final int isCurrent = b.getInt("isCurrent", 0);
            final long createdDate = b.getLong("createdDate");

            Log.i("YMT-Inserted", "Got back " + createdDate);

            SimpleDateFormat monthDayFormat = new SimpleDateFormat("dd MMMM yyyy");
            String monthDayString = monthDayFormat.format(createdDate);

            txtName.setText(nameString);
            txtDescription.setText(descriptionString);
            txtStatsCreatedOn.setText(txtStatsCreatedOn.getText().toString() + monthDayString);

            // Count # of Focus
            Cursor cursorExpress = getActivity().getContentResolver().query(
                    DataContract.ExpressEntry.CONTENT_URI,
                    null, DataContract.ExpressEntry.COLUMN_THING_ID + " = ? AND " +
                            DataContract.ExpressEntry.COLUMN_TYPE + " = ? ",
                    new String[] {String.valueOf(thingId), String.valueOf(1)},
                    DataContract.ExpressEntry.COLUMN_DATE + " ASC");
            int focusCount = cursorExpress.getCount();

            txtStatsFocusCount.setText(txtStatsFocusCount.getText().toString() +
                    focusCount + " time(s).");

            // Focusing Since value

            if (isCurrent == 1 && focusCount > 0) {
                txtStatsFocusingSince.setVisibility(View.VISIBLE);
                cursorExpress.moveToLast();
                long date = cursorExpress.getLong(2);

                SimpleDateFormat monthDayFormat2 = new SimpleDateFormat("dd MMMM yyyy");
                String monthDayString2 = monthDayFormat2.format(date);
                txtStatsFocusingSince.setText(txtStatsFocusingSince.getText().toString() +
                        monthDayString2);

            }
            else {
                txtStatsFocusingSince.setVisibility(View.GONE);
            }

            final Button btnSwitchFocus = (Button) rootView.findViewById(R.id.btnSwitchFocus);
            btnSwitchFocus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Time time = new Time();
                    time.setToNow();
                    long timeNow = time.toMillis(true);

                    if (thingId == -1L) {
                        return;
                    }

                    // Remove current focus

                    Cursor cursorOld = getActivity().getContentResolver().query(
                            DataContract.ThingEntry.CONTENT_URI,
                            null, DataContract.ThingEntry.COLUMN_IS_CURRENT + " = ?",
                            new String[] {String.valueOf(1)}, null);
                    if (cursorOld.moveToFirst()) {
                        Log.i("YMT", "Old removed");
                        long thingIdOld = cursorOld.getLong(0);
                        int ctouchNowOld = cursorOld.getInt(5);
                        ContentValues contentValuesOld = Utils.convertCursorRowToContentValThing(
                                cursorOld);
                        contentValuesOld.put(DataContract.ThingEntry.COLUMN_IS_CURRENT, 0);

                        int rowsAffectedOld = getActivity().getContentResolver().update(
                                DataContract.ThingEntry.CONTENT_URI,
                                contentValuesOld,
                                DataContract.ThingEntry.TABLE_NAME + "." +
                                        DataContract.ThingEntry._ID + " = ?",
                                new String[] {String.valueOf(thingIdOld)});

                        assert(rowsAffectedOld == 1);

                        // Record departure in Express

                        ContentValues contentValues = new ContentValues();
                        contentValues.put(DataContract.ExpressEntry.COLUMN_THING_ID, thingIdOld);
                        contentValues.put(DataContract.ExpressEntry.COLUMN_DATE, timeNow);
                        contentValues.put(DataContract.ExpressEntry.COLUMN_TYPE, 0);
                        contentValues.put(DataContract.ExpressEntry.COLUMN_CTOUCH_ATM, ctouchNowOld);

                        getActivity().getContentResolver().insert(
                                DataContract.ExpressEntry.CONTENT_URI, contentValues);
                        Log.i("YMT", "Old value departure recorded.");
                    }


                    // Set new focus

                    Cursor cursor = getActivity().getContentResolver().query(
                            DataContract.ThingEntry.buildThingUri(thingId),
                            ThingFragment.THING_COLUMNS, null, null, null);
                    if (!cursor.moveToFirst()) {
                        Log.i("YMT-", "No cursor for " + thingId);
                        return;
                    }
                    int ctouchNowNew = cursor.getInt(5);
                    ContentValues contentValues = Utils.convertCursorRowToContentValThing(cursor);
                    contentValues.put(DataContract.ThingEntry.COLUMN_IS_CURRENT, 1);

                    int rowsAffected = getActivity().getContentResolver().update(
                            DataContract.ThingEntry.CONTENT_URI,
                            contentValues,
                            DataContract.ThingEntry.TABLE_NAME + "." +
                                    DataContract.ThingEntry._ID + " = ?",
                            new String[] {String.valueOf(thingId)});

                    assert(rowsAffected == 1);

                    txtWarning.setVisibility(View.VISIBLE);
                    btnSwitchFocus.setVisibility(View.GONE);

                    // Record entry in Express

                    ContentValues expressContentValues = new ContentValues();
                    expressContentValues.put(DataContract.ExpressEntry.COLUMN_THING_ID, thingId);
                    expressContentValues.put(DataContract.ExpressEntry.COLUMN_DATE, timeNow);
                    expressContentValues.put(DataContract.ExpressEntry.COLUMN_TYPE, 1);
                    expressContentValues.put(DataContract.ExpressEntry.COLUMN_CTOUCH_ATM,
                            ctouchNowNew);

                    getActivity().getContentResolver().insert(
                            DataContract.ExpressEntry.CONTENT_URI, expressContentValues);


                    Toast.makeText(getActivity(), "This has become your current focus now.",
                            Toast.LENGTH_SHORT).show();
                }
            });

            if (isCurrent == 0) {
                txtWarning.setVisibility(View.GONE);
            }
            else {
                btnSwitchFocus.setVisibility(View.GONE);
            }

            return rootView;
        }
    }
}
