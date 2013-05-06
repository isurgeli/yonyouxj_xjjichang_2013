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
import java.net.URLEncoder;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nc.bs.dao.BaseDAO;
import nc.bs.dao.DAOException;
import nc.bs.framework.common.NCLocator;
import nc.bs.logging.Logger;
import nc.itf.uap.sf.ISMVerifyService;
import nc.jdbc.framework.processor.VectorProcessor;
import nc.vo.framework.rsa.Encode;
import nc.vo.pub.BusinessException;
import nc.vo.pub.lang.UFDate;
import nc.vo.sm.login.LoginFailureInfo;


/**
 * @author yzy
 * @update lkp ����lfw�ĵ�¼��ʽ
 * @author gd 2008-07-02 ϸ������ṹ,������ϸ����־��¼
 * @version NC5.6
 * @since NC5.0
 */
public class NcLogin extends AbstractLogin {
	private static  String NC_REG = "/service/RegisterServlet"; 
	private static  String NC_LOGIN = "/login.jsp"; 
	
	public String getGateUrl(HttpServletRequest req, HttpServletResponse res, String usercode, String password)
			throws BusinessException {
		try {
			String ncUrl = getncUrl(req); 
			String gateUrl = ncUrl+NC_LOGIN;
			
			
			Logger.debug("===NCLogin��getGateUrl����:��ȡԭʼgateUrl=" + gateUrl);
			Logger.debug("===NCLogin��getGateUrl����:��ȡNC��runtimeUrl=" + ncUrl);
			
			String accountcode = "XJJCNC"; //�̶�����
			String pkcorp = getDefaultCorp(usercode); //��ѯĬ�Ϲ�˾
			String language = "simpchn";
			String key = req.getSession().getId();
			StringBuffer parameters = new StringBuffer("key=" + key);
			if(pkcorp != null && pkcorp.trim().length() != 0)
			    parameters.append("&pkcorp=" + pkcorp);
			else
				parameters.append("&pkcorp=0001");
			
			UFDate tick = new UFDate(new Date().getTime());
			String workdate = req.getParameter("workdate");
			if(workdate == null || "".equals(workdate))
				workdate = tick.toString();
			
			parameters.append("&accountcode=" + accountcode);
			parameters.append("&workdate=" + workdate);
			parameters.append("&language=" + language);
			// ���û����ƺ��������URL����,ȷ�������ַ��ܹ�ͨ��
			parameters.append("&usercode=" + URLEncoder.encode(usercode, "UTF-8"));
			
			UserInfoValue info = new UserInfoValue();
			info.accountcode = accountcode;
			info.language = language;
			info.password = password;
			info.pkcorp = pkcorp;
			info.userId = usercode;
			
			Logger.debug("===NCLogin��getGateUrl����:��ȡ��credential��Ϣ,usercode=" + usercode + ";accountcode=" + accountcode
					+ ";pkcorp=" + pkcorp + ";language=" + language + ";key=" + key);
			// �û���Ϣ��֤
			if (/*LoginFailureInfo.LOGIN_LEGALIDENTITY!=verifyUserInfobyAD(req, info)*/true) {
				verifyUserInfobyNC(req, info);
			}else{
				// ��AD����ΪNC����
				String sql="select user_password from sm_user where user_code='"+usercode+"'";
		        BaseDAO bd = new BaseDAO("JCNC"); //TODO ָ������Դ

				String ncpassword = null;
				try {
					@SuppressWarnings("unchecked")
					Vector<Vector<Object>> resultSet = (Vector<Vector<Object>>)bd.executeQuery(sql, new VectorProcessor());
					if (resultSet != null && resultSet.size() > 0)
						ncpassword=resultSet.get(0).get(0).toString();
				} catch (DAOException e) {
					e.printStackTrace();
					throw new BusinessException(e.getMessage());
				}
				Encode encode=new Encode();
		        password =encode.decode(ncpassword) ;
			}
			parameters.append("&pwd=" + URLEncoder.encode(password, "UTF-8"));
			// NC��½��Ϣע��
			Logger.debug("===NCLogin��getGateUrl����:���ɵ�ע�������Ϣ=" + parameters.toString());
			String registeResult = ncRegiste(parameters.toString(), ncUrl+NC_REG);
			Logger.debug("===NCLogin��getGateUrl����:registeResult=" + registeResult);
			if(registeResult != null && !registeResult.equals("OK") && !registeResult.equals(key))
				throw new BusinessException(registeResult);
			
			// ����NCϵͳ
			int clientWidth = 2048;
			int clientHeight = 1436;
			String screenWidth,screenHeight;
			screenWidth = (String)req.getAttribute("screenWidth");
			screenHeight = (String)req.getAttribute("screenHeight");
			if(screenWidth != null && !screenWidth.trim().equals(""))
				clientWidth = Integer.parseInt(screenWidth);
			if(screenHeight != null && !screenHeight.trim().equals(""))
				clientHeight = Integer.parseInt(screenHeight);
			
			gateUrl += "?key=" + key + "&clienttype=portal&width=" + clientWidth + "&height=" + clientHeight;
			Logger.debug("===NCLogin��getGateUrl����:��ȡ���յĵ�¼NC��gateUrl=" + gateUrl);
			return gateUrl;
		} catch (IOException e) {
			Logger.error("===NCLogin��getGateUrl����:��¼��������,�����ɷ�����û���������������Ӳ�ͨ!",e); 
			throw new BusinessException("��¼NC��������,�����ɷ�����û�����������������޷��ﵽ!");
		} catch (BusinessException e) {
			Logger.error(e, e);
			throw new BusinessException("У��NC��¼ʧ�ܣ�" + e.getMessage());
		}
	}
	
