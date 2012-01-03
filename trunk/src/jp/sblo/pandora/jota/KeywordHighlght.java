package jp.sblo.pandora.jota;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.i18n.phonenumbers.RegexCache;

import jp.sblo.pandora.jota.text.SpannableStringBuilder;
import jp.sblo.pandora.jota.text.style.ForegroundColorSpan;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.text.Spannable;
import android.text.TextUtils;

public class KeywordHighlght {

    private static final String PATH     = Environment.getExternalStorageDirectory() + "/.jota/keyword/";
    private static final String USERPATH     = Environment.getExternalStorageDirectory() + "/.jota/keyword/user/";
    private static final String EXT      = ".txt";
    private static final String ASSET_PATH     = "keyword";
    private static final String COLOR_PATH     = "colorsetteing.txt";

    public Pattern pattern;
    public int color;

    static ArrayList<KeywordHighlght> sList = new ArrayList<KeywordHighlght>();
    static ArrayList<ForegroundColorSpan> sFcsList = new ArrayList<ForegroundColorSpan>();
    static HashMap<String,Integer> sColorMap = new HashMap<String,Integer>();

    private KeywordHighlght( String regexp , int _color )
    {
        pattern = Pattern.compile(regexp,Pattern.DOTALL);
        color = _color;
    }

    static private void addKeyword( String regexp , int color )
    {
        if ( color != 0 && !TextUtils.isEmpty(regexp) ){
            sList.add( new KeywordHighlght(regexp , color|0xFF000000) );
        }
    }

    static private void clearKeyword( )
    {
        sList.clear();
    }

    static public boolean needHighlight()
    {
        return sList.size()!=0;
    }

    static public void setHighlight( SpannableStringBuilder buf, int start , int end )
    {
        CharSequence target = buf.subSequence(start, end);
        for( KeywordHighlght syn : sList ){
            try{
                Matcher m= syn.pattern.matcher(target);

                while (m.find()) {
                    int matchstart = start+m.start();
                    int matchend = start+m.end();
                    if ( matchstart!=matchend ){
                        boolean found = false;
                        for( ForegroundColorSpan fcs : sFcsList ){
                            if ( fcs.isLapped(matchstart, matchend)){
                                found = true;
                                break;
                            }
                        }
                        if ( !found ){
                            ForegroundColorSpan fgspan = ForegroundColorSpan.obtain(syn.color,matchstart,matchend);
                            buf.setSpan(fgspan, matchstart, matchend, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            sFcsList.add(fgspan);
                        }
                    }
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    static public void removeHighlight( SpannableStringBuilder buf )
    {
        for( ForegroundColorSpan o: sFcsList )
        {
            buf.removeSpan(o);
            o.recycle();
        }
        sFcsList.clear();
    }

    static private void loadColorSettings()
    {
        sColorMap.clear();
        String path = USERPATH + COLOR_PATH;

        File f = new File(path);
        if (!f.exists() ){
            path = PATH + COLOR_PATH;
            f = new File(path);
            if (!f.exists() ){
                return;
            }
        }

        // parse ini file
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            String line;
            while( (line = br.readLine()) != null ){
                line = line.replaceAll( "^//.*$" , "" );
                line = line.replaceAll( "[ \\t]+$", "" );

                int separator = line.indexOf('=');
                if ( separator!=-1 ){
                    String head = line.substring(0, separator);
                    String body = line.substring(separator+1);

                    try{
                        int color = Integer.parseInt(body, 16);
                        sColorMap.put(head , color );
                    }
                    catch(Exception e){}
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if ( br != null ){
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static public boolean loadHighlight( String filename )
    {
        clearKeyword();
        if (filename == null){
            return false;
        }
        int point = filename.lastIndexOf(".");
        if (point == -1) {
            return false;
        }
        String ext = filename.substring(point + 1);


        String path = USERPATH + ext + EXT;

        File f = new File(path);
        if (!f.exists() ){
            path = PATH + ext + EXT;
            f = new File(path);
            if (!f.exists() ){
                return false;
            }
        }
        loadColorSettings();

        // parse ini file
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            String line;
            while( (line = br.readLine()) != null ){
                line = line.replaceAll( "^//.*$" , "" );
                line = line.replaceAll( "[ \\t]+$", "" );

                int separator = line.indexOf('=');
                if ( separator!=-1 ){
                    String head = line.substring(0, separator);
                    String body = line.substring(separator+1);

                    Integer color = sColorMap.get(head);
                    if ( color!=null ){
                        addKeyword( body , color );
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if ( br != null ){
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        sColorMap.clear();
        return true;
    }

    static public void extractFromAssets( Context context)
    {
        AssetManager am = context.getAssets();
        byte[] buf = new byte[4096];

        try {
            String[] list = am.list(ASSET_PATH);

            for( String filename : list ){
                File ofile = new File(PATH  + filename);
                ofile.getParentFile().mkdirs();
                InputStream in = am.open(ASSET_PATH + "/"+ filename);
                OutputStream out = new FileOutputStream(ofile);
                try{
                    int len;
                    while( (len = in.read(buf))>0 ){
                        out.write(buf, 0, len);
                    }
                }
                catch(Exception e){}
                in.close();
                out.close();
            }
            new File(USERPATH).mkdirs();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
