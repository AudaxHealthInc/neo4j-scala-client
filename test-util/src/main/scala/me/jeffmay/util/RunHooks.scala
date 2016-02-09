package me.jeffmay.util

import scala.collection.immutable.ListMap
import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait HookRunOrder {
  self: RunHooks =>

  implicit protected def executionContext: ExecutionContext

  protected def timeout: FiniteDuration

  def runHooks(): Unit
}

trait RunHooksBlockingInOrder extends HookRunOrder {
  self: RunHooks =>

  final override implicit protected lazy val executionContext: ExecutionContext = new ExecutionContext {
    override def reportFailure(cause: Throwable): Unit = println(s"Encountered $cause during shutdown.")
    override def execute(runnable: Runnable): Unit = runnable.run()
  }

  override def runHooks(): Unit = runHooksInOrder()
}

trait RunHooksAsyncInParallel extends HookRunOrder {
  self: RunHooks =>

  final override implicit protected def executionContext: ExecutionContext = ExecutionContext.global

  override def runHooks(): Unit = runHooksInParallel()
}

trait RunHooks {
  self: HookRunOrder =>

  // Execute the hooks upon construction
  runHooks()

  protected def log(msg: String): Unit = {
    println(s"[RunHooks] $msg")
  }

  protected def hooks: Seq[(String, ExecutionContext => Future[Any])]

  private def attemptHook(name: String, hookFn: ExecutionContext => Future[Any]): Future[Unit] = {
    log(s"Attempting hook: $name")
    Try(hookFn(executionContext)) match {
      case Success(hook) =>
        hook.map(_ => log(s"Finished running hook: $name")).recover {
          case ex =>
            log(s"ERROR executing hook '$name': $ex")
        }
      case Failure(ex) =>
        log(s"ERROR creating hook '$name': $ex")
        Future.successful(())
    }
  }

  protected def runHooksInOrder(): Unit = {
    val executedHooks = hooks.foldLeft(Future.successful(())) {
      case (acc, (hookName, hookFn)) =>
        acc.flatMap(_ => attemptHook(hookName, hookFn))(executionContext)
    }
    Await.result(executedHooks, timeout)
  }

  protected def runHooksInParallel(): Unit = {
    val executedHooks = Future.traverse(hooks) {
      case (hookName, hookFn) => attemptHook(hookName, hookFn)
    }
    Await.result(executedHooks, timeout)
  }
}
object RunHooks {

  private var _shutdownHooks: ListMap[String, ExecutionContext => Future[Any]] = ListMap.empty
  def shutdownHooks: Seq[(String, ExecutionContext => Future[Any])] = _shutdownHooks.toSeq

  def addShutdownHook(name: String)(hook: => Any): Unit = {
    addAsyncShutdownHook(name) { implicit ex: ExecutionContext => Future(hook) }
  }

  def addAsyncShutdownHook(name: String)(hook: ExecutionContext => Future[Any]): Unit = {
    require(!(_shutdownHooks contains name), s"Cannot add hook named '$name'. This hook name has already been registered")
    _shutdownHooks += name -> hook
  }
}

abstract class RunStartupHooks(
  override val timeout: FiniteDuration,
  startupHooks: ExecutionContext => Map[String, () => Future[Any]]
) extends RunHooks {
  self: HookRunOrder =>

  override protected def hooks: Seq[(String, ExecutionContext => Future[Any])] = {
    // pass the execution context immediately and then ignore it
    startupHooks(executionContext).mapValues { f =>
      { _: ExecutionContext => f() }
    }.toSeq
  }
}

abstract class RunShutdownHooks extends RunHooks {
  self: HookRunOrder =>

  override protected def hooks: Seq[(String, ExecutionContext => Future[Any])] = RunHooks.shutdownHooks
}

class TestCleanupInParallel(override val timeout: FiniteDuration = 30.seconds)
  extends RunShutdownHooks
    with RunHooksAsyncInParallel

class TestCleanupInOrder(override val timeout: FiniteDuration = 30.seconds)
  extends RunShutdownHooks
    with RunHooksBlockingInOrder
