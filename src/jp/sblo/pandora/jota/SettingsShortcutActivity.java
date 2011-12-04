package jp.sblo.pandora.jota;

import java.util.HashMap;

import jp.sblo.pandora.jota.text.EditText;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.KeyEvent;

public class SettingsShortcutActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private static final String KEY_SHORTCUT                    = "SHORTCUT_ASSIGN";

    static class DefineShortcut {
        int key;
        String name;
        int function;

        public DefineShortcut( int k, String n , int f){
            key = k;
            name = n;
            function = f;
        }
    };


    private final static int[] TBL_SUMMARY = {
        R.string.no_assign,
        R.string.selectAll,
        R.string.menu_edit_undo,
        R.string.copy,
        R.string.cut,
        R.string.paste,
        R.string.menu_direct,
        R.string.menu_file_save,
        R.string.enter,
        R.string.tab,
        R.string.backspace,
        R.string.trackball_centering,
        R.string.label_search,
        R.string.label_open_file,
        R.string.label_new_file,
        R.string.label_redo,
        R.string.trackball_contextmenu,
        R.string.menu_edit_jump,
        R.string.label_forward_del,
    };

    private final static int[] TBL_FUNCTION = {
        EditText.FUNCTION_NONE,
        EditText.FUNCTION_SELECT_ALL,
        EditText.FUNCTION_UNDO,
        EditText.FUNCTION_COPY,
        EditText.FUNCTION_CUT,
        EditText.FUNCTION_PASTE,
        EditText.FUNCTION_DIRECTINTENT,
        EditText.FUNCTION_SAVE,
        EditText.FUNCTION_ENTER,
        EditText.FUNCTION_TAB,
        EditText.FUNCTION_DEL,
        EditText.FUNCTION_CENTERING,
        EditText.FUNCTION_SEARCH,
        EditText.FUNCTION_OPEN,
        EditText.FUNCTION_NEWFILE,
        EditText.FUNCTION_REDO,
        EditText.FUNCTION_CONTEXTMENU,
        EditText.FUNCTION_JUMP,
        EditText.FUNCTION_FORWARD_DEL,
    };

    public String getFunctionName(int func)
    {
        if ( 0<= func && func < TBL_SUMMARY.length ){
            return getString( TBL_SUMMARY[func] );
        }
        return "";
    }

    private static final DefineShortcut[] TBL_SHORTCUT = new DefineShortcut[]{
        new DefineShortcut( KeyEvent.KEYCODE_A ,"A" , EditText.FUNCTION_SELECT_ALL ),
        new DefineShortcut( KeyEvent.KEYCODE_B ,"B" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_C ,"C" , EditText.FUNCTION_COPY ),
        new DefineShortcut( KeyEvent.KEYCODE_D ,"D" , EditText.FUNCTION_DIRECTINTENT ),
        new DefineShortcut( KeyEvent.KEYCODE_E ,"E" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_F ,"F" , EditText.FUNCTION_SEARCH ),
        new DefineShortcut( KeyEvent.KEYCODE_G ,"G" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_H ,"H" , EditText.FUNCTION_DEL ),
        new DefineShortcut( KeyEvent.KEYCODE_I ,"I" , EditText.FUNCTION_TAB ),
        new DefineShortcut( KeyEvent.KEYCODE_J ,"J" , EditText.FUNCTION_JUMP ),
        new DefineShortcut( KeyEvent.KEYCODE_K ,"K" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_L ,"L" , EditText.FUNCTION_CENTERING ),
        new DefineShortcut( KeyEvent.KEYCODE_M ,"M" , EditText.FUNCTION_ENTER ),
        new DefineShortcut( KeyEvent.KEYCODE_N ,"N" , EditText.FUNCTION_NEWFILE ),
        new DefineShortcut( KeyEvent.KEYCODE_O ,"O" , EditText.FUNCTION_OPEN ),
        new DefineShortcut( KeyEvent.KEYCODE_P ,"P" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_Q ,"Q" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_R ,"R" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_S ,"S" , EditText.FUNCTION_SAVE ),
        new DefineShortcut( KeyEvent.KEYCODE_T ,"T" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_U ,"U" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_V ,"V" , EditText.FUNCTION_PASTE ),
        new DefineShortcut( KeyEvent.KEYCODE_W ,"W" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_X ,"X" , EditText.FUNCTION_CUT ),
        new DefineShortcut( KeyEvent.KEYCODE_Y ,"Y" , EditText.FUNCTION_REDO ),
        new DefineShortcut( KeyEvent.KEYCODE_Z ,"Z" , EditText.FUNCTION_UNDO ),
        new DefineShortcut( KeyEvent.KEYCODE_1 ,"1" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_2 ,"2" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_3 ,"3" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_4 ,"4" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_5 ,"5" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_6 ,"6" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_7 ,"7" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_8 ,"8" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_9 ,"9" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_0 ,"0" , EditText.FUNCTION_NONE ),
        new DefineShortcut( KeyEvent.KEYCODE_DEL ,"Del" , EditText.FUNCTION_FORWARD_DEL ),
    };

    private PreferenceScreen mPs = null;
    private PreferenceManager mPm = getPreferenceManager();
    private PreferenceCategory mCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPm = getPreferenceManager();

        mPs = mPm.createPreferenceScreen(this);
        mCategory = new PreferenceCategory(this);
        mCategory.setTitle(R.string.label_customize_shortcut);

        mPs.addPreference(mCategory);

        int len = TBL_SUMMARY.length;
        String[] tbl_summary =  new String[ len ];
        String[] tbl_function =  new String[ len ];
        for( int i=0;i<len;i++ ){
            tbl_summary[i] = getString(TBL_SUMMARY[i]);
            tbl_function[i] = Integer.toString(TBL_FUNCTION[i]);
        }

        for( DefineShortcut sd : TBL_SHORTCUT )
        {
            final ListPreference pr = new ListPreference(this);
            pr.setKey(KEY_SHORTCUT + sd.key );
            pr.setTitle( sd.name );
            pr.setEntries(tbl_summary);
            pr.setEntryValues(tbl_function);
            mCategory.addPreference(pr);
        }
        setPreferenceScreen(mPs);
        setSummary();
    }

    static public HashMap<Integer,Integer> loadShortcuts(Context context)
    {
        HashMap<Integer,Integer> result = new HashMap<Integer,Integer>();
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        for( DefineShortcut sd : TBL_SHORTCUT )
        {
            String key = KEY_SHORTCUT + sd.key;
            String strfunction = sp.getString(key, "0" );
            int function = Integer.parseInt(strfunction);
            result.put(sd.key, function);
        }
        return result;
    }

    static public void writeDefaultShortcuts(Context context)
    {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = sp.edit();
        for( DefineShortcut sd : TBL_SHORTCUT )
        {
            String key = KEY_SHORTCUT + sd.key;
            if ( !sp.contains(key) ){
                editor.putString(key, Integer.toString(sd.function) );
            }
            editor.commit();
        }
    }

    private void setSummary()
    {
        int prlen = mCategory.getPreferenceCount();
        for( int i=0;i<prlen ;i++ ){
            Preference pr = mCategory.getPreference(i);
            if ( pr instanceof ListPreference ){
                ListPreference lpr = (ListPreference)pr;
                lpr.setSummary(lpr.getEntry());
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setSummary();
    }
    @Override
    protected void onResume() {
        super.onResume();
        mPs.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }
    @Override
    protected void onPause() {
        super.onPause();
        mPs.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

}
