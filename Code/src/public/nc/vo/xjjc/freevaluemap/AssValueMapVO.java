/***************************************************************\
 *     The skeleton of this class is generated by an automatic *
 * code generator for NC product. It is based on Velocity.     *
\***************************************************************/
package nc.vo.xjjc.freevaluemap;
	
import nc.vo.pub.*;
import nc.vo.pub.lang.*;
	
/**
 * <b> �ڴ˴���Ҫ��������Ĺ��� </b>
 * <p>
 *     �ڴ˴����Ӵ����������Ϣ
 * </p>
 * ��������:2013-04-26 13:38:19
 * @author 
 * @version NCPrj ??
 */
@SuppressWarnings("serial")
public class AssValueMapVO extends SuperVO {
	private java.lang.String pk_freevaluemap;
	private java.lang.String pk_corp;
	private java.lang.String pk_usedfreevalue;
	private java.lang.String pk_bdinfo;
	private java.lang.Integer obj_id;
	private java.lang.String vothercode;
	private java.lang.String vothername;
	private java.lang.String votherbiz;
	private java.lang.String pk_freevalue;
	private java.lang.String vmemo;
	private java.lang.Integer dr;
	private nc.vo.pub.lang.UFDateTime ts;

	public static final String PK_FREEVALUEMAP = "pk_freevaluemap";
	public static final String PK_CORP = "pk_corp";
	public static final String PK_USEDFREEVALUE = "pk_usedfreevalue";
	public static final String PK_BDINFO = "pk_bdinfo";
	public static final String OBJ_ID = "obj_id";
	public static final String VOTHERCODE = "vothercode";
	public static final String VOTHERNAME = "vothername";
	public static final String VOTHERBIZ = "votherbiz";
	public static final String PK_FREEVALUE = "pk_freevalue";
	public static final String VMEMO = "vmemo";
			
