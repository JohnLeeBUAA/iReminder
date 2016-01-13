package com.example.ireminder;

import java.util.ArrayList;
import com.example.ireminder.Text;

public class Conv {
	
	//电话号码
	public String phone;
	
	//联系人姓名
	public String name;
	
	//短信列表
	public ArrayList<Text> textlist;
	
	public Conv(){
		phone = "";
		name = "";
		textlist = new ArrayList<Text>();
	}
	
	public Conv(String tempphone, String tempname){
		phone = tempphone;
		name = tempname;
		textlist = new ArrayList<Text>();
	}

}