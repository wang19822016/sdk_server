package com.seastar.controller;

import com.seastar.common.Const;
import com.seastar.dao.AppDao;
import com.seastar.dao.UserBaseDao;
import com.seastar.dao.UserChannelDao;
import com.seastar.dao.UserTokenDao;
import com.seastar.entity.*;
import com.seastar.model.*;
import com.seastar.config.annotation.Authorization;
import com.seastar.config.annotation.Token;
import com.seastar.utils.JWT;
import com.seastar.utils.Utils;
import com.sun.mail.util.MailSSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Properties;

/**
 * Created by osx on 17/3/6.
 */
@RestController
public class UserController {
    @Autowired
    private AppDao appDao;

    @Autowired
    private UserBaseDao userBaseDao;

    @Autowired
    private UserChannelDao userChannelDao;

    @Autowired
    private UserTokenDao userTokenDao;

    private Logger logger = LoggerFactory.getLogger(UserController.class);
    private Logger bossLogger = LoggerFactory.getLogger("boss");

    /**
     * 帐号名注册
     * 使用HTTP Basic Authentication
     * account: Authorization: Basic base64(username:password)
     * guest: Authorization: Basic base64(deviceid:deviceid)
     * facebook: Authorization: Basic base64(id:token)
     * google: Authorization: Basic base64(id:id)
     * 失败时code＝401
     * 成功时code=200
     */
    @Transactional
    @RequestMapping(value = "/api/user", method = RequestMethod.POST)
    public ResponseEntity<Void> doRegister(@RequestHeader HttpHeaders headers, @RequestBody UserReq req) {
        String httpUsername = Utils.getHttpBasicAuthUsername(headers);
        String httpPassword = Utils.getHttpBasicAuthPassword(headers);
        if (httpPassword == null || httpUsername == null) {
            logger.error("授权信息不全, username: {}, password: {}", httpUsername, httpPassword);
            return new ResponseEntity<Void>(HttpStatus.NON_AUTHORITATIVE_INFORMATION);
        }

        App app = appDao.findOne(req.getAppId());
        if (app == null) {
            logger.error("应用不存在, username: {}, password: {}, appId: {}", httpUsername, httpPassword, req.getAppId());
            return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
        }
        if (!Utils.md5encode(req.getAppId() + "" + req.getType() + req.getEmail() + app.getKey()).equals(req.getSign())) {
            logger.error("签名不正确, username: {}, password: {}, appId: {}, type: {}, email: {}, sign: {}", httpUsername, httpPassword, req.getAppId(), req.getType(), req.getEmail(), req.getSign());
            return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
        }

        if (req.getType() == Const.USER_TYPE_ACCOUNT) {
            if (userBaseDao.findOneByName(httpUsername) != null) {
                logger.error("海星帐号已存在, username: {}", httpUsername);
                return new ResponseEntity<Void>(HttpStatus.CONFLICT);
            }

            UserBase userBase = new UserBase(userBaseDao.getMaxId(), httpUsername, httpPassword, req.getEmail(), Const.ALLOW, new Date());
            userBaseDao.save(userBase);

            logger.info("海星帐号注册成功, username: {}, password: {}", httpUsername, httpPassword);
        } else {
            if (userChannelDao.findOne(req.getType(), httpUsername) != null) {
                logger.error("第三方帐号已存在, username: {}", httpUsername);
                return new ResponseEntity<Void>(HttpStatus.CONFLICT);
            }

            UserBase userBase = new UserBase(userBaseDao.getMaxId(), Utils.getRandomString(8), req.getEmail(), Const.ALLOW, new Date());
            userBaseDao.save(userBase);

            UserChannel userChannel = new UserChannel(userBase.getId(), httpUsername, req.getType(), new Date());
            userChannelDao.save(userChannel);
            logger.info("第三方帐号注册成功, username: {}, password: {}", httpUsername, httpPassword);
        }

        return new ResponseEntity<Void>(HttpStatus.CREATED);
    }

