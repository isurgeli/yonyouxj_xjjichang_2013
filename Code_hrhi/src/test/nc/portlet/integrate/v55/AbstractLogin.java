package nc.portlet.integrate.v55;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.servlet.http.HttpServletRequest;

import nc.bs.framework.common.NCLocator;
import nc.itf.uap.busibean.ISysInitQry;
import nc.vo.sm.login.LoginFailureInfo;

public class AbstractLogin {

	public AbstractLogin() {
		super();
	}

	protected int verifyUserInfobyAD(HttpServletRequest req, UserInfoValue info) {
		String ncUrl = getncUrl(req);
		Properties props = new Properties();
		props.setProperty("SERVICEDISPATCH_URL", ncUrl	+ "/ServiceDispatcherServlet");
		NCLocator locator = NCLocator.getInstance(props);
		ISysInitQry sysQry = locator.lookup(ISysInitQry.class);
		// TODO 获取AD服务域名称及IP地址
		String adservername = "";
		String adserverip = "";
		
		Hashtable<String,String> env = new Hashtable<String, String>(11);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		// env.put(Context.SECURITY_PRINCIPAL,"CN="+userVO.getUserNote()+",DC=chamc,DC=com,DC=cn");//User
	
		env.put(Context.SECURITY_PRINCIPAL, info.userId+"@"+adservername);// User
	
		env.put(Context.SECURITY_CREDENTIALS, info.password);// Password
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
	
		// env.put(Context.PROVIDER_URL,"ldap://"+ad_code+":389");
		env.put(Context.PROVIDER_URL, "ldap://"+adserverip+":389");
	
		try {
			DirContext ctx = new InitialDirContext(env);
			Attributes attr = ctx.getAttributes("");
			// System.out.println("Domain Name:"+ attr.get("name").get());
			return LoginFailureInfo.LOGIN_LEGALIDENTITY;
		} catch (NamingException e) {
			if (e.getRootCause() instanceof UnknownHostException)
				return LoginFailureInfo.UNKNOWN_ERROR;
			if (e.getRootCause() instanceof ConnectException)
				return LoginFailureInfo.UNKNOWN_ERROR;
			if (e instanceof AuthenticationException) {
				String errmsg = e.getMessage();
				String errcode = errmsg.substring(errmsg.indexOf("data") + 5,
						errmsg.indexOf("data") + 8);
				/*
				 * 525 - user not found 52e - invalid credentials 530 - not
				 * permitted to logon at this time 532 - password expired 533 -
				 * account disabled 701 - account expired 773 - user must reset
				 * password
				 * 
				 */
				if (errcode.equals("525"))
					return LoginFailureInfo.NAME_WRONG;
				else if (errcode.equals("52e"))
					return LoginFailureInfo.NAME_RIGHT_PWD_WRONG;
				else if (errcode.equals("530"))
					return LoginFailureInfo.USER_BEFORE_EFFECT;
				else if (errcode.equals("532"))
					return LoginFailureInfo.USER_EXPIRED;
				else if (errcode.equals("533"))
					return LoginFailureInfo.USER_LOCKED;
				else if (errcode.equals("701"))
					return LoginFailureInfo.USER_EXPIRED;
				else if (errcode.equals("773"))
					return LoginFailureInfo.UNKNOWN_ERROR;
				else
					return LoginFailureInfo.UNKNOWN_ERROR;
			}
	
			// System.err.println("Problem getting attribute: " + e);
			return LoginFailureInfo.NAME_RIGHT_PWD_WRONG;
		}
	}

	protected String getDefaultCorp(String usercode) {
		// TODO 查询默认公司PK
		return "1022";
	}

	protected String getncUrl(HttpServletRequest req) {
		String url = req.getRequestURL().toString();
		url = url.substring(0, url.indexOf("/", 10));
		return url;
	}

}