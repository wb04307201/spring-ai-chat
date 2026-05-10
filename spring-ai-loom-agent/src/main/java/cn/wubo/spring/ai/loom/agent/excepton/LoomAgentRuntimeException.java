package cn.wubo.spring.ai.loom.agent.excepton;

public class LoomAgentRuntimeException extends RuntimeException {
    public LoomAgentRuntimeException(String message) {
        super(message);
    }

    public LoomAgentRuntimeException(Throwable cause) {
        super(cause);
    }
}
