package com.example.ireminder;

import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.example.ireminder.Text;
import com.example.ireminder.Conv;
import com.example.ireminder.R;


public class MainActivity extends Activity {
	public Button button;
	public TextView textView1;
	public TextView textView2;
	public ProgressBar progressBar1;
	
	//对话列表
	public ArrayList<Conv> convlist;
	
	//如果没有约会，empty为true
	public boolean empty = true;
	public int hourtoset;
	public int minutetoset;
	public int daytoset;
	public int monthtoset;
	public int yeartoset;
	
	//约会日期与短信日期的天数差
	public int offset;
	public int dayofweek;
	
	//是否找到具体时间信息
	public boolean findspecifictime;
	
	//如果string与时间列表有匹配，该值记录string中匹配的起始下标
	public int timematchstartindex;
	
	//产生匹配的时间列表下标
	public int timelistindex;
	
	//如果string与天数列表有匹配，该值记录string中匹配的起始下标
	public int daymatchstartindex;
		
	//产生匹配的天数列表下标
	public int daylistindex;
	
	//动词列表
	public String[] verblist = {"吃饭","聚会","约会","出来","看电影","开会","集合","答辩","展示","见客户"};
	
	//确定短语列表
	public String[] confirmlist = {"收到","行","好","没问题","就这么定了","OK","ok","可以","好的"};
	
	//地点介词列表
	public String[] placepreplist = {"去","到","在"};
	
	//时间列表
	public String[] timelist = {"*:**","**:**","*点","**点","*点**","**点**","*点半","**点半","*点一刻","**点一刻"};//*在前，**在后
	
