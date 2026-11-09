package modcore.agent;

import java.lang.instrument.Instrumentation;

/** Dynamic entry; application initialization belongs to the controlled launch gate. */
public final class AgentMain {
    private AgentMain() {}

    public static void premain(String arguments, Instrumentation instance) {
        throw new IllegalStateException("Startup agents are not supported; use the controlled launch gate");
    }

    public static void agentmain(String arguments, Instrumentation instance) throws Exception {
        DynamicAgent.install(arguments, instance);
    }
}
