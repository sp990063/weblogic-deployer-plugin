/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic.data;

import java.io.Serializable;

import com.thoughtworks.xstream.annotations.XStreamAlias;


/**
 * @author rchaumie
 *
 */
@XStreamAlias(value="weblogic-target")
public class WeblogicEnvironment implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 332669158137739272L;

	/**
	 * 
	 */
	private String name;
	
	/**
	 * 
	 */
	private String host;
	
	/**
	 * 
	 */
	private String port;
	
	/**
	 * 
	 */
	private String login;
	
	/**
	 * 
	 */
	private String password;
	
	/**
	 * 
	 */
	private String ftpHost;
	
	/**
	 * 
	 */
	private String ftpUser;
	
	/**
	 * 
	 */
	private String ftpPassowrd;
	
	/**
	 * 
	 */
	private String remoteDir;
	
	/**
	 * 
	 */
	private String userconfigfile;
	
	/**
	 * 
	 */
	private String userkeyfile;
	
	/**
	 * 
	 */
	private WebLogicAuthenticationMode authMode;

	private String jobFolderPath;

	/**
	 * 	
	 * @param name
	 * @param host
	 * @param port
	 * @param login
	 * @param password
	 */
	public WeblogicEnvironment(String name, String host, String port, String login, String password) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.login = login;
		this.password = password;
	}
	
	/**
	 * 	
	 * @param name
	 * @param host
	 * @param port
	 * @param login
	 * @param password
	 * @param jobFolderPath
	 */
	public WeblogicEnvironment(String name, String host, String port, String login, String password, String jobFolderPath) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.login = login;
		this.password = password;
		this.jobFolderPath = jobFolderPath;
	}
	
	/**
	 * 
	 * @param name
	 * @param host
	 * @param port
	 * @param login
	 * @param password
	 * @param ftpHost
	 * @param ftpUser
	 * @param ftpPassowrd
	 * @param remoteDir
	 */
	public WeblogicEnvironment(String name, String host, String port,
			String login, String password, String ftpHost, String ftpUser,
			String ftpPassowrd, String remoteDir) {
		super();
		this.name = name;
		this.host = host;
		this.port = port;
		this.login = login;
		this.password = password;
		this.ftpHost = ftpHost;
		this.ftpUser = ftpUser;
		this.ftpPassowrd = ftpPassowrd;
		this.remoteDir = remoteDir;
	}
	
	/**
	 * @param name
	 * @param host
	 * @param port
	 * @param login
	 * @param password
	 * @param ftpHost
	 * @param ftpUser
	 * @param ftpPassowrd
	 * @param remoteDir
	 * @param userconfigfile
	 * @param userkeyfile
	 * @param authMode
	 */
	public WeblogicEnvironment(String name, String host, String port,
			String login, String password, String ftpHost, String ftpUser,
			String ftpPassowrd, String remoteDir, String userconfigfile,
			String userkeyfile, WebLogicAuthenticationMode authMode) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.login = login;
		this.password = password;
		this.ftpHost = ftpHost;
		this.ftpUser = ftpUser;
		this.ftpPassowrd = ftpPassowrd;
		this.remoteDir = remoteDir;
		this.userconfigfile = userconfigfile;
		this.userkeyfile = userkeyfile;
		this.authMode = authMode;
	}

	/**
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	public String getHost() {
		return host;
	}

	public String getPort() {
		return port;
	}

	public String getLogin() {
		return login;
	}

	public String getPassword() {
		return password;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFtpHost() {
		return ftpHost;
	}

	public void setFtpHost(String ftpHost) {
		this.ftpHost = ftpHost;
	}

	public String getFtpUser() {
		return ftpUser;
	}

	public void setFtpUser(String ftpUser) {
		this.ftpUser = ftpUser;
	}

	public String getFtpPassowrd() {
		return ftpPassowrd;
	}

	public void setFtpPassowrd(String ftpPassowrd) {
		this.ftpPassowrd = ftpPassowrd;
	}

	public String getRemoteDir() {
		return remoteDir;
	}

	public void setRemoteDir(String remoteDir) {
		this.remoteDir = remoteDir;
	}

	public String getUserconfigfile() {
		return userconfigfile;
	}

	public void setUserconfigfile(String userconfigfile) {
		this.userconfigfile = userconfigfile;
	}

	public String getUserkeyfile() {
		return userkeyfile;
	}

	public void setUserkeyfile(String userkeyfile) {
		this.userkeyfile = userkeyfile;
	}

	public WebLogicAuthenticationMode getAuthMode() {
		return authMode;
	}

	public void setAuthMode(WebLogicAuthenticationMode authMode) {
		this.authMode = authMode;
	}

	public String getJobFolderPath() {
		return jobFolderPath;
	}

	public void setJobFolderPath(String jobFolderPath) {
		this.jobFolderPath = jobFolderPath;
	}
}
