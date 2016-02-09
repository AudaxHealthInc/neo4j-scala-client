package me.jeffmay.neo4j.client

import me.jeffmay.neo4j.client.Neo4jStatusCode.{Category, SubCategory}
import play.api.libs.json._

import scala.collection.mutable

/**
  * @see <a href="http://neo4j.com/docs/stable/status-codes.html">Status Codes Documentation</a>
  */
object StatusCodes {

  sealed abstract class NeoClassification(override val name: String)
    extends SubCategory {

    override final def parent: Neo4jStatusCode.Category = StatusCodes.Neo

    override lazy val statusCodes: Seq[Neo4jStatusCode] = subcategories.flatMap(_.statusCodes)
  }

  sealed class NeoCategory private[StatusCodes](override val parent: NeoClassification, override val name: String)
    extends SubCategory {

    override def subcategories: Seq[SubCategory] = Seq.empty

    private[this] val _codes = new mutable.ListBuffer[Neo4jStatusCode]()

    override final def statusCodes: Seq[Neo4jStatusCode] = _codes

    protected def hasChild(status: Neo4jStatusCode): Neo4jStatusCode = {
      _codes.append(status)
      status
    }
  }

  /**
    * The root of all Neo4j status codes.
    */
  object Neo extends Category {

    override final def name: String = path
    override final val path: String = "Neo"

    override def isRoot: Boolean = true

    override val subcategories: Seq[SubCategory] = Seq(
      ClientError,
      ClientNotification,
      DatabaseError,
      TransientError
    )

    override val statusCodes: Seq[Neo4jStatusCode] = subcategories.flatMap(_.statusCodes)

    /**
      * The client sent a bad request - changing the request might yield a successful outcome.
      *
      * @note Causes rollback
      */
    object ClientError extends NeoClassification("ClientError") {
      classification =>

      override val subcategories: Seq[SubCategory] = Seq(
        LegacyIndex,
        General,
        Request,
        Schema,
        Security,
        Statement,
        Transaction
      )

      object LegacyIndex extends NeoCategory(classification, "LegacyIndex") {

        final val NoSuchIndex = this hasChild Neo4jStatusCode(
          "Neo.ClientError.LegacyIndex.NoSuchIndex",
          "The request (directly or indirectly) referred to a index that does not exist.",
          this
        )
      }

      object General extends NeoCategory(classification, "General") {

        final val ReadOnly = this hasChild Neo4jStatusCode(
          "Neo.ClientError.General.ReadOnly",
          "This is a read only database, writing or modifying the database is not allowed.",
          this
        )
      }

      object Request extends NeoCategory(classification, "Request") {

        final val Invalid = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Request.Invalid",
          "The client provided an invalid request.",
          this
        )

        final val InvalidFormat = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Request.InvalidFormat",
          "The client provided a request that was missing required fields, or had values that are not allowed.",
          this
        )
      }

      object Schema extends NeoCategory(classification, "Schema") {

        final val ConstraintAlreadyExists = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Schema.ConstraintAlreadyExists",
          "Unable to perform operation because it would clash with a pre-existing constraint.",
          this
        )

