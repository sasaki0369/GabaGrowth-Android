package jp.gabagrowth.walk;
import android.app.*;import android.content.*;import android.os.*;import android.speech.tts.TextToSpeech;import java.util.*;
public class WalkService extends Service implements TextToSpeech.OnInitListener {
 public static final String STOP="STOP", START="START"; private TextToSpeech tts; private Handler h=new Handler(Looper.getMainLooper()); private int i=0;private boolean active=false;private String[] lines;
 private final String[] morning={"Welcome to your morning walk. Listen and repeat.","How do you maintain aging infrastructure?","How do you prioritise road maintenance?","Could you give me a specific example?","How do you share data across organisations?","What are the biggest challenges?"};
 private final String[] evening={"Welcome back. Let's practise your questions.","How do you use digital technology in maintenance?","How do you measure cost savings?","Could you explain that in more detail?","How do local councils work together?","Thank you for sharing your experience."};
 @Override public void onCreate(){super.onCreate();tts=new TextToSpeech(this,this);}
 @Override public int onStartCommand(Intent in,int flags,int id){if(in!=null&&STOP.equals(in.getAction())){stopSelf();return START_NOT_STICKY;}
 String mode=in==null?"morning":in.getStringExtra("mode");int minutes=in==null?10:in.getIntExtra("minutes",10);lines="evening".equals(mode)?evening:morning;
 NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);nm.createNotificationChannel(new NotificationChannel("walk","Walk & Talk",NotificationManager.IMPORTANCE_LOW));
 Intent stop=new Intent(this,WalkService.class).setAction(STOP);PendingIntent pi=PendingIntent.getService(this,0,stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
 Notification n=new Notification.Builder(this,"walk").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Gaba Growth Walk & Talk").setContentText("Audio practice playing").addAction(new Notification.Action.Builder(android.R.drawable.ic_media_pause,"Stop",pi).build()).build();startForeground(10,n);
 active=true;i=0;h.removeCallbacksAndMessages(null);long end=SystemClock.elapsedRealtime()+minutes*60000L;loop(end);return START_NOT_STICKY;}
 private void loop(long end){if(!active)return;if(SystemClock.elapsedRealtime()>=end){tts.speak("Well done. Your walk is complete.",TextToSpeech.QUEUE_FLUSH,null,"end");h.postDelayed(this::stopSelf,3500);return;}
 String s=lines[i++%lines.length];if(tts!=null){tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"line"+i);}h.postDelayed(()->loop(end),Math.max(6500,Math.min(11000,s.length()*115L+4200)));}
 @Override public void onInit(int status){if(status==TextToSpeech.SUCCESS){tts.setLanguage(Locale.US);tts.setSpeechRate(0.86f);}}
 @Override public void onDestroy(){active=false;h.removeCallbacksAndMessages(null);if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
 @Override public android.os.IBinder onBind(Intent in){return null;}
}
