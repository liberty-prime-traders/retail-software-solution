package me.ezra_home.retail_software_solution.util.async

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.configuration.session.withSession
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

@Component
class SessionAwareAsyncExecutor : AsyncExecutor {

  private val executor = Executors.newCachedThreadPool()

  override fun execute(block: () -> Unit) {
    val session = SessionContextProvider.getSession()
    executor.submit { withSession(session) { block() } }
  }
}
