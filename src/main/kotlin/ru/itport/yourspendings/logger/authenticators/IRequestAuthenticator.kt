package authenticators

import io.javalin.Context
import java.util.HashMap

/**
 * Public interface which all HTTP authenticators must implement
 */
interface IRequestAuthenticator {
    fun configure(config: HashMap<String, Any>)
    fun authenticate(ctx: Context): Boolean
    fun sendDenyResponse(ctx: Context)

}
