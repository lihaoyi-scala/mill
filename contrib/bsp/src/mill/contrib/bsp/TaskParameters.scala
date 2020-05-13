package mill.contrib.bsp

import ch.epfl.scala.bsp4j.{BuildTargetIdentifier, CompileParams, RunParams, TestParams}

import scala.collection.JavaConverters._

/**
  * Common trait to represent BSP request parameters that
  * have a specific form: include one or more targetIds,
  * arguments for the execution of the task, and an optional
  * origin id generated by the client.
  */
trait Parameters {
  def getTargets: List[BuildTargetIdentifier]

  def getArguments: Option[Seq[String]]

  def getOriginId: Option[String]
}

case class CParams(compileParams: CompileParams) extends Parameters {

  override def getTargets: List[BuildTargetIdentifier] =
    compileParams.getTargets.asScala.toList

  override def getArguments: Option[Seq[String]] =
    try Option(compileParams.getArguments.asScala.toSeq)
    catch {
      case e: Exception => Option.empty[Seq[String]]
    }

  override def getOriginId: Option[String] =
    try Option(compileParams.getOriginId)
    catch {
      case e: Exception => Option.empty[String]
    }

}

case class RParams(runParams: RunParams) extends Parameters {

  override def getTargets: List[BuildTargetIdentifier] =
    List(runParams.getTarget)

  override def getArguments: Option[Seq[String]] =
    try Option(runParams.getArguments.asScala.toSeq)
    catch {
      case e: Exception => Option.empty[Seq[String]]
    }

  override def getOriginId: Option[String] =
    try Option(runParams.getOriginId)
    catch {
      case e: Exception => Option.empty[String]
    }

}

case class TParams(testParams: TestParams) extends Parameters {

  override def getTargets: List[BuildTargetIdentifier] =
    testParams.getTargets.asScala.toList

  override def getArguments: Option[Seq[String]] =
    try Option(testParams.getArguments.asScala.toSeq)
    catch {
      case e: Exception => Option.empty[Seq[String]]
    }

  override def getOriginId: Option[String] =
    try Option(testParams.getOriginId)
    catch {
      case e: Exception => Option.empty[String]
    }
}

object TaskParameters {

  /**
    * Convert parameters specific to the compile request
    * to the common trait Parameters.
    *
    * @param compileParams compile request parameters
    * @return general task parameters containing compilation info
    */
  def fromCompileParams(compileParams: CompileParams): Parameters =
    CParams(compileParams)

  /**
    * Convert parameters specific to the run request
    * to the common trait Parameters.
    *
    * @param runParams run request parameters
    * @return general task parameters containing running info
    */
  def fromRunParams(runParams: RunParams): Parameters =
    RParams(runParams)

  /**
    * Convert parameters specific to the test request
    * to the common trait Parameters.
    *
    * @param testParams compile request parameters
    * @return general task parameters containing testing info
    */
  def fromTestParams(testParams: TestParams): Parameters =
    TParams(testParams)
}
