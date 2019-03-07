package org.ebookdroid.common.settings.books;

import android.content.Context;
import android.content.SharedPreferences.Editor;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.ExportSettingsManager;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.sh.MySharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class SharedDB {

	MySharedPreferences prefs;

	public SharedDB(Context context) {
		//prefs = context.getSharedPreferences(ExportSettingsManager.PREFIX_BOOKS, Context.MODE_PRIVATE);
		prefs =new MySharedPreferences(ExportSettingsManager.PREFIX_BOOKS);
	}

	public void load() {
		prefs.load();
	}

	public synchronized BookSettings getBookSettings(String fileName) {
		String json = prefs.getString("" + fileName.hashCode(), null);
		try {
			
		if (json == null) {
			// create new ones
			json = findBookSettingsByName(fileName);
			if (json != null) {
				BookSettings bs = new BookSettings(new JSONObject(json));
				bs.fileName = fileName;
				return bs;
			}
		}

		if (json == null) {
			return new BookSettings(fileName);
		}
		LOG.d("SharedDB", "READ", fileName, json);
		
			BookSettings bookSettings = new BookSettings(new JSONObject(json));
			return bookSettings;
		} catch (JSONException e) {
			LOG.e(e);
		}
		return new BookSettings(fileName);
	}

	public String findBookSettingsByName(String name) {
		try {
			String fileName = ExtUtils.getFileName(name);
			Map<String, String> all = (Map<String, String>) prefs.getAll();
			for (String values : all.values()) {
				LOG.d("findBookSettingsByName", fileName, values);
				if (values.contains(fileName)) {
					LOG.d("findBookSettingsByName Contains");
					return values;
				}
				LOG.d("findBookSettingsByName not Contains", name);
			}
		} catch (Exception e) {
			LOG.e(e);
		}
		return null;
	}

	public synchronized void storeBookSettings(BookSettings current) {
		Editor edit = prefs.edit();
		try {
			String json = current.toJSON().toString();
			edit.putString("" + current.fileName.hashCode(), json);
			LOG.d("SharedDB", "SAVE", current.fileName, json);
		} catch (JSONException e) {
			LOG.e(e);
		}
		edit.commit();

	}

	public void delete(BookSettings bs) {
		if (bs == null) {
			return;
		}
		Editor edit = prefs.edit();
		edit.remove(String.valueOf(bs.fileName.hashCode()));
		edit.commit();
	}


}
