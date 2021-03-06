package org.notests.sharedsequence

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.TestScheduler
import org.junit.*
import org.notests.sharedsequence.api.ErrorReporting
import org.notests.sharedsequence.utils.*

/**
 * Created by juraj begovac on 13/9/18
 */
class DriverTest : Assert() {

  private lateinit var scheduler: TestScheduler
  private lateinit var observer: MyTestSubscriber<Int>

  @get:Rule
  val testSchedulerRule = TestSchedulerRule()

  @Before
  fun setUp() {
    scheduler = testSchedulerRule.testScheduler
    observer = scheduler.createMyTestSubscriber()
  }

  @After
  fun tearDown() {
    observer.dispose()
  }

  @Test
  fun drive_WhenErroring() {

    val exception = Exception("3 reached")

    val errorObserver = scheduler.createMyTestSubscriber<Throwable>()
    ErrorReporting.exceptions().subscribe(errorObserver)

    val mutableList = mutableListOf<Int>()

    observableRange()
      .asDriverCompleteOnError()
      // Even if we throw in the drive method we shouldn't unsubscribe. We should report the error
      // via ErrorReporting and remain subscribed.
      .drive { if(it == 3) throw exception else mutableList.add(it) }

    assertEquals(listOf(next(0, exception)), errorObserver.recordedEvents())
    assertEquals(listOf(1, 2, 4, 5, 6, 7, 8, 9, 10), mutableList)
  }

  @Test
  fun driverCompleteOnError() {
    scheduler.scheduleAt(0) {
      observableRange()
        .map {
          if (it == 5) throw Exception()
          else it
        }
        .asDriverCompleteOnError()
        .drive(observer)
    }
    scheduler.triggerActions()

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, 3),
                        next(0, 4),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun driverOnErrorJustReturn() {
    val returnOnError = 7

    scheduler.scheduleAt(0) {
      observableRange()
        .map {
          if (it == 5) throw Exception()
          else it
        }
        .asDriver(returnOnError)
        .drive(observer)
    }
    scheduler.triggerActions()

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, 3),
                        next(0, 4),
                        next(0, 7),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun driverOnErrorDriveWith() {
    scheduler.scheduleAt(0) {
      observableRange()
        .map {
          if (it == 5) throw Exception()
          else it
        }
        .asDriver(observableRange().asDriverCompleteOnError())
        .drive(observer)
    }
    scheduler.triggerActions()

    assertEquals(
      listOf(next(0, 1),
             next(0, 2),
             next(0, 3),
             next(0, 4),
             next(0, 1),
             next(0, 2),
             next(0, 3),
             next(0, 4),
             next(0, 5),
             next(0, 6),
             next(0, 7),
             next(0, 8),
             next(0, 9),
             next(0, 10),
             complete(0)),
      observer.recordedEvents())
  }

  @Test
  fun defer() {
    scheduler.scheduleAt(0) {
      Driver.defer { observableRange().asDriverCompleteOnError() }
        .drive(observer)
    }
    scheduler.triggerActions()

    assertEquals(
      arrayListOf(
        next(0, 1),
        next(0, 2),
        next(0, 3),
        next(0, 4),
        next(0, 5),
        next(0, 6),
        next(0, 7),
        next(0, 8),
        next(0, 9),
        next(0, 10),
        complete(0)),
      observer.recordedEvents())
  }

  @Suppress("ConstantConditionIf")
  @Test
  fun deferOnErrorComplete() {
    scheduler.scheduleAt(0) {
      Driver.defer {
        if (true) throw Exception()
        else
          observableRange().asDriverCompleteOnError()
      }
        .drive(observer)
    }
    scheduler.triggerActions()

    assertEquals(listOf(complete<Int>(0)), observer.recordedEvents())
  }

