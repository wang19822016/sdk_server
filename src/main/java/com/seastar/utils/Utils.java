package com.seastar.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

/**
 * Created by wjl on 2016/6/3.
 */
public class Utils {

    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    public static String md5encode(String s) {
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
                'f' };
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(s.getBytes());
            byte[] bytes = md5.digest();

            StringBuffer stringbuffer = new StringBuffer(2 * bytes.length);
            for (int l = 0; l < bytes.length; l++) {
                char c0 = hexDigits[(bytes[l] & 0xf0) >> 4];
                char c1 = hexDigits[bytes[l] & 0xf];
                stringbuffer.append(c0);
                stringbuffer.append(c1);
            }
            return stringbuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String b64encode(String s) {
        s = s.trim();
        if (s.isEmpty())
            return s;
        try {
            return Base64.getEncoder().encodeToString(s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {

        }
        return s;
    }

    public static String b64decode(String s) {
        if (s.isEmpty())
            return s;
        return new String(Base64.getDecoder().decode(s));
    }

    public static String base64UrlEncode(String simple) {
        try {
            String s = new String(Base64.getEncoder().encode(simple.getBytes("UTF-8"))); // Regular base64 encoder
            s = s.split("=")[0]; // Remove any trailing '='s
            s = s.replace('+', '-'); // 62nd char of encoding
            s = s.replace('/', '_'); // 63rd char of encoding
            return s;
        } catch (UnsupportedEncodingException e) {

        }

        return simple;
    }

    public static String base64UrlDecode(String cipher) {
        String s = cipher;
        s = s.replace('-', '+'); // 62nd char of encoding
        s = s.replace('_', '/'); // 63rd char of encoding
        switch (s.length() % 4) { // Pad with trailing '='s
            case 0:
                break; // No pad chars in this case
            case 2:
                s += "==";
                break; // Two pad chars
            case 3:
                s += "=";
                break; // One pad char
            default:
                break;
        }
        return new String(Base64.getDecoder().decode(s)); // Standard base64 decoder
    }

    public static String sha256encode(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(s.getBytes());
            byte[] result = md.digest();

            String des = "";
            String tmp = null;
            for (int i = 0; i < result.length; i++) {
                tmp = (Integer.toHexString(result[i] & 0xFF));
                if (tmp.length() == 1) {
                    des += "0";
                }
                des += tmp;
            }

            return des;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getRandomString(int length) { //length表示生成字符串的长度
        String base = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    public static String getHttpBasicAuthUsername(HttpHeaders headers) {
        if (!headers.containsKey("Authorization"))
            return null;
        String credential = headers.getFirst("Authorization");
        String base64Credentials = credential.substring("Basic".length()).trim();
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), Charset.forName("UTF-8"));
        final String[] values = credentials.split(":",2);
        if (values.length != 2)
            return null;
        return values[0];
    }

    public static String getHttpBasicAuthPassword(HttpHeaders headers) {
        if (!headers.containsKey("Authorization"))
            return null;
        String credential = headers.getFirst("Authorization");
        String base64Credentials = credential.substring("Basic".length()).trim();
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), Charset.forName("UTF-8"));
        final String[] values = credentials.split(":",2);
        if (values.length != 2)
            return null;
        return values[1];
    }

    public static String getOAuthToken(HttpServletRequest request) {
        return getOAuthToken(request.getHeader("Authorization"));
    }

    public static String getOAuthToken(NativeWebRequest request) {
        return getOAuthToken(request.getHeader("Authorization"));
    }

    private static String getOAuthToken(String header) {
        if (header == null || header.isEmpty())
            return null;

        String token = header.substring("Bearer".length()).trim();
        if (token.isEmpty()) {
            return null;
        }

        return token;
    }

}
