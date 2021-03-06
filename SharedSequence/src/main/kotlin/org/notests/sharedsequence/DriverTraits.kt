package org.notests.sharedsequence

import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.notests.sharedsequence.api.ErrorReporting
import org.notests.sharedsequence.api.SharedSequence
import org.notests.sharedsequence.api.SharingTrait

/**
 * Created by markotron on 11/6/17.
 */
@SharedSequence("Driver")
object DriverTraits : SharingTrait {
  override val scheduler: Scheduler
    get() = AndroidSchedulers.mainThread()

  override fun <Element> share(source: Observable<Element>): Observable<Element> =
    source.replay(1).refCount()
}

fun <Element> Driver<Element>.drive(onNext: (Element) -> Unit = {}): Disposable = this
  .asObservable()
  .subscribe {
    try {
      onNext(it)
    } catch (e: Exception) {
      ErrorReporting.report(e)
    }
  }

/**
 * Subscribe with a custom observer. Keep in mind that you have to handle errors manually here.
 */
fun <Element> Driver<Element>.drive(observer: Observer<Element>): Unit =
  this.asObservable().subscribe(observer)
