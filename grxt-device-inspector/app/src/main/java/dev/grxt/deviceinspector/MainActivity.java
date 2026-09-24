package dev.grxt.deviceinspector;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.*;
import android.view.*;
import android.content.res.ColorStateList;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout content;
    private final int BG=Color.rgb(11,15,20), CARD=Color.rgb(25,31,38), TEXT=Color.WHITE, MUTED=Color.rgb(170,180,190), BLUE=Color.rgb(77,163,255), GREEN=Color.rgb(110,220,120), YELLOW=Color.rgb(255,190,60);
    private int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+.5f); }

    @Override public void onCreate(Bundle b){ super.onCreate(b); getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG); showHome(); }

    private TextView t(String s,int sp,int c,boolean bold){ TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(c); v.setPadding(dp(4),dp(4),dp(4),dp(4)); if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return v; }
    private LinearLayout col(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout card(String title){ LinearLayout c=col(); c.setBackgroundColor(CARD); c.setPadding(dp(14),dp(12),dp(14),dp(12)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(dp(12),dp(6),dp(12),dp(6)); c.setLayoutParams(p); if(title!=null)c.addView(t(title,16,BLUE,true)); return c; }
    private void kv(LinearLayout c,String k,String v){ LinearLayout r=row(); TextView a=t(k,14,MUTED,false), b=t(v,14,TEXT,true); r.addView(a,new LinearLayout.LayoutParams(0,-2,1)); r.addView(b); c.addView(r); }
    private String read(String path){ try(BufferedReader br=new BufferedReader(new FileReader(path))){ StringBuilder s=new StringBuilder(); String x; while((x=br.readLine())!=null)s.append(x).append('\n'); return s.toString().trim(); }catch(Exception e){return "—";} }
    private String prop(String k){ try{ Process p=Runtime.getRuntime().exec(new String[]{"getprop",k}); BufferedReader b=new BufferedReader(new InputStreamReader(p.getInputStream())); String s=b.readLine(); return s==null||s.isEmpty()?"—":s; }catch(Exception e){return "—";} }
    private String fmt(long n){ double g=n/1073741824.0; if(g>=1)return String.format(Locale.US,"%.2f GB",g); return String.format(Locale.US,"%.0f MB",n/1048576.0); }
    private long memTotal(){ String m=read("/proc/meminfo"); try{ for(String l:m.split("\n"))if(l.startsWith("MemTotal:"))return Long.parseLong(l.replaceAll("[^0-9]",""))*1024L; }catch(Exception ignored){} return 0; }
    private long memAvail(){ String m=read("/proc/meminfo"); try{ for(String l:m.split("\n"))if(l.startsWith("MemAvailable:"))return Long.parseLong(l.replaceAll("[^0-9]",""))*1024L; }catch(Exception ignored){} return 0; }
    private boolean hasRoot(){ try{ Process p=Runtime.getRuntime().exec(new String[]{"su","-c","id"}); return p.waitFor()==0; }catch(Exception e){ return false; } }

    private void shell(String title, int active){ LinearLayout root=col(); root.setBackgroundColor(BG);
        TextView h=t(title,22,TEXT,true); h.setPadding(dp(18),dp(16),dp(18),dp(8)); root.addView(h);
        ScrollView sv=new ScrollView(this); content=col(); sv.addView(content); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=row(); String[] ns={"Главная","Батарея","Система","Диагностика"}; for(int i=0;i<4;i++){ final int x=i; Button b=new Button(this); b.setText(ns[i]); b.setTextColor(i==active?BLUE:MUTED); b.setTextSize(12); b.setAllCaps(false); b.setBackgroundColor(BG); b.setOnClickListener(v->{ if(x==0)showHome(); else if(x==1)showBattery(); else if(x==2)showSystem(); else showDiag(); }); nav.addView(b,new LinearLayout.LayoutParams(0,dp(58),1)); } root.addView(nav); setContentView(root); }

    private void showHome(){ shell("GRXT Device Inspector",0);
        LinearLayout d=card(null); d.addView(t(Build.MANUFACTURER+" "+Build.MODEL,20,TEXT,true)); kv(d,"Кодовое имя",Build.DEVICE); kv(d,"Fingerprint",Build.FINGERPRINT); content.addView(d);
        LinearLayout basics=card("Устройство"); kv(basics,"Android",Build.VERSION.RELEASE+" / SDK "+Build.VERSION.SDK_INT); kv(basics,"Kernel",System.getProperty("os.version","—")); kv(basics,"Root",hasRoot()?"Magisk / SU доступен":"Нет доступа SU"); kv(basics,"SELinux",read("/sys/fs/selinux/enforce").equals("1")?"Enforcing":"Permissive/Unknown"); content.addView(basics);
        long total=memTotal(), free=memAvail(); LinearLayout ram=card("Оперативная память (RAM)"); kv(ram,"Всего",fmt(total)); kv(ram,"Использовано",fmt(Math.max(0,total-free))); kv(ram,"Доступно",fmt(free)); content.addView(ram);
        StatFs st=new StatFs("/data"); long tt=st.getTotalBytes(), ff=st.getAvailableBytes(); LinearLayout store=card("Хранилище /data"); kv(store,"Всего",fmt(tt)); kv(store,"Занято",fmt(tt-ff)); kv(store,"Свободно",fmt(ff)); content.addView(store);
        LinearLayout quick=card("Быстрая информация"); kv(quick,"Аптайм",formatUptime(SystemClock.elapsedRealtime())); kv(quick,"Treble",prop("ro.treble.enabled")); kv(quick,"Dynamic partitions",prop("ro.boot.dynamic_partitions")); kv(quick,"A/B",prop("ro.build.ab_update")); content.addView(quick);
    }

    private String formatUptime(long ms){ long m=ms/60000, d=m/1440; m%=1440; long h=m/60; m%=60; return d+" д. "+h+" ч. "+m+" м."; }

    private void showBattery(){ shell("Батарея",1); BatteryManager bm=(BatteryManager)getSystemService(BATTERY_SERVICE); Intent in=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); int level=in==null?0:in.getIntExtra(BatteryManager.EXTRA_LEVEL,0); int temp=in==null?0:in.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0); int volt=in==null?0:in.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0); long current=bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW); double watts=Math.abs((current/1000000.0)*(volt/1000.0));
        LinearLayout preview=card("Предпросмотр статус-бара"); kv(preview,"01:45",String.format(Locale.US,"%.1f°C · %.1fW",temp/10.0,watts)); content.addView(preview);
        LinearLayout stat=card("Состояние батареи"); kv(stat,"Заряд",level+"%"); kv(stat,"Температура",String.format(Locale.US,"%.1f °C",temp/10.0)); kv(stat,"Ток",String.format(Locale.US,"%.0f mA",current/1000.0)); kv(stat,"Мощность (оценка)",String.format(Locale.US,"%.2f W",watts)); kv(stat,"Напряжение",String.format(Locale.US,"%.3f V",volt/1000.0)); kv(stat,"Ёмкость charge counter",bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)+" µAh"); content.addView(stat);
        LinearLayout cfg=card("Индикатор в статус-баре"); Switch sw=new Switch(this); sw.setText("Показывать в статус-баре"); sw.setTextColor(TEXT); sw.setChecked(getPreferences(0).getBoolean("status",true)); sw.setOnCheckedChangeListener((a,b)->getPreferences(0).edit().putBoolean("status",b).apply()); cfg.addView(sw);
        String[] vals={"Температура","Мощность зарядки","Ток","Напряжение","Процент"}; Spinner p1=new Spinner(this); p1.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,vals)); cfg.addView(t("Основной показатель",13,MUTED,false)); cfg.addView(p1); Spinner p2=new Spinner(this); p2.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,vals)); p2.setSelection(1); cfg.addView(t("Второй показатель",13,MUTED,false)); cfg.addView(p2); Switch two=new Switch(this); two.setText("Показывать второй"); two.setTextColor(TEXT); two.setChecked(true); cfg.addView(two); TextView note=t("SystemUI-хук для вывода текста рядом со временем будет отдельным совместимым слоем. В v0.1 настройка и предпросмотр уже работают, но SystemUI не патчится — это специально, чтобы первая прошивка не могла вызвать бутлуп.",12,YELLOW,false); cfg.addView(note); content.addView(cfg);
    }

    private void showSystem(){ shell("Система",2);
        LinearLayout cpu=card("Процессор (CPU)"); kv(cpu,"Hardware",Build.HARDWARE); kv(cpu,"ABI",Build.SUPPORTED_ABIS.length>0?Build.SUPPORTED_ABIS[0]:"—"); kv(cpu,"Ядер",String.valueOf(Runtime.getRuntime().availableProcessors())); kv(cpu,"Governor",read("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")); kv(cpu,"CPU info",firstLine(read("/proc/cpuinfo"))); content.addView(cpu);
        LinearLayout ker=card("Ядро (Kernel)"); kv(ker,"Версия",System.getProperty("os.version","—")); kv(ker,"/proc/version",read("/proc/version")); kv(ker,"cmdline",read("/proc/cmdline")); content.addView(ker);
        LinearLayout part=card("Разделы"); String[] ps={"/data","/system","/vendor","/product","/system_ext","/odm","/metadata"}; for(String p:ps){ try{ StatFs s=new StatFs(p); kv(part,p,fmt(s.getAvailableBytes())+" свободно / "+fmt(s.getTotalBytes())); }catch(Exception ignored){} } kv(part,"Dynamic",prop("ro.boot.dynamic_partitions")); kv(part,"Slot",prop("ro.boot.slot_suffix")); kv(part,"Treble",prop("ro.treble.enabled")); content.addView(part);
        LinearLayout props=card("Android properties"); kv(props,"Build type",Build.TYPE); kv(props,"Tags",Build.TAGS); kv(props,"Security patch",Build.VERSION.SECURITY_PATCH); kv(props,"Bootloader",Build.BOOTLOADER); kv(props,"Verified boot",prop("ro.boot.verifiedbootstate")); content.addView(props);
    }
    private String firstLine(String s){ int i=s.indexOf('\n'); return i<0?s:s.substring(0,i); }

    private void showDiag(){ shell("Диагностика",3); Button run=new Button(this); run.setText("Полная проверка"); run.setTextSize(17); run.setTextColor(Color.WHITE); run.setAllCaps(false); run.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(35,92,150))); LinearLayout box=card(null); box.addView(run); content.addView(box); run.setOnClickListener(v->runDiag()); runDiag(); }
    private void runDiag(){ while(content.getChildCount()>1) content.removeViewAt(1); LinearLayout r=card("Результаты проверки"); diag(r,"CPU",Runtime.getRuntime().availableProcessors()>0); diag(r,"RAM",memTotal()>0); try{diag(r,"Storage",new StatFs("/data").getTotalBytes()>0);}catch(Exception e){diag(r,"Storage",false);} diag(r,"Root (Magisk/SU)",hasRoot()); diag(r,"Treble",!prop("ro.treble.enabled").equals("—")); diag(r,"Kernel",!read("/proc/version").equals("—")); content.addView(r);
        Intent in=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); int temp=in==null?0:in.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0); StatFs s=new StatFs("/data"); int free=(int)(100*s.getAvailableBytes()/Math.max(1,s.getTotalBytes())); LinearLayout w=card("Предупреждения"); if(temp>=430)kv(w,"⚠ Температура батареи",String.format(Locale.US,"%.1f°C",temp/10.0)); if(free<10)kv(w,"⚠ /data свободно",free+"%"); if(read("/sys/fs/selinux/enforce").equals("0"))kv(w,"⚠ SELinux","Permissive"); if(w.getChildCount()==1)kv(w,"Состояние","Критических предупреждений не найдено"); content.addView(w); }
    private void diag(LinearLayout r,String n,boolean ok){ kv(r,(ok?"✓ ":"✗ ")+n,ok?"OK":"FAIL"); }
}
