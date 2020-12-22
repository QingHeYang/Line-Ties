package cn.leancloud.demo.todo;

public class ContentValues {
    public static String appId = System.getenv("LEAN_APP_ID");//leanCloud项目的appId
    public static String appKey = System.getenv("LEAN_APP_KEY");//leanCloud项目的appKey
    public static String appMasterKey = System.getenv("LEAN_MASTER_KEY");//leanCloud项目的appMasterKey
    public static String adminEmail = System.getenv("ADMIN_EMAIL");//输入博客主自己的邮箱，用来接收邮件
    public static String aliAccessKyeId = System.getenv("ALI_ACCESSKEY_ID");//阿里云账户的accessKeyId
    public static String aliReginId = System.getenv("ALI_REGIN_ID");//阿里云账户的reginId，一般默认填写cn-hangzhou
    public static String aliSecret = System.getenv("ALI_SECRET");//阿里云账户的accessKeyId
    public static String emailAccountName = System.getenv("EMAIL_ACCOUNT_NAME");//阿里云邮件推送的发信地址
    public static String emailAlias = System.getenv("EMAIL_ALIAS");//博客名称
    public static String emailTag = System.getenv("EMAIL_TAG");//阿里云邮件推送的tag，在控制台创建
    public static String emailTitle = System.getenv("EMAIL_TITLE");//邮件title，推送收到的title
    public static String hrefLinkUrl = System.getenv("HREF_LINK_URL");//超链接地址，填写自己博客的域名,不可以加路由,例如我的是https://blog.deep-blue.cloud
}
