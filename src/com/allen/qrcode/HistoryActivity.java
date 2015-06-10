package com.allen.qrcode;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.allen.myqrcode.R;
import com.allen.qrcode.db.DatabaseUtil;

public class HistoryActivity extends Activity {
	private ListView listView;
	private TextView resultTV, timeTV;
	private Button button_delete;
	private SimpleCursorAdapter sca;
	private Cursor cursor;
	private DatabaseUtil dbUtil;
	private int item_id, myposition = 0;
	private boolean vibrate;// Υπ¶―

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		dbUtil = new DatabaseUtil(this);
		dbUtil.open();
		setContentView(R.layout.activity_history);
		listView = (ListView) findViewById(R.id.listView1);
		resultTV = (TextView) findViewById(R.id.textView_result);
		timeTV = (TextView) findViewById(R.id.textView_time);
		button_delete = (Button) findViewById(R.id.btn_delete);
		button_delete.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				playVibrate();
				dbUtil.deleteAllLocation();

				cursor = dbUtil.fetchAllLocation();

				sca = new SimpleCursorAdapter(HistoryActivity.this,
						R.layout.history_items, cursor, new String[] { "time",
								"result" }, new int[] { R.id.textView_time,
								R.id.textView_result });

				listView.setAdapter(sca);
			}
		});

		cursor = dbUtil.fetchAllLocation();

		sca = new SimpleCursorAdapter(this, R.layout.history_items, cursor,
				new String[] { "time", "result" }, new int[] {
						R.id.textView_time, R.id.textView_result });

		listView.setAdapter(sca);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub

				TextView textView_result = (TextView) view
						.findViewById(R.id.textView_result);
				String result = textView_result.getText().toString();

				Intent intent = new Intent(HistoryActivity.this,
						ShowActivity.class);
				Bundle bundle = new Bundle();
				bundle.putString("msg", result);
				intent.putExtras(bundle);
				startActivity(intent);

			}
		});
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				item_id = (int) id;
				myposition = position;
				final String[] mItems = { "ΙΎ³ύ" };
				AlertDialog.Builder builder = new AlertDialog.Builder(
						HistoryActivity.this);
				builder.setItems(mItems, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// TODO Auto-generated method stub
						dbUtil.deleteLocation(item_id);

						cursor = dbUtil.fetchAllLocation();

						sca = new SimpleCursorAdapter(HistoryActivity.this,
								R.layout.history_items, cursor, new String[] {
										"time", "result" }, new int[] {
										R.id.textView_time,
										R.id.textView_result });

						listView.setAdapter(sca);
					}
				});
				builder.create().show();

				return true;
			}
		});

	}

	private static final long VIBRATE_DURATION = 50;

	private void playVibrate() {

		Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		vibrator.vibrate(VIBRATE_DURATION);

	}

}
