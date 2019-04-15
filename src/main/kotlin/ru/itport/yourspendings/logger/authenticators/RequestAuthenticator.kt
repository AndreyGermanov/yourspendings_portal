package authenticators

import config.ConfigManager

import java.util.HashMap

/**
 * Basic class for all HTTP authenticators
 */
abstract class RequestAuthenticator : IRequestAuthenticator {
    companion object {
        /** Factory method which returns instance of concrete authenticator class based on name in configuration file
         * @param name Unique name of authenticator
         * @return Instance of authenticator
         */
        operator fun get(name: String): IRequestAuthenticator? {
            val config = ConfigManager.getInstance().getConfigNode("authenticators", name)
            if (config == null || !config.containsKey("type")) return null
            when (config["type"].toString()) {
                "basic" -> return BasicRequestAuthenticator(config)
                else -> return null
            }
        }
    }
}