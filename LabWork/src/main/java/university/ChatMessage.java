package university;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { TEXT, FILE }

    public final int    id;           // DB auto-increment PK (0 when not yet persisted)
    public final String senderRole;   // "TEACHER" or "STUDENT"
    public final String senderName;
    public final Type   type;
    public final String content;      // text body  OR  original filename for FILE messages
    public final byte[] fileData;     // null for TEXT messages
    public final long   timestamp;    // System.currentTimeMillis()

    public ChatMessage(String senderRole, String senderName, String text) {
        this(0, senderRole, senderName, Type.TEXT, text, null, System.currentTimeMillis());
    }

    public ChatMessage(String senderRole, String senderName, String filename, byte[] fileData) {
        this(0, senderRole, senderName, Type.FILE, filename, fileData, System.currentTimeMillis());
    }

    public ChatMessage(int id, String senderRole, String senderName,
                       Type type, String content, byte[] fileData, long timestamp) {
        this.id         = id;
        this.senderRole = senderRole;
        this.senderName = senderName;
        this.type       = type;
        this.content    = content;
        this.fileData   = fileData;
        this.timestamp  = timestamp;
    }

    @Override
    public String toString() {
        String tag = type == Type.FILE ? "[FILE: " + content + "]" : content;
        return "[" + senderRole + "] " + senderName + ": " + tag;
    }
}