	//天数列表
	public String[] daylist = {"今天","明天","后天","星期*","下星期*","周*","下周*","*日","**日","*号","**号","月底"};//*在前，**在后

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button)findViewById(R.id.button1);
        textView1 = (TextView)findViewById(R.id.textView1);
        textView2 = (TextView)findViewById(R.id.textView2);
        progressBar1 = (ProgressBar)findViewById(R.id.progressBar1);
        progressBar1.setVisibility(ProgressBar.INVISIBLE);
        button.setText("press to set appointment");
        textView1.setText("");
        textView2.setText("");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    /*
     * 按钮响应函数
     */
    public void click(View v) {
    	button.setVisibility(Button.INVISIBLE);
    	textView1.setText("processing...please wait");
    	progressBar1.setVisibility(ProgressBar.VISIBLE);
    	empty = true;
    	try {
    		
    		//初始化对话列表
    		getSms();
    		
    		//手动生成对话，测试用
    		//initconvlistfortest();
    		
    		//显示对话，测试用
    		//displaytext();
    		
    		//分析对话
    		analyse();
    	}
    	catch (Exception e) {
    		
    	}
    	
    	//如果没有约会
    	if(empty) {
    		textView1.setText("no appointment to be set");
    		progressBar1.setVisibility(ProgressBar.INVISIBLE);
    	}
    	else {
    		textView1.setText("done!");
    		progressBar1.setVisibility(ProgressBar.INVISIBLE);
    	}
    }
    
    /*
     * 设置日历事件
     * 参数：date日期，location地点，description事件描述
     */
    public void setCalendar(Date date, String location, String description) {
    	 Intent calIntent = new Intent(Intent.ACTION_INSERT);
    	 calIntent.setType("vnd.android.cursor.item/event"); 
    	 calIntent.putExtra(Events.TITLE, "提醒事件"); 
    	 calIntent.putExtra(Events.EVENT_LOCATION, location);
    	 calIntent.putExtra(Events.DESCRIPTION, description);   
    	 calIntent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);
    	 calIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, date.getTime());
    	 calIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, date.getTime());  
    	 startActivity(calIntent); 
    }
    
    /*
     * 将手机短信读入对话列表
     */
    public void getSms() {
        final String SMS_URI_ALL   = "content://sms/"; 
        try{   
            ContentResolver cr = getContentResolver();   
            String[] projection = new String[]{"_id", "address", "person",    
                    "body", "date", "type"};   
            Uri uri = Uri.parse(SMS_URI_ALL);   
            Cursor cur = cr.query(uri, projection, null, null, "date asc");
            
            String lastphonenumber = "";
            convlist = new ArrayList<Conv>();
            int index = -1;
      
            if (cur.moveToFirst()) {   
                String name;    
                String phonenumber;          
                String smsbody;      
                
                int phonenumberColumn = cur.getColumnIndex("address");   
                int smsbodyColumn = cur.getColumnIndex("body");   
                int dateColumn = cur.getColumnIndex("date");   
                int typeColumn = cur.getColumnIndex("type");   
                
                do{   
                    name = "X";
                    phonenumber = cur.getString(phonenumberColumn);
                    String originnumber = phonenumber;
                    
                    //去掉号码中的无用字符
                    phonenumber = phonenumber.replace("-","");
                    phonenumber = phonenumber.replace(" ","");
                    int len = phonenumber.length();
                    phonenumber = phonenumber.substring(len-11, len);
                    
                    //如果电话号码改变，产生新的对话
                    if(!phonenumber.equals(lastphonenumber))
                    {
                    	//在联系人列表中查找号码对应的联系人姓名
                    	Uri personUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, originnumber);
                        Cursor cursor = getContentResolver().query(personUri, new String[]{PhoneLookup.DISPLAY_NAME}, null, null, null);
                        if(cursor.moveToFirst()) {
                        	int nameIndex = cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME);
                            name = cursor.getString(nameIndex);
                            cursor.close();
                        }
                        
                        //如果没有该联系人或未找到姓名，将姓名赋为电话号码
                        else {
                        	name = phonenumber;
                        }
                        
                        //将对话加入对话列表
                    	convlist.add(new Conv(phonenumber, name));
                    	index++;
                    	lastphonenumber = phonenumber;
                    }
                    
                    //将短信加入对话
                    smsbody = cur.getString(smsbodyColumn);   
                    if(smsbody == null) smsbody = "";
                    Date date = new Date(Long.parseLong(cur.getString(dateColumn)));   
                    int typeId = cur.getInt(typeColumn);
                    
                    convlist.get(index).textlist.add(new Text(smsbody, date, typeId));
                    
                }while(cur.moveToNext());   
            }
        } 
        catch(SQLiteException ex) {   
            Log.d("SQLiteException in getSmsInPhone", ex.getMessage());   
        }   
    }
    
    /*
     * 显示对话列表，测试用
     */
    public void displaytext() {
    	StringBuilder display = new StringBuilder();
    	for(int i = 0; i < convlist.size(); i++) {
    		display.append(convlist.get(i).phone + "/" + convlist.get(i).name + "\n");
    		for(int j = 0; j < convlist.get(i).textlist.size(); j++) {
    			display.append(convlist.get(i).textlist.get(j).body + "/" + convlist.get(i).textlist.get(j).date.toString() + "/" + convlist.get(i).textlist.get(j).type + "\n");
    		}
    	}
    	textView2.setText(display);
    }
    
    /*
     * 手动产生对话，测试用
     */
    public void initconvlistfortest() {
    	convlist = new ArrayList<Conv>();
    	convlist.add(new Conv("18810446564", "李子瑨"));
 	    convlist.get(0).textlist.add(new Text("在忙吗，明天一起吃饭吧？", new Date(System.currentTimeMillis()),1));
    	convlist.get(0).textlist.add(new Text("明天没空额，后天吧", new Date(System.currentTimeMillis()),2));
    	convlist.get(0).textlist.add(new Text("那后天晚上06:00点，去肯德基？", new Date(System.currentTimeMillis()),1));
    	convlist.get(0).textlist.add(new Text("肯德基吃腻了都，去必胜客吃披萨如何。六点早了点，六点半吧", new Date(System.currentTimeMillis()),2));
    	convlist.get(0).textlist.add(new Text("那就这么定了，明天见喽！", new Date(System.currentTimeMillis()),1));
    	convlist.add(new Conv("13233223232", "王小二"));
    	convlist.get(1).textlist.add(new Text("下周二咱俩的答辩记得准备一下~", new Date(System.currentTimeMillis()),1));
    	convlist.get(1).textlist.add(new Text("不是改成月底了吗？", new Date(System.currentTimeMillis()),2));
    	convlist.get(1).textlist.add(new Text("我忘记了。那也提早准备吧。", new Date(System.currentTimeMillis()),1));
    	convlist.get(1).textlist.add(new Text("好的。", new Date(System.currentTimeMillis()),2));
//    	convlist.add(new Conv("13233223232", "王小二"));
//    	convlist.get(1).textlist.add(new Text("明天聚会有空吗？", new Date(System.currentTimeMillis()),1));
//    	convlist.get(1).textlist.add(new Text("什么时候？", new Date(System.currentTimeMillis()),2));
//    	convlist.get(1).textlist.add(new Text("8:45，在麦当劳", new Date(System.currentTimeMillis()),1));
//    	convlist.get(1).textlist.add(new Text("行", new Date(System.currentTimeMillis()),2));
    }
    
    /*
     * 分析对话
     */
    @SuppressWarnings("deprecation")
	public void analyse() {
    	String verb = new String("");
    	Date date = new Date();
    	date = null;
    	String location = new String("");
    	String description = new String("");
    	
    	//扫描每条短信
    	for(int i = 0; i < convlist.size(); i++) {
    		for (int j = 0; j < convlist.get(i).textlist.size(); j++) {
    			
    			verb = "";
    			date = null;
    			location = "";
    			description = "";
    			
    			//找动词
    			verb = findverb(i,j);
    			
    			//如果找到动词
    			if(!verb.equals("")) {
    				
    				//根据时间差找到属于一段对话的textno最小值和最大值
    				int min = j, max = j, confirm = -1;
    				
    				while(min > 0 && convlist.get(i).textlist.get(min).date.getTime() - convlist.get(i).textlist.get(min-1).date.getTime() < 30*60*1000 ) {
    					min--;
    				}
    				while(max < convlist.get(i).textlist.size()-1 && convlist.get(i).textlist.get(max+1).date.getTime() - convlist.get(i).textlist.get(max).date.getTime() < 30*60*1000 ) {
    					max++;
    				}
    				
    				//找肯定短语
    				confirm = findconfirm(i, j, max);
    				
    				
    				
    				//如果找到肯定短语
    				if(confirm != -1) {
    					
    					//找时间
    					date = finddate(i, min, confirm);
    					
    					//如果找到时间，并且约会时间在当前时间以后，如果时间合法，则可以生成日历事件
        				if(date != null && date.getTime() >= System.currentTimeMillis()) {
        			    //if(date != null) {	
        					//找地点
        					location = findlocation(i, min, confirm);
        					
        					//根据是否找到地点设定不同的日历描述
        					if(!location.equals("")) {
        						if(findspecifictime) {
        							description = String.valueOf(date.getHours()) + ":" + String.valueOf(date.getMinutes()) + " 与" + convlist.get(i).name + "在" + location + verb;
        						}
        						else {
        							description = "与" + convlist.get(i).name + "在" + location + verb;
        						}
        					}
        					else {
        						location = "未知";
        						if(findspecifictime) {
        							description = String.valueOf(date.getHours()) + ":" + String.valueOf(date.getMinutes()) + " 与" + convlist.get(i).name + verb;
        						}
        						else {
        							description = "与" + convlist.get(i).name + verb;
        						}
        					}
        					
        					//显示日历事件信息，测试用
        					//displaycalendartest(date, "地点：" + location, description);
        					
        					//设定日历事件
        					setCalendar(date, "地点：" + location, description);
        					empty = false;
        				}
        				
        				j = confirm;
    				}
    			}
    		}
    	}
    }
    
    /*
     * 查找动词
     * 参数：convno要查找的对话序号，textno要查找的对话中的短信序号
     * 返回值：找到返回动词，未找到返回""
     */
    public String findverb(int convno, int textno) {
    	for(int i = 0; i < verblist.length; i++) {
    		if(convlist.get(convno).textlist.get(textno).body.contains(verblist[i])) {
    			return verblist[i];
    		}
    	}
    	return "";
    }
    
    /*
     * 查找肯定短语
     * 参数：convno要查找的对话序号，min要查找的对话中短信序号的起始值，max要查找的对话中短信序号的结束值
     * 返回值：找到返回肯定短语所在的短信序号，未找到返回-1
     */
    public int findconfirm(int convno, int min, int max) {
    	for(int i = max; i >= min; i--) {
    		for(int j = 0; j < confirmlist.length; j++)
    		if(convlist.get(convno).textlist.get(i).body.contains(confirmlist[j])) {
    			return i;
    		}
    	}
    	return -1;
    }
    
    /*
     * 查找时间
     * 参数：convno要查找的对话序号，min要查找的对话中短信序号的起始值，max要查找的对话中短信序号的结束值
     * 返回值：找到返回设定好的日期，未找到返回null
     */
	@SuppressWarnings("deprecation")
	public Date finddate(int convno, int min, int max) {
		findspecifictime = false;
		
		//设置24小时制时用来判断时间是am还是pm
		boolean pmset = false;
		
		//记录是否找到
    	boolean find = false;
    	
    	//origin为对话发生的时间，也是设置约会时间的依据
    	Date origin = convlist.get(convno).textlist.get(min).date;
    	hourtoset = 0;
    	minutetoset = 0;
    	offset = 0;
    	daytoset = origin.getDate();
    	dayofweek = origin.getDay();
    	monthtoset = origin.getMonth();
    	yeartoset = origin.getYear();
    	for(int i = min; i <= max; i++) {
    		
    		//将中文数字转换成阿拉伯数字，方便抽取信息
    		convlist.get(convno).textlist.get(i).body = convlist.get(convno).textlist.get(i).body.replace('一', '1')
    		.replace('二', '2').replace('两', '2').replace('三', '3').replace('四', '4').replace('五', '5')
    		.replace('六', '6').replace('七', '7').replace('八', '8').replace('九', '9').replace('零', '0')
    		.replace("星期日", "星期7").replace("星期天", "星期7").replace("周日", "周7").replace("周天", "周7");
    		
    		//“十”比较特殊，分情况设置
    		int tenindex = convlist.get(convno).textlist.get(i).body.indexOf("十");
    		while(tenindex != -1)
    		{
    			if(tenindex == 0) {
    				int oor = (int)convlist.get(convno).textlist.get(i).body.charAt(tenindex + 1);
    				//十*
    				if(oor >= 48 && oor <= 57) {
    					convlist.get(convno).textlist.get(i).body = convlist.get(convno).textlist.get(i).body.replaceFirst("十", "1");
    				}
    				//十
    				else {
    					convlist.get(convno).textlist.get(i).body = convlist.get(convno).textlist.get(i).body.replaceFirst("十", "10");
    				}
    			}
    			else if(tenindex == convlist.get(convno).textlist.get(i).body.length() - 1) {
    				int oof = (int)convlist.get(convno).textlist.get(i).body.charAt(tenindex - 1);
    				//*十
    				if(oof >= 48 && oof <= 57) {
    					convlist.get(convno).textlist.get(i).body = convlist.get(convno).textlist.get(i).body.replaceFirst("十", "0");
    				}
    				//十
    				else {
    					convlist.get(convno).textlist.get(i).body = convlist.get(convno).textlist.get(i).body.replaceFirst("十", "10");
    				}
    			}
    			else {
    				int f = (int)convlist.get(convno).textlist.get(i).body.charAt(tenindex - 1);
        			int r = (int)convlist.get(convno).textlist.get(i).body.charAt(tenindex + 1);
        			if(f >= 48 && f <= 57) {
        				//*十*
        				if(r >= 48 && r <= 57) {
        					convlist.get(convno).textlist.get(i).body = convlist.get(convno).textlist.get(i).body.replaceFirst("十", "");
        				}
        				//*十
        				else {
        					convlist.get(convno).textlist.get(i).body = convlist.get(convno).textlist.get(i).body.replaceFirst("十", "0");
        				}
        			}
        			else {
        				//十*
        				if(r >= 48 && r <= 57) {
        					convlist.get(convno).textlist.get(i).body = convlist.get(convno).textlist.get(i).body.replaceFirst("十", "1");
        				}
        				//十
        				else {
        					convlist.get(convno).textlist.get(i).body = convlist.get(convno).textlist.get(i).body.replaceFirst("十", "10");
        				}
        			}
    			}
    			tenindex = convlist.get(convno).textlist.get(i).body.indexOf("十");
    		}
    		
    		String tempsms = convlist.get(convno).textlist.get(i).body;
    		
    		if(tempsms.contains("下午") || tempsms.contains("晚上")) {
    			pmset = true;
    		}
    		
    		daymatchstartindex = -1;
    		daylistindex = -1;
    		
    		if(havedaymatch(convno, i)) {
    			
    			getoffset(convno, i);
    			find = true;
    		}
    		
    		timematchstartindex = -1;
    		timelistindex = -1;
    		
    		//如果找到时间匹配
    		if(havetimematch(convno, i)) {
    			
    			//设定hourtoset munutetoset数值
    			gethourminute(convno, i);
    			findspecifictime = true;
    		}
    		
    	}
    	
    	//如果找到设定日期
    	if(find) {
    		if(findspecifictime) {
    			if(pmset && hourtoset < 12) {
    				hourtoset += 12;
    			}
    			origin.setHours(hourtoset);
        		origin.setMinutes(minutetoset);
    		}
    		Calendar calendar = new GregorianCalendar(); 
    	    calendar.setTime(origin); 
    	    calendar.add(calendar.DATE, offset);
    	    origin = calendar.getTime();
    		return origin;
    	}
    	return null;
    }
    
	/*
	 * 查找日期匹配
	 * 参数：convno要查找的对话序号，textno要查找的对话中的短信序号
	 * 返回值：找到返回true，未找到返回false
	 */
	public boolean havedaymatch(int convno, int textno) {
		boolean havematch = false;
    	String text = convlist.get(convno).textlist.get(textno).body;
    	for(int i = 0; i < daylist.length; i++) {
    		for(int j = 0; j <= text.length()-daylist[i].length(); j++) {
    			boolean flag = true;
    			for(int k = 0; k < daylist[i].length(); k++) {
    				char a = text.charAt(j+k);
    				if(daylist[i].charAt(k)=='*') {
    					if((int)a<48||(int)a>57) {
    						flag = false;
    					}
    				}
    				else {
    					if(daylist[i].charAt(k)!=a) {
    						flag = false;
    					}
    				}
    				if(!flag) {
    					break;
    				}
    			}
    			if(flag) {
    				daymatchstartindex = j;
    				daylistindex = i;
    				havematch = true;
    			}
    		}
    	}
    	return havematch;
	}
	
	/*
	 * 设定offset数值
	 * 参数：convno要查找的对话序号，textno要查找的对话中的短信序号
	 */
    public void getoffset(int convno, int textno) {
    	String temp = convlist.get(convno).textlist.get(textno).body;
    	int newdayofweek = dayofweek;
    	switch (daylistindex) {
    	case 0 :
			offset = 0;
			break;
		case 1 :
			offset = 1;
			break;
		case 2 :
			offset = 2;
			break;
		case 4:
		case 6:
			if(newdayofweek >= dayofweek) {
				offset = newdayofweek - dayofweek + 7;
			}
			else {
				offset = newdayofweek + 7 - dayofweek;
			}
			break;
		case 3:
		case 5:
			offset = newdayofweek - dayofweek;
			break;
		case 7:
		case 9:
			offset = (int)temp.charAt(daymatchstartindex) - 48 - daytoset;
		case 8:
		case 10:
			offset = ((int)temp.charAt(daymatchstartindex) - 48) * 10 +
			         (int)temp.charAt(daymatchstartindex + 1) - 48 - daytoset;
		case 11:
			 Calendar cal = Calendar.getInstance();
			 cal.set(Calendar.YEAR, yeartoset);
	         cal.set(Calendar.MONTH, monthtoset);
	         offset = cal.getActualMaximum(Calendar.DAY_OF_MONTH) - daytoset;
    	}
    	
    }
	
	/*
	 * 查找时间匹配
	 * 参数：convno要查找的对话序号，textno要查找的对话中的短信序号
	 * 返回值：找到返回true，未找到返回false
	 */
    public boolean havetimematch(int convno, int textno) {
    	boolean havematch = false;
    	String text = convlist.get(convno).textlist.get(textno).body;
    	for(int i = 0; i < timelist.length; i++) {
    		for(int j = 0; j <= text.length()-timelist[i].length(); j++) {
    			boolean flag = true;
    			for(int k = 0; k < timelist[i].length(); k++) {
    				char a = text.charAt(j+k);
    				if(timelist[i].charAt(k)=='*') {
    					if((int)a<48||(int)a>57) {
    						flag = false;
    					}
    				}
    				else {
    					if(timelist[i].charAt(k)!=a) {
    						flag = false;
    					}
    				}
    				if(!flag) {
    					break;
    				}
    			}
    			if(flag) {
    				timematchstartindex = j;
    				timelistindex = i;
    				havematch = true;
    			}
    		}
    	}
    	return havematch;
    }
    
    /*
	 * 设定hourtoset minutetoset数值
	 * 参数：convno要查找的对话序号，textno要查找的对话中的短信序号
	 */
    public void gethourminute(int convno, int textno) {
    	String temp = convlist.get(convno).textlist.get(textno).body;
    	switch (timelistindex) {
    		case 1:
    		case 5:
    			hourtoset = ((int)temp.charAt(timematchstartindex) -48)* 10 +
    					    (int)temp.charAt(timematchstartindex + 1)-48;
    			minutetoset = ((int)temp.charAt(timematchstartindex + 3) -48)* 10 +
					          (int)temp.charAt(timematchstartindex + 4)-48;
    			break;
    		case 7:
    			hourtoset = ((int)temp.charAt(timematchstartindex) -48)* 10 +
			                (int)temp.charAt(timematchstartindex + 1)-48;
    			minutetoset = 30;
    			break;
    		case 9:
    			hourtoset = ((int)temp.charAt(timematchstartindex) -48)* 10 +
			                (int)temp.charAt(timematchstartindex + 1)-48;
    			minutetoset = 15;
    			break;
    		case 3:
    			hourtoset = ((int)temp.charAt(timematchstartindex) -48)* 10 +
			                (int)temp.charAt(timematchstartindex + 1)-48;
    			minutetoset = 0;
    			break;
    		case 0:
    		case 4:
    			hourtoset = (int)temp.charAt(timematchstartindex)-48;
    			minutetoset = ((int)temp.charAt(timematchstartindex + 2) -48)* 10 +
				              (int)temp.charAt(timematchstartindex + 3)-48;
    			break;
    		case 6:
    			hourtoset = (int)temp.charAt(timematchstartindex)-48;
    			minutetoset = 30;
    			break;
    		case 8:
    			hourtoset = (int)temp.charAt(timematchstartindex)-48;
    			minutetoset = 15;
    			break;
    		case 2:
    			hourtoset = (int)temp.charAt(timematchstartindex)-48;
    			minutetoset = 0;
    			break;
    	}
    	
    }
    
    /*
     * 查找地点
     * 参数：convno要查找的对话序号，min要查找的对话中短信序号的起始值，max要查找的对话中短信序号的结束值
     * 返回值：找到返回地点短语，未找到返回""
     */
    public String findlocation(int convno, int min, int max) {
    	String loc = "";
    	for(int i = min; i <= max; i++) {
    		ArrayList<String> parsewordlist = parsesentence(convlist.get(convno).textlist.get(i).body);
    		for(int j = 0; j < placepreplist.length; j++) {
    			for(int k = 0; k < parsewordlist.size(); k++) {
    				if(parsewordlist.get(k).equals(placepreplist[j])) {
    					if(k < parsewordlist.size() -1) {
    						loc = parsewordlist.get(k+1);
    					}
    				}
    			}
    			
    		}
    	}
    	return loc;
    }
    
    /*
     * 显示日历事件信息，测试用
     * 参数：date日期，location地点，description事件描述
     */
    public void displaycalendartest(Date date, String location, String description) {
    	textView2.setText(date.toString() + "/" + location + "/" + description + "\n");
    }
    
    /*
     * 分词函数
     * 参数：要分词的句子
     * 返回值：分词后的句子
     */
    public ArrayList<String> parsesentence(String content) {
    	ArrayList<String> result = new ArrayList<String>();
    	String[] wordlist = {"今天","明天","后天","肯德基","麦当劳","吃饭","一起","没空","晚上","吃腻","要不","必胜客"
    			,"披萨","如何","这么"};
        int maxwordlen = (5 < content.length() ? 5 : content.length());
        while(content.length() > 0) {
        	String sub = "";
        	if(content.length() > maxwordlen) {
        		sub = content.substring(content.length() - maxwordlen);
        	}
        	else {
        		sub = content;
        	}
        	
        	while(sub.length()>1) {
        		boolean find = false;
        		for(int i = 0; i < wordlist.length; i++) {
        			if(wordlist[i].equals(sub)) {
        				find = true;
        				break;
        			}
        		}
        		if(find) {
        			break;
        		}
        		else {
        			sub = sub.substring(1);
        		}
        	}
        	
        	result.add(0, sub);
        	content = content.substring(0, content.length() - sub.length());
        	
        }
    	return result;
    }
}
