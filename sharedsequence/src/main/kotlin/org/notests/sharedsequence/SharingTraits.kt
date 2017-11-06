package org.notests.sharedsequence

import io.reactivex.Observable
import io.reactivex.Scheduler

interface SharingTrait {
  val scheduler: Scheduler
  fun <Element> share(source: Observable<Element>): Observable<Element>
}