	/**
	 * ����pk_freevaluemap��Getter����.
	 * ��������:2013-04-26 13:38:19
	 * @return java.lang.String
	 */
	public java.lang.String getPk_freevaluemap () {
		return pk_freevaluemap;
	}   
	/**
	 * ����pk_freevaluemap��Setter����.
	 * ��������:2013-04-26 13:38:19
	 * @param newPk_freevaluemap java.lang.String
	 */
	public void setPk_freevaluemap (java.lang.String newPk_freevaluemap ) {
	 	this.pk_freevaluemap = newPk_freevaluemap;
	} 	  
	/**
	 * ����pk_corp��Getter����.
	 * ��������:2013-04-26 13:38:19
	 * @return java.lang.String
	 */
	public java.lang.String getPk_corp () {
		return pk_corp;
	}   
	/**
	 * ����pk_corp��Setter����.
	 * ��������:2013-04-26 13:38:19
	 * @param newPk_corp java.lang.String
	 */
	public void setPk_corp (java.lang.String newPk_corp ) {
	 	this.pk_corp = newPk_corp;
	} 	  
	/**
	 * ����pk_usedfreevalue��Getter����.
	 * ��������:2013-04-26 13:38:19
	 * @return java.lang.String
	 */
	public java.lang.String getPk_usedfreevalue () {
		return pk_usedfreevalue;
	}   
	/**
	 * ����pk_usedfreevalue��Setter����.
	 * ��������:2013-04-26 13:38:19
	 * @param newPk_usedfreevalue java.lang.String
	 */
	public void setPk_usedfreevalue (java.lang.String newPk_usedfreevalue ) {
	 	this.pk_usedfreevalue = newPk_usedfreevalue;
	} 	  
	/**
	 * ����pk_bdinfo��Getter����.
	 * ��������:2013-04-26 13:38:19
	 * @return java.lang.String
	 */
	public java.lang.String getPk_bdinfo () {
		return pk_bdinfo;
	}   
	/**
	 * ����pk_bdinfo��Setter����.
	 * ��������:2013-04-26 13:38:19
	 * @param newPk_bdinfo java.lang.String
	 */
	public void setPk_bdinfo (java.lang.String newPk_bdinfo ) {
	 	this.pk_bdinfo = newPk_bdinfo;
	} 	  
	/**
	 * ����obj_id��Getter����.
	 * ��������:2013-04-26 13:38:19
	 * @return java.lang.Integer
	 */
	public java.lang.Integer getObj_id () {
		return obj_id;
	}   
	/**
	 * ����obj_id��Setter����.
	 * ��������:2013-04-26 13:38:19
	 * @param newObj_id java.lang.Integer
	 */
	public void setObj_id (java.lang.Integer newObj_id ) {
	 	this.obj_id = newObj_id;
	} 	  
	/**
	 * ����vothercode��Getter����.
	 * ��������:2013-04-26 13:38:19
	 * @return java.lang.String
	 */
	public java.lang.String getVothercode () {
		return vothercode;
	}   
	/**
	 * ����vothercode��Setter����.
	 * ��������:2013-04-26 13:38:19
	 * @param newVothercode java.lang.String
	 */
	public void setVothercode (java.lang.String newVothercode ) {
	 	this.vothercode = newVothercode;
	} 	  
	/**
	 * ����vothername��Getter����.
	 * ��������:2013-04-26 13:38:19
	 * @return java.lang.String
	 */
	public java.lang.String getVothername () {
		return vothername;
	}   
	/**
	 * ����vothername��Setter����.
	 * ��������:2013-04-26 13:38:19
	 * @param newVothername java.lang.String
	 */
	public void setVothername (java.lang.String newVothername ) {
	 	this.vothername = newVothername;
	} 	  
	/**
	 * ����votherbiz��Getter����.
	 * ��������:2013-04-26 13:38:19
	 * @return java.lang.String
	 */
	public java.lang.String getVotherbiz () {
		return votherbiz;
	}   
	/**
	 * ����votherbiz��Setter����.
	 * ��������:2013-04-26 13:38:19
	 * @param newVotherbiz java.lang.String
	 */
	public void setVotherbiz (java.lang.String newVotherbiz ) {
	 	this.votherbiz = newVotherbiz;
	} 	  
	/**
	 * ����pk_freevalue��Getter����.
	 * ��������:2013-04-26 13:38:19
	 * @return java.lang.String
	 */
	public java.lang.String getPk_freevalue () {
		return pk_freevalue;
	}   
	/**
	 * ����pk_freevalue��Setter����.
	 * ��������:2013-04-26 13:38:19
	 * @param newPk_freevalue java.lang.String
	 */
	public void setPk_freevalue (java.lang.String newPk_freevalue ) {
	 	this.pk_freevalue = newPk_freevalue;
	} 	  
	/**
	 * ����vmemo��Getter����.
	 * ��������:2013-04-26 13:38:19
	 * @return java.lang.String
	 */
	public java.lang.String getVmemo () {
		return vmemo;
	}   
	/**
	 * ����vmemo��Setter����.
	 * ��������:2013-04-26 13:38:19
	 * @param newVmemo java.lang.String
	 */
	public void setVmemo (java.lang.String newVmemo ) {
	 	this.vmemo = newVmemo;
	} 	  
	/**
	 * ����dr��Getter����.
	 * ��������:2013-04-26 13:38:19
	 * @return java.lang.Integer
	 */
	public java.lang.Integer getDr () {
		return dr;
	}   
	/**
	 * ����dr��Setter����.
	 * ��������:2013-04-26 13:38:19
	 * @param newDr java.lang.Integer
	 */
	public void setDr (java.lang.Integer newDr ) {
	 	this.dr = newDr;
	} 	  
	/**
	 * ����ts��Getter����.
	 * ��������:2013-04-26 13:38:19
	 * @return nc.vo.pub.lang.UFDateTime
	 */
	public nc.vo.pub.lang.UFDateTime getTs () {
		return ts;
	}   
	/**
	 * ����ts��Setter����.
	 * ��������:2013-04-26 13:38:19
	 * @param newTs nc.vo.pub.lang.UFDateTime
	 */
	public void setTs (nc.vo.pub.lang.UFDateTime newTs ) {
	 	this.ts = newTs;
	} 	  
 
	/**
	  * <p>ȡ�ø�VO�����ֶ�.
	  * <p>
	  * ��������:2013-04-26 13:38:19
	  * @return java.lang.String
	  */
	public java.lang.String getParentPKFieldName() {
	    return null;
	}   
    
	/**
	  * <p>ȡ�ñ�����.
	  * <p>
	  * ��������:2013-04-26 13:38:19
	  * @return java.lang.String
	  */
	public java.lang.String getPKFieldName() {
	  return "pk_freevaluemap";
	}
    
	/**
	 * <p>���ر�����.
	 * <p>
	 * ��������:2013-04-26 13:38:19
	 * @return java.lang.String
	 */
	public java.lang.String getTableName() {
		return "xjjc_bd_freevaluemap";
	}    
    
    /**
	  * ����Ĭ�Ϸ�ʽ����������.
	  *
	  * ��������:2013-04-26 13:38:19
	  */
     public AssValueMapVO() {
		super();	
	}    
} 