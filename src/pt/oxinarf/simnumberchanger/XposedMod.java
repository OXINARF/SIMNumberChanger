package pt.oxinarf.simnumberchanger;

import java.io.File;

import android.content.Context;
import android.os.Environment;
import android.telephony.TelephonyManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMod implements IXposedHookLoadPackage {

	private final String MY_PACKAGE_NAME = XposedMod.class.getPackage().getName();

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (lpparam.packageName.equals("com.android.phone")) {
			Class<?> hookClass;

			try {
				hookClass = XposedHelpers.findClass("com.android.phone.PhoneGlobals", lpparam.classLoader);
			}
			catch (ClassNotFoundError e) {
				try {
					hookClass = XposedHelpers.findClass("com.android.phone.PhoneApp", lpparam.classLoader);
				}
				catch (ClassNotFoundError ex) {
					XposedBridge.log("Classes don't exist - version not supported");
					return;
				}
			}

			File prefFile = new File(Environment.getDataDirectory(), "data/" + MY_PACKAGE_NAME + "/shared_prefs/" + "MainActivity.xml");

			XSharedPreferences prefs;

			if (prefFile.exists())
				prefs = new XSharedPreferences(MY_PACKAGE_NAME, "MainActivity");
			else
				return;

			final String number = prefs.getString("number", null);

			if(number == null)
				return;

			XposedHelpers.findAndHookMethod(hookClass, "onCreate", new XC_MethodHook() {				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					final Object phone;

					try {
						phone = XposedHelpers.getObjectField(param.thisObject, "phone");
					}
					catch (NoSuchFieldError ex) {
						XposedBridge.log("Field 'phone' doesn't exist - version not supported");
						return;
					}

					if(phone == null) {
						XposedBridge.log("Phone was null, it wasn't supposed!");
						return;
					}

					final TelephonyManager tm = (TelephonyManager) ((Context) param.thisObject).getSystemService("phone");

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
								XposedBridge.log("Tag was empty - 'Line 1' now");
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