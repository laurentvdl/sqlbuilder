package sqlbuilder.pool

/**
 * Provides end-user identity for tracing purposes.
 * @author vdlindla
 */
public trait IdentityPlugin {
    val traceUsername: String?
}
