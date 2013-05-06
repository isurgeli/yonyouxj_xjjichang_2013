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
 * @update lkp 采用lfw的登录方式
 * @author gd 2008-07-02 细化代码结构,加入详细的日志记录
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
			
			
			Logger.debug("===NCLogin类getGateUrl方法:获取原始gateUrl=" + gateUrl);
			Logger.debug("===NCLogin类getGateUrl方法:获取NC的runtimeUrl=" + ncUrl);
			
			String accountcode = "XJJCNC"; //固定帐套
			String pkcorp = getDefaultCorp(usercode); //查询默认公司
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
			// 对用户名称和密码进行URL编码,确保特殊字符能够通过
			parameters.append("&usercode=" + URLEncoder.encode(usercode, "UTF-8"));
			
			UserInfoValue info = new UserInfoValue();
			info.accountcode = accountcode;
			info.language = language;
			info.password = password;
			info.pkcorp = pkcorp;
			info.userId = usercode;
			
			Logger.debug("===NCLogin类getGateUrl方法:获取的credential信息,usercode=" + usercode + ";accountcode=" + accountcode
					+ ";pkcorp=" + pkcorp + ";language=" + language + ";key=" + key);
			// 用户信息验证
			if (/*LoginFailureInfo.LOGIN_LEGALIDENTITY!=verifyUserInfobyAD(req, info)*/true) {
				verifyUserInfobyNC(req, info);
			}else{
				// 变AD口令为NC口令
				String sql="select user_password from sm_user where user_code='"+usercode+"'";
		        BaseDAO bd = new BaseDAO("JCNC"); //TODO 指定数据源

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
			// NC登陆信息注册
			Logger.debug("===NCLogin类getGateUrl方法:生成的注册参数信息=" + parameters.toString());
			String registeResult = ncRegiste(parameters.toString(), ncUrl+NC_REG);
			Logger.debug("===NCLogin类getGateUrl方法:registeResult=" + registeResult);
			if(registeResult != null && !registeResult.equals("OK") && !registeResult.equals(key))
				throw new BusinessException(registeResult);
			
			// 进入NC系统
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
			Logger.debug("===NCLogin类getGateUrl方法:获取最终的登录NC的gateUrl=" + gateUrl);
			return gateUrl;
		} catch (IOException e) {
			Logger.error("===NCLogin类getGateUrl方法:登录发生错误,被集成服务器没有启动或网络连接不通!",e); 
			throw new BusinessException("登录NC发生错误,被集成服务器没有启动或网络连接无法达到!");
		} catch (BusinessException e) {
			Logger.error(e, e);
			throw new BusinessException("校验NC登录失败：" + e.getMessage());
		}
	}
	
	public boolean verifyUserInfobyNC(HttpServletRequest req, UserInfoValue info) throws BusinessException
	{
		// 校验
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
			Logger.debug("===NCLogin类verifyUserInfo方法:向" + ncUrl + "所在的认证服务ISMVerifyService发送认证信息.");
			Logger.debug("===NCLogin类verifyUserInfo方法:认证信息language=" + info.language + ";accountcode="  + info.accountcode + ";pkcorp=" + info.pkcorp + ";tick=" + tick.toString() + ";userId=" + info.userId);
			int verifyResult = verifyService.verifyLoginInfo(info.language,info.accountcode,info.pkcorp,workdate,info.userId,info.password);
			if (verifyResult != LoginFailureInfo.LOGIN_SUCCESS) {
				Logger.debug("===NCLogin类credentialProcess方法:制作凭证时没有认证成功result=" + verifyResult + "[" + LoginFailureInfo.RESULTSTRING[verifyResult] + "]");
				throw new BusinessException(LoginFailureInfo.RESULTSTRING[verifyResult]);
			}
		} catch (BusinessException e) {
			Logger.error(e, e);
			throw new BusinessException(e.getMessage());
		}
		return true;
	}

	/**
	 * 向NC系统注册登录凭证
	 */
	private String ncRegiste(String parameters, String registrUrl)
			throws IOException {
		
		Logger.debug("===NCLogin类ncRegiste方法:NC registry URL:" + registrUrl);
		Logger.debug("===NCLogin类ncRegiste方法:NC registry parameters:" + parameters);
		
		// 构造NC注册URL
		URL preUrl = new URL(registrUrl);
		URLConnection uc = preUrl.openConnection();
		// 表明程序必须把名称/值对输出到服务器程序资源
		uc.setDoOutput(true);
		// 表明只能返回有用的信息
		uc.setUseCaches(false);
		// 设置Content-Type头部指示指定URL已编码数据的窗体MIME类型
		uc.setRequestProperty("Content-Type",
				"application/x-www-form-urlencoded");
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
		
		// 获取NC对凭证的验证结果
		InputStream is = hc.getInputStream();
		String returnFlag = "";
		int ch;
		while ((ch = is.read()) != -1) {
			returnFlag += String.valueOf((char) ch);
		}
		Logger.debug("===NCLogin类ncRegiste方法:NC Registe result=" + returnFlag);
		if (is != null)
			is.close();
		return returnFlag;
	}
}
