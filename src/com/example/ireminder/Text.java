package com.example.ireminder;

import java.util.Date;

public class Text {
	
	//短信内容
	public String body;
	
	//短信日期
	public Date date;
	
	//1接收，2发送
	public int type;
	
	public Text(String tempbody, Date tempdate, int temptype){
		body = tempbody;
		date = tempdate;
		type = temptype;
	}

}
