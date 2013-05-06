/**
 * 
 */
package nc.portlet.integrate.v55;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.jdbc.framework.processor.VectorProcessor;
import nc.lfw.core.exception.LfwRuntimeException;
import nc.lfw.log.LfwLogger;
import nc.lfw.log.LoggerManager;
import nc.vo.framework.rsa.Encode;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFDate;
import nc.vo.sm.login.LoginFailureInfo;

/**
 * IUFO����portlet
 * @author yzy Created on 2006-7-10
 * @author gd 2008-07-15 �޸�iufo��λΪ������ʽ
 * @version NC5.6
 * @since NC5.0
 */
public class IufoLogin extends AbstractLogin{
 
	private static final String IUFO_LOGIN = "/service/~iufo/com.ufida.web.action.ActionServlet";
	private static final String IUFO_REG = "/service/~iufo/nc.ui.iufo.login.LoginValidateServlet";
	private LfwLogger log = LoggerManager.getLogger(IufoLogin.class.getName());
	
	public String getGateUrl(HttpServletRequest req, HttpServletResponse res, String usercode, String password) throws BusinessException {
		log.debug("===IufoLogin��getGateUrl����:����IufoPortlet��getGateUrl������");
		String ncUrl = getncUrl(req); 
		String gateUrl = ncUrl+IUFO_LOGIN;
		log.debug("===IufoLogin��getGateUrl����:��ȡԭʼ��gateUrl=" + gateUrl);
		
		/**
		 * ���û�����Ϊ׼������ܹ�ȡ����ֵ����˵�����û����ε�¼��ѡ�������µ�¼�����û���������ϢΪ׼��
		 * �������ȡ�������Ե�ǰ����Ϊ׼��
		 */
		String workdate = req.getParameter("workdate");
		if(workdate == null) workdate = getNowTime();
		String language = "simpchn";
		String m_strUnitCode = getDefaultUnitCode(usercode);
		
		UserInfoValue info = new UserInfoValue();
		info.language = language;
		info.password = password;
		info.userId = usercode;
		info.iufoUnitCode = m_strUnitCode;
		
		if (/*LoginFailureInfo.LOGIN_LEGALIDENTITY==verifyUserInfobyAD(req, info)*/true) {
			// ��AD����ΪIufo����
			String sql="select password from iufo_userinfo where user_code='"+usercode+"'";
	        BaseDAO bd = new BaseDAO("JCNC"); //TODO ָ������Դ

			String iufopassword = null;
			try {
				@SuppressWarnings("unchecked")
				Vector<Vector<Object>> resultSet = (Vector<Vector<Object>>)bd.executeQuery(sql, new VectorProcessor());
				if (resultSet != null && resultSet.size() > 0)
					iufopassword=resultSet.get(0).get(0).toString();
			} catch (DAOException e) {
				e.printStackTrace();
				throw new BusinessException(e.getMessage());
			}
			Encode encode=new Encode();
			info.password =encode.decode(iufopassword) ;
		}
		
		// �û���Ϣ��֤
		verifyUserInfo(req, info);
		// ����IUFOϵͳ
		String loggerGateUrl = gateUrl + "?action=nc.ui.iufo.login.LoginAction&method=login&logintype=portal&m_strLangCode=" + language + "&m_strUserCode=" 
		+ usercode + "&m_strLoginDate=" + workdate + "&m_strUnitCode=" + m_strUnitCode;
		
		gateUrl += "?action=nc.ui.iufo.login.LoginAction&method=login&logintype=portal&m_strLangCode=" + language + "&m_strUserCode=" 
			+ usercode + "&m_strPassword=" + password + "&m_strLoginDate=" + workdate + "&m_strUnitCode=" + m_strUnitCode;
		
		log.debug("===IufoLogin��getGateUrl����:��ȡ���յĵ�¼��ַ,gateUrl=" + loggerGateUrl);
		log.debug("===IufoLogin��getGateUrl����:�˳�IufoPortlet��getGateUrl()����");
		return gateUrl;
	}

	private String getDefaultUnitCode(String usercode) {
		// TODO ��ȡIUFOĬ�ϵ�¼UNIT
		// iufo_userinfo��unit_id��ӦIUFO_UNIT_INFO���ر��� 
		return "00";
	}
	
	public String verifyUserInfo(HttpServletRequest req, UserInfoValue info) throws BusinessException
	{
		String registryUrl = getncUrl(req)+IUFO_REG;
		String language = info.language;
		String userId = info.userId;
		String password = info.password;
		String workdate = req.getParameter("workdate");
		if(workdate == null)
			workdate = getNowTime();
		String m_strUnitCode = info.iufoUnitCode;
		String parameters = "logintype=portal&m_strLangCode=" + language + "&m_strUserCode=" 
							+ userId + "&m_strPassword=" + password + "&m_strLoginDate=" + workdate + "&m_strUnitCode=" + m_strUnitCode;
		
		log.debug("===IufoLogin��verifyUserInfo����:ע���url��ַ=" + registryUrl);
		String logParameters = "logintype=portal&m_strLangCode=" + language + "&m_strUserCode=" 
		+ userId + "&m_strLoginDate=" + workdate + "&m_strUnitCode=" + m_strUnitCode;
		log.debug("===IufoLogin��verifyUserInfo����:ע��Ĳ�����Ϣ=" + logParameters);
		
		String returnFlag = null;
		try {
			returnFlag = iufoRegiste(parameters, registryUrl);
			log.debug("===IufoLogin��verifyUserInfo����:ע��Ľ��returnFlag=" + returnFlag);
		} catch (IOException e) {
			log.error(e, e);
			throw new BusinessException("��¼��������,�����ɷ�����û�����������������޷��ﵽ!");
		}
		
		if (returnFlag != null && !returnFlag.equals("true"))
			throw new BusinessException("IUFO�û������߿������!");
		return returnFlag;
	}
	
	private String iufoRegiste(String parameters, String registrUrl) throws IOException {
		log.debug("===IufoLogin��iufoRegiste����:����iufoPortlet��iufoRegiste()����");
		// ����IUFOע��URL
		URL preUrl = new URL(registrUrl);
		URLConnection uc = preUrl.openConnection();
		// ����������������/ֵ�������������������Դ
		uc.setDoOutput(true);
		// ����ֻ�ܷ������õ���Ϣ
		uc.setUseCaches(false);
		// ����Content-Typeͷ��ָʾָ��URL�ѱ������ݵĴ���MIME����
		uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		// ����Content-Typeͷ��ָʾָ��URL�ѱ������ݵĴ���MIME����
		uc.setRequestProperty("Content-Length", "" + parameters.length());
		// ��ȡ���ӵ��ʵ�������
		HttpURLConnection hc = (HttpURLConnection) uc;
		// ��HTTP���󷽷�����ΪPOST��Ĭ�ϵ���GET��
		hc.setRequestMethod("POST");
		// �������
		OutputStream os = hc.getOutputStream();
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeBytes(parameters);
		dos.flush();
		dos.close();
		// ��ȡIUFO��ƾ֤����֤���
		InputStream is = hc.getInputStream();
		String returnFlag = "";
		int ch;
		while ((ch = is.read()) != -1) {
			returnFlag += String.valueOf((char) ch);
		}
		if (is != null) is.close();
		
		log.debug("===IufoLogin��iufoRegiste����:ע��Ľ��returnFlag=" + returnFlag);
		return returnFlag;
	}
	
	private String getNowTime()
	{
		UFDate date = new UFDate(new Date().getTime());
		return date.toString();
	}
}
