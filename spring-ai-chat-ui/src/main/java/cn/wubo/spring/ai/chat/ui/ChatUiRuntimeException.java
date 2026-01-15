package cn.wubo.spring.ai.chat.ui;

public class ChatUiRuntimeException extends RuntimeException {
    public ChatUiRuntimeException(String message) {
        super(message);
    }

    public ChatUiRuntimeException(Throwable cause) {
        super(cause);
    }
}
