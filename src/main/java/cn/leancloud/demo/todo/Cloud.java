package cn.leancloud.demo.todo;

import cn.leancloud.*;
import cn.leancloud.sms.AVSMS;
import cn.leancloud.sms.AVSMSOption;
import cn.leancloud.types.AVNull;
import cn.leancloud.utils.StringUtil;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dm.model.v20151123.SingleSendMailRequest;
import com.aliyuncs.dm.model.v20151123.SingleSendMailResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.List;

public class Cloud {
    private static final Logger logger = LogManager.getLogger(Cloud.class);
    private static String emailTitle;

    @EngineHook(className = "Comment", type = EngineHookType.afterSave)
    public static void reviewAfterSaveHook(AVObject review) throws Exception {
        doSendEmail(review);
    }

    private static void doSendEmail(AVObject review) {
        String pid = review.getString("pid");
        String mail = review.getString("mail");
        logger.info(mail + "   " + pid);
        if (mail == null || mail.equals("null")) {
            mail = "";
        }
        if (pid == null || pid.equals("null")) {
            pid = "";
        }
        if (pid.length() != 0) {
            logger.info("有人给别人评论，牛逼啊，查一下被评论的人有没有在表中");
            doQuery(review);
        } else {
            logger.info("有人给自己评论，看看这个别人是谁");
        }
        if (mail.equals(ContentValues.adminEmail)) {
            logger.info("嗨，别人还是自己，没啥意思，不发邮件");
            return;
        } else {
            logger.info("嗨，还真是外人，走一个，去发邮件去");
        }
        sendToAdmin(review);
    }

    private static void doQuery(AVObject review) {
        String pid = review.getString("pid");
        AVQuery<AVObject> query = new AVQuery<>("Comment");
        query.whereEqualTo("objectId", pid);
        query.findInBackground().subscribe(new Observer<List<AVObject>>() {
            public void onSubscribe(Disposable disposable) {
            }

            public void onNext(List<AVObject> comment) {
                if (comment.size() != 0) {
                    logger.info("庆幸博主没有删除数据，查出来个评论，去看看他有没有邮箱");
                    logger.info(comment.get(0));
                    String mail = comment.get(0).getString("mail");
                    if (mail != null && mail.length() != 0) {
                        logger.info("我擦咧，他还真有邮箱，告诉他又一个评论,看看他邮箱是啥，牛逼啊");
                        if (mail == ContentValues.adminEmail) {
                            logger.info("淦，别人给自己评论，用不着发邮件，滚吧");
                        } else {
                            sendToOthers(comment.get(0), review);
                        }
                    } else {
                        logger.info("淦，这个人没留下邮箱，告诉一下发评论的人，这个人没有留下邮箱，可能联系不上");
                    }
                } else {
                    logger.info("啊，一定是这个人言语不友好，博主把他评论删掉了");
                }
            }

            public void onError(Throwable throwable) {
            }

            public void onComplete() {
            }
        });
    }

    private static void sendToOthers(AVObject reader, AVObject sender) {
        String nick = reader.getString("nick");
        String nick_sender = sender.getString("nick");
        String mail = reader.getString("mail");
        String mail_sender = sender.getString("mail");
        String content = reader.getString("comment");
        String content_sender = sender.getString("comment");
        String url = sender.getString("url");
        if (mail.equals(mail_sender)) {
            logger.info("这个人给自己评论了不少，就不给他发了，他脑袋有包");
        }
        String letter = "";
        if (nick == "Anonymous") {
            letter = "您好：<br> 您在" + ContentValues.emailAlias + "博客中的留言收到了一条回复～<br>";
        } else {
            letter = nick + " 您好：<br> 您在Deep-Blue博客中的留言收到了一条回复～<br>";
        }
        letter = letter + "回复人昵称：" + nick_sender + "(如果是Anonymous则对方为匿名评论)<br>";
        letter = letter + "回复内容如下：<br><hr><blockquote>" + content_sender + "</blockquote><hr><br>";
        letter = letter + "您当时的留言为：<br><hr><blockquote>" + content + "</blockquote><hr><br>";
        if (mail_sender.length() == 0) {
            letter = letter + "很遗憾，对方没有留下他的联系方式～您的回复他可能收不到，但是博主肯定会收到的呦～<br>";
        } else {
            letter = letter + "对方留下了他的联系方式，您的回复可以直接发送给他呦<br>";
        }
        letter = letter + "点击链接快速前往文章：<a href=\"" + ContentValues.hrefLinkUrl + url + "\">" + ContentValues.hrefLinkUrl + url + "</a><br>";
        logger.info(letter);
        doSend(letter, mail);
    }

