package sqlbuilder.pool

/**
 * Provides end-user identity for tracing purposes.
 * @author vdlindla
 */
interface IdentityPlugin {
    val traceUsername: String?
}
