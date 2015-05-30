package sqlbuilder.pool

/**
 * Provides end-user identity for tracing purposes.
 * @author vdlindla
 */
public interface IdentityPlugin {
    val traceUsername: String?
}
