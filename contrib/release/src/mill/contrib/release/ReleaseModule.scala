package mill.contrib.release

import mill._, scalalib._

import os.Path

trait ReleaseModule extends Module {

  implicit val wd = os.pwd

  def versionFile: String = "version"
  val versionFilePath: Path = wd / versionFile

  def currentVersion = T { Version.of(os.read(versionFilePath)) }
  def releaseVersion = T { currentVersion().asRelease }
  def nextVersion(bump: String) = T.command { releaseVersion().bump(bump) }

  def setReleaseVersion = T {
    val commitMessage = s"Setting release version to ${releaseVersion()}"
    
    T.ctx.log.info(commitMessage)

    os.write.over(
      versionFilePath,
      releaseVersion().toString
    )

    os.proc("git", "commit", "-am", commitMessage).call()
    os.proc("git", "tag", releaseVersion().toString).call()
  }

  def setNextVersion(bump: String) = T.command {
    val commitMessage = s"Setting next version to ${nextVersion(bump)}"

    T.ctx.log.info(commitMessage)

    os.write.over(
      versionFilePath,
      nextVersion(bump).toString
    )

    os.proc("git", "commit", "-am", commitMessage).call()
    os.proc("git", "push", "origin", "master", "--tags").call()
  }
}
