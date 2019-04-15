package webservers

import authenticators.IRequestAuthenticator
import main.ISyslog

import java.util.HashMap

/**
 * Interface which any webserver class must implement
 */
interface IWebServer : Runnable, ISyslog.Loggable {
    val syslog: ISyslog?
    fun configure(config: HashMap<String, Any>)
    fun setup()
    fun getAuthenticator(url: String): IRequestAuthenticator?
}
