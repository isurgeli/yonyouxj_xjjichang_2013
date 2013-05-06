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
 * IUFO集成portlet
 * @author yzy Created on 2006-7-10
 * @author gd 2008-07-15 修改iufo单位为参照形式
 * @version NC5.6
 * @since NC5.0
 */
public class IufoLogin extends AbstractLogin{
 
	private static final String IUFO_LOGIN = "/service/~iufo/com.ufida.web.action.ActionServlet";
	private static final String IUFO_REG = "/service/~iufo/nc.ui.iufo.login.LoginValidateServlet";
	private LfwLogger log = LoggerManager.getLogger(IufoLogin.class.getName());
	
	public String getGateUrl(HttpServletRequest req, HttpServletResponse res, String usercode, String password) throws BusinessException {
		log.debug("===IufoLogin类getGateUrl方法:进入IufoPortlet的getGateUrl方法中");
		String ncUrl = getncUrl(req); 
		String gateUrl = ncUrl+IUFO_LOGIN;
		log.debug("===IufoLogin类getGateUrl方法:获取原始的gateUrl=" + gateUrl);
		
		/**
		 * 以用户输入为准，如果能够取到该值，则说明是用户初次登录或选择了重新登录，以用户的输入信息为准。
		 * 如果不能取到，则以当前日期为准。
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
			// 变AD口令为Iufo口令
			String sql="select password from iufo_userinfo where user_code='"+usercode+"'";
	        BaseDAO bd = new BaseDAO("JCNC"); //TODO 指定数据源

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
		
		// 用户信息验证
		verifyUserInfo(req, info);
		// 进入IUFO系统
		String loggerGateUrl = gateUrl + "?action=nc.ui.iufo.login.LoginAction&method=login&logintype=portal&m_strLangCode=" + language + "&m_strUserCode=" 
		+ usercode + "&m_strLoginDate=" + workdate + "&m_strUnitCode=" + m_strUnitCode;
		
		gateUrl += "?action=nc.ui.iufo.login.LoginAction&method=login&logintype=portal&m_strLangCode=" + language + "&m_strUserCode=" 
			+ usercode + "&m_strPassword=" + password + "&m_strLoginDate=" + workdate + "&m_strUnitCode=" + m_strUnitCode;
		
		log.debug("===IufoLogin类getGateUrl方法:获取最终的登录地址,gateUrl=" + loggerGateUrl);
		log.debug("===IufoLogin类getGateUrl方法:退出IufoPortlet的getGateUrl()方法");
		return gateUrl;
	}

	private String getDefaultUnitCode(String usercode) {
		// TODO 获取IUFO默认登录UNIT
		// iufo_userinfo中unit_id对应IUFO_UNIT_INFO表返回编码 
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
		
		log.debug("===IufoLogin类verifyUserInfo方法:注册的url地址=" + registryUrl);
		String logParameters = "logintype=portal&m_strLangCode=" + language + "&m_strUserCode=" 
		+ userId + "&m_strLoginDate=" + workdate + "&m_strUnitCode=" + m_strUnitCode;
		log.debug("===IufoLogin类verifyUserInfo方法:注册的参数信息=" + logParameters);
		
		String returnFlag = null;
		try {
			returnFlag = iufoRegiste(parameters, registryUrl);
			log.debug("===IufoLogin类verifyUserInfo方法:注册的结果returnFlag=" + returnFlag);
		} catch (IOException e) {
			log.error(e, e);
			throw new BusinessException("登录发生错误,被集成服务器没有启动或网络连接无法达到!");
		}
		
		if (returnFlag != null && !returnFlag.equals("true"))
			throw new BusinessException("IUFO用户名或者口令错误!");
		return returnFlag;
	}
	
	private String iufoRegiste(String parameters, String registrUrl) throws IOException {
		log.debug("===IufoLogin类iufoRegiste方法:进入iufoPortlet的iufoRegiste()方法");
		// 构造IUFO注册URL
		URL preUrl = new URL(registrUrl);
		URLConnection uc = preUrl.openConnection();
		// 表明程序必须把名称/值对输出到服务器程序资源
		uc.setDoOutput(true);
		// 表明只能返回有用的信息
		uc.setUseCaches(false);
		// 设置Content-Type头部指示指定URL已编码数据的窗体MIME类型
		uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		// 设置Content-Type头部指示指定URL已编码数据的窗体MIME类型
		uc.setRequestProperty("Content-Length", "" + parameters.length());
		// 提取连接的适当的类型
		HttpURLConnection hc = (HttpURLConnection) uc;
		// 把HTTP请求方法设置为POST（默认的是GET）
		hc.setRequestMethod("POST");
		// 输出内容
		OutputStream os = hc.getOutputStream();
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeBytes(parameters);
		dos.flush();
		dos.close();
		// 获取IUFO对凭证的验证结果
		InputStream is = hc.getInputStream();
		String returnFlag = "";
		int ch;
		while ((ch = is.read()) != -1) {
			returnFlag += String.valueOf((char) ch);
		}
		if (is != null) is.close();
		
		log.debug("===IufoLogin类iufoRegiste方法:注册的结果returnFlag=" + returnFlag);
		return returnFlag;
	}
	
	private String getNowTime()
	{
		UFDate date = new UFDate(new Date().getTime());
		return date.toString();
	}
}
