package pt.oxinarf.simnumberchanger;

import java.io.File;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.telephony.TelephonyManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {
	
	private static SharedPreferences prefs;
	
	private static final String MY_PACKAGE_NAME = XposedMod.class.getPackage().getName();
	
	private static Object phone;
	
	private static String number;
	
	private static TelephonyManager tm;

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals("com.android.phone")) {
			XposedHelpers.findAndHookMethod("com.android.phone.PhoneApp", lpparam.classLoader, "onCreate", new XC_MethodHook() {

				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					File prefFile = new File(Environment.getDataDirectory(), "data/" + MY_PACKAGE_NAME + "/shared_prefs/" + "MainActivity.xml");
					
					if (prefFile.exists())
						prefs = AndroidAppHelper.getSharedPreferencesForPackage(MY_PACKAGE_NAME, "MainActivity", Context.MODE_PRIVATE);
					else
						return;
					
					number = prefs.getString("number", null);
					
					if(number == null)
						return;
					
					phone = XposedHelpers.getObjectField(param.thisObject, "phone");
					
					if(phone == null) {
						XposedBridge.log("Phone was null, it wasn't supposed!");
						return;
					}
					
					tm = (TelephonyManager) ((Application) param.thisObject).getSystemService("phone");
					
					new Thread(new Runnable() {
				        public void run() {
				        	
				        	for(int simState = tm.getSimState(); simState != TelephonyManager.SIM_STATE_READY; simState = tm.getSimState()) {
				        		try {
				        			Thread.sleep(5000);
				        		} catch (InterruptedException e) {}
				        	}

				        	String tag = (String) XposedHelpers.callMethod(phone, "getLine1AlphaTag");
					
				        	if(tag == null || tag.isEmpty() || tag.trim().equals("")) {
				        		tag = "Line 1";
				        		XposedBridge.log("Tag was empty - Line 1 now");
				        	}

				        	String actualNumber = (String) XposedHelpers.callMethod(phone, "getLine1Number");
					
				        	XposedBridge.log("Actual Number is: " + actualNumber);
					
				        	if(number.equals(actualNumber))
				        		return;
					
				        	XposedBridge.log("Going to write SIM number (" + number + ") now!");
					
				        	XposedHelpers.callMethod(phone, "setLine1Number", tag, number, null);
				        }
					}).start();
				}
			});
		}
	}
}