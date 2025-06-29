package fm.jiecao.jcvideoplayer_lib;

public class MessageEventPublic {
    private String msgId;
    private String url;
    private String userId;

    public MessageEventPublic(String msgId, String url, String userId) {
        this.userId = userId;
        this.msgId = msgId;
        this.url = url;
    }

    public String getMsgId() {
        return msgId;
    }

    public String getUrl() {
        return url;
    }

    public String getUserId() {
        return userId;
    }
}
