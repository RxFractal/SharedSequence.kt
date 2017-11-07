package org.notests.sharedsequence

import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.lang.Exception
import java.lang.RuntimeException

/**
 * Created by markotron on 11/6/17.
 */

class FatalError(cause: Throwable) : RuntimeException(cause) {}

object ErrorReporting {
  private val reportingSubject = PublishSubject.create<Throwable>()

  /**
   * This will prevent fatal errors from firing.
   * SHOULD ONLY BE USED IN UNIT TESTS EXPLICITLY TESTING FOR EXCEPTIONS
   */
  var suppressFatalError = false

  fun <T> reportAndRethrow(action: () -> T): T {
    try {
      return action()
    } catch (e: Exception) {
      this.fatalErrorInDebugOrReportError(e)
      throw e
    }
  }

  fun <T> reportAndFallback(default: T, action: () -> T): T {
    return try {
      action()
    } catch (e: Exception) {
      this.fatalErrorInDebugOrReportError(e)
      default
    }
  }

  fun fatalErrorInDebugOrReportError(message: String) {
    this.fatalErrorInDebugOrReportError(kotlin.RuntimeException(message), true)
  }

  fun fatalErrorInDebugOrReportError(throwable: Throwable) {
    this.fatalErrorInDebugOrReportError(throwable, true)
  }

  fun <T> fatalErrorInDebugOrReportError(default: T, action: () -> T): T {
    return try {
      action()
    } catch (e: Exception) {
      this.fatalErrorInDebugOrReportError(e, true)
      default
    }
  }

  fun fatalErrorInDebugOrReportError(throwable: Throwable, shouldAssert: Boolean = false) {
    reportingSubject.onNext(throwable)
    if (shouldAssert && !suppressFatalError) {
      fatalError(throwable)
    }
  }
}

private fun fatalError(throwable: Throwable) {
  Schedulers.io().createWorker().schedule {
    throw FatalError(throwable)
  }
}