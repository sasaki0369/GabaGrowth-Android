package jp.gabagrowth.walk;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.speech.*;
import android.speech.tts.*;
import java.util.*;

/** User-started, foreground walking session. Recognition is best effort: Android speech
 * services can refuse background recognition or stop listening on some devices. */
public class WalkService extends Service implements TextToSpeech.OnInitListener {
 public static final String STOP="STOP", START="START", ACTION_STATUS="jp.gabagrowth.walk.STATUS", SET_SPEED="SET_SPEED";
 private static final String CHANNEL="walk";
 private final Handler handler=new Handler(Looper.getMainLooper());
 private float speechRate=1.0f;
 private TextToSpeech tts;
 private SpeechRecognizer recognizer;
 private boolean ready=false, active=false, conversation=false, listening=false;
 private int index=0, errors=0, successes=0, recognitionGeneration=0, speechGeneration=0;
 private long endAt=0, sessionStartedAt=0;
 private String sessionId=""; private int prompts=0; private boolean sessionSaved=false;
 private String mode="morning"; private org.json.JSONArray qaItems=new org.json.JSONArray();
 private final String[] morning={
  "How do you manage aging infrastructure?",
  "How do you decide which roads to repair first?",
  "Could you give me a specific example?",
  "How do government agencies share infrastructure data?",
  "What are the biggest challenges for local councils?",
  "How do you measure the benefits of digital technology?"};
 private final String[] evening={
  "We would like to learn about your infrastructure management approach.",
  "How do you use digital technology to reduce maintenance costs?",
  "Could you explain how different councils work together?",
  "How do you prioritize infrastructure investment?",
  "What results have you achieved so far?",
  "Thank you. That is very helpful."};
 @Override public void onCreate(){super.onCreate();tts=new TextToSpeech(this,this);}
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(intent!=null&&STOP.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
  if(intent!=null&&SET_SPEED.equals(intent.getAction())){speechRate=Math.max(0.6f,Math.min(1.8f,intent.getFloatExtra("speed",1f)));if(tts!=null&&ready)tts.setSpeechRate(speechRate);return START_NOT_STICKY;}
  speechRate=0.6f+getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getInt("speed_step",4)*0.1f;
  if(tts!=null&&ready)tts.setSpeechRate(speechRate);
  stopSession();
  mode=intent==null?"morning":intent.getStringExtra("mode");
  if(mode==null)mode="morning";
  int minutes=intent==null?10:intent.getIntExtra("minutes",10);
  conversation=intent!=null&&intent.getBooleanExtra("conversation",false);
  endAt=SystemClock.elapsedRealtime()+Math.max(1,Math.min(30,minutes))*60000L;
  qaItems=new org.json.JSONArray();try{org.json.JSONArray all=QaBank.load(this);android.content.SharedPreferences settings=getSharedPreferences(QaBank.PREF,MODE_PRIVATE);String org=settings.getString("organization","ALL"),week=settings.getString("week","");for(int k=0;k<all.length();k++){org.json.JSONObject q=all.getJSONObject(k);if(("ALL".equals(org)||org.equals(q.optString("organization")))&&(week.isEmpty()||week.equals(q.optString("week"))))qaItems.put(q);}}catch(Exception ignored){}
  active=true;index=0;errors=0;successes=0;prompts=0;sessionSaved=false;sessionStartedAt=System.currentTimeMillis();sessionId=java.util.UUID.randomUUID().toString();
  NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Walk & Talk",NotificationManager.IMPORTANCE_LOW));
  Intent stop=new Intent(this,WalkService.class).setAction(STOP);
  PendingIntent stopPi=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Intent open=new Intent(this,MainActivity.class);
  PendingIntent openPi=PendingIntent.getActivity(this,2,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification notification=new Notification.Builder(this,CHANNEL)
   .setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Gaba Growth · Walk & Talk")
   .setContentText(conversation?"Voice conversation · listening and speaking":"Listen and repeat")
   .setContentIntent(openPi)
   .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_pause,"Stop",stopPi).build())
   .setOngoing(true).build();
  int type=android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK;
  if(conversation&&Build.VERSION.SDK_INT>=30)type|=android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
  if(Build.VERSION.SDK_INT>=29)startForeground(10,notification,type);else startForeground(10,notification);
  report("Started: "+(conversation?"conversation":"repeat"));
  if(ready)handler.postDelayed(this::next,700);
  return START_NOT_STICKY;
 }
 private String translate(String phrase){switch(phrase){case "How do you manage aging infrastructure?":return "老朽化したインフラをどのように管理していますか？";case "How do you decide which roads to repair first?":return "どの道路を優先して補修するか、どのように決めていますか？";case "Could you give me a specific example?":return "具体的な例を挙げていただけますか？";case "How do government agencies share infrastructure data?":return "行政機関はインフラのデータをどのように共有していますか？";case "What are the biggest challenges for local councils?":return "地方自治体が直面する最大の課題は何ですか？";case "How do you measure the benefits of digital technology?":return "デジタル技術の効果をどのように測定していますか？";case "We would like to learn about your infrastructure management approach.":return "皆さんのインフラ管理の方法について学びたいと考えています。";case "How do you use digital technology to reduce maintenance costs?":return "維持管理費の削減にデジタル技術をどのように活用していますか？";case "Could you explain how different councils work together?":return "自治体同士がどのように連携しているか説明していただけますか？";case "How do you prioritize infrastructure investment?":return "インフラ投資の優先順位をどのように決めていますか？";case "What results have you achieved so far?":return "これまでにどのような成果を上げていますか？";case "Thank you. That is very helpful.":return "ありがとうございます。大変参考になりました。";default:return "";}}
 private String[] lines(){return "evening".equals(mode)?evening:morning;}
 private void next(){
  if(!active)return;
  if(SystemClock.elapsedRealtime()>=endAt){finish();return;}
  String phrase,translation;String direction=getSharedPreferences(QaBank.PREF,MODE_PRIVATE).getString("direction","ask");
  if(qaItems.length()>0){org.json.JSONObject q=qaItems.optJSONObject(index%qaItems.length());boolean useAnswer="answer".equals(direction)||("both".equals(direction)&&(index%2==1));phrase=q.optString(useAnswer?"answer_en":"question_en");translation=q.optString(useAnswer?"answer_ja":"question_ja");index++;}
  else{phrase=lines()[index++%lines().length];translation=translate(phrase);}prompts++;
  reportText("phrase",phrase);reportText("translation",translation);reportText("answer","—");report("Speaking question / 質問を再生中");
  speak(phrase,()->{
   if(!active)return;
   if(conversation){report("Prompt finished / 質問の再生完了");speak("Your turn.",()->{report("Preparing recognition / 音声認識を準備中");handler.postDelayed(this::listen,650);});}
   else handler.postDelayed(this::next,Math.max(4200,phrase.length()*85L));
  });
 }
 private void speak(String phrase,Runnable done){
  if(!active)return;
  if(tts==null||!ready){handler.postDelayed(()->{if(active)speak(phrase,done);},500);return;}
  final int generation=++speechGeneration;
  final boolean[] completed={false};
  final Runnable complete=()->{
   if(!active||generation!=speechGeneration||completed[0])return;
   completed[0]=true;
   handler.post(done);
  };
  String utterance="walk-"+generation+"-"+SystemClock.elapsedRealtime();
  tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
   @Override public void onStart(String id){}
   @Override public void onDone(String id){if(utterance.equals(id))handler.post(complete);}
   @Override public void onError(String id){if(utterance.equals(id))handler.post(complete);}
  });
  int result=tts.speak(phrase,TextToSpeech.QUEUE_FLUSH,null,utterance);
  if(result==TextToSpeech.ERROR){report("Speech playback error / 再生エラー・続行します");handler.postDelayed(complete,600);return;}
  // Some device TTS engines never send onDone. A bounded watchdog avoids a
  // permanent Speaking state, including when playback speed is adjusted.
  long estimated=(long)(phrase.length()*130L/Math.max(0.6f,speechRate))+6500L;
  long timeout=Math.max(11000L,Math.min(45000L,estimated));
  handler.postDelayed(()->{
   if(active&&generation==speechGeneration&&!completed[0]){
    report("Speech completion timeout / 再生終了通知なし・次へ進みます");
    if(tts!=null)tts.stop();
    complete.run();
   }
  },timeout);
 }
 private void listen(){
  if(!active||!conversation)return;
  if(SystemClock.elapsedRealtime()>=endAt){finish();return;}
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){report("Microphone permission missing; switching to repeat mode");conversation=false;next();return;}
  if(!SpeechRecognizer.isRecognitionAvailable(this)){report("Speech service unavailable; switching to repeat mode");conversation=false;next();return;}
  releaseRecognizer();
  try{
   recognizer=SpeechRecognizer.createSpeechRecognizer(this);
   recognizer.setRecognitionListener(new RecognitionListener(){
    public void onReadyForSpeech(Bundle b){report("Listening · speak English now");}
    public void onBeginningOfSpeech(){report("Hearing your answer…");}
    public void onRmsChanged(float f){}public void onBufferReceived(byte[] b){}
    public void onEndOfSpeech(){report("Processing your answer…");}
    public void onError(int code){
     listening=false;releaseRecognizer();if(!active)return;
     errors++;report("Recognition error "+code+" · continuing");
     if(errors>=3){conversation=false;report("Microphone unavailable · switched to repeat mode");}
     handler.postDelayed(()->{if(active)next();},1600);
    }
    public void onResults(Bundle b){
     listening=false;ArrayList<String> words=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
     String answer=words==null||words.isEmpty()?"":words.get(0);
     releaseRecognizer();if(!active)return;
     errors=0;successes++;reportText("answer",answer);report("You said: "+answer);
     handler.postDelayed(()->{if(active)next();},1600);
    }
    public void onPartialResults(Bundle b){}public void onEvent(int type,Bundle b){}
   });
   Intent request=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
   request.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
   request.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US");
   request.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"en-US");
   request.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
   request.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,1300L);
   request.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,1100L);
   listening=true;int generation=++recognitionGeneration;report("Starting microphone / マイク起動中");recognizer.startListening(request);
   handler.postDelayed(()->{if(active&&listening&&generation==recognitionGeneration){report("Recognition timeout / 認識応答なし・次の質問へ");releaseRecognizer();handler.postDelayed(this::next,700);}},12500);
  }catch(Exception ex){releaseRecognizer();errors++;report("Recognizer unavailable: "+ex.getClass().getSimpleName());if(errors>=3)conversation=false;handler.postDelayed(this::next,1800);}
 }
 private void finish(){saveSession("completed");active=false;releaseRecognizer();report("Completed · "+successes+" spoken answers");if(ready&&tts!=null)tts.speak("Great work. Your walk is complete.",TextToSpeech.QUEUE_FLUSH,null,"complete");handler.postDelayed(this::stopSelf,3500);}
 private void saveSession(String outcome){
  if(!active||sessionSaved||sessionStartedAt<=0)return;
  sessionSaved=true;
  long elapsed=Math.max(0,System.currentTimeMillis()-sessionStartedAt);
  // Discard accidental taps shorter than 15 seconds.
  if(elapsed<15000)return;
  try{
   org.json.JSONObject item=new org.json.JSONObject();
   item.put("id",sessionId);item.put("startedAt",sessionStartedAt);
   item.put("endedAt",System.currentTimeMillis());item.put("seconds",elapsed/1000);
   item.put("mode",mode);item.put("conversation",conversation);
   item.put("prompts",prompts);item.put("spokenAnswers",successes);
   item.put("outcome",outcome);item.put("source","android-walk");
   android.content.SharedPreferences prefs=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE);
   org.json.JSONArray rows=new org.json.JSONArray(prefs.getString("sessions","[]"));
   rows.put(item);
   org.json.JSONArray recent=new org.json.JSONArray();
   for(int i=Math.max(0,rows.length()-500);i<rows.length();i++)recent.put(rows.getJSONObject(i));
   prefs.edit().putString("sessions",recent.toString()).apply();
   if(new FirebaseSync(this).hasLogin())new FirebaseSync(this).syncNow(null);
  }catch(Exception ex){android.util.Log.e("GabaGrowth","Unable to save walk",ex);}
 }
 private void reportText(String key,String value){getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putString("phrase".equals(key)?"last_phrase":("translation".equals(key)?"last_translation":"last_answer"),value).apply();Intent update=new Intent(ACTION_STATUS);update.setPackage(getPackageName());update.putExtra(key,value);sendBroadcast(update);}
 private void report(String message){Intent update=new Intent(ACTION_STATUS);update.setPackage(getPackageName());update.putExtra("status",message);sendBroadcast(update);}
 private void releaseRecognizer(){recognitionGeneration++;listening=false;if(recognizer!=null){try{recognizer.cancel();recognizer.destroy();}catch(Exception ignored){}recognizer=null;}}
 private void stopSession(){saveSession("stopped");active=false;speechGeneration++;handler.removeCallbacksAndMessages(null);releaseRecognizer();if(tts!=null)tts.stop();}
 @Override public void onInit(int status){if(status==TextToSpeech.SUCCESS){ready=true;tts.setLanguage(Locale.US);tts.setSpeechRate(speechRate);if(active)handler.post(this::next);}else report("Text-to-speech initialization failed");}
 @Override public void onDestroy(){stopSession();if(tts!=null)tts.shutdown();super.onDestroy();}
 @Override public IBinder onBind(Intent intent){return null;}
}
