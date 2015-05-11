package com.seebye.messengerapi.api;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.seebye.messengerapi.api.constants.General;
import com.seebye.messengerapi.api.constants.SPKey;
import com.seebye.messengerapi.api.utils.LogUtils;
import com.seebye.messengerapi.api.utils.LuckyUtils;
import com.seebye.messengerapi.api.utils.PackageUtils;
import com.seebye.messengerapi.api.utils.SecureRandom;

/**
 * Created by Nico on 11.04.2015.
 */
public class App extends Application
{
	private static App s_instance;

	@Override
	public void onCreate()
	{
		super.onCreate();
		s_instance = this;

		new LuckyCheck().start();

		if(isModule())
		{
			if(MessengerAPI.isInstalled())
			{
				initializeModul();
			}
			else
			{
				// yeah we will wait for it to be installed..
				InstallReceiver.register();
			}
		}
	}

	public boolean isModule() { return true; }

	public static App getInstance()
	{
		return s_instance;
	}

	/**
	 * This method should be used by the API only.
	 */
	public static sp getSPAPI()
	{
		return sp.SP(App.getInstance(), "smapi");
	}

	private void initializeModul()
	{
		if(!App.getSPAPI().getBool(SPKey.SETUP))
		{
			App.getSPAPI().set(SPKey.SETUP, true);
			setup();
		}

		LogUtils.i("request secret..");
		App.getSPAPI().set(SPKey.SECRET_REQUEST_ID, MessengerAPI.requestSecret().send().getID());
	}

	private void setup()
	{
		// we're going to set the last broadcast id to a random value
		// worst case scenario: Long.MAX_VALUE-Integer.MAX_VALUE = 9223372036854775807-2147483647 = 9 223 372 034 707 292 160 broadcast ids
		// aim = protect modules from fake broadcasts - example pass wrong secret to stop the app from working
		App.getSPAPI().set(SPKey.LAST_BROADCAST_ID, SecureRandom.get(Integer.MIN_VALUE, Integer.MAX_VALUE));
	}

	private static class InstallReceiver extends BroadcastReceiver
	{
		public static void register()
		{
			try
			{
				IntentFilter filter = new IntentFilter();
				filter.addAction(Intent.ACTION_PACKAGE_ADDED);
				filter.addDataScheme("package");
				App.getInstance().registerReceiver(new InstallReceiver(), filter);
			}
			catch(Exception e) {}
		}

		@Override
		public void onReceive(Context context, Intent intent)
		{
			String strPackage = intent.getDataString().replaceAll("package:", "");

			if(intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)
					&& General.PKG_MESSENGERAPI.equals(strPackage))
			{
				// API installed - let's initialize the module
				// sending explicit (more specific than the used one in the API class) broadcast to start the API
				Intent i = new Intent(General.ACTION_MESSENGERAPI);
				i.setComponent(PackageUtils.getBroadcastWithAction(General.PKG_MESSENGERAPI, General.ACTION_MESSENGERAPI));
				context.sendBroadcast(i);

				App.getInstance().initializeModul();
				context.unregisterReceiver(this);
			}
		}
	}

	// we don't know the duration to search after LP in the log file so we're going to do it inside a thread
	private class LuckyCheck extends Thread
	{
		@Override
		public void run()
		{
			LuckyUtils.checkXposedLog();
		}
	}
}