  @Test
  fun catchErrorAndComplete() {
    scheduler.scheduleAt(0) {
      observableRange()
        .map {
          if (it == 5)
            throw Exception()
          else it
        }
        .asDriverCompleteOnError()
        .drive(observer)
    }
    scheduler.triggerActions()

    assertEquals(
      listOf(next(0, 1),
             next(0, 2),
             next(0, 3),
             next(0, 4),
             complete(0)),
      observer.recordedEvents())
  }

  @Test
  fun catchErrorAndReturn() {
    val returnOnError = 7

    observableRange()
      .map {
        if (it == 5)
          throw Exception()
        else it
      }
      .asDriver(Driver.just(returnOnError))
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, 3),
                        next(0, 4),
                        next(0, 7),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun map() {
    val exception = Exception("5 reached")

    val errorObserver = scheduler.createMyTestSubscriber<Throwable>()

    ErrorReporting.exceptions()
      .subscribe(errorObserver)

    observableRange()
      .asDriverCompleteOnError()
      .map {
        if (it == 5)
          throw exception
        else it
      }
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, 3),
                        next(0, 4),
                        next(0, 6),
                        next(0, 7),
                        next(0, 8),
                        next(0, 9),
                        next(0, 10),
                        complete(0)),
                 observer.recordedEvents())

    assertEquals(listOf(next(0, exception)),
                 errorObserver.recordedEvents())
  }

  @Test
  fun filter() {
    observableRange()
      .asDriverCompleteOnError()
      .filter {
        if (it == 5)
          throw Exception()
        else true
      }
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, 3),
                        next(0, 4),
                        next(0, 6),
                        next(0, 7),
                        next(0, 8),
                        next(0, 9),
                        next(0, 10),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun flatMap() {
    observableRange()
      .asDriverCompleteOnError()
      .flatMapDriver {
        if (it == 5)
          throw Exception()
        else Driver.just(it)
      }
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, 3),
                        next(0, 4),
                        next(0, 6),
                        next(0, 7),
                        next(0, 8),
                        next(0, 9),
                        next(0, 10),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun driverSharing_WhenErroring() {
    val observer1 = scheduler.createMyTestSubscriber<Int>()
    val observer2 = scheduler.createMyTestSubscriber<Int>()
    val observer3 = scheduler.createMyTestSubscriber<Int>()

    var disposable1: Disposable? = null
    var disposable2: Disposable? = null
    var disposable3: Disposable? = null

    val coldObservable = scheduler.createColdObservable(
      next(10, 0),
      next(20, 1),
      next(30, 2),
      next(40, 3),
      error(50, Error("Test")))

    val driver = coldObservable.asDriver(-1)

    scheduler.scheduleAt(200) {
      disposable1 =
        driver.asObservable()
          .subscribe({ observer1.onNext(it) },
                     { observer1.onError(it) },
                     { observer1.onComplete() })
    }

    scheduler.scheduleAt(225) {
      disposable2 =
        driver.asObservable()
          .subscribe({ observer2.onNext(it) },
                     { observer2.onError(it) },
                     { observer2.onComplete() })
    }

    scheduler.scheduleAt(235) { disposable1!!.dispose() }

    scheduler.scheduleAt(260) { disposable2!!.dispose() }

    // resubscription
    scheduler.scheduleAt(260) {
      disposable3 =
        driver.asObservable()
          .subscribe({ observer3.onNext(it) },
                     { observer3.onError(it) },
                     { observer3.onComplete() })
    }

    scheduler.scheduleAt(285) { disposable3!!.dispose() }

    scheduler.advanceTimeBy(1000)

    assertEquals(listOf(next(210, 0),
                        next(220, 1),
                        next(230, 2)),
                 observer1.recordedEvents())

    assertEquals(listOf(next(225, 1),
                        next(230, 2),
                        next(240, 3),
                        next(250, -1),
                        complete(250)),
                 observer2.recordedEvents())

    assertEquals(listOf(next(270, 0),
                        next(280, 1)),
                 observer3.recordedEvents())
  }

  @Test
  fun driverSharing_WhenCompleted() {
    val observer1 = scheduler.createMyTestSubscriber<Int>()
    val observer2 = scheduler.createMyTestSubscriber<Int>()
    val observer3 = scheduler.createMyTestSubscriber<Int>()

    var disposable1: Disposable? = null
    var disposable2: Disposable? = null
    var disposable3: Disposable? = null

    val coldObservable = scheduler.createColdObservable(
      next(10, 0),
      next(20, 1),
      next(30, 2),
      next(40, 3),
      complete(50))

    val driver = coldObservable.asDriver(-1)

    scheduler.scheduleAt(200) {
      disposable1 =
        driver.asObservable()
          .subscribe({ observer1.onNext(it) },
                     { observer1.onError(it) },
                     { observer1.onComplete() })
    }

    scheduler.scheduleAt(225) {
      disposable2 =
        driver.asObservable()
          .subscribe({ observer2.onNext(it) },
                     { observer2.onError(it) },
                     { observer2.onComplete() })
    }

    scheduler.scheduleAt(235) { disposable1!!.dispose() }

    scheduler.scheduleAt(260) { disposable2!!.dispose() }

    // resubscription
    scheduler.scheduleAt(260) {
      disposable3 =
        driver.asObservable()
          .subscribe({ observer3.onNext(it) },
                     { observer3.onError(it) },
                     { observer3.onComplete() })
    }

    scheduler.scheduleAt(285) { disposable3!!.dispose() }

    scheduler.advanceTimeBy(1000)

    assertEquals(listOf(next(210, 0),
                        next(220, 1),
                        next(230, 2)),
                 observer1.recordedEvents())

    assertEquals(listOf(next(225, 1),
                        next(230, 2),
                        next(240, 3),
                        complete(250)),
                 observer2.recordedEvents())

    assertEquals(listOf(next(270, 0),
                        next(280, 1)),
                 observer3.recordedEvents())
  }

  @Test
  fun asDriver_onErrorJustReturn() {
    val source = scheduler.createColdObservable(next(0, 1), next(0, 2), error(0, Error("Test")))

    source.asDriver(-1)
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun asDriver_onErrorDriveWith() {
    val source = scheduler.createColdObservable(next(0, 1), next(0, 2), error(0, Error("Test")))

    source.asDriver(Driver.just(-1))
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun asDriver_onErrorRecover() {
    val source = scheduler.createColdObservable(next(0, 1), next(0, 2), error(0, Error("Test")))

    source.asDriver(Driver.empty())
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun asDriver_defer() {
    val source = scheduler.createColdObservable(next(0, 1), next(0, 2), error(0, Error("Test")))

    Driver.defer { source.asDriver(-1) }
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun asDriver_map() {
    val source = scheduler.createColdObservable(next(0, 1), next(0, 2), error(0, Error("Test")))

    source.asDriver(-1)
      .map { it + 1 }
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 2),
                        next(0, 3),
                        next(0, 0),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun asDriver_filter() {
    val source = scheduler.createColdObservable(next(0, 1), next(0, 2), error(0, Error("Test")))

    source.asDriver(-1)
      .filter { it % 2 == 0 }
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 2),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun asDriver_switchMap() {
    val observable = scheduler.createColdObservable(next(0, 0),
                                                    next(1, 1),
                                                    error(2, Error("Test")),
                                                    complete(3))
    val observable1 = scheduler.createColdObservable(next(0, 1),
                                                     next(0, 2),
                                                     error(0, Error("Test")))
    val observable2 = scheduler.createColdObservable(next(0, 10),
                                                     next(0, 11),
                                                     error(0, Error("Test")))
    val errorObservable = scheduler.createColdObservable(complete<Int>(0))

    val drivers = arrayListOf(
      observable1.asDriver(-2),
      observable2.asDriver(-3),
      errorObservable.asDriver(-4))

    observable.asDriver(2)
      .switchMapDriver { drivers[it] }
      .drive(observer)

    scheduler.advanceTimeBy(10)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -2),
                        next(1, 10),
                        next(1, 11),
                        next(1, -3),
                        complete(2)),
                 observer.recordedEvents())
  }

  @Test
  fun asDriver_switchMap_overlapping() {
    val observable = scheduler.createColdObservable(next(0, 0),
                                                    next(1, 1),
                                                    error(2, Error("Test")),
                                                    complete(3))
    val observable1 = scheduler.createColdObservable(next(0, 1),
                                                     error(0, Error("Test")),
                                                     next(1, 2))
    val observable2 = scheduler.createColdObservable(next(0, 10),
                                                     error(0, Error("Test")),
                                                     next(1, 11))
    val errorObservable = scheduler.createColdObservable(complete<Int>(0))

    val drivers = arrayListOf(observable1.asDriver(-2),
                              observable2.asDriver(-3),
                              errorObservable.asDriver(-4))

    observable.asDriver(2)
      .switchMapDriver { drivers[it] }
      .drive(observer)

    scheduler.advanceTimeBy(10)

    assertEquals(listOf(next(0, 1),
                        next(0, -2),
                        next(1, 10),
                        next(1, -3),
                        complete(2)),
                 observer.recordedEvents())
  }

  @Test
  fun asDriver_doOnNext() {
    val observable = scheduler.createColdObservable(next(0, 1),
                                                    next(0, 2),
                                                    error(0, Error("Test")))

    var events = emptyArray<Int>()

    observable.asDriver(-1)
      .doOnNext {
        events += it
      }
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())

    val expectedEvents = arrayOf(1, 2, -1)
    assertArrayEquals(expectedEvents, events)
  }

  @Test
  fun asDriver_distinctUntilChanged1() {
    val observable = scheduler.createColdObservable(next(0, 1),
                                                    next(0, 2),
                                                    next(0, 2),
                                                    error(0, Error("Test")))
    observable.asDriver(-1)
      .distinctUntilChanged()
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun asDriver_distinctUntilChanged2() {
    val observable = scheduler.createColdObservable(next(0, 1),
                                                    next(0, 2),
                                                    next(0, 2),
                                                    error(0, Error("Test")))
    observable.asDriver(-1)
      .distinctUntilChanged { e -> e }
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun asDriver_distinctUntilChanged3() {
    val observable = scheduler.createColdObservable(next(0, 1),
                                                    next(0, 2),
                                                    next(0, 2),
                                                    error(0, Error("Test")))
    observable.asDriver(-1)
      .distinctUntilChanged { e1, e2 -> e1 == e2 }
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun asDriver_flatMap() {
    val observable = scheduler.createColdObservable(next(0, 1),
                                                    next(0, 2),
                                                    error(0, Error("Test")))
    observable.asDriver(-1)
      .flatMapDriver {
        Driver.just(it + 1)
      }
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 2),
                        next(0, 3),
                        next(0, 0),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun asDriver_merge() {
    val observable: Observable<Int> = scheduler.createColdObservable(next(0, 1),
                                                                     next(0, 2),
                                                                     error(0, Error("Test")))

    Driver.merge(arrayListOf(observable.asDriver(-1).flatMapDriver { Driver.just(it + 1) }))
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 2),
                        next(0, 3),
                        next(0, 0),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun asDriver_scan() {
    val observable: Observable<Int> = scheduler.createColdObservable(next(0, 1),
                                                                     next(0, 2),
                                                                     error(0, Error("Test")))

    observable.asDriver(-1)
      .scan(0) { a, n -> a + n }
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 0),
                        next(0, 1),
                        next(0, 3),
                        next(0, 2),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun asDriver_startWith() {
    val observable: Observable<Int> = scheduler.createColdObservable(next(0, 1),
                                                                     next(0, 2),
                                                                     error(0, Error("Test")))
    observable.asDriver(-1)
      .startWith(0)
      .drive(observer)

    scheduler.triggerActions()

    assertEquals(listOf(next(0, 0),
                        next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())
  }

  private fun observableRange() = Observable.range(1, 10)
}