    private static void sendToAdmin(AVObject review) {
        String comment = review.getString("comment");
        String nick = review.getString("nick");
        String pid = review.getString("pid");
        String url = review.getString("url");
        String mail_sender = review.getString("mail");
        if (pid == null) {
            pid = "";
        }
        if (mail_sender == null) {
            mail_sender = "";
        }
        String mail = "";
        mail += "么西么西～～博客来留言啦～～<br>" + "评论用户昵称为：" + nick +
                "<br>评论内容如下：<br><hr><blockquote>";
        mail += comment;
        mail += "<br></blockquote> <hr>";
        if (pid.length() == 0) {
            mail += "当前留言是给您的<br>";
        } else {
            mail += "当前留言是给别人评论的<br>";
        }
        mail = mail + "发送者邮箱： " + mail_sender + "<br>";
        logger.info(mail);

        mail = mail + "点击链接快速前往：<a href=\"" + ContentValues.hrefLinkUrl + url + "\">" + ContentValues.hrefLinkUrl + url + "</a><br>";
        doSend(mail, ContentValues.adminEmail);
    }

    public static void doSend(String comment, String toAddr) {
        // 如果是除杭州region外的其它region（如新加坡、澳洲Region），需要将下面的"cn-hangzhou"替换为"ap-southeast-1"、或"ap-southeast-2"。
        IClientProfile profile = DefaultProfile.getProfile(ContentValues.aliReginId, ContentValues.aliAccessKyeId, ContentValues.aliSecret);
        // 如果是除杭州region外的其它region（如新加坡region）， 需要做如下处理
        //try {
        //DefaultProfile.addEndpoint("dm.ap-southeast-1.aliyuncs.com", "ap-southeast-1", "Dm",  "dm.ap-southeast-1.aliyuncs.com");
        //} catch (ClientException e) {
        //e.printStackTrace();
        //}
        IAcsClient client = new DefaultAcsClient(profile);
        SingleSendMailRequest request = new SingleSendMailRequest();
        try {
            //request.setVersion("2017-06-22");// 如果是除杭州region外的其它region（如新加坡region）,必须指定为2017-06-22

            request.setAccountName(ContentValues.emailAccountName);
            request.setFromAlias(ContentValues.emailAlias);
            request.setAddressType(1);
            request.setTagName(ContentValues.emailTag);
            request.setReplyToAddress(true);
            request.setToAddress(toAddr);
            //可以给多个收件人发送邮件，收件人之间用逗号分开，批量发信建议使用BatchSendMailRequest方式
            //request.setToAddress("邮箱1,邮箱2");
            logger.info(ContentValues.emailTitle);
            request.setSubject(ContentValues.emailTitle);
            //如果采用byte[].toString的方式的话请确保最终转换成utf-8的格式再放入htmlbody和textbody，若编码不一致则会被当成垃圾邮件。
            //注意：文本邮件的大小限制为3M，过大的文本会导致连接超时或413错误
            request.setHtmlBody(comment);
            //SDK 采用的是http协议的发信方式, 默认是GET方法，有一定的长度限制。
            //若textBody、htmlBody或content的大小不确定，建议采用POST方式提交，避免出现uri is not valid异常
            request.setMethod(MethodType.POST);
            logger.info("发送邮件：\n" +
                    "阿里云aliReginId: "+ContentValues.aliReginId+"\n" +
                    "阿里云aliAccessKyeId："+ContentValues.aliAccessKyeId+"\n" +
                    "阿里云aliSecret："+ContentValues.aliSecret+"\n" +
                    "阿里云emailAccountName："+ContentValues.emailAccountName+"\n" +
                    "阿里云emailAlias："+ContentValues.emailAlias+"\n" +
                    "阿里云emailTag："+ContentValues.emailTag+"\n" +
                    "阿里云emailTitle："+ContentValues.emailTitle+"\n" +
                    "ToAddr："+toAddr+"\n" +
                    "Comment："+comment+"\n");
            //开启需要备案，0关闭，1开启
            //request.setClickTrace("0");
            //如果调用成功，正常返回httpResponse；如果调用失败则抛出异常，需要在异常中捕获错误异常码；错误异常码请参考对应的API文档;
            SingleSendMailResponse httpResponse = client.getAcsResponse(request);
        } catch (Exception e) {
            //捕获错误异常码
            //System.out.println("ErrCode : " + e.getErrCode());
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }
}


