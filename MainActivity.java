package jp.gabagrowth.walk;
import android.Manifest;import android.app.*;import android.os.*;import android.content.*;import android.content.pm.PackageManager;import android.graphics.Color;import android.speech.*;import android.view.*;import android.widget.*;import java.util.*;
public class MainActivity extends Activity {
 private LinearLayout root;private TextView result;private SpeechRecognizer sr;
 private TextView label(String s,int sp){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(Color.rgb(20,35,60));v.setPadding(8,14,8,14);root.addView(v);return v;}
 private void button(String title,Runnable r){Button b=new Button(this);b.setText(title);root.addView(b);b.setOnClickListener(v->r.run());}
 @Override public void onCreate(Bundle b){super.onCreate(b);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,30,24,20);root.setBackgroundColor(Color.WHITE);ScrollView sc=new ScrollView(this);sc.addView(root);setContentView(sc);
 label("Gaba Growth 2.0",28);label("Walk & Talk · English (US)",18);
 label("Audio-only walking mode: listen, then repeat aloud during the pauses. No microphone is used while the screen is off.",14);
 button("Morning walk · 10 min",()->start("morning",10));button("Evening walk · 10 min",()->start("evening",10));button("Easy day · 3 min",()->start("morning",3));button("Stop walk",()->stopService(new Intent(this,WalkService.class)));
 label("Speech recognition test (screen on)",18);button("Speak English",this::recognize);result=label("Your recognised English appears here.",16);
 button("Open existing Gaba Growth",()->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://sasaki0369.github.io/GabaGrowth/"))));
 label("Prototype: background audio playback only. Screen-off speech recognition, translation and AI feedback are NOT included yet. Keep awareness of traffic while walking.",13);
 if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);
 }
 private void start(String mode,int min){Intent i=new Intent(this,WalkService.class);i.putExtra("mode",mode);i.putExtra("minutes",min);startForegroundService(i);}
 private void recognize(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},8);return;}
 if(!SpeechRecognizer.isRecognitionAvailable(this)){result.setText("Speech recognition unavailable on this device.");return;}
 if(sr!=null)sr.destroy();sr=SpeechRecognizer.createSpeechRecognizer(this);sr.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle b){result.setText("Listening…");}public void onBeginningOfSpeech(){}public void onRmsChanged(float f){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){}public void onError(int e){result.setText("Recognition error: "+e);}public void onResults(Bundle b){ArrayList<String> a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);result.setText(a==null||a.isEmpty()?"No result":a.get(0));}public void onPartialResults(Bundle b){}public void onEvent(int a,Bundle b){} });
 Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"en-US");sr.startListening(i);}
 @Override public void onDestroy(){if(sr!=null)sr.destroy();super.onDestroy();}
}