        final val ConstraintVerificationFailure = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Schema.ConstraintVerificationFailure",
          "Unable to create constraint because data that exists in the database violates it.",
          this
        )

        final val ConstraintViolation = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Schema.ConstraintViolation",
          "A constraint imposed by the database was violated.",
          this
        )

        final val IllegalTokenName = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Schema.IllegalTokenName",
          "A token name, such as a label, relationship type or property key, used is not valid. " +
            "Tokens cannot be empty strings and cannot be null.",
          this
        )

        final val IndexAlreadyExists = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Schema.IndexAlreadyExists",
          "Unable to perform operation because it would clash with a pre-existing index.",
          this
        )

        final val IndexBelongsToConstraint = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Schema.IndexBelongsToConstraint",
          "A requested operation can not be performed on the specified index because the index is part of a constraint. " +
            "If you want to drop the index, for instance, you must drop the constraint.",
          this
        )

        final val IndexLimitReached = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Schema.IndexLimitReached",
          "The maximum number of index entries supported has been reached, no more entities can be indexed.",
          this
        )

        final val LabelLimitReached = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Schema.LabelLimitReached",
          "The maximum number of labels supported has been reached, no more labels can be created.",
          this
        )

        final val NoSuchConstraint = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Schema.NoSuchConstraint",
          "The request (directly or indirectly) referred to a constraint that does not exist.",
          this
        )

        final val NoSuchIndex = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Schema.NoSuchIndex",
          "The request (directly or indirectly) referred to an index that does not exist.",
          this
        )
      }

      object Security extends NeoCategory(classification, "Security") {

        final val AuthenticationFailed = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Security.AuthenticationFailed",
          "The client provided an incorrect username and/or password.",
          this
        )

        final val AuthenticationRateLimit = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Security.AuthenticationRateLimit",
          "The client has provided incorrect authentication details too many times in a row.",
          this
        )

        final val AuthorizationFailed = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Security.AuthorizationFailed",
          "The client does not have privileges to perform the operation requested.",
          this
        )
      }

      object Statement extends NeoCategory(classification, "Statement") {

        final val ArithmeticError = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Statement.ArithmeticError",
          "Invalid use of arithmetic, such as dividing by zero.",
          this
        )

        final val ConstraintViolation = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Statement.ConstraintViolation",
          "A constraint imposed by the statement is violated by the data in the database.",
          this
        )

        final val EntityNotFound = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Statement.EntityNotFound",
          "The statement is directly referring to an entity that does not exist.",
          this
        )

        final val InvalidArguments = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Statement.InvalidArguments",
          "The statement is attempting to perform operations using invalid arguments.",
          this
        )

        final val InvalidSemantics = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Statement.InvalidSemantics",
          "The statement is syntactically valid, but expresses something that the database cannot do.",
          this
        )

        final val InvalidSyntax = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Statement.InvalidSyntax",
          "The statement contains invalid or unsupported syntax.",
          this
        )

        final val InvalidType = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Statement.InvalidType",
          "The statement is attempting to perform operations on values with types that are not supported by the operation.",
          this
        )

        final val NoSuchLabel = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Statement.NoSuchLabel",
          "The statement is referring to a label that does not exist.",
          this
        )

        final val NoSuchProperty = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Statement.NoSuchProperty",
          "The statement is referring to a property that does not exist.",
          this
        )

        final val ParameterMissing = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Statement.ParameterMissing",
          "The statement is referring to a parameter that was not provided in the request.",
          this
        )
      }

      object Transaction extends NeoCategory(classification, "Transaction") {

        final val ConcurrentRequest = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Transaction.ConcurrentRequest",
          "There were concurrent requests accessing the same transaction, which is not allowed.",
          this
        )

        final val EventHandlerThrewException = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Transaction.EventHandlerThrewException",
          "A transaction event handler threw an exception. The transaction will be rolled back.",
          this
        )

        final val HookFailed = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Transaction.HookFailed",
          "Transaction hook failure.",
          this
        )

        final val InvalidType = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Transaction.InvalidType",
          "The transaction is of the wrong type to service the request. " +
            "For instance, a transaction that has had schema modifications performed in it cannot be used to " +
            "subsequently perform data operations, and vice versa.",
          this
        )

        final val MarkAsFailed = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Transaction.MarkAsFailed",
          "Transaction was marked as both successful and failed. Failure takes precedence and so this transaction was " +
            "rolled back although it may have looked like it was going to be committed.",
          this
        )

        final val UnknownId = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Transaction.UnknownId",
          "The request referred to a transaction that does not exist.",
          this
        )

        final val ValidationFailed = this hasChild Neo4jStatusCode(
          "Neo.ClientError.Transaction.ValidationFailed",
          "Transaction changes did not pass validation checks.",
          this
        )
      }

    }

    /**
      * There are notifications about the request sent by the client.
      *
      * @note Does not cause rollback
      */
    object ClientNotification extends NeoClassification("ClientNotification") {
      classification =>

      override val subcategories: Seq[SubCategory] = Seq(
        Statement
      )

      object Statement extends NeoCategory(classification, "Statement") {

        final val CartesianProduct = this hasChild Neo4jStatusCode(
          "Neo.ClientNotification.Statement.CartesianProduct",
          "This query builds a cartesian product between disconnected patterns.",
          this
        )

        final val DeprecationWarning = this hasChild Neo4jStatusCode(
          "Neo.ClientNotification.Statement.DeprecationWarning",
          "This feature is deprecated and will be removed in future versions.",
          this
        )

        final val DynamicPropertyWarning = this hasChild Neo4jStatusCode(
          "Neo.ClientNotification.Statement.DynamicPropertyWarning",
          "Queries using dynamic properties will use neither index seeks nor index scans for those properties.",
          this
        )

        final val EagerWarning = this hasChild Neo4jStatusCode(
          "Neo.ClientNotification.Statement.EagerWarning",
          "The execution plan for this query contains the Eager operator, " +
            "which forces all dependent data to be materialized in main memory before proceeding.",
          this
        )

        final val IndexMissingWarning = this hasChild Neo4jStatusCode(
          "Neo.ClientNotification.Statement.IndexMissingWarning",
          "Adding a schema index may speed up this query.",
          this
        )

        final val JoinHintUnfulfillableWarning = this hasChild Neo4jStatusCode(
          "Neo.ClientNotification.Statement.JoinHintUnfulfillableWarning",
          "The database was unable to plan a hinted join.",
          this
        )

        final val JoinHintUnsupportedWarning = this hasChild Neo4jStatusCode(
          "Neo.ClientNotification.Statement.JoinHintUnsupportedWarning",
          "Queries with join hints are not supported by the RULE planner.",
          this
        )

        final val LabelMissingWarning = this hasChild Neo4jStatusCode(
          "Neo.ClientNotification.Statement.LabelMissingWarning",
          "The provided label is not in the database.",
          this
        )

        final val PlannerUnsupportedWarning = this hasChild Neo4jStatusCode(
          "Neo.ClientNotification.Statement.PlannerUnsupportedWarning",
          "This query is not supported by the COST planner.",
          this
        )

        final val PropertyNameMissingWarning = this hasChild Neo4jStatusCode(
          "Neo.ClientNotification.Statement.PropertyNameMissingWarning",
          "The provided property name is not in the database.",
          this
        )

        final val RelTypeMissingWarning = this hasChild Neo4jStatusCode(
          "Neo.ClientNotification.Statement.RelTypeMissingWarning",
          "The provided relationship type is not in the database.",
          this
        )

        final val RuntimeUnsupportedWarning = this hasChild Neo4jStatusCode(
          "Neo.ClientNotification.Statement.RuntimeUnsupportedWarning",
          "This query is not supported by the compiled runtime.",
          this
        )

        final val UnboundedPatternWarning = this hasChild Neo4jStatusCode(
          "Neo.ClientNotification.Statement.UnboundedPatternWarning",
          "The provided pattern is unbounded, consider adding an upper limit to the number of node hops.",
          this
        )
      }

    }

    /**
      * The database failed to service the request.
      *
      * @note Causes rollback
      */
    object DatabaseError extends NeoClassification("DatabaseError") {
      classification =>

      override val subcategories: Seq[SubCategory] = Seq(
        General,
        Schema,
        Statement,
        Transaction
      )

      object General extends NeoCategory(classification, "General") {

        final val CorruptSchemaRule = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.General.CorruptSchemaRule",
          "A malformed schema rule was encountered. Please contact your support representative.",
          this
        )

        final val FailedIndex = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.General.FailedIndex",
          "The request (directly or indirectly) referred to an index that is in a failed state. " +
            "The index needs to be dropped and recreated manually.",
          this
        )

        final val UnknownFailure = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.General.UnknownFailure",
          "An unknown failure occurred.",
          this
        )
      }

      object Schema extends NeoCategory(classification, "Schema") {

        final val ConstraintCreationFailure = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Schema.ConstraintCreationFailure",
          "Creating a requested constraint failed.",
          this
        )

        final val ConstraintDropFailure = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Schema.ConstraintDropFailure",
          "The database failed to drop a requested constraint.",
          this
        )

        final val DuplicateSchemaRule = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Schema.DuplicateSchemaRule",
          "The request referred to a schema rule that defined multiple times.",
          this
        )

        final val IndexCreationFailure = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Schema.IndexCreationFailure",
          "Failed to create an index.",
          this
        )

        final val IndexDropFailure = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Schema.IndexDropFailure",
          "The database failed to drop a requested index.",
          this
        )

        final val NoSuchLabel = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Schema.NoSuchLabel",
          "The request accessed a label that did not exist.",
          this
        )

        final val NoSuchPropertyKey = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Schema.NoSuchPropertyKey",
          "The request accessed a property that does not exist.",
          this
        )

        final val NoSuchRelationshipType = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Schema.NoSuchRelationshipType",
          "The request accessed a relationship type that does not exist.",
          this
        )

        final val NoSuchSchemaRule = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Schema.NoSuchSchemaRule",
          "The request referred to a schema rule that does not exist.",
          this
        )
      }

      object Statement extends NeoCategory(classification, "Statement") {

        final val ExecutionFailure = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Statement.ExecutionFailure",
          "The database was unable to execute the statement.",
          this
        )
      }

      object Transaction extends NeoCategory(classification, "Transaction") {

        final val CouldNotBegin = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Transaction.CouldNotBegin",
          "The database was unable to start the transaction.",
          this
        )

        final val CouldNotCommit = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Transaction.CouldNotCommit",
          "The database was unable to commit the transaction.",
          this
        )

        final val CouldNotRollback = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Transaction.CouldNotRollback",
          "The database was unable to roll back the transaction.",
          this
        )

        final val CouldNotWriteToLog = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Transaction.CouldNotWriteToLog",
          "The database was unable to write transaction to log.",
          this
        )

        final val ReleaseLocksFailed = this hasChild Neo4jStatusCode(
          "Neo.DatabaseError.Transaction.ReleaseLocksFailed",
          "The transaction was unable to release one or more of its locks.",
          this
        )
      }

    }

    /**
      * The database cannot service the request right now, retrying later might yield a successful outcome.
      *
      * @note Causes rollback
      */
    object TransientError extends NeoClassification("TransientError") {
      classification =>

      override val subcategories: Seq[SubCategory] = Seq(
        General,
        Network,
        Schema,
        Security,
        Statement,
        Transaction
      )

      object General extends NeoCategory(classification, "General") {

        final val DatabaseUnavailable = this hasChild Neo4jStatusCode(
          "Neo.TransientError.General.DatabaseUnavailable",
          "The database is not currently available to serve your request, refer to the database logs for more details. " +
            "Retrying your request at a later time may succeed.",
          this
        )
      }

      object Network extends NeoCategory(classification, "Network") {

        final val UnknownFailure = this hasChild Neo4jStatusCode(
          "Neo.TransientError.Network.UnknownFailure",
          "An unknown network failure occurred, a retry may resolve the issue.",
          this
        )
      }

      object Schema extends NeoCategory(classification, "Schema") {

        final val ModifiedConcurrently = this hasChild Neo4jStatusCode(
          "Neo.TransientError.Schema.ModifiedConcurrently",
          "The database schema was modified while this transaction was running, the transaction should be retried.",
          this
        )
      }

      object Security extends NeoCategory(classification, "Security") {

        final val ModifiedConcurrently = this hasChild Neo4jStatusCode(
          "Neo.TransientError.Security.ModifiedConcurrently",
          "The user was modified concurrently to this request.",
          this
        )
      }

      object Statement extends NeoCategory(classification, "Statement") {

        final val ModifiedConcurrently = this hasChild Neo4jStatusCode(
          "Neo.TransientError.Statement.ExternalResourceFailure",
          "The user was modified concurrently to this request.",
          this
        )
      }

      object Transaction extends NeoCategory(classification, "Transaction") {

        final val AcquireLockTimeout = this hasChild Neo4jStatusCode(
          "Neo.TransientError.Transaction.AcquireLockTimeout",
          "The transaction was unable to acquire a lock, for instance due to a timeout " +
            "or the transaction thread being interrupted.",
          this
        )

        final val ConstraintsChanged = this hasChild Neo4jStatusCode(
          "Neo.TransientError.Transaction.ConstraintsChanged",
          "The transaction was unable to acquire a lock, for instance due to a timeout " +
            "or the transaction thread being interrupted.",
          this
        )

        final val DeadlockDetected = this hasChild Neo4jStatusCode(
          "Neo.TransientError.Transaction.DeadlockDetected",
          "This transaction, and at least one more transaction, has acquired locks " +
            "in a way that it will wait indefinitely, and the database has aborted it. " +
            "Retrying this transaction will most likely be successful.",
          this
        )
      }
    }

  }
}

