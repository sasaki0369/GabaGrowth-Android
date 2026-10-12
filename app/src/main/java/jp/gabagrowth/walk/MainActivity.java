package jp.gabagrowth.walk;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.speech.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
 private LinearLayout root; private TextView result, status; private SpeechRecognizer recognizer; private WebView webView; private boolean recognizing=false;
 private final int NAVY=Color.rgb(20,43,79), BLUE=Color.rgb(26,104,205);
 private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(NAVY);t.setPadding(6,14,6,14);root.addView(t);return t;}
 private void button(String title,Runnable task){Button b=new Button(this);b.setText(title);b.setAllCaps(false);root.addView(b);b.setOnClickListener(v->task.run());}
 @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(Color.rgb(240,243,248));
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(22,22,22,24);root.setBackgroundColor(Color.WHITE);
  ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
  label("Gaba Growth 2.1",28);label("Walk & Talk  |  English (US)",18);
  label("通勤の徒歩10分を英会話の時間に。朝は復唱、帰りは質問練習。",14);
  button("☀  朝の10分 / Morning Walk",()->startWalk("morning",10));
  button("☾  帰りの10分 / Evening Walk",()->startWalk("evening",10));
  button("▶  今日は3分だけ / Easy Day",()->startWalk("morning",3));
  button("■  散歩学習を終了 / Stop",()->stopService(new Intent(this,WalkService.class)));
  label("Speaking Test  |  英語音声認識",19);
  label("静かな場所でボタンを押し、『Listening』が表示されてから話してください。",13);
  button("🎤  Speak English",this::recognize);
  status=label("Ready / 準備完了",14);result=label("Recognised English appears here. / 認識結果",16);
  button("🌐  Web版 Gaba Growth をアプリ内で開く",this::openWeb);
  label("画面オフの音声再生に対応。音声認識は画面表示中のテストです。歩行中の画面操作は避けてください。",13);
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);
 }
 private void startWalk(String mode,int minutes){Intent i=new Intent(this,WalkService.class);i.putExtra("mode",mode);i.putExtra("minutes",minutes);startForegroundService(i);Toast.makeText(this,"Walk & Talk started",Toast.LENGTH_SHORT).show();}
 private void recognize(){
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},8);status.setText("マイクの権限を許可してから再度押してください。");return;}
  if(!SpeechRecognizer.isRecognitionAvailable(this)){status.setText("音声認識サービスが利用できません。Google音声認識の設定をご確認ください。");return;}
  if(recognizing){status.setText("認識中です。少しお待ちください。");return;}
  if(recognizer!=null){recognizer.destroy();recognizer=null;}
  recognizer=SpeechRecognizer.createSpeechRecognizer(this);recognizing=true;status.setText("Preparing microphone… / マイク準備中");result.setText("…");
  recognizer.setRecognitionListener(new RecognitionListener(){
   public void onReadyForSpeech(Bundle b){status.setText("Listening… / 話してください");}
   public void onBeginningOfSpeech(){status.setText("Hearing you… / 音声を取得中");}
   public void onRmsChanged(float f){} public void onBufferReceived(byte[] b){}
   public void onEndOfSpeech(){status.setText("Processing… / 認識中");}
   public void onError(int code){recognizing=false;String message;
    switch(code){case SpeechRecognizer.ERROR_NO_MATCH:message="No match (7): 聞き取れませんでした。静かな場所で、Listening表示後に再試行してください。";break;
     case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:message="Speech timeout (6): 音声が検出されませんでした。";break;
     case SpeechRecognizer.ERROR_NETWORK:case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:message="Network error ("+code+"): 通信環境を確認してください。";break;
     case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:message="Permission error (9): マイク権限を確認してください。";break;
     case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:message="Recognizer busy (8): 数秒後に再試行してください。";break;
     default:message="Recognition error "+code+". 再度お試しください。";}
    status.setText(message);result.setText("Tap Speak English to retry / タップして再試行");}
   public void onResults(Bundle b){recognizing=false;ArrayList<String> list=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);status.setText("Done / 認識完了");result.setText(list==null||list.isEmpty()?"No transcript":list.get(0));}
   public void onPartialResults(Bundle b){} public void onEvent(int a,Bundle b){}
  });
  Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
  intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
  intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US");intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"en-US");
  intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false);
  try{recognizer.startListening(intent);}catch(Exception e){recognizing=false;status.setText("Unable to start: "+e.getMessage());}
 }
 private void openWeb(){
  webView=new WebView(this);webView.getSettings().setJavaScriptEnabled(true);webView.getSettings().setDomStorageEnabled(true);
  webView.setWebViewClient(new WebViewClient());webView.setWebChromeClient(new WebChromeClient());
  LinearLayout container=new LinearLayout(this);container.setOrientation(LinearLayout.VERTICAL);
  Button back=new Button(this);back.setText("← Android Walk & Talk に戻る");back.setOnClickListener(v->{webView.destroy();webView=null;recreate();});
  container.addView(back);container.addView(webView,new LinearLayout.LayoutParams(-1,0,1));setContentView(container);
  webView.loadUrl("https://sasaki0369.github.io/GabaGrowth/");
 }
 @Override public void onBackPressed(){if(webView!=null){if(webView.canGoBack()){webView.goBack();}else{webView.destroy();webView=null;recreate();}}else super.onBackPressed();}
 @Override public void onDestroy(){if(recognizer!=null){recognizer.cancel();recognizer.destroy();}if(webView!=null)webView.destroy();super.onDestroy();}
}
