package jp.gabagrowth.walk;

import android.content.*;
import android.os.Handler;
import android.os.Looper;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Firebase Auth Google ID token + Realtime Database sync. Never embed database secrets. */
public final class FirebaseSync {
 public interface Callback { void done(String message); }
 private static final String API_KEY="AIzaSyDvRGU9YuF6SDsCIZ0qoOEvN8Qjp2ouGmY";
 private static final String DB="https://gabagrowth-default-rtdb.asia-southeast1.firebasedatabase.app";
 private static final ExecutorService EXEC=Executors.newSingleThreadExecutor();
 private static final Handler MAIN=new Handler(Looper.getMainLooper());
 private final Context context;
 private String token="", uid=""; private long expiresAt=0;
 public FirebaseSync(Context c){context=c.getApplicationContext();}
 private android.content.SharedPreferences prefs(){return context.getSharedPreferences("gaba_firebase_auth_v1",Context.MODE_PRIVATE);}
 private static String readText(InputStream input) throws IOException {
  try(InputStream s=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] b=new byte[4096];int n;while((n=s.read(b))!=-1)out.write(b,0,n);
   return new String(out.toByteArray(),StandardCharsets.UTF_8);
  }
 }
 private static JSONObject call(String url,String method,JSONObject body) throws Exception {
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
  c.setRequestMethod(method);c.setConnectTimeout(12000);c.setReadTimeout(15000);
  c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
  if(body!=null){c.setDoOutput(true);try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
  int status=c.getResponseCode();InputStream stream=status<400?c.getInputStream():c.getErrorStream();
  String data="";if(stream!=null){try(InputStream s=stream){data=readText(s);}}
  c.disconnect();if(status>=400)throw new IOException("HTTP "+status+": "+data.substring(0,Math.min(data.length(),180)));
  return data.isEmpty()||data.equals("null")?new JSONObject():new JSONObject(data);
 }
 private void notify(Callback cb,String s){if(cb!=null)MAIN.post(()->cb.done(s));}
 public void loginWithGoogleIdToken(String googleIdToken,Callback cb){EXEC.execute(()->{try{
  JSONObject body=new JSONObject().put("postBody","id_token="+URLEncoder.encode(googleIdToken,"UTF-8")+"&providerId=google.com").put("requestUri","http://localhost").put("returnSecureToken",true).put("returnIdpCredential",false);
  JSONObject r=call("https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key="+API_KEY,"POST",body);
  String email=r.optString("email","");
  if(!"sasaki0369@gmail.com".equalsIgnoreCase(email)){notify(cb,"許可されていないGoogleアカウントです");return;}
  saveTokens(r.getString("localId"),r.getString("refreshToken"),r.getString("idToken"),r.optLong("expiresIn",3600));
  notify(cb,"Googleログイン成功。学習記録を同期します。");syncNow(cb);
 }catch(Exception e){notify(cb,"Googleログイン失敗: "+e.getMessage());}});}
 private void saveTokens(String u,String refresh,String id,long seconds){uid=u;token=id;expiresAt=System.currentTimeMillis()+Math.max(60,seconds-90)*1000;
  prefs().edit().putString("uid",u).putString("refresh",refresh).apply();}
 private String validToken() throws Exception {
  if(!token.isEmpty()&&System.currentTimeMillis()<expiresAt)return token;
  String refresh=prefs().getString("refresh","");if(refresh.isEmpty())throw new IllegalStateException("ログインが必要です");
  String form="grant_type=refresh_token&refresh_token="+URLEncoder.encode(refresh,"UTF-8");
  HttpURLConnection c=(HttpURLConnection)new URL("https://securetoken.googleapis.com/v1/token?key="+API_KEY).openConnection();
  c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(12000);c.setReadTimeout(15000);
  c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
  try(OutputStream os=c.getOutputStream()){os.write(form.getBytes(StandardCharsets.UTF_8));}
  int status=c.getResponseCode();InputStream stream=status<400?c.getInputStream():c.getErrorStream();
  String data=readText(stream);c.disconnect();
  if(status>=400)throw new IOException("Token refresh HTTP "+status);
  JSONObject r=new JSONObject(data);saveTokens(r.getString("user_id"),r.getString("refresh_token"),r.getString("id_token"),r.optLong("expires_in",3600));return token;
 }
 public boolean hasLogin(){return !prefs().getString("refresh","").isEmpty();}
 public void logout(){prefs().edit().clear().apply();uid="";token="";expiresAt=0;}
 public void syncNow(Callback cb){EXEC.execute(()->{
  if(!hasLogin()){notify(cb,"未ログイン。端末内に記録を保存しています。");return;}
  try{
   String id=validToken();String user=prefs().getString("uid","");
   if(!user.matches("[A-Za-z0-9_-]+"))throw new IOException("Invalid user ID");
   android.content.SharedPreferences local=context.getSharedPreferences("gaba_walk_v1",Context.MODE_PRIVATE);
   JSONArray rows=new JSONArray(local.getString("sessions","[]"));int uploaded=0;
   for(int i=0;i<rows.length();i++){
    JSONObject row=rows.getJSONObject(i);String sessionId=row.optString("id","");
    if(!sessionId.matches("[A-Za-z0-9_-]+"))continue;
    String url=DB+"/gabaGrowth/users/"+user+"/walkSessions/"+sessionId+".json?auth="+URLEncoder.encode(id,"UTF-8");
    call(url,"PUT",row);uploaded++;
   }
   notify(cb,"同期完了："+uploaded+"件の徒歩学習記録を送信しました。");
  }catch(Exception e){notify(cb,"同期待ち（端末内の記録は保持）："+e.getMessage());}
 });}
}
