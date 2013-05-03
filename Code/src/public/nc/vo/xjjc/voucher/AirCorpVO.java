package nc.vo.xjjc.voucher;

import java.io.Serializable;

public class AirCorpVO implements Serializable{
	private static final long serialVersionUID = 8200964835292330745L;
	private String name;
	private String code;
	private int type;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	
	public String toString(){
		return name;
	}
}