	public boolean verifyUserInfobyNC(HttpServletRequest req, UserInfoValue info) throws BusinessException
	{
		// У��
		String ncUrl = getncUrl(req);
		Properties props = new Properties();
		props.setProperty("SERVICEDISPATCH_URL", ncUrl	+ "/ServiceDispatcherServlet");
		NCLocator locator = NCLocator.getInstance(props);
		ISMVerifyService verifyService = (ISMVerifyService)locator.lookup(ISMVerifyService.class.getName());
		String workdate = req.getParameter("workdate");
		UFDate tick = new UFDate(new Date().getTime());
		if(workdate == null || "".equals(workdate))
			workdate = tick.toString();
		try {
			Logger.debug("===NCLogin��verifyUserInfo����:��" + ncUrl + "���ڵ���֤����ISMVerifyService������֤��Ϣ.");
			Logger.debug("===NCLogin��verifyUserInfo����:��֤��Ϣlanguage=" + info.language + ";accountcode="  + info.accountcode + ";pkcorp=" + info.pkcorp + ";tick=" + tick.toString() + ";userId=" + info.userId);
			int verifyResult = verifyService.verifyLoginInfo(info.language,info.accountcode,info.pkcorp,workdate,info.userId,info.password);
			if (verifyResult != LoginFailureInfo.LOGIN_SUCCESS) {
				Logger.debug("===NCLogin��credentialProcess����:����ƾ֤ʱû����֤�ɹ�result=" + verifyResult + "[" + LoginFailureInfo.RESULTSTRING[verifyResult] + "]");
				throw new BusinessException(LoginFailureInfo.RESULTSTRING[verifyResult]);
			}
		} catch (BusinessException e) {
			Logger.error(e, e);
			throw new BusinessException(e.getMessage());
		}
		return true;
	}

	/**
	 * ��NCϵͳע���¼ƾ֤
	 */
	private String ncRegiste(String parameters, String registrUrl)
			throws IOException {
		
		Logger.debug("===NCLogin��ncRegiste����:NC registry URL:" + registrUrl);
		Logger.debug("===NCLogin��ncRegiste����:NC registry parameters:" + parameters);
		
		// ����NCע��URL
		URL preUrl = new URL(registrUrl);
		URLConnection uc = preUrl.openConnection();
		// ����������������/ֵ�������������������Դ
		uc.setDoOutput(true);
		// ����ֻ�ܷ������õ���Ϣ
		uc.setUseCaches(false);
		// ����Content-Typeͷ��ָʾָ��URL�ѱ������ݵĴ���MIME����
		uc.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
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
		
		// ��ȡNC��ƾ֤����֤���
		InputStream is = hc.getInputStream();
		String returnFlag = "";
		int ch;
		while ((ch = is.read()) != -1) {
			returnFlag += String.valueOf((char) ch);
		}
		Logger.debug("===NCLogin��ncRegiste����:NC Registe result=" + returnFlag);
		if (is != null)
			is.close();
		return returnFlag;
	}
}
