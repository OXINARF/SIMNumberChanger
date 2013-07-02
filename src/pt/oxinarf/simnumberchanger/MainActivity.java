package pt.oxinarf.simnumberchanger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity implements android.content.DialogInterface.OnClickListener {
	
	private SharedPreferences prefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		prefs = getPreferences(Context.MODE_WORLD_READABLE);
		
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		String phone = tm.getLine1Number();
		
		setContentView(R.layout.activity_main);
		
		if(phone != null && !phone.isEmpty() && !phone.trim().equals("")) {
			TextView tv = (TextView) findViewById(R.id.actualNumber);
			tv.setText(phone);
			tv.setTypeface(null, Typeface.NORMAL);
		}
		
		String number = prefs.getString("number", "");
		EditText ev = (EditText) findViewById(R.id.newNumber);
		ev.setText(number);
		
		Button b = (Button) findViewById(R.id.changeButton);
		b.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				EditText ev = (EditText) findViewById(R.id.newNumber);
				String number = ev.getText().toString().trim();

				boolean saved = prefs.edit().putString("number", number).commit();
				
				if(saved) {
					showDialog(1);
				}
				else {
					showDialog(2);
				}
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch(id) {
		case 1:
			return new AlertDialog.Builder(this).setMessage(R.string.preferences_saved).setNeutralButton("Ok", this).create();
		case 2:
			return new AlertDialog.Builder(this).setMessage(R.string.preferences_not_saved).setNeutralButton("Ok", this).create();
		default:
			return null;
		}
	}

	@Override
	public void onClick(DialogInterface arg0, int arg1) {
		arg0.dismiss();
	}	
}