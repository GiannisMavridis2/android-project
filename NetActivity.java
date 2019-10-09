package giannhs.mavridis.nevercoldingreece;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;


import java.io.BufferedReader;
import java.io.InputStream;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class NetActivity extends ActionBarActivity {
    private Context ctx=this;
    private TextView text;
    private boolean yparxei_syndesh = true;
    public static String town = "η πολη για την οποια θελω να δω τον καιρο"; //το key για το extra στο intent
    private String name_town;
    private String basic_url = "http://api.wunderground.com/api/eb6c8ee6af11716e/forecast10day/q/Greece/";
    private String url;
    private HttpURLConnection conn;
    private InputStream in;
    private ListView lv;
    private MyAdapter ad;
    public Day[] days = new Day[7]; //ΔΗΜΙΟΥΡΓΩ 7 NULL POINTERS...δεν δειχνουν καπου εδω

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        //το πανω πανω text view της activity χρησιμοποιειται για την ενδειξη loading... , για error messages και για το ονομα της πολης
        text = (TextView) findViewById(R.id.town);
        text.setText("loading . . .");
        //τσεκαρω αν ειναι συνδεδεμενος στο Internet
        //αν δεν ειναι error message
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (!(networkInfo != null && networkInfo.isConnected())) {
            text.setTextColor(getResources().getColor(R.color.red));
            text.setTextSize(25);
            text.setText("No Internet connection");
            yparxei_syndesh = false;
        }
        //αν ειμαι συνδεδεμενος στ Internet ξεκινω ενα AsyncTask για να παρω τον καιρο
        if (yparxei_syndesh) {
            //ανακτω απο το intent που πηρα το ονομα της πολης για την οποια θελω τον καιρο
            Intent intent = getIntent();
            name_town = intent.getStringExtra(town);
            new MyAsyncTask().execute(name_town);
        }
        //παιξε με τον "fake" invisible list
        lv = (ListView) findViewById(R.id.listView);
        for (int i=0;i<7;i++){
            days[i]=new Day();        //λυνει το java.lang.nullpointerexception ...θυμησου java DOES HAVE POINTERS..εδω αρχιζουν και δειχνουν καπου..σε διαφορετικες  μνημης
        }
        ad = new MyAdapter(this, R.layout.show_the_weather_item,days);
        lv.setAdapter(ad);
        lv.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (in != null)
                in.close();
            conn.disconnect();
        } catch (Exception e) {
            //Log.i("error",":(");
        }
    }

    private class MyAsyncTask extends AsyncTask<String, Void, Day[]> {
        @Override
        protected Day[] doInBackground(String... name_town) {
          try {
               int pos=-1;
               url=basic_url + name_town[0] + ".xml";
               //Log.i("i",url);
               URL temp = new URL(url);
                //στελνω το get request
                conn = (HttpURLConnection) temp.openConnection();
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();
                Thread.sleep(1000);  //περιμενω 1 seconds μεχρι να λαβω το response
                if(conn.getResponseCode()==200) {
                    //Log.i("i",new Integer(conn.getResponseCode()).toString());
                    //αν ολα πηγαν καλα -> 200=ok
                    //μπορω να read το response μεσω του in
                    in = conn.getInputStream();
                    //Log.i("i","kai edo");
                }
              //do XML parsing και γεμισε τον πινακα days
              XmlPullParserFactory f = XmlPullParserFactory.newInstance();
              XmlPullParser xpp = f.newPullParser();
              xpp.setInput(new BufferedReader(new InputStreamReader(in)));
              int eventType;
              //Log.i("i","kai edo1");

              do{
                  eventType = xpp.next();
              }while(!(eventType == XmlPullParser.START_TAG && xpp.getName().equals("simpleforecast")));

              //Log.i("i","kai edo2");

              while (eventType != XmlPullParser.END_DOCUMENT) {
                  if (eventType == XmlPullParser.START_TAG){
                      String s=xpp.getName();
                      if (s.equals("forecastday")) {
                          pos=pos+1;
                          if (pos==7){break;}
                      }else if(s.equals("day")){
                           days[pos].date=xpp.nextText();
                      }else if(s.equals("month")){
                           days[pos].date= days[pos].date + "/" + xpp.nextText();
                      }else if(s.equals("weekday_short")){
                           days[pos].date=  xpp.nextText() + " " + days[pos].date;
                      }else if(s.equals("high")){
                          do{
                              eventType = xpp.next();
                          }while(!(eventType == XmlPullParser.START_TAG && xpp.getName().equals("celsius")));
                          days[pos].temp_max=xpp.nextText();
                      }else if(s.equals("low")) {
                          do{
                              eventType = xpp.next();
                          }while(!(eventType == XmlPullParser.START_TAG && xpp.getName().equals("celsius")));
                          days[pos].temp_min=xpp.nextText();
                      }else if(s.equals("conditions")) {
                          days[pos].weather_description = xpp.nextText();
                      }
                  }
                  eventType = xpp.next();
                }
                  // for(int i=0;i<7;i++){
                    // Log.i("i",days[i].date);
                     //Log.i("i",days[i].weather_description);
                     //Log.i("i",days[i].temp_max);
                     //Log.i("i",days[i].temp_min);
                     //Log.i("i","--------------");
                    //}
                   return days;
           } catch (Exception e){
                 //Log.i("error",e.toString());
                 days=null;
                 return days;
           }
        }



       protected void onPostExecute(Day[] days) {
           if (days !=null){
               if (name_town.equals("Agios%20Nikolaos")){
                   text.setText("Agios Nikolaos");
               }else if (name_town.equals("Skiathos%20Island")) {
                   text.setText("Skiathos");
               }else{
                   text.setText(name_town);
               }
              //τα data που πηρα απο το web service ta πετω στο list view του gui
               ad = new MyAdapter(ctx, R.layout.show_the_weather_item,days);
               lv.setAdapter(ad);
               lv.setVisibility(View.VISIBLE);
           }else{
              text.setTextColor(getResources().getColor(R.color.red));
              text.setTextSize(25);
              text.setText("An error occured :( ");
           }
        }

    }



}
