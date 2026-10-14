package jp.gabagrowth.walk;
import android.content.*;
import org.json.*;
import java.io.*;
import java.util.*;
public final class QaBank {
 public static final String PREF="nsw_qa_bank_v1";
 public static final String[] HEADERS={"id","category","organization","topic","level","week","practice_direction","question_en","question_ja","answer_en","answer_ja","followup_en","followup_ja","keywords","notes"};
 public static JSONArray parse(String csv) throws Exception {
  if(csv.startsWith("\uFEFF"))csv=csv.substring(1);
  ArrayList<ArrayList<String>> lines=new ArrayList<>();ArrayList<String> row=new ArrayList<>();StringBuilder field=new StringBuilder();boolean quoted=false;
  for(int i=0;i<csv.length();i++){char c=csv.charAt(i);if(quoted){if(c=='"'&&i+1<csv.length()&&csv.charAt(i+1)=='"'){field.append('"');i++;}else if(c=='"')quoted=false;else field.append(c);}else if(c=='"')quoted=true;else if(c==','){row.add(field.toString());field.setLength(0);}else if(c=='\r'||c=='\n'){if(c=='\r'&&i+1<csv.length()&&csv.charAt(i+1)=='\n')i++;row.add(field.toString());field.setLength(0);if(!row.isEmpty()&&row.stream().anyMatch(v->!v.isEmpty()))lines.add(row);row=new ArrayList<>();}else field.append(c);}
  if(quoted)throw new IllegalArgumentException("Unclosed CSV quote");row.add(field.toString());if(row.stream().anyMatch(v->!v.isEmpty()))lines.add(row);if(lines.isEmpty())throw new IllegalArgumentException("Empty CSV");ArrayList<String> cols=lines.remove(0);for(String h:HEADERS)if(!cols.contains(h))throw new IllegalArgumentException("Missing column: "+h);
  JSONArray result=new JSONArray();HashSet<String> ids=new HashSet<>();for(ArrayList<String> line:lines){if(line.size()!=cols.size())throw new IllegalArgumentException("Invalid column count on row "+(result.length()+2));JSONObject obj=new JSONObject();for(String h:HEADERS)obj.put(h,line.get(cols.indexOf(h)));String id=obj.optString("id");if(id.isEmpty()||obj.optString("question_en").isEmpty()||obj.optString("answer_en").isEmpty()||!ids.add(id))throw new IllegalArgumentException("Invalid or duplicate ID: "+id);result.put(obj);}if(result.length()==0)throw new IllegalArgumentException("No Q&A");return result;
 }
 public static JSONArray load(Context c){try{String value=c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString("items","");if(!value.isEmpty())return new JSONArray(value);try(InputStream in=c.getAssets().open("NSW_GabaGrowth_QA_full_v1.csv")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);return parse(out.toString("UTF-8"));}}catch(Exception ex){return new JSONArray();}}
 public static void save(Context c,JSONArray rows){c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString("items",rows.toString()).apply();}
}
