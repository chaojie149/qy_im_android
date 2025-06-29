package com.tongxin.caihong.helper;

import com.tongxin.caihong.util.Base64;
import com.tongxin.caihong.util.secure.MAC;
import com.tongxin.caihong.util.secure.Parameter;

import java.util.Map;

public class MessageSecureHelper {
    public static void mac(String messageKey, Map<String, Object> message) {
        String mac = MAC.encodeBase64((Parameter.joinObjectValues(message)).getBytes(), Base64.decode(messageKey));
        message.put("mac", mac);
    }
}