    /**
     *
     * 登录使用HTTP Basic Authentication
     * account: Authorization: Basic base64(username:password)
     * guest: Authorization: Basic base64(deviceid:deviceid)
     * facebook: Authorization: Basic base64(id:token)
     * google: Authorization: Basic base64(id:id)
     * 失败时code＝401 头=WWW-authenticate: Basic realm=域
     * 成功时code=200 body={"access_token" : {ACCESS_TOKEN}, "expires_in" : 9223372036854775, "token_type" : "bearer"} 这样获得的token可以用于之后的操作
     */
    @RequestMapping(value = "/api/user/token", method = RequestMethod.POST)
    public ResponseEntity<UserTokenRsp> doLogin(@RequestHeader HttpHeaders headers, @RequestBody UserTokenReq req) {
        String httpUsername = Utils.getHttpBasicAuthUsername(headers);
        String httpPassword = Utils.getHttpBasicAuthPassword(headers);
        if (httpPassword == null || httpUsername == null) {
            logger.error("授权信息不全, username: {}, password: {}", httpUsername, httpPassword);
            return new ResponseEntity<UserTokenRsp>(HttpStatus.NON_AUTHORITATIVE_INFORMATION);
        }

        headers = new HttpHeaders();
        headers.add("WWW-authenticate", "Basic realm=vrseastar.com");

        App app = appDao.findOne(req.getAppId());
        if (app == null) {
            logger.error("应用不存在, username: {}, appId: {}", httpUsername, req.getAppId());
            return new ResponseEntity<UserTokenRsp>(headers, HttpStatus.BAD_REQUEST);
        }

        // 验证数据
        if (!Utils.md5encode(req.getAppId() + "" + req.getType() + app.getKey()).equals(req.getSign())) {
            logger.error("签名不正确, username: {}, appId: {}, type: {}, sign: {}", httpUsername, req.getAppId(), req.getType(), req.getSign());
            return new ResponseEntity<UserTokenRsp>(headers, HttpStatus.BAD_REQUEST);
        }

        UserBase userBase = null;
        if (req.getType() == Const.USER_TYPE_ACCOUNT) {
            userBase = userBaseDao.findOneByName(httpUsername);
            if (userBase == null) {
                logger.error("海星帐号不存在, username:{}", httpUsername);
                return new ResponseEntity<UserTokenRsp>(headers, HttpStatus.NOT_FOUND);
            }

            if (!userBase.getPassword().equals(httpPassword)) {
                logger.error("海星帐号密码不对, username: {}, password: {}, passwordInServer: {}", httpUsername, httpPassword, userBase.getPassword());
                return new ResponseEntity<UserTokenRsp>(headers, HttpStatus.BAD_REQUEST);
            }
        } else {
            UserChannel userChannel = userChannelDao.findOne(req.getType(), httpUsername);
            if (userChannel == null) {
                logger.error("第三方帐号不存在, username: {}", httpUsername);
                return new ResponseEntity<UserTokenRsp>(headers, HttpStatus.NOT_FOUND);
            }

            userBase = userBaseDao.findOne(userChannel.getUserId());
            if (userBase == null) {
                logger.error("第三方帐号对应的基础数据不存在, username: {}", httpUsername);
                return new ResponseEntity<UserTokenRsp>(headers, HttpStatus.NOT_FOUND);
            }
        }

        // 生成token
        JWT jwt = new JWT(userBase.getId(), userBase.getName(), req.getAppId(), app.getPayType(), req.getType(), app.getSecret());
        userTokenDao.save(jwt);

        UserTokenRsp rsp = new UserTokenRsp();
        rsp.setAccess_token(jwt.getToken());
        rsp.setExpires_in(jwt.getPayload().getExp());
        rsp.setToken_type("Bearer");

        logger.info("登录成功, username: {}, token: {}", httpUsername, jwt.getToken());
        return new ResponseEntity<UserTokenRsp>(rsp, headers, HttpStatus.OK);
    }

    @Authorization
    @RequestMapping(value = "/api/user", method = RequestMethod.GET)
    public ResponseEntity<UserInfoRsp> getUserInfo(@Token JWT jwt) {
        UserBase userBase = userBaseDao.findOne(jwt.getPayload().getUserId());
        if (userBase == null) {
            logger.error("帐号不存在, username: {}, token: {}", jwt.getPayload().getUsername(), jwt.getToken());
            return new ResponseEntity<UserInfoRsp>(HttpStatus.NOT_FOUND);
        }

        UserInfoRsp rsp =  new UserInfoRsp();
        rsp.setEmail(userBase.getEmail());
        rsp.setUsername(userBase.getName());

        logger.info("获取帐号信息成功, username: {}, token: {}", jwt.getPayload().getUsername(), jwt.getToken());
        return new ResponseEntity<UserInfoRsp>(rsp, HttpStatus.OK);
    }