/**
  * A status code received from the Neo4j REST API.
  *
  * @see [[StatusCodes]] for the enumeration of all status codes
  *
  * @param code the full dot separated name for this status
  * @param description documentation about this status code
  * @param parent the parent classification
  */
case class Neo4jStatusCode(code: String, description: String, parent: Neo4jStatusCode.SubCategory) {

  /**
    * Whether this code represents a notification about how the request can be improved.
    */
  def isNotification: Boolean = StatusCodes.Neo.ClientNotification contains this

  /**
    * Whether this code represents that something is wrong with the request.
    */
  def isClientError: Boolean = StatusCodes.Neo.ClientError contains this

  /**
    * Whether this code represents that something is wrong with the server.
    */
  def isServerError: Boolean = StatusCodes.Neo.DatabaseError contains this

  /**
    * Whether this code represents that a transient error occurred and the request is safe to retry.
    */
  def isTransient: Boolean = StatusCodes.Neo.TransientError contains this

  /**
    * A rollback was necessary.
    */
  def causedRollback: Boolean = !isNotification

  /**
    * Whether this request is safe to retry.
    */
  @inline def safeToRetry: Boolean = isTransient
}

object Neo4jStatusCode {

  /**
    * Formats [[Neo4jStatusCode]]s as a String in Json using the full key path.
    */
  implicit object FormatStatusCodeAsString extends Format[Neo4jStatusCode] {
    override def reads(json: JsValue): JsResult[Neo4jStatusCode] = {
      json.validate[String].flatMap { code =>
        findByCode(code) match {
          case Some(status) => JsSuccess(status)
          case None => JsError("error.expected.Neo4jStatusCode")
        }
      }
    }
    override def writes(status: Neo4jStatusCode): JsValue = JsString(status.code)
  }

  /**
    * A map of all the full key paths to [[Neo4jStatusCode]]s.
    */
  lazy val directory: Map[String, Neo4jStatusCode] = {
    StatusCodes.Neo.statusCodes
      .iterator
      .map(code => (code.code, code))
      .toMap
      .withDefault { key =>
        throw new NoSuchElementException(
          s"Cannot find Neo4jStatusCode with code '$key'. " +
          "(See http://neo4j.com/docs/stable/status-codes.html)"
        )
      }
  }

  def findByCodeOrThrow(code: String): Neo4jStatusCode = directory(code)

  def findByCode(code: String): Option[Neo4jStatusCode] = directory.get(code)

  sealed trait Category {
    def path: String
    def name: String
    def subcategories: Seq[SubCategory]
    def statusCodes: Seq[Neo4jStatusCode]
    def isRoot: Boolean
    def contains(code: Neo4jStatusCode): Boolean = (code.parent eq this) || (code.parent.parent eq this)
  }

  sealed trait SubCategory extends Category {
    def parent: Category
    override lazy val path: String = parent.path + "." + this.name
    final override def isRoot: Boolean = false
  }
}
