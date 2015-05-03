package com.seebye.messengerapi.api.utils;

import android.util.Log;

import com.seebye.messengerapi.api.BuildConfig;

/**
 * Created by Nico on 03.05.2015.
 */
public class LogUtils
{
	public static void i(String strText, Object... aArgs)
	{
		if(BuildConfig.DEBUG)
		{
			Log.i("MAPIM", String.format(strText, aArgs));
		}
	}
}