    @Authorization
    @RequestMapping(value = "/api/user", method = RequestMethod.PUT)
    public ResponseEntity<Void> updateUserInfo(@Token JWT jwt, @RequestBody UserInfoReq req) {
        UserBase userBase = userBaseDao.findOne(jwt.getPayload().getUserId());
        if (userBase == null) {
            logger.error("帐号不存在, username: {}, token: {}", jwt.getPayload().getUsername(), jwt.getToken());
            return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
        }

        userBase.setEmail(req.getEmail());
        userBaseDao.save(userBase);

        logger.info("更新帐号邮箱成功, username: {}, token: {}", jwt.getPayload().getUsername(), jwt.getToken());
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @Authorization
    @RequestMapping(value = "/api/user/log", method = RequestMethod.POST)
    public ResponseEntity<Void> onUserLog(@Token JWT jwt, @RequestBody UserLog log) {
        bossLogger.info("{} {} {} {}", jwt.getPayload().getAppId(), jwt.getPayload().getUserId(), jwt.getPayload().getLoginType(), log.toString());
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    /*
    @Authorization
    @RequestMapping(value = "/user/bind", method = RequestMethod.POST)
    public ResponseEntity<Void> doBind(@Token JWT jwt, BindReq req) {
        UserBase userBase = userBaseDao.findOne(jwt.getPayload().getUserId());
        if (userBase == null) {
            return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        UserChannel userChannel = userChannelDao.findOne(req.type, req.id);
        if (userChannel != null) {
            return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        userChannel = new UserChannel(jwt.getPayload().getUserId(), req.id, req.type, new Date());
        userChannelDao.save(userChannel);

        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @Authorization
    @RequestMapping(value = "/user/unbind", method = RequestMethod.POST)
    public ResponseEntity<Void> doUnBind(@Token JWT jwt, BindReq req) {
        UserBase userBase = userBaseDao.findOne(jwt.getPayload().getUserId());
        if (userBase == null) {
            return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        UserChannel userChannel = userChannelDao.findOne(req.type, req.id);
        if (userChannel == null) {
            return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        userChannelDao.delete(userChannel);

        return new ResponseEntity<Void>(HttpStatus.OK);
    }
    */

    @RequestMapping(value = "/api/user/pwd", method = RequestMethod.GET)
    public ResponseEntity<Void> doFindPwd(@RequestParam(value = "username", required = true) String userName,
                                          @RequestParam(value = "appId", required = true) int appId,
                                          @RequestParam(value = "sign", required = true) String sign) {
        logger.info("找回密码, username: {}, appId: {}, sign: {}", userName, appId, sign);
        App app = appDao.findOne(appId);
        if (app != null) {
            if (sign.equals(Utils.md5encode(appId + userName + app.getKey() ) ) ) {

                UserBase userBase = userBaseDao.findOneByName(userName);
                if (userBase != null && !userBase.getEmail().isEmpty()) {
                    sendMail(userBase.getEmail(), userBase.getName(), userBase.getPassword());
                }
            }
        }

        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    // 验证
    @RequestMapping(value = "/v2/auth/loginverify", method = RequestMethod.POST)
    public LoginVerifyRsp doLoginVerify(LoginVerifyReq req) {
        JWT jwt = new JWT(req.token);
        if (jwt.getPayload() != null) {
            App app = appDao.findOne(jwt.getPayload().getAppId());
            if (app != null) {
                if (jwt.check(app.getSecret())) {
                    LoginVerifyRsp rsp = new LoginVerifyRsp();
                    rsp.code = "0";
                    rsp.codeDesc = "ok";
                    rsp.cparam = req.cparam;
                    rsp.userId = req.userId;
                    return rsp;
                }
            }
        }

        LoginVerifyRsp rsp = new LoginVerifyRsp();
        rsp.code = "63";
        rsp.codeDesc = "no user";
        return rsp;
    }

    private Session session;

    private void sendMail(String mailTo, String mailUsername, String mailPassword) {
        String username = "forgetpwd@vrseastar.com";
        String password = "Aa1234#.";
        String subject = "Get back your Seastar password";
        String content = "Hello!\n\nyour accout : " + mailUsername + "\nyour password : " + mailPassword + "\n\nPlease keep your account information in mind, do not share it with anyone.";
        content += "\n\n\n\n";
        content += "您好：\n\n您的账号：" + mailUsername + "\n您的密碼：" + mailPassword + "\n\n請小心保管您的密碼，切勿與他人共享。";
        if (session == null) {
            Properties prop = new Properties();
            //协议
            prop.setProperty("mail.transport.protocol", "smtp");
            //服务器
            prop.setProperty("mail.smtp.host", "smtp.exmail.qq.com");
            //端口
            prop.setProperty("mail.smtp.port", "465");
            //使用smtp身份验证
            prop.setProperty("mail.smtp.auth", "true");
            //使用SSL，企业邮箱必需！
            //开启安全协议
            MailSSLSocketFactory sf = null;
            try {
                sf = new MailSSLSocketFactory();
                sf.setTrustAllHosts(true);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
            prop.put("mail.smtp.ssl.enable", "true");
            prop.put("mail.smtp.ssl.socketFactory", sf);


            session = Session.getDefaultInstance(prop, new MyAuthenricator(username, password));
            //session.setDebug(true);
        }

        MimeMessage mimeMessage = new MimeMessage(session);
        try {
            mimeMessage.setFrom(new InternetAddress(username));
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(mailTo));
            mimeMessage.setSubject(subject);
            mimeMessage.setSentDate(new Date());
            mimeMessage.setText(content);
            mimeMessage.saveChanges();
            Transport.send(mimeMessage);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    static class MyAuthenricator extends Authenticator {
        String u = null;
        String p = null;

        public MyAuthenricator(String u, String p) {
            this.u = u;
            this.p = p;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(u, p);
        }
    }
}
