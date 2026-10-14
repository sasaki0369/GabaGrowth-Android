package jp.gabagrowth.walk;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
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
 private FirebaseSync firebase; private View webContainer; private ScrollView nativeWalkScreen; private LinearLayout root; private TextView result, status; private SpeechRecognizer recognizer; private WebView webView; private boolean recognizing=false;
 private final int NAVY=Color.rgb(20,43,79), BLUE=Color.rgb(26,104,205);
 private final int TEAL=Color.rgb(0,169,167), WHITE=Color.WHITE;
 private TextView spokenText, answerText, playbackState, speedLabel; private SeekBar speedSlider; private boolean receiverRegistered=false;
 private final BroadcastReceiver walkUpdates=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(i.hasExtra("phrase")&&spokenText!=null)spokenText.setText(i.getStringExtra("phrase"));if(i.hasExtra("answer")&&answerText!=null)answerText.setText(i.getStringExtra("answer"));if(i.hasExtra("status")&&playbackState!=null)playbackState.setText(i.getStringExtra("status"));}};
 private LinearLayout section; private TextView loginStatus; private LinearLayout loginActions;
 private GradientDrawable gradient(int start,int end,int radius){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});g.setCornerRadius(dp(radius));return g;}
 private TextView label(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(NAVY);t.setPadding(dp(5),dp(12),dp(5),dp(12));section.addView(t);return t;}
 private void button(String title,Runnable task){TextView b=new TextView(this);b.setText(title);b.setTextSize(16);b.setTypeface(null,Typeface.BOLD);b.setTextColor(NAVY);b.setGravity(Gravity.CENTER_VERTICAL);b.setPadding(dp(18),0,dp(12),0);GradientDrawable bg=gradient(Color.rgb(239,247,255),Color.rgb(225,244,247),18);b.setBackground(bg);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(64));lp.setMargins(0,0,0,dp(10));section.addView(b,lp);b.setOnClickListener(v->task.run());}
 private void heading(String text){TextView t=new TextView(this);t.setText(text);t.setTextSize(20);t.setTypeface(null,Typeface.BOLD);t.setTextColor(NAVY);t.setPadding(0,dp(6),0,dp(15));section.addView(t);}
 private void card(String title){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(14));c.setBackground(gradient(WHITE,Color.rgb(245,250,255),24));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(16));root.addView(c,lp);section=c;heading(title);}
 private void hero(){
  LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(22),dp(30),dp(22),dp(28));hero.setBackground(gradient(Color.rgb(5,32,84),Color.rgb(5,132,177),28));
  LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(0,0,0,dp(18));root.addView(hero,hp);
  TextView brand=new TextView(this);brand.setText("Gaba ✈\nGrowth");brand.setTextSize(39);brand.setTypeface(null,Typeface.BOLD);brand.setTextColor(WHITE);brand.setLineSpacing(-dp(8),1f);hero.addView(brand);
  TextView tagline=new TextView(this);tagline.setText("ENGLISH FOR GLOBAL BUSINESS");tagline.setTextColor(Color.rgb(208,235,255));tagline.setTextSize(12);tagline.setLetterSpacing(.09f);tagline.setPadding(0,dp(10),0,dp(18));hero.addView(tagline);
  TextView ver=new TextView(this);ver.setText("Walk & Talk  •  Ver.3.3  •  English (US)");ver.setTextColor(WHITE);ver.setTextSize(14);hero.addView(ver);
  TextView description=new TextView(this);description.setText("歩く時間が、英語の成長時間に。\n通勤の10分を、世界へつながる学びに。");description.setTextSize(15);description.setTextColor(WHITE);description.setPadding(0,dp(20),0,0);hero.addView(description);
 }
 @Override public void onCreate(Bundle state){super.onCreate(state);firebase=new FirebaseSync(this);getWindow().setStatusBarColor(Color.rgb(10,46,83));getWindow().setNavigationBarColor(Color.rgb(236,245,251));
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(18),dp(16),dp(30));root.setBackgroundColor(Color.rgb(235,245,251));
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);nativeWalkScreen=scroll;setContentView(scroll);applySafeInsets(scroll);
  hero();
  card("🎧  Walk & Talk");
  button("☀   朝の10分 / Morning Walk",()->startWalk("morning",10));
  button("☾   帰りの10分 / Evening Walk",()->startWalk("evening",10));
  button("▶   今日は3分だけ / Easy Day",()->startWalk("morning",3));
  card("🔊  Playback speed / 再生速度");
  speedLabel=label("",16);
  speedSlider=new SeekBar(this);speedSlider.setMax(12);section.addView(speedSlider);
  int stored=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getInt("speed_step",4);
  speedSlider.setProgress(Math.max(0,Math.min(12,stored)));
  updateSpeedLabel(stored);
  speedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar b,int progress,boolean fromUser){updateSpeedLabel(progress);if(fromUser){getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putInt("speed_step",progress).apply();Intent i=new Intent(MainActivity.this,WalkService.class).setAction(WalkService.SET_SPEED);i.putExtra("speed",speedValue(progress));startService(i);}}
   public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}
  });
  label("0.6×〜1.8×（0.1刻み）。次の発話から反映されます。",12);
  card("📝  Spoken English / 音声テキスト");
  label("再生中の英文と、会話モードで認識した自分の発言を表示します。",13);
  heading("Now speaking / 再生中");
  spokenText=label("学習を開始すると英文が表示されます。",18);
  heading("Your answer / あなたの発言");
  answerText=label("会話モードで発言すると認識結果が表示されます。",16);
  playbackState=label("Ready",13);
  card("🎙  Conversation");
  button("朝の会話10分 / Morning Conversation",()->startConversation("morning",10));
  button("帰りの会話10分 / Evening Conversation",()->startConversation("evening",10));
  button("3分だけ会話 / Easy Conversation",()->startConversation("morning",3));
  button("■   学習を終了 / Stop",()->stopService(new Intent(this,WalkService.class)));
  label("英文を聞く → Your turn → 英語で答える → 次へ。",13);
  card("🎤  Speaking Test");
  label("静かな場所でListening表示後に話してください。",13);
  button("Speak English",this::recognize);
  status=label("Ready / 準備完了",14);result=label("Recognised English appears here. / 認識結果",15);
  card("📊  Learning records");
  button("学習記録を見る / Walk history",this::showWalkHistory);
  button("学習記録を共有（JSON）",this::shareWalkHistory);
  card("☁  Cloud sync");
  label("GoogleアカウントでログインするとWeb版と徒歩学習記録を同期します。",13);
  loginStatus=label("",14);
  loginActions=new LinearLayout(this);loginActions.setOrientation(LinearLayout.VERTICAL);section.addView(loginActions);
  addLoginButton("Googleでログイン",this::showGoogleLogin);
  addLoginButton("今すぐ同期",()->firebase.syncNow(this::syncMessage));
  addLoginButton("ログアウト",()->{firebase.logout();refreshLoginUI();syncMessage("ログアウトしました");});
  refreshLoginUI();
  card("🌐  Gaba Growth");
  button("学習管理へ / Learning dashboard",this::openWeb);
  label("歩行中は画面を操作しないでください。会話モードの画面オフ認識は端末によって停止する場合があります。",12);
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);
  showNativeWalk(); // Open native Walk & Talk on launch
 }
 private void addLoginButton(String text,Runnable action){TextView b=new TextView(this);b.setText(text);b.setTextSize(16);b.setTypeface(null,Typeface.BOLD);b.setTextColor(NAVY);b.setPadding(dp(18),dp(16),dp(12),dp(16));b.setBackground(gradient(Color.rgb(239,247,255),Color.rgb(225,244,247),18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));loginActions.addView(b,lp);b.setOnClickListener(v->action.run());}
 private void refreshLoginUI(){if(loginStatus==null||loginActions==null)return;boolean logged=firebase.hasLogin();loginStatus.setText(logged?"✓ Googleログイン済み · 学習記録の同期が利用できます":"未ログイン · Googleアカウントでログインしてください");loginActions.getChildAt(0).setVisibility(logged?View.GONE:View.VISIBLE);loginActions.getChildAt(1).setVisibility(logged?View.VISIBLE:View.GONE);loginActions.getChildAt(2).setVisibility(logged?View.VISIBLE:View.GONE);}
 private void syncMessage(String message){refreshLoginUI();runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Gaba Growth 同期").setMessage(message).setPositiveButton("OK",null).show());}
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
   firebase.loginWithGoogleIdToken(idToken,message->{refreshLoginUI();syncMessage(message);});
  }catch(ApiException e){syncMessage("Googleログインエラー: "+e.getStatusCode()+"。SHA-1とOAuth設定をご確認ください。");}
 }
 private float speedValue(int step){return 0.6f+Math.max(0,Math.min(12,step))*0.1f;}
 private void updateSpeedLabel(int step){if(speedLabel!=null)speedLabel.setText(String.format(Locale.US,"Speech speed: %.1f×",speedValue(step)));}
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
    v.setPadding(v.getPaddingLeft(),sys.top,v.getPaddingRight(),sys.bottom);
    return insets;
   });
   view.requestApplyInsets();
  }else{
   view.setFitsSystemWindows(true);
  }
 }
 private void returnToWalk(){showNativeWalk();}
 private String walkHistory(){return getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getString("sessions","[]");}
 private void showWalkHistory(){
  try{
   org.json.JSONArray sessions=new org.json.JSONArray(walkHistory());
   long seconds=0;int answers=0;
   for(int i=0;i<sessions.length();i++){org.json.JSONObject row=sessions.getJSONObject(i);seconds+=row.optLong("seconds");answers+=row.optInt("spokenAnswers");}
   new AlertDialog.Builder(this).setTitle("Walk & Talk 学習記録")
    .setMessage("学習回数："+sessions.length()+" 回\n累計時間："+(seconds/60)+" 分\n音声認識回答："+answers+" 回\n\n記録はこのAndroidアプリ内に保存されています。Googleログイン済みの場合、クラウドへ同期できます。")
    .setPositiveButton("OK",(d,w)->{}).setNeutralButton("共有",(d,w)->shareWalkHistory()).show();
  }catch(Exception e){Toast.makeText(this,"記録を読み取れませんでした",Toast.LENGTH_LONG).show();}
 }
 private void shareWalkHistory(){
  Intent share=new Intent(Intent.ACTION_SEND);share.setType("application/json");
  share.putExtra(Intent.EXTRA_TEXT,walkHistory());
  startActivity(Intent.createChooser(share,"徒歩学習の記録を共有"));
 }
 private void openWeb(){
  if(webView!=null){if(webContainer.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)webContainer.getParent()).removeView(webContainer);setContentView(webContainer);applySafeInsets(webContainer);return;}
  webView=new WebView(this);
  webView.getSettings().setJavaScriptEnabled(true);
  webView.getSettings().setDomStorageEnabled(true);
  webView.getSettings().setMediaPlaybackRequiresUserGesture(true);
  webView.setWebViewClient(new WebViewClient(){
   private boolean navigate(android.net.Uri uri){
    if("gabagrowth".equalsIgnoreCase(uri.getScheme()) && "walk".equalsIgnoreCase(uri.getHost())){runOnUiThread(()->showNativeWalk());return true;}
    if(!"https".equalsIgnoreCase(uri.getScheme()) || !"sasaki0369.github.io".equalsIgnoreCase(uri.getHost())){
     try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception e){Toast.makeText(MainActivity.this,"リンクを開けません",Toast.LENGTH_SHORT).show();}
     return true;
    }
    return false;
   }
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return navigate(request.getUrl());}
   @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return navigate(android.net.Uri.parse(url));}
  });
  webView.setWebChromeClient(new WebChromeClient());
  LinearLayout container=new LinearLayout(this);container.setOrientation(LinearLayout.VERTICAL);container.setBackgroundColor(Color.WHITE);
  LinearLayout bar=new LinearLayout(this);bar.setOrientation(LinearLayout.HORIZONTAL);bar.setPadding(dp(10),dp(8),dp(10),dp(8));bar.setBackgroundColor(Color.rgb(10,46,83));
  TextView walk=new TextView(this);walk.setText("🎧 Walk & Talk");walk.setTextColor(Color.WHITE);walk.setTypeface(null,Typeface.BOLD);walk.setTextSize(16);walk.setGravity(Gravity.CENTER);walk.setBackground(gradient(Color.rgb(21,105,174),Color.rgb(0,151,151),14));
  bar.addView(walk,new LinearLayout.LayoutParams(0,dp(48),1));walk.setOnClickListener(v->showNativeWalk());
  TextView browser=new TextView(this);browser.setText("↗ Chrome");browser.setTextColor(Color.WHITE);browser.setTextSize(13);browser.setGravity(Gravity.CENTER);
  bar.addView(browser,new LinearLayout.LayoutParams(dp(100),dp(48)));browser.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://sasaki0369.github.io/GabaGrowth/"))));
  container.addView(bar);container.addView(webView,new LinearLayout.LayoutParams(-1,0,1));
  webContainer=container;setContentView(container);applySafeInsets(container);
  webView.loadUrl("https://sasaki0369.github.io/GabaGrowth/?embedded=android&v=31");
 }
 private void showNativeWalk(){
  if(nativeWalkScreen!=null){
   if(nativeWalkScreen.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)nativeWalkScreen.getParent()).removeView(nativeWalkScreen);
   setContentView(nativeWalkScreen);applySafeInsets(nativeWalkScreen);
  }
 }
  @Override protected void onResume(){super.onResume();refreshLoginUI();if(!receiverRegistered){IntentFilter f=new IntentFilter(WalkService.ACTION_STATUS);if(Build.VERSION.SDK_INT>=33)registerReceiver(walkUpdates,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(walkUpdates,f);receiverRegistered=true;}android.content.SharedPreferences p=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE);if(spokenText!=null)spokenText.setText(p.getString("last_phrase","学習を開始すると英文が表示されます。"));if(answerText!=null)answerText.setText(p.getString("last_answer","会話モードで発言すると認識結果が表示されます。"));}
 @Override protected void onPause(){if(receiverRegistered){unregisterReceiver(walkUpdates);receiverRegistered=false;}super.onPause();}
 @Override protected void onNewIntent(Intent intent){
   super.onNewIntent(intent);
   setIntent(intent);
   if(intent!=null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData()!=null
      && "gabagrowth".equalsIgnoreCase(intent.getData().getScheme())
      && "walk".equalsIgnoreCase(intent.getData().getHost())){
    if(webView!=null) showNativeWalk();
   }
  }

 private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
 @Override public void onBackPressed(){if(webView!=null && webView.isShown()){if(webView.canGoBack())webView.goBack();else new AlertDialog.Builder(this).setMessage("Gaba Growthを終了しますか？").setNegativeButton("キャンセル",null).setPositiveButton("終了",(d,w)->finish()).show();}else if(webView!=null)openWeb();else super.onBackPressed();}
 @Override public void onDestroy(){if(recognizer!=null){recognizer.cancel();recognizer.destroy();}if(webView!=null)webView.destroy();super.onDestroy();}
}
