/***************************************************************\
 *     The skeleton of this class is generated by an automatic *
 * code generator for NC product. It is based on Velocity.     *
\***************************************************************/
package nc.vo.xjjc.accsubjmap;
	
import nc.vo.pub.*;
import nc.vo.pub.lang.*;
	
/**
 * <b> 在此处简要描述此类的功能 </b>
 * <p>
 *     在此处添加此类的描述信息
 * </p>
 * 创建日期:2013-04-26 15:13:50
 * @author 
 * @version NCPrj ??
 */
@SuppressWarnings("serial")
public class BizTypeVO extends SuperVO {
	private java.lang.String pk_subjbiztype;
	private java.lang.String vcode;
	private java.lang.String vname;
	private java.lang.Integer dr;
	private nc.vo.pub.lang.UFDateTime ts;

	public static final String PK_SUBJBIZTYPE = "pk_subjbiztype";
	public static final String VCODE = "vcode";
	public static final String VNAME = "vname";
			
	/**
	 * 属性pk_subjbiztype的Getter方法.
	 * 创建日期:2013-04-26 15:13:50
	 * @return java.lang.String
	 */
	public java.lang.String getPk_subjbiztype () {
		return pk_subjbiztype;
	}   
	/**
	 * 属性pk_subjbiztype的Setter方法.
	 * 创建日期:2013-04-26 15:13:50
	 * @param newPk_subjbiztype java.lang.String
	 */
	public void setPk_subjbiztype (java.lang.String newPk_subjbiztype ) {
	 	this.pk_subjbiztype = newPk_subjbiztype;
	} 	  
	/**
	 * 属性vcode的Getter方法.
	 * 创建日期:2013-04-26 15:13:50
	 * @return java.lang.String
	 */
	public java.lang.String getVcode () {
		return vcode;
	}   
	/**
	 * 属性vcode的Setter方法.
	 * 创建日期:2013-04-26 15:13:50
	 * @param newVcode java.lang.String
	 */
	public void setVcode (java.lang.String newVcode ) {
	 	this.vcode = newVcode;
	} 	  
	/**
	 * 属性vname的Getter方法.
	 * 创建日期:2013-04-26 15:13:50
	 * @return java.lang.String
	 */
	public java.lang.String getVname () {
		return vname;
	}   
	/**
	 * 属性vname的Setter方法.
	 * 创建日期:2013-04-26 15:13:50
	 * @param newVname java.lang.String
	 */
	public void setVname (java.lang.String newVname ) {
	 	this.vname = newVname;
	} 	  
	/**
	 * 属性dr的Getter方法.
	 * 创建日期:2013-04-26 15:13:50
	 * @return java.lang.Integer
	 */
	public java.lang.Integer getDr () {
		return dr;
	}   
	/**
	 * 属性dr的Setter方法.
	 * 创建日期:2013-04-26 15:13:50
	 * @param newDr java.lang.Integer
	 */
	public void setDr (java.lang.Integer newDr ) {
	 	this.dr = newDr;
	} 	  
	/**
	 * 属性ts的Getter方法.
	 * 创建日期:2013-04-26 15:13:50
	 * @return nc.vo.pub.lang.UFDateTime
	 */
	public nc.vo.pub.lang.UFDateTime getTs () {
		return ts;
	}   
	/**
	 * 属性ts的Setter方法.
	 * 创建日期:2013-04-26 15:13:50
	 * @param newTs nc.vo.pub.lang.UFDateTime
	 */
	public void setTs (nc.vo.pub.lang.UFDateTime newTs ) {
	 	this.ts = newTs;
	} 	  
 
	/**
	  * <p>取得父VO主键字段.
	  * <p>
	  * 创建日期:2013-04-26 15:13:50
	  * @return java.lang.String
	  */
	public java.lang.String getParentPKFieldName() {
	    return null;
	}   
    
	/**
	  * <p>取得表主键.
	  * <p>
	  * 创建日期:2013-04-26 15:13:50
	  * @return java.lang.String
	  */
	public java.lang.String getPKFieldName() {
	  return "pk_subjbiztype";
	}
    
	/**
	 * <p>返回表名称.
	 * <p>
	 * 创建日期:2013-04-26 15:13:50
	 * @return java.lang.String
	 */
	public java.lang.String getTableName() {
		return "xjjc_bd_accsubjbiztype";
	}    
    
    /**
	  * 按照默认方式创建构造子.
	  *
	  * 创建日期:2013-04-26 15:13:50
	  */
     public BizTypeVO() {
		super();	
	}    
} 
