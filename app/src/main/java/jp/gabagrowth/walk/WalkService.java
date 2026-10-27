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
 public static final String STOP="STOP", START="START", ACTION_STATUS="jp.gabagrowth.walk.STATUS", SET_SPEED="SET_SPEED", SET_GAP="SET_GAP", SET_GAP_MODE="SET_GAP_MODE", SET_TRANSLATION_MODE="SET_TRANSLATION_MODE", PREVIOUS="PREVIOUS", NEXT_PHRASE="NEXT_PHRASE", PAUSE="PAUSE", RESUME="RESUME", SET_JAPANESE_SPEED="SET_JAPANESE_SPEED", SET_ACCENTS="SET_ACCENTS", SET_FAVORITES_ONLY="SET_FAVORITES_ONLY";
 private static final String CHANNEL="walk";
 private final Handler handler=new Handler(Looper.getMainLooper());
 private float speechRate=1.0f, japaneseSpeechRate=1.3f;
 private boolean accentUs=true, accentAu=false; private boolean favoritesOnly=false;
 private boolean paused=false; private long pausedAt=0L;
 private int gapStep=4;private boolean gapAuto=true;private int lastPhraseLength=0;
 private long phraseGapMs(){
  if(!gapAuto)return Math.max(0,Math.min(20,gapStep))*500L;
  return conversation?1600L:Math.max(4200L,Math.round(lastPhraseLength*85.0));
 }
 private TextToSpeech tts; private TextToSpeech japaneseTts; private boolean japaneseReady=false; private String translationMode="before";
 private SpeechRecognizer recognizer;
 private boolean ready=false, active=false, conversation=false, listening=false, ttsInitFailed=false;
 private boolean awaitingInitialTts=false; private int ttsFailureCount=0;
 private long lastNavigationAt=0L;
 private final java.util.ArrayList<Integer> playedSlots=new java.util.ArrayList<>();
 private int index=0, currentSlot=-1, errors=0, successes=0, recognitionGeneration=0, speechGeneration=0, promptGeneration=0;
 private String progressKey="";
 private String progressKey(){android.content.SharedPreferences p=getSharedPreferences(QaBank.PREF,MODE_PRIVATE);return "resume_v42:"+QaBank.activeId(this)+":"+p.getString("organization","ALL")+":"+p.getString("week","")+":"+p.getString("direction","ask")+":"+mode;}
 private void commitProgress(){if(currentSlot<0||progressKey.isEmpty())return;getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putInt(progressKey,currentSlot+1).apply();}
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
 @Override public void onCreate(){super.onCreate();
  japaneseTts=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS&&japaneseTts!=null){
   int result=japaneseTts.setLanguage(Locale.JAPANESE);
   japaneseReady=result!=TextToSpeech.LANG_MISSING_DATA&&result!=TextToSpeech.LANG_NOT_SUPPORTED;
   japaneseTts.setSpeechRate(japaneseSpeechRate);
  }});
  tts=new TextToSpeech(this,this);
 }
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(intent!=null&&STOP.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
  if(intent!=null&&PAUSE.equals(intent.getAction())){pauseWalk();return START_NOT_STICKY;}
  if(intent!=null&&RESUME.equals(intent.getAction())){resumeWalk();return START_NOT_STICKY;}
  if(intent!=null&&SET_ACCENTS.equals(intent.getAction())){accentUs=intent.getBooleanExtra("us",true);accentAu=intent.getBooleanExtra("au",false);if(!accentUs&&!accentAu)accentUs=true;return START_NOT_STICKY;}
  if(intent!=null&&SET_JAPANESE_SPEED.equals(intent.getAction())){japaneseSpeechRate=Math.max(0.6f,Math.min(2.0f,intent.getFloatExtra("speed",1.3f)));if(japaneseTts!=null&&japaneseReady)japaneseTts.setSpeechRate(japaneseSpeechRate);return START_NOT_STICKY;}
  if(intent!=null&&SET_FAVORITES_ONLY.equals(intent.getAction())){
   favoritesOnly=intent.getBooleanExtra("enabled",false);
   getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putBoolean("favorites_only",favoritesOnly).apply();
   if(active){interruptForNavigation();index=Math.max(0,currentSlot>=0?currentSlot:index);currentSlot=-1;
    if(paused){int target=findEligibleSlot(index,1);if(target>=0){showPhraseAt(target);report("Paused / お気に入りモードを更新しました");}else report("No matching phrases / 対象の文章がありません");}
    else handler.post(this::next);
   }
   return START_NOT_STICKY;
  }
  if(intent!=null&&PREVIOUS.equals(intent.getAction())){previousPhrase();return START_NOT_STICKY;}
  if(intent!=null&&NEXT_PHRASE.equals(intent.getAction())){nextPhrase();return START_NOT_STICKY;}
  if(intent!=null&&SET_SPEED.equals(intent.getAction())){speechRate=Math.max(0.6f,Math.min(1.8f,intent.getFloatExtra("speed",1f)));if(tts!=null&&ready)tts.setSpeechRate(speechRate);return START_NOT_STICKY;}
  if(intent!=null&&SET_GAP.equals(intent.getAction())){gapStep=Math.max(0,Math.min(20,intent.getIntExtra("gap_step",4)));return START_NOT_STICKY;}
  if(intent!=null&&SET_TRANSLATION_MODE.equals(intent.getAction())){
   translationMode=validTranslationMode(intent.getStringExtra("translation_mode"));return START_NOT_STICKY;
  }
  if(intent!=null&&SET_GAP_MODE.equals(intent.getAction())){gapAuto=intent.getBooleanExtra("gap_auto",true);return START_NOT_STICKY;}
  accentUs=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getBoolean("accent_us",true);
  accentAu=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getBoolean("accent_au",false);
  if(!accentUs&&!accentAu)accentUs=true;
  translationMode=validTranslationMode(getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getString("translation_mode","before"));
  gapStep=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getInt("gap_step",4);
  gapAuto=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getBoolean("gap_auto",true);
  speechRate=0.6f+getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getInt("speed_step",4)*0.1f;
  japaneseSpeechRate=0.6f+getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getInt("japanese_speed_step",7)*0.1f;
  if(japaneseTts!=null&&japaneseReady)japaneseTts.setSpeechRate(japaneseSpeechRate);
  if(tts!=null&&ready)tts.setSpeechRate(speechRate);
  stopSession();
  mode=intent==null?"morning":intent.getStringExtra("mode");
  if(mode==null)mode="morning";
  int minutes=intent==null?10:intent.getIntExtra("minutes",10);
  conversation=intent!=null&&intent.getBooleanExtra("conversation",false);
  endAt=SystemClock.elapsedRealtime()+Math.max(1,Math.min(30,minutes))*60000L;
  qaItems=new org.json.JSONArray();try{org.json.JSONArray all=QaBank.load(this);android.content.SharedPreferences settings=getSharedPreferences(QaBank.PREF,MODE_PRIVATE);String org=settings.getString("organization","ALL"),week=settings.getString("week","");for(int k=0;k<all.length();k++){org.json.JSONObject q=all.getJSONObject(k);if(("ALL".equals(org)||org.equals(q.optString("organization")))&&(week.isEmpty()||week.equals(q.optString("week"))))qaItems.put(q);}}catch(Exception ignored){}
  favoritesOnly=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getBoolean("favorites_only",false);
  playedSlots.clear();progressKey=progressKey();index=Math.max(0,getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getInt(progressKey,0));currentSlot=-1;
  getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putBoolean("walk_paused",false).apply();reportPauseState(false);
  active=true;paused=false;pausedAt=0L;lastNavigationAt=0L;errors=0;successes=0;prompts=0;sessionSaved=false;sessionStartedAt=System.currentTimeMillis();sessionId=java.util.UUID.randomUUID().toString();
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
  awaitingInitialTts=true;
  if(ready){awaitingInitialTts=false;handler.postDelayed(this::next,700);}
  else handler.postDelayed(()->{
   if(active&&awaitingInitialTts&&!ready){
    awaitingInitialTts=false;ttsInitFailed=true;
    report("Audio unavailable: TTS initialization timed out / 音声エンジンの起動に失敗。再生を停止しました");
   }
  },10000);
  return START_NOT_STICKY;
 }
 private String translate(String phrase){switch(phrase){case "How do you manage aging infrastructure?":return "老朽化したインフラをどのように管理していますか？";case "How do you decide which roads to repair first?":return "どの道路を優先して補修するか、どのように決めていますか？";case "Could you give me a specific example?":return "具体的な例を挙げていただけますか？";case "How do government agencies share infrastructure data?":return "行政機関はインフラのデータをどのように共有していますか？";case "What are the biggest challenges for local councils?":return "地方自治体が直面する最大の課題は何ですか？";case "How do you measure the benefits of digital technology?":return "デジタル技術の効果をどのように測定していますか？";case "We would like to learn about your infrastructure management approach.":return "皆さんのインフラ管理の方法について学びたいと考えています。";case "How do you use digital technology to reduce maintenance costs?":return "維持管理費の削減にデジタル技術をどのように活用していますか？";case "Could you explain how different councils work together?":return "自治体同士がどのように連携しているか説明していただけますか？";case "How do you prioritize infrastructure investment?":return "インフラ投資の優先順位をどのように決めていますか？";case "What results have you achieved so far?":return "これまでにどのような成果を上げていますか？";case "Thank you. That is very helpful.":return "ありがとうございます。大変参考になりました。";default:return "";}}
 private String[] lines(){return "evening".equals(mode)?evening:morning;}
 private String validTranslationMode(String value){
  return "after".equals(value)||"both".equals(value)||"none".equals(value)?value:"before";
 }
 private void recordEnglishPlayback(String key,String accent){
  if(key==null||key.isEmpty())return;
  android.content.SharedPreferences p=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE);
  String k="playcount_v43:"+key;
  int count=p.getInt(k,0)+1;
  String accentKey="playcount_v47:"+accent+":"+key;
  p.edit().putInt(k,count).putInt(accentKey,p.getInt(accentKey,0)+1).putLong("lastplayed_v43:"+key,System.currentTimeMillis()).apply();
  reportText("accent_counts",p.getInt("playcount_v47:US:"+key,0)+" / "+p.getInt("playcount_v47:AU:"+key,0));
  reportText("play_count",Integer.toString(count));
 }
 private void speakJapanese(String translation,Runnable done){
  if(!active||paused)return;
  if(translation==null||translation.trim().isEmpty()){handler.post(done);return;}
  if(!japaneseReady||japaneseTts==null){
   report("Japanese TTS unavailable / 日本語音声の準備ができていません");
   handler.post(done);return;
  }
  final int generation=++speechGeneration;
  final boolean[] completed={false};
  final Runnable complete=()->{
   if(!active||paused||generation!=speechGeneration||completed[0])return;
   completed[0]=true;handler.post(done);
  };
  String utterance="ja-"+generation+"-"+SystemClock.elapsedRealtime();
  japaneseTts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
   @Override public void onStart(String id){}
   @Override public void onDone(String id){if(utterance.equals(id))handler.post(complete);}
   @Override public void onError(String id){if(utterance.equals(id))handler.post(complete);}
  });
  japaneseTts.setSpeechRate(japaneseSpeechRate);
  int result=japaneseTts.speak(translation,TextToSpeech.QUEUE_FLUSH,null,utterance);
  if(result==TextToSpeech.ERROR){
   report("Japanese TTS speak failed / 日本語読み上げの開始に失敗");
   handler.post(complete);return;
  }
  long timeout=Math.max(7000L,Math.min(30000L,translation.length()*220L+4500L));
  handler.postDelayed(()->{if(active&&!paused&&generation==speechGeneration&&!completed[0]){
   japaneseTts.stop();complete.run();
  }},timeout);
 }
 private void pauseWalk(){
  if(!active||paused)return;
  paused=true;pausedAt=SystemClock.elapsedRealtime();
  speechGeneration++;promptGeneration++;handler.removeCallbacksAndMessages(null);
  releaseRecognizer();if(tts!=null)tts.stop();if(japaneseTts!=null)japaneseTts.stop();
  reportPauseState(true);report("Paused / 一時停止中");
 }
 private void resumeWalk(){
  if(!active||!paused)return;
  endAt+=Math.max(0L,SystemClock.elapsedRealtime()-pausedAt);
  paused=false;pausedAt=0L;
  if(currentSlot>=0){
   index=currentSlot;currentSlot=-1;
   if(!playedSlots.isEmpty())playedSlots.remove(playedSlots.size()-1);
  }
  reportPauseState(false);report("Resumed / 再開しました");handler.post(this::next);
 }
 private int phraseCount(){
  String direction=getSharedPreferences(QaBank.PREF,MODE_PRIVATE).getString("direction","ask");
  return qaItems.length()>0?qaItems.length()*("both".equals(direction)?2:1):lines().length;
 }
 private boolean showPhraseAt(int start){
  int available=phraseCount();if(available<=0)return false;
  String direction=getSharedPreferences(QaBank.PREF,MODE_PRIVATE).getString("direction","ask");
  java.util.Set<String> mastered=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getStringSet("mastered_phrases",java.util.Collections.emptySet());
  for(int offset=0;offset<available;offset++){
   int slot=Math.floorMod(start+offset,available);int normalized=slot;
   String phrase,translation,key;
   if(qaItems.length()>0){
    org.json.JSONObject q=qaItems.optJSONObject(normalized%qaItems.length());if(q==null)continue;
    boolean answer="answer".equals(direction)||("both".equals(direction)&&normalized>=qaItems.length());
    phrase=q.optString(answer?"answer_en":"question_en");translation=q.optString(answer?"answer_ja":"question_ja");
    key="qa:"+QaBank.activeId(this)+":"+q.optString("id")+":"+(answer?"answer":"question");
   }else{phrase=lines()[normalized];translation=translate(phrase);key="default:"+phrase;}
   if(phrase.trim().isEmpty()||mastered.contains(key)||(favoritesOnly&&!isFavorite(key)))continue;
   currentSlot=slot;index=slot+1;
   reportText("phrase_key",key);reportText("phrase",phrase);reportText("translation",translation);reportText("answer","—");reportPhrasePosition(slot);
   getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putInt(progressKey,slot).apply();
   return true;
  }
  return false;
 }
 private void reportPhrasePosition(int slot){
  int total=phraseCount();
  if(total<=0)return;
  int current=Math.floorMod(slot,total)+1;
  getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit()
   .putInt("last_phrase_position",current).putInt("last_phrase_total",total).apply();
  Intent update=new Intent(ACTION_STATUS);update.setPackage(getPackageName());
  update.putExtra("phrase_position",current);update.putExtra("phrase_total",total);
  sendBroadcast(update);
 }
 private void interruptForNavigation(){
  releaseRecognizer();speechGeneration++;promptGeneration++;recognitionGeneration++;
  handler.removeCallbacksAndMessages(null);
  if(tts!=null)tts.stop();if(japaneseTts!=null)japaneseTts.stop();
 }
 private boolean allowNavigation(){
  long now=SystemClock.elapsedRealtime();
  if(now-lastNavigationAt<250L)return false;
  lastNavigationAt=now;return true;
 }
 private boolean isFavorite(String key){return getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getStringSet("favorite_phrases",java.util.Collections.emptySet()).contains(key);}
 private int findEligibleSlot(int start,int step){
  int total=phraseCount();if(total<=0)return -1;
  String direction=getSharedPreferences(QaBank.PREF,MODE_PRIVATE).getString("direction","ask");
  java.util.Set<String> mastered=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getStringSet("mastered_phrases",java.util.Collections.emptySet());
  for(int offset=0;offset<total;offset++){
   int slot=Math.floorMod(start+offset*step,total);String phrase,key;
   if(qaItems.length()>0){org.json.JSONObject q=qaItems.optJSONObject(slot%qaItems.length());if(q==null)continue;
    boolean answer="answer".equals(direction)||("both".equals(direction)&&slot>=qaItems.length());
    phrase=q.optString(answer?"answer_en":"question_en");key="qa:"+QaBank.activeId(this)+":"+q.optString("id")+":"+(answer?"answer":"question");
   }else{phrase=lines()[slot];key="default:"+phrase;}
   if(!phrase.trim().isEmpty()&&!mastered.contains(key)&&(!favoritesOnly||isFavorite(key)))return slot;
  }
  return -1;
 }
 private void movePhrase(int direction){
  if(!active){report("Start a lesson first / 先に学習を開始してください");return;}
  if(!allowNavigation())return;
  // Navigate relative to the displayed slot, never the playback history or the
  // asynchronously advanced index. This prevents jumping several phrases back.
  int base=currentSlot>=0?Math.floorMod(currentSlot,Math.max(1,phraseCount())):
   Math.floorMod(index-1,Math.max(1,phraseCount()));
  int target=findEligibleSlot(base+direction,direction);
  if(target<0){report("No matching phrases / 対象の文章がありません");return;}
  interruptForNavigation();index=target;currentSlot=-1;
  if(paused){showPhraseAt(target);report("Paused / 一時停止中・文章を切り替えました");}
  else {report("Navigating / 文章を切り替えます");handler.post(this::next);}
 }
 private void previousPhrase(){movePhrase(-1);}
 private void nextPhrase(){movePhrase(1);}
 private void next(){
  if(!active||paused)return;
  if(SystemClock.elapsedRealtime()>=endAt){finish();return;}
  String phrase=null,translation="",phraseKey="";
  String direction=getSharedPreferences(QaBank.PREF,MODE_PRIVATE).getString("direction","ask");
  java.util.Set<String> mastered=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getStringSet("mastered_phrases",java.util.Collections.emptySet());
  int available=qaItems.length()>0?qaItems.length()*("both".equals(direction)?2:1):lines().length;
  for(int attempt=0;attempt<available;attempt++){
   String candidate,candidateTranslation,key;
   if(qaItems.length()>0){
    int slot=index++%available;org.json.JSONObject q=qaItems.optJSONObject(slot%qaItems.length());
    boolean useAnswer="answer".equals(direction)||("both".equals(direction)&&slot>=qaItems.length());
    candidate=q.optString(useAnswer?"answer_en":"question_en");candidateTranslation=q.optString(useAnswer?"answer_ja":"question_ja");
    key="qa:"+QaBank.activeId(this)+":"+q.optString("id")+":"+(useAnswer?"answer":"question");
   }else{candidate=lines()[index++%lines().length];candidateTranslation=translate(candidate);key="default:"+candidate;}
   if(candidate.trim().isEmpty()||mastered.contains(key)||(favoritesOnly&&!isFavorite(key)))continue;
   phrase=candidate;translation=candidateTranslation;phraseKey=key;currentSlot=index-1;break;
  }
  if(phrase==null){report("All selected phrases mastered / 対象の教材はすべて習得済みです");reportText("phrase","再生対象の英文・単語がありません。習得済みのチェックを解除してください。");reportText("translation","");reportText("phrase_key","");finish();return;}
  playedSlots.add(currentSlot);
  lastPhraseLength=phrase.length();prompts++;
  reportText("phrase_key",phraseKey);
  reportText("phrase",phrase);reportText("translation",translation);reportText("answer","—");reportPhrasePosition(currentSlot);report("Speaking question / 質問を再生中");
  // A device TTS engine may never call onDone. Conversation must still reach
  // SpeechRecognizer. A short independent deadline is not tied to TTS callbacks.
  final int prompt=++promptGeneration;
  final boolean[] advanced={false};
  Runnable advance=()->{
   if(!active||paused||prompt!=promptGeneration||advanced[0])return;
   advanced[0]=true;
   if(conversation){
    if(tts!=null)tts.stop();
    speechGeneration++;
    report("Preparing recognition / 音声認識を準備中");
    handler.postDelayed(()->{if(active&&!paused&&prompt==promptGeneration)listen();},350);
   }else{commitProgress();handler.postDelayed(()->{if(active&&!paused&&prompt==promptGeneration)next();},phraseGapMs());}
  };
  final String spokenPhrase=phrase,spokenTranslation=translation,spokenKey=phraseKey;
  final boolean before="before".equals(translationMode)||"both".equals(translationMode);
  final boolean after="after".equals(translationMode)||"both".equals(translationMode);
  final java.util.ArrayList<Locale> accents=new java.util.ArrayList<>();
  if(accentUs)accents.add(Locale.US);
  if(accentAu)accents.add(new Locale("en","AU"));
  if(accents.isEmpty())accents.add(Locale.US);
  Runnable afterEnglish=()->{
   if(!active||paused||prompt!=promptGeneration)return;
   if(after){report("Speaking Japanese translation / 日本語訳を再生中");speakJapanese(spokenTranslation,advance);}
   else advance.run();
  };
  final int[] accentIndex={0};
  final Runnable[] playAccent=new Runnable[1];
  playAccent[0]=()->{
   if(!active||paused||prompt!=promptGeneration)return;
   if(accentIndex[0]>=accents.size()){afterEnglish.run();return;}
   Locale locale=accents.get(accentIndex[0]++);
   String accent="AU".equals(locale.getCountry())?"AU":"US";
   report("Speaking English ("+accent+") / 英語を再生中");
   recordEnglishPlayback(spokenKey,accent);
   speak(spokenPhrase,locale,()->{
    if(active&&!paused&&prompt==promptGeneration)playAccent[0].run();
   });
  };
  Runnable english=playAccent[0];
  if(before){
   report("Speaking Japanese translation / 日本語訳を再生中");
   speakJapanese(spokenTranslation,english);
  }else english.run();
 }
 private void speak(String phrase,Locale accent,Runnable done){
  if(!active||paused)return;
  if(tts==null||!ready){
   report("English TTS unavailable / 英語音声エンジン未準備。学習の自動進行を停止しました");
   return;
  }
  final int generation=++speechGeneration;
  final boolean[] completed={false};
  final Runnable complete=()->{
   if(!active||paused||generation!=speechGeneration||completed[0])return;
   completed[0]=true;
   handler.post(done);
  };
  String utterance="walk-"+generation+"-"+SystemClock.elapsedRealtime();
  tts.setOnUtteranceProgressListener(new UtteranceProgressListener(){
   @Override public void onStart(String id){if(utterance.equals(id))handler.post(()->{ttsFailureCount=0;});}
   @Override public void onDone(String id){if(utterance.equals(id))handler.post(complete);}
   @Override public void onError(String id){if(utterance.equals(id))handler.post(()->audioFailure("English TTS callback error / 音声合成に失敗しました"));}
  });
  int languageResult=tts.setLanguage(accent);
  if(languageResult==TextToSpeech.LANG_MISSING_DATA||languageResult==TextToSpeech.LANG_NOT_SUPPORTED){
   audioFailure("Selected English voice unavailable ("+accent.toLanguageTag()+") / 選択した英語音声がありません");return;
  }
  tts.setSpeechRate(speechRate);
  int result=tts.speak(phrase,TextToSpeech.QUEUE_FLUSH,null,utterance);
  if(result==TextToSpeech.ERROR){audioFailure("English TTS speak() failed / 英語の再生開始に失敗しました");return;}
  // Some device TTS engines never send onDone. A bounded watchdog avoids a
  // permanent Speaking state, including when playback speed is adjusted.
  long estimated=(long)(phrase.length()*130L/Math.max(0.6f,speechRate))+6500L;
  long timeout=Math.max(9000L,Math.min(22000L,estimated));
  handler.postDelayed(()->{
   if(active&&!paused&&generation==speechGeneration&&!completed[0]){
    audioFailure("English TTS timeout / 読み上げ応答がありません。自動進行を停止しました");
   }
  },timeout);
 }
 private void audioFailure(String reason){
  if(!active||paused)return;
  ttsFailureCount++;
  speechGeneration++;promptGeneration++;handler.removeCallbacksAndMessages(null);
  if(tts!=null)tts.stop();if(japaneseTts!=null)japaneseTts.stop();
  paused=true;pausedAt=SystemClock.elapsedRealtime();
  reportPauseState(true);
  report(reason+" — 再開で再試行できます");
 }
 private void listen(){
  if(!active||paused||!conversation)return;
  if(SystemClock.elapsedRealtime()>=endAt){finish();return;}
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){report("Microphone permission missing; switching to repeat mode");conversation=false;commitProgress();next();return;}
  if(!SpeechRecognizer.isRecognitionAvailable(this)){report("Speech service unavailable; switching to repeat mode");conversation=false;commitProgress();next();return;}
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
     commitProgress();handler.postDelayed(()->{if(active)next();},phraseGapMs());
    }
    public void onResults(Bundle b){
     listening=false;ArrayList<String> words=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
     String answer=words==null||words.isEmpty()?"":words.get(0);
     releaseRecognizer();if(!active)return;
     errors=0;successes++;reportText("answer",answer);report("You said: "+answer);
     commitProgress();handler.postDelayed(()->{if(active)next();},phraseGapMs());
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
   handler.postDelayed(()->{if(active&&listening&&generation==recognitionGeneration){report("Recognition timeout / 認識応答なし・次の質問へ");releaseRecognizer();commitProgress();handler.postDelayed(this::next,phraseGapMs());}},12500);
  }catch(Exception ex){releaseRecognizer();errors++;report("Recognizer unavailable: "+ex.getClass().getSimpleName());if(errors>=3)conversation=false;commitProgress();handler.postDelayed(this::next,phraseGapMs());}
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
 private void reportText(String key,String value){getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putString("phrase".equals(key)?"last_phrase":("translation".equals(key)?"last_translation":("phrase_key".equals(key)?"last_phrase_key":"last_answer")),value).apply();Intent update=new Intent(ACTION_STATUS);update.setPackage(getPackageName());update.putExtra(key,value);sendBroadcast(update);}
 private void reportPauseState(boolean value){getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putBoolean("walk_paused",value).apply();Intent update=new Intent(ACTION_STATUS);update.setPackage(getPackageName());update.putExtra("paused",value);sendBroadcast(update);}
 private void report(String message){Intent update=new Intent(ACTION_STATUS);update.setPackage(getPackageName());update.putExtra("status",message);sendBroadcast(update);}
 private void releaseRecognizer(){recognitionGeneration++;listening=false;if(recognizer!=null){try{recognizer.cancel();recognizer.destroy();}catch(Exception ignored){}recognizer=null;}}
 private void stopSession(){saveSession("stopped");active=false;paused=false;awaitingInitialTts=false;reportPauseState(false);speechGeneration++;promptGeneration++;handler.removeCallbacksAndMessages(null);releaseRecognizer();if(tts!=null)tts.stop();if(japaneseTts!=null)japaneseTts.stop();}
 @Override public void onInit(int status){
  if(status==TextToSpeech.SUCCESS){
   ready=true;ttsInitFailed=false;
   if(tts!=null){tts.setLanguage(Locale.US);tts.setSpeechRate(speechRate);}
   if(active&&awaitingInitialTts){awaitingInitialTts=false;handler.postDelayed(this::next,350);}
  }else{
   ready=false;ttsInitFailed=true;awaitingInitialTts=false;
   report("Text-to-speech initialization failed / 音声エンジン初期化失敗。自動進行を停止しました");
  }
 }
 @Override public void onDestroy(){stopSession();if(tts!=null)tts.shutdown();if(japaneseTts!=null)japaneseTts.shutdown();super.onDestroy();}
 @Override public IBinder onBind(Intent intent){return null;}
}
