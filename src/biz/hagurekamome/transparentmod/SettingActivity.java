package biz.hagurekamome.transparentmod;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class SettingActivity extends Activity {
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content,new PrefFragment()).commit();
	}

	public static class PrefFragment extends PreferenceFragment{
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			PreferenceManager prm = getPreferenceManager();
			prm.setSharedPreferencesMode(MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
			addPreferencesFromResource(R.xml.settings);
		}
	}

	@Override
	public void onDestroy(){
		Toast.makeText(getApplication(), "設定の変更は端末再起動後に反映されます", Toast.LENGTH_SHORT).show();
		super.onDestroy();
	}
}
