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
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class MainActivity extends Activity {
 private static final int GOOGLE_SIGN_IN=912;
 private static final String GOOGLE_WEB_CLIENT_ID="337987684766-4d2a9ud7dhkndtis54got4v8re0mvpau.apps.googleusercontent.com";
 private FirebaseSync firebase; private LinearLayout root; private TextView result, status; private SpeechRecognizer recognizer; private WebView webView; private boolean recognizing=false;
 private final int NAVY=Color.rgb(20,43,79), BLUE=Color.rgb(26,104,205);
 private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(NAVY);t.setPadding(6,14,6,14);root.addView(t);return t;}
 private void button(String title,Runnable task){Button b=new Button(this);b.setText(title);b.setAllCaps(false);root.addView(b);b.setOnClickListener(v->task.run());}
 @Override public void onCreate(Bundle state){super.onCreate(state);firebase=new FirebaseSync(this);getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(Color.rgb(240,243,248));
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(22,22,22,24);root.setBackgroundColor(Color.WHITE);
  ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);applySafeInsets(scroll);
  label("Gaba Growth 2.7",28);label("Walk & Talk  |  English (US)",18);
  label("通勤の徒歩10分を英会話の時間に。朝は復唱、帰りは質問練習。",14);
  button("☀  朝の10分 / Morning Walk",()->startWalk("morning",10));
  button("☾  帰りの10分 / Evening Walk",()->startWalk("evening",10));
  button("▶  今日は3分だけ / Easy Day",()->startWalk("morning",3));
  button("🎙  朝の会話10分 / Morning Conversation",()->startConversation("morning",10));
  button("🎙  帰りの会話10分 / Evening Conversation",()->startConversation("evening",10));
  button("🎙  3分だけ会話 / Easy Conversation",()->startConversation("morning",3));
  button("■  散歩学習を終了 / Stop",()->stopService(new Intent(this,WalkService.class)));
  label("会話モード：英文を聞く → Your turn → 英語で答える → 次へ。マイクの利用許可が必要です。",13);
  label("Speaking Test  |  英語音声認識",19);
  label("静かな場所でボタンを押し、『Listening』が表示されてから話してください。",13);
  button("🎤  Speak English",this::recognize);
  status=label("Ready / 準備完了",14);result=label("Recognised English appears here. / 認識結果",16);
  button("📊  徒歩学習の記録を見る / Walk history",this::showWalkHistory);
  button("📤  徒歩学習記録を共有（JSON）",this::shareWalkHistory);
  label("☁  Web版と学習記録を同期",19);
  label("Googleアカウント sasaki0369@gmail.com でログインしてください。同期するのは徒歩学習記録のみです。",13);
  button("🔐  Googleでログイン",this::showGoogleLogin);
  button("🔄  今すぐ同期",()->firebase.syncNow(this::syncMessage));
  button("ログアウト",()->{firebase.logout();syncMessage("ログアウトしました");});
  button("🌐  Web版 Gaba Growth をアプリ内で開く",this::openWeb);
  label("会話モードの画面オフ認識は試験機能です。端末や音声認識サービスによって停止する場合があります。歩行中は画面を操作しないでください。",13);
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);
 }
 private void syncMessage(String message){runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Gaba Growth 同期").setMessage(message).setPositiveButton("OK",null).show());}
 private void showGoogleLogin(){
  GoogleSignInOptions options=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestIdToken(GOOGLE_WEB_CLIENT_ID).build();
  GoogleSignInClient client=GoogleSignIn.getClient(this,options);
  client.signOut().addOnCompleteListener(this,task->{try{startActivityForResult(client.getSignInIntent(),GOOGLE_SIGN_IN);}catch(Exception e){syncMessage("Googleログインを開始できません: "+e.getMessage());}});
 }
 @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
  super.onActivityResult(requestCode,resultCode,data);
  if(requestCode!=GOOGLE_SIGN_IN)return;
  try{
   GoogleSignInAccount account=GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
   if(account==null||!"sasaki0369@gmail.com".equalsIgnoreCase(account.getEmail())){syncMessage("このGoogleアカウントは利用できません");return;}
   String idToken=account.getIdToken();if(idToken==null){syncMessage("Google IDトークンを取得できません。FirebaseのSHA-1登録をご確認ください。");return;}
   firebase.loginWithGoogleIdToken(idToken,this::syncMessage);
  }catch(ApiException e){syncMessage("Googleログインエラー: "+e.getStatusCode()+"。SHA-1とOAuth設定をご確認ください。");}
 }
 private void startWalk(String mode,int minutes){Intent i=new Intent(this,WalkService.class);i.putExtra("mode",mode);i.putExtra("minutes",minutes);startForegroundService(i);Toast.makeText(this,"Walk & Talk started",Toast.LENGTH_SHORT).show();}
 private void startConversation(String mode,int minutes){
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
   requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},8);
   Toast.makeText(this,"マイクを許可してから、もう一度会話モードを押してください",Toast.LENGTH_LONG).show();return;
  }
  Intent i=new Intent(this,WalkService.class);i.putExtra("mode",mode);i.putExtra("minutes",minutes);i.putExtra("conversation",true);
  try{startForegroundService(i);Toast.makeText(this,"Conversation started · 画面オフは実機で検証してください",Toast.LENGTH_LONG).show();}
  catch(Exception e){Toast.makeText(this,"Cannot start: "+e.getMessage(),Toast.LENGTH_LONG).show();}
 }
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
 private void applySafeInsets(View view){
  if(Build.VERSION.SDK_INT>=30){
   view.setOnApplyWindowInsetsListener((v,insets)->{
    android.graphics.Insets sys=insets.getInsets(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars()|WindowInsets.Type.displayCutout());
    v.setPadding(0,sys.top,0,sys.bottom);
    return insets;
   });
   view.requestApplyInsets();
  }else{
   view.setFitsSystemWindows(true);
  }
 }
 private void returnToWalk(){
  if(webView!=null){webView.stopLoading();webView.destroy();webView=null;}
  recreate();
 }
 private String walkHistory(){return getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getString("sessions","[]");}
 private void showWalkHistory(){
  try{
   org.json.JSONArray sessions=new org.json.JSONArray(walkHistory());
   long seconds=0;int answers=0;
   for(int i=0;i<sessions.length();i++){org.json.JSONObject row=sessions.getJSONObject(i);seconds+=row.optLong("seconds");answers+=row.optInt("spokenAnswers");}
   new AlertDialog.Builder(this).setTitle("Walk & Talk 学習記録")
    .setMessage("学習回数："+sessions.length()+" 回\n累計時間："+(seconds/60)+" 分\n音声認識回答："+answers+" 回\n\n記録はこのAndroidアプリ内に保存されています。Chrome版への自動同期はまだ有効ではありません。")
    .setPositiveButton("OK",(d,w)->{}).setNeutralButton("共有",(d,w)->shareWalkHistory()).show();
  }catch(Exception e){Toast.makeText(this,"記録を読み取れませんでした",Toast.LENGTH_LONG).show();}
 }
 private void shareWalkHistory(){
  Intent share=new Intent(Intent.ACTION_SEND);share.setType("application/json");
  share.putExtra(Intent.EXTRA_TEXT,walkHistory());
  startActivity(Intent.createChooser(share,"徒歩学習の記録を共有"));
 }
 private void openWeb(){
  webView=new WebView(this);
  webView.getSettings().setJavaScriptEnabled(true);
  webView.getSettings().setDomStorageEnabled(true);
  webView.setWebViewClient(new WebViewClient());
  webView.setWebChromeClient(new WebChromeClient());
  LinearLayout container=new LinearLayout(this);container.setOrientation(LinearLayout.VERTICAL);container.setBackgroundColor(Color.WHITE);
  LinearLayout toolbar=new LinearLayout(this);toolbar.setOrientation(LinearLayout.HORIZONTAL);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setBackgroundColor(NAVY);
  Button back=new Button(this);back.setText("← Walk & Talk に戻る");back.setAllCaps(false);
  back.setOnClickListener(v->returnToWalk());
  toolbar.addView(back,new LinearLayout.LayoutParams(-1,dp(54)));
  container.addView(toolbar,new LinearLayout.LayoutParams(-1,dp(58)));
  container.addView(webView,new LinearLayout.LayoutParams(-1,0,1));
  setContentView(container);
  applySafeInsets(container);
  webView.loadUrl("https://sasaki0369.github.io/GabaGrowth/");
 }
 private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
 @Override public void onBackPressed(){if(webView!=null){if(webView.canGoBack()){webView.goBack();}else{returnToWalk();}}else super.onBackPressed();}
 @Override public void onDestroy(){if(recognizer!=null){recognizer.cancel();recognizer.destroy();}if(webView!=null)webView.destroy();super.onDestroy();}
}
