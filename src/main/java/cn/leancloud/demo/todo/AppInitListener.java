package cn.leancloud.demo.todo;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import cn.leancloud.AVLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cn.leancloud.AVCloud;
import cn.leancloud.core.AVOSCloud;
import cn.leancloud.core.GeneralRequestSignature;
import cn.leancloud.AVObject;

import cn.leancloud.LeanEngine;

@WebListener
public class AppInitListener implements ServletContextListener {

  private static final Logger logger = LogManager.getLogger(AppInitListener.class);

  private String hookKey = System.getenv("LEANCLOUD_APP_HOOK_KEY");
  private String appEnv = System.getenv("LEANCLOUD_APP_ENV");
  private String haveStaging = System.getenv("LEAN_CLI_HAVE_STAGING");

  @Override
  public void contextDestroyed(ServletContextEvent arg0) {}

  @Override
  public void contextInitialized(ServletContextEvent arg0) {
    logger.info("LeanEngine app init.");
    // Enables debug logging.
    AVOSCloud.setLogLevel(AVLogger.Level.DEBUG);
    // Registers subclass.
    AVObject.registerSubclass(Todo.class);

    if ("development".equals(appEnv) && "true".equals(haveStaging) || "stage".equals(appEnv)) {
      AVCloud.setProductionMode(false);
    }
    LeanEngine.initialize(ContentValues.appId, ContentValues.appKey, ContentValues.appMasterKey, hookKey);
    // Uses masterKey for the whole project.
    GeneralRequestSignature.setMasterKey(ContentValues.appMasterKey);
    // Registers cloud functions.
    LeanEngine.register(Cloud.class);
    // 初始化AVOSCloud，请保证在整个项目中间只初始化一次
  }
}
