package org.ebookdroid.common.settings.books;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.ExtUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SharedDB {

	SharedPreferences prefs;

	public SharedDB(Context context) {
		prefs = context.getSharedPreferences("BOOKS", Context.MODE_PRIVATE);
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
