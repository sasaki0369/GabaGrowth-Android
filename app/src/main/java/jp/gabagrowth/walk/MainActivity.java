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
 private static final int GOOGLE_SIGN_IN=912, CSV_IMPORT=913;
 private static final String GOOGLE_WEB_CLIENT_ID="337987684766-4d2a9ud7dhkndtis54got4v8re0mvpau.apps.googleusercontent.com";
 private FirebaseSync firebase; private View webContainer; private ScrollView nativeWalkScreen; private LinearLayout root; private TextView result, status; private SpeechRecognizer recognizer; private WebView webView; private boolean recognizing=false;
 private final int NAVY=Color.rgb(20,43,79), BLUE=Color.rgb(26,104,205);
 private final int TEAL=Color.rgb(0,169,167), WHITE=Color.WHITE;
 private TextView spokenText, translatedText, answerText, playbackState, speedLabel, gapLabel, playCountLabel, pauseResumeButton; private boolean walkPaused=false; private SeekBar speedSlider, gapSlider; private CheckBox masteredCheck; private String currentPhraseKey=""; private boolean receiverRegistered=false;
 private final BroadcastReceiver walkUpdates=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){if(i.hasExtra("phrase")&&spokenText!=null)spokenText.setText(i.getStringExtra("phrase"));if(i.hasExtra("phrase_key"))setCurrentPhraseKey(i.getStringExtra("phrase_key"));if(i.hasExtra("play_count")&&playCountLabel!=null)playCountLabel.setText("英語の再生回数： "+i.getStringExtra("play_count")+" 回");if(i.hasExtra("accent_counts")&&playCountLabel!=null)playCountLabel.setText(playCountLabel.getText()+" (米国 / 豪州: "+i.getStringExtra("accent_counts")+")");if(i.hasExtra("translation")&&translatedText!=null)translatedText.setText(i.getStringExtra("translation"));if(i.hasExtra("answer")&&answerText!=null)answerText.setText(i.getStringExtra("answer"));if(i.hasExtra("status")&&playbackState!=null)playbackState.setText(i.getStringExtra("status"));if(i.hasExtra("paused")){walkPaused=i.getBooleanExtra("paused",false);refreshPauseResumeButton();}}};
 private String pendingWebTab="home"; private LinearLayout section; private TextView loginStatus; private LinearLayout loginActions;
 private GradientDrawable gradient(int start,int end,int radius){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});g.setCornerRadius(dp(radius));return g;}
 private TextView label(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(NAVY);t.setPadding(dp(5),dp(12),dp(5),dp(12));section.addView(t);return t;}
 private void button(String title,Runnable task){TextView b=new TextView(this);b.setText(title);b.setTextSize(16);b.setTypeface(null,Typeface.BOLD);b.setTextColor(NAVY);b.setGravity(Gravity.CENTER_VERTICAL);b.setPadding(dp(18),0,dp(12),0);GradientDrawable bg=gradient(Color.rgb(239,247,255),Color.rgb(225,244,247),18);b.setBackground(bg);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(64));lp.setMargins(0,0,0,dp(10));section.addView(b,lp);b.setOnClickListener(v->task.run());}
 private void refreshPauseResumeButton(){if(pauseResumeButton!=null)pauseResumeButton.setText(walkPaused?"▶   再開 / Resume":"⏸   一時停止 / Pause");}
 private void heading(String text){TextView t=new TextView(this);t.setText(text);t.setTextSize(20);t.setTypeface(null,Typeface.BOLD);t.setTextColor(NAVY);t.setPadding(0,dp(6),0,dp(15));section.addView(t);}
 private void card(String title){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(14));c.setBackground(gradient(WHITE,Color.rgb(245,250,255),24));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(16));root.addView(c,lp);section=c;heading(title);}
 private void hero(){
  LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(22),dp(30),dp(22),dp(28));hero.setBackground(gradient(Color.rgb(5,32,84),Color.rgb(5,132,177),28));
  LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(0,0,0,dp(18));root.addView(hero,hp);
  TextView brand=new TextView(this);brand.setText("Gaba ✈\nGrowth");brand.setTextSize(39);brand.setTypeface(null,Typeface.BOLD);brand.setTextColor(WHITE);brand.setLineSpacing(-dp(8),1f);hero.addView(brand);
  TextView tagline=new TextView(this);tagline.setText("ENGLISH FOR GLOBAL BUSINESS");tagline.setTextColor(Color.rgb(208,235,255));tagline.setTextSize(12);tagline.setLetterSpacing(.09f);tagline.setPadding(0,dp(10),0,dp(18));hero.addView(tagline);
  TextView ver=new TextView(this);ver.setText("Walk & Talk  •  Ver.4.8  •  English (US/AU)");ver.setTextColor(WHITE);ver.setTextSize(14);hero.addView(ver);
  TextView description=new TextView(this);description.setText("歩く時間が、英語の成長時間に。\n通勤の10分を、世界へつながる学びに。");description.setTextSize(15);description.setTextColor(WHITE);description.setPadding(0,dp(20),0,0);hero.addView(description);
 }
 @Override public void onCreate(Bundle state){super.onCreate(state);firebase=new FirebaseSync(this);getWindow().setStatusBarColor(Color.rgb(10,46,83));getWindow().setNavigationBarColor(Color.rgb(236,245,251));
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(18),dp(16),dp(30));root.setBackgroundColor(Color.rgb(235,245,251));
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);nativeWalkScreen=scroll;setContentView(scroll);applySafeInsets(scroll);
  hero();
  card("📚  NSW Q&A教材 / CSV Import");
  label("添付CSVと同じ15列のフォーマットに対応。初期教材230件を収録。",13);
  label("複数のCSVを個別に保存し、利用する教材を選択できます。",13);
  android.widget.Spinner datasetPicker=new android.widget.Spinner(this);section.addView(datasetPicker);
  java.util.ArrayList<String> datasetIds=new java.util.ArrayList<>();java.util.ArrayList<String> datasetNames=new java.util.ArrayList<>();
  datasetIds.add(QaBank.DEFAULT_ID);datasetNames.add("初期教材 / NSW Q&A (bundled)");
  org.json.JSONObject library=QaBank.library(this);java.util.Iterator<String> keys=library.keys();while(keys.hasNext()){String key=keys.next();org.json.JSONObject entry=library.optJSONObject(key);if(entry!=null){datasetIds.add(key);datasetNames.add(QaBank.displayFilename(entry.optString("name","CSV"))+" ("+(entry.optJSONArray("rows")!=null?entry.optJSONArray("rows").length():0)+"件)");}}
  datasetPicker.setAdapter(new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,datasetNames));
  int selectedDataset=datasetIds.indexOf(QaBank.activeId(this));datasetPicker.setSelection(Math.max(0,selectedDataset));
  datasetPicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){String chosen=datasetIds.get(pos);if(!chosen.equals(QaBank.activeId(MainActivity.this))){QaBank.select(MainActivity.this,chosen);recreate();}}});
  button("選択中のCSVを削除 / Delete selected CSV",()->{String id=QaBank.activeId(this);if(QaBank.DEFAULT_ID.equals(id)){Toast.makeText(this,"初期教材は削除できません",Toast.LENGTH_SHORT).show();return;}new AlertDialog.Builder(this).setTitle("教材を削除しますか？").setMessage("選択したCSV教材だけを削除します。他の教材は残ります。").setNegativeButton("キャンセル",null).setPositiveButton("削除",(d,w)->{QaBank.remove(this,id);recreate();}).show();});
  button("CSVファイルを取り込む / Import CSV",()->{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType("text/*");pick.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/csv","text/comma-separated-values","text/plain","application/vnd.ms-excel"});startActivityForResult(pick,CSV_IMPORT);});
  label("訪問先・週・練習方向を選び、Walk & Talkで質問と回答を練習できます。",13);
  card("🎧  Walk & Talk");
  label("教材: NSW Q&A / 取り込んだCSV",14);
  android.widget.Spinner qaOrg=new android.widget.Spinner(this);section.addView(qaOrg);
  java.util.ArrayList<String> orgs=new java.util.ArrayList<>();orgs.add("ALL");orgs.add("COMMON");try{org.json.JSONArray a=QaBank.load(this);for(int k=0;k<a.length();k++){String v=a.getJSONObject(k).optString("organization");if(!orgs.contains(v))orgs.add(v);}}catch(Exception ignored){}
  qaOrg.setAdapter(new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,orgs));
  qaOrg.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){getSharedPreferences(QaBank.PREF,MODE_PRIVATE).edit().putString("organization",orgs.get(pos)).apply();}});
  android.widget.Spinner qaWeek=new android.widget.Spinner(this);section.addView(qaWeek);java.util.ArrayList<String> weeks=new java.util.ArrayList<>();weeks.add("ALL WEEKS");for(int w=1;w<=12;w++)weeks.add("Week "+w);qaWeek.setAdapter(new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,weeks));qaWeek.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){getSharedPreferences(QaBank.PREF,MODE_PRIVATE).edit().putString("week",pos==0?"":String.valueOf(pos)).apply();}});
  android.widget.Spinner qaDirection=new android.widget.Spinner(this);section.addView(qaDirection);String[] directions={"質問する / Ask","回答する / Answer","両方 / Both"};qaDirection.setAdapter(new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,directions));qaDirection.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){getSharedPreferences(QaBank.PREF,MODE_PRIVATE).edit().putString("direction",pos==0?"ask":pos==1?"answer":"both").apply();}});

  button("☀   朝の10分 / Morning Walk",()->startWalk("morning",10));
  button("☾   帰りの10分 / Evening Walk",()->startWalk("evening",10));
  button("▶   今日は3分だけ / Easy Day",()->startWalk("morning",3));
  button("↺   選択中の教材を最初から / Restart from beginning",()->new AlertDialog.Builder(this)
   .setTitle("最初から再生しますか？")
   .setMessage("選択中の教材・絞り込み条件の朝と帰りの再生位置をリセットします。習得済みチェックは維持されます。")
   .setNegativeButton("キャンセル",null)
   .setPositiveButton("リセット",(d,w)->{android.content.SharedPreferences p=getSharedPreferences(QaBank.PREF,MODE_PRIVATE);String prefix="resume_v42:"+QaBank.activeId(this)+":"+p.getString("organization","ALL")+":"+p.getString("week","")+":"+p.getString("direction","ask")+":";getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().remove(prefix+"morning").remove(prefix+"evening").apply();Toast.makeText(this,"次回は先頭の未習得項目から再生します",Toast.LENGTH_LONG).show();}).show());
  card("🌏  English accents / 英語のアクセント");
  label("再生する英語を選択（両方選ぶと米国→豪州の順で各1回）",14);
  android.content.SharedPreferences accentPrefs=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE);
  CheckBox usAccent=new CheckBox(this);usAccent.setText("🇺🇸 アメリカ英語 / American English");usAccent.setTextColor(NAVY);section.addView(usAccent);
  CheckBox auAccent=new CheckBox(this);auAccent.setText("🇦🇺 オーストラリア英語 / Australian English");auAccent.setTextColor(NAVY);section.addView(auAccent);
  usAccent.setChecked(accentPrefs.getBoolean("accent_us",true));
  auAccent.setChecked(accentPrefs.getBoolean("accent_au",false));
  android.widget.CompoundButton.OnCheckedChangeListener accentsChanged=(button,checked)->{
   if(!usAccent.isChecked()&&!auAccent.isChecked()){
    Toast.makeText(this,"少なくとも一つ選択してください",Toast.LENGTH_SHORT).show();
    button.setChecked(true);
    return;
   }
   boolean us=usAccent.isChecked(),au=auAccent.isChecked();
   accentPrefs.edit().putBoolean("accent_us",us).putBoolean("accent_au",au).apply();
   Intent setting=new Intent(MainActivity.this,WalkService.class).setAction(WalkService.SET_ACCENTS);
   setting.putExtra("us",us);setting.putExtra("au",au);startService(setting);
  };
  usAccent.setOnCheckedChangeListener(accentsChanged);auAccent.setOnCheckedChangeListener(accentsChanged);
  card("🔊  English playback speed / 英語の再生速度");
  speedLabel=label("",16);
  speedSlider=new SeekBar(this);speedSlider.setMax(12);section.addView(speedSlider);
  int stored=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getInt("speed_step",4);
  speedSlider.setProgress(Math.max(0,Math.min(12,stored)));
  updateSpeedLabel(stored);
  speedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar b,int progress,boolean fromUser){updateSpeedLabel(progress);if(fromUser){getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putInt("speed_step",progress).apply();Intent i=new Intent(MainActivity.this,WalkService.class).setAction(WalkService.SET_SPEED);i.putExtra("speed",speedValue(progress));startService(i);}}
   public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}
  });
  label("英語：0.6×〜1.8×（0.1刻み）。次の発話から反映されます。",12);
  card("🇯🇵  Japanese playback speed / 日本語訳の再生速度");
  TextView japaneseSpeedLabel=label("",16);
  SeekBar japaneseSpeedSlider=new SeekBar(this);japaneseSpeedSlider.setMax(14);section.addView(japaneseSpeedSlider);
  int japaneseStored=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getInt("japanese_speed_step",7);
  japaneseSpeedSlider.setProgress(Math.max(0,Math.min(14,japaneseStored)));
  japaneseSpeedLabel.setText(String.format(Locale.US,"日本語訳の再生速度: %.1f×",0.6f+japaneseStored*0.1f));
  japaneseSpeedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar b,int step,boolean fromUser){
    float speed=0.6f+step*0.1f;
    japaneseSpeedLabel.setText(String.format(Locale.US,"日本語訳の再生速度: %.1f×",speed));
    if(fromUser){getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putInt("japanese_speed_step",step).apply();
     Intent i=new Intent(MainActivity.this,WalkService.class).setAction(WalkService.SET_JAPANESE_SPEED);i.putExtra("speed",speed);startService(i);}
   }
   public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}
  });
  label("日本語訳：0.6×〜2.0×（0.1刻み）。初期値は1.3×。",12);
  card("⏱  Pause between phrases / 発話間隔");
  gapLabel=label("",16);
  gapSlider=new SeekBar(this);gapSlider.setMax(20);section.addView(gapSlider);
  android.widget.RadioGroup gapModes=new android.widget.RadioGroup(this);
  gapModes.setOrientation(android.widget.RadioGroup.HORIZONTAL);
  android.widget.RadioButton automatic=new android.widget.RadioButton(this);automatic.setText("自動 / Auto");automatic.setId(View.generateViewId());
  android.widget.RadioButton manual=new android.widget.RadioButton(this);manual.setText("手動 / Manual");manual.setId(View.generateViewId());
  gapModes.addView(automatic);gapModes.addView(manual);section.addView(gapModes);
  boolean autoGap=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getBoolean("gap_auto",true);
  gapModes.check(autoGap?automatic.getId():manual.getId());gapSlider.setEnabled(!autoGap);
  gapModes.setOnCheckedChangeListener((group,id)->{
   boolean auto=id==automatic.getId();gapSlider.setEnabled(!auto);
   getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putBoolean("gap_auto",auto).apply();
   Intent change=new Intent(MainActivity.this,WalkService.class).setAction(WalkService.SET_GAP_MODE);change.putExtra("gap_auto",auto);startService(change);
   updateGapLabel(gapSlider.getProgress());
  });
  int savedGap=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getInt("gap_step",4);
  gapSlider.setProgress(Math.max(0,Math.min(20,savedGap)));
  updateGapLabel(savedGap);
  gapSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar b,int progress,boolean fromUser){updateGapLabel(progress);if(fromUser){getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putInt("gap_step",progress).apply();Intent i=new Intent(MainActivity.this,WalkService.class).setAction(WalkService.SET_GAP);i.putExtra("gap_step",progress);startService(i);}}
   public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}
  });
  label("自動：英文の長さに応じて最低4.2秒（v3.9方式）。手動：0〜10秒（0.5秒刻み）。会話モードの自動間隔は回答後1.6秒です。",12);
  card("🇯🇵  日本語訳の音声読み上げ / Translation audio");
  label("歩きながら意味を理解できるよう、英語の前後に日本語訳を読み上げます。",13);
  android.widget.RadioGroup translationModes=new android.widget.RadioGroup(this);
  String[] translationLabels={"前だけ：日本語 → 英語","後だけ：英語 → 日本語","両方：日本語 → 英語 → 日本語","なし：英語のみ"};
  String[] translationValues={"before","after","both","none"};
  String savedTranslation=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getString("translation_mode","before");
  for(int t=0;t<translationValues.length;t++){
   android.widget.RadioButton r=new android.widget.RadioButton(this);
   r.setId(View.generateViewId());r.setText(translationLabels[t]);r.setTag(translationValues[t]);
   translationModes.addView(r);
   if(translationValues[t].equals(savedTranslation))translationModes.check(r.getId());
  }
  section.addView(translationModes);
  translationModes.setOnCheckedChangeListener((group,id)->{
   android.widget.RadioButton selected=group.findViewById(id);
   if(selected==null)return;
   String value=String.valueOf(selected.getTag());
   getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putString("translation_mode",value).apply();
   Intent change=new Intent(MainActivity.this,WalkService.class).setAction(WalkService.SET_TRANSLATION_MODE);
   change.putExtra("translation_mode",value);startService(change);
  });
  card("📝  Spoken English / 音声テキスト");
  label("再生中の英文と、会話モードで認識した自分の発言を表示します。",13);
  heading("Now speaking / 再生中");
  spokenText=label("学習を開始すると英文が表示されます。",18);
  heading("日本語訳 / Translation");
  translatedText=label("英文の日本語訳を表示します。",16);
  playCountLabel=label("英語の再生回数： 0 回",15);
  masteredCheck=new CheckBox(this);masteredCheck.setText("☑ 習得済み：次回からこの英文・単語を再生しない");masteredCheck.setTextColor(NAVY);masteredCheck.setTextSize(15);section.addView(masteredCheck);
  masteredCheck.setOnCheckedChangeListener((button,checked)->{
   if(currentPhraseKey.isEmpty())return;
   java.util.Set<String> saved=new java.util.HashSet<>(getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getStringSet("mastered_phrases",java.util.Collections.emptySet()));
   if(checked)saved.add(currentPhraseKey);else saved.remove(currentPhraseKey);
   getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putStringSet("mastered_phrases",saved).apply();
  });
  button("習得済み一覧を確認・解除 / Review mastered",this::reviewMastered);
  heading("Your answer / あなたの発言");
  answerText=label("会話モードで発言すると認識結果が表示されます。",16);
  playbackState=label("Ready",13);
  button("⏸   一時停止 / Pause",()->{Intent i=new Intent(this,WalkService.class).setAction(walkPaused?WalkService.RESUME:WalkService.PAUSE);startService(i);});
  pauseResumeButton=(TextView)section.getChildAt(section.getChildCount()-1);
  walkPaused=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getBoolean("walk_paused",false);refreshPauseResumeButton();
  button("⏮   一つ前の文章に戻る / Previous phrase",()->{Intent i=new Intent(this,WalkService.class).setAction(WalkService.PREVIOUS);startService(i);});
  button("■   学習を終了 / Stop session",()->{stopService(new Intent(this,WalkService.class));playbackState.setText("Stopped / 学習を終了しました");walkPaused=false;refreshPauseResumeButton();});
  card("🎙  Conversation");
  button("朝の会話10分 / Morning Conversation",()->startConversation("morning",10));
  button("帰りの会話10分 / Evening Conversation",()->startConversation("evening",10));
  button("3分だけ会話 / Easy Conversation",()->startConversation("morning",3));
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
  if(requestCode==CSV_IMPORT){if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null){try(java.io.InputStream in=getContentResolver().openInputStream(data.getData());java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);org.json.JSONArray qa=QaBank.parse(out.toString("UTF-8"));String filename="Imported CSV";try{android.database.Cursor cursor=getContentResolver().query(data.getData(),null,null,null,null);if(cursor!=null){try{if(cursor.moveToFirst()){int col=cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);if(col>=0)filename=cursor.getString(col);}}finally{cursor.close();}}}catch(Exception ignored){}QaBank.add(this,QaBank.displayFilename(filename),qa);Toast.makeText(this,QaBank.displayFilename(filename)+" : "+qa.length()+"件を追加",Toast.LENGTH_LONG).show();recreate();}catch(Exception e){new AlertDialog.Builder(this).setTitle("CSV Import error").setMessage(e.getMessage()).setPositiveButton("OK",null).show();}}return;}
  if(requestCode!=GOOGLE_SIGN_IN)return;
  try{
   GoogleSignInAccount account=GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
   if(account==null||!"sasaki0369@gmail.com".equalsIgnoreCase(account.getEmail())){syncMessage("このGoogleアカウントは利用できません");return;}
   String idToken=account.getIdToken();if(idToken==null){syncMessage("Google IDトークンを取得できません。FirebaseのSHA-1登録をご確認ください。");return;}
   firebase.loginWithGoogleIdToken(idToken,message->{refreshLoginUI();syncMessage(message);});
  }catch(ApiException e){syncMessage("Googleログインエラー: "+e.getStatusCode()+"。SHA-1とOAuth設定をご確認ください。");}
 }
 private float speedValue(int step){return 0.6f+Math.max(0,Math.min(12,step))*0.1f;}
 private void updateGapLabel(int step){if(gapLabel!=null)gapLabel.setText(String.format(Locale.US,"Pause between phrases: %.1f sec / 発話間隔",Math.max(0,Math.min(20,step))*0.5f));}
 private void updateSpeedLabel(int step){if(speedLabel!=null)speedLabel.setText(String.format(Locale.US,"Speech speed: %.1f×",speedValue(step)));}
 private void setCurrentPhraseKey(String key){
  currentPhraseKey=key==null?"":key;
  if(playCountLabel!=null)playCountLabel.setText("英語の再生回数： "+getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getInt("playcount_v43:"+currentPhraseKey,0)+" 回");
  if(masteredCheck!=null){masteredCheck.setOnCheckedChangeListener(null);
   masteredCheck.setEnabled(!currentPhraseKey.isEmpty());
   masteredCheck.setChecked(getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getStringSet("mastered_phrases",java.util.Collections.emptySet()).contains(currentPhraseKey));
   masteredCheck.setOnCheckedChangeListener((button,checked)->{
    if(currentPhraseKey.isEmpty())return;
    java.util.Set<String> saved=new java.util.HashSet<>(getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getStringSet("mastered_phrases",java.util.Collections.emptySet()));
    if(checked)saved.add(currentPhraseKey);else saved.remove(currentPhraseKey);
    getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().putStringSet("mastered_phrases",saved).apply();
   });
  }
 }
 private void reviewMastered(){
  java.util.Set<String> mastered=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).getStringSet("mastered_phrases",java.util.Collections.emptySet());
  new AlertDialog.Builder(this).setTitle("習得済み教材 / Mastered ("+mastered.size()+")")
   .setMessage("チェックした英文・単語は次回以降の再生対象から除外されます。\n\n全件を再学習する場合はチェックを一括解除できます。")
   .setNegativeButton("閉じる",null).setPositiveButton("すべて解除",(d,w)->{
    getSharedPreferences("gaba_walk_v1",MODE_PRIVATE).edit().remove("mastered_phrases").apply();setCurrentPhraseKey(currentPhraseKey);
   }).show();
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
 private LinearLayout bottomTabs(String active){
  LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);tabs.setBackgroundColor(Color.WHITE);
  String[] labels={"⌂\nHome","⚑\nMissions","🎧\nWalk","▤\nPractice","▥\nGrowth"};
  String[] targets={"home","missions","walk","practice","growth"};
  for(int j=0;j<labels.length;j++){
   final String target=targets[j];TextView item=new TextView(this);item.setText(labels[j]);item.setTextSize(12);item.setTypeface(null,Typeface.BOLD);
   item.setGravity(Gravity.CENTER);item.setTextColor(target.equals(active)?Color.rgb(6,99,141):Color.rgb(95,120,138));
   if(target.equals(active))item.setBackground(gradient(Color.rgb(233,246,252),Color.rgb(233,246,252),13));
   tabs.addView(item,new LinearLayout.LayoutParams(0,dp(61),1));
   item.setOnClickListener(v->{if("walk".equals(target))showNativeWalk();else showWebTab(target);});
  }
  return tabs;
 }
 private void showWebTab(String target){pendingWebTab=target;openWeb();if(webView!=null)webView.evaluateJavascript("if(typeof tab==='function')tab('"+target+"')",null);}
 private void openWeb(){
  if(webView==null){
   webView=new WebView(this);webView.getSettings().setJavaScriptEnabled(true);webView.getSettings().setDomStorageEnabled(true);
   webView.getSettings().setMediaPlaybackRequiresUserGesture(true);
   webView.setWebViewClient(new WebViewClient(){
    @Override public void onPageFinished(WebView view,String url){view.evaluateJavascript("if(typeof tab==='function')tab('"+pendingWebTab+"')",null);}
    private boolean navigate(android.net.Uri uri){
     if("gabagrowth".equalsIgnoreCase(uri.getScheme())&&"walk".equalsIgnoreCase(uri.getHost())){runOnUiThread(()->showNativeWalk());return true;}
     if(!"https".equalsIgnoreCase(uri.getScheme())||!"sasaki0369.github.io".equalsIgnoreCase(uri.getHost())){
      try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception e){Toast.makeText(MainActivity.this,"リンクを開けません",Toast.LENGTH_SHORT).show();}return true;
     }return false;
    }
    @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return navigate(request.getUrl());}
    @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return navigate(android.net.Uri.parse(url));}
   });
   webView.setWebChromeClient(new WebChromeClient());
   webView.loadUrl("https://sasaki0369.github.io/GabaGrowth/?embedded=android&v=34");
  }
  LinearLayout container=new LinearLayout(this);container.setOrientation(LinearLayout.VERTICAL);container.setBackgroundColor(Color.WHITE);
  if(webView.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)webView.getParent()).removeView(webView);
  container.addView(webView,new LinearLayout.LayoutParams(-1,0,1));container.addView(bottomTabs(pendingWebTab));
  webContainer=container;setContentView(container);applySafeInsets(container);
 }
 private void showNativeWalk(){
  if(nativeWalkScreen==null)return;
  if(nativeWalkScreen.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)nativeWalkScreen.getParent()).removeView(nativeWalkScreen);
  LinearLayout container=new LinearLayout(this);container.setOrientation(LinearLayout.VERTICAL);
  container.addView(nativeWalkScreen,new LinearLayout.LayoutParams(-1,0,1));container.addView(bottomTabs("walk"));
  setContentView(container);applySafeInsets(container);
 }
  @Override protected void onResume(){super.onResume();refreshLoginUI();if(!receiverRegistered){IntentFilter f=new IntentFilter(WalkService.ACTION_STATUS);if(Build.VERSION.SDK_INT>=33)registerReceiver(walkUpdates,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(walkUpdates,f);receiverRegistered=true;}android.content.SharedPreferences p=getSharedPreferences("gaba_walk_v1",MODE_PRIVATE);if(spokenText!=null)spokenText.setText(p.getString("last_phrase","学習を開始すると英文が表示されます。"));if(answerText!=null)answerText.setText(p.getString("last_answer","会話モードで発言すると認識結果が表示されます。"));if(translatedText!=null)translatedText.setText(p.getString("last_translation","英文の日本語訳を表示します。"));setCurrentPhraseKey(p.getString("last_phrase_key",""));walkPaused=p.getBoolean("walk_paused",false);refreshPauseResumeButton();}
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
