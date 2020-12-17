import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

// Change this to control the default release bump for th next version
val nextReleaseBump = sbtrelease.Version.Bump.Minor

// Version & Release flows
val releaseProcessBumpAndTag: Seq[ReleaseStep] = Seq(
  inquireVersions,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease
)

val releaseProcessSnapshotBump: Seq[ReleaseStep] = Seq(
  inquireVersions,
  setNextVersion,
  commitNextVersion
)

def bumpedVersion(bump: sbtrelease.Version.Bump, state: State)(version: String): String = {
  sbtrelease
    .Version(version)
    .map {
      case v if version == v.withoutQualifier.string =>
        v.bump(bump).withoutQualifier.string
      case v => v.withoutQualifier.string
    }
    .getOrElse(sbtrelease.versionFormatError(version))
}

def nextSnapshotVersion(bump: sbtrelease.Version.Bump, state: State)(version: String): String = {
  val shortHashLength = 7
  val shortHash = vcs(state).currentHash.substring(0, shortHashLength)
  sbtrelease
    .Version(version)
    .map(
      _.copy(qualifier = Some(s"-$shortHash-SNAPSHOT")).string
    )
    .getOrElse(sbtrelease.versionFormatError(version))
}

def bump(bump: sbtrelease.Version.Bump, steps: Seq[ReleaseStep])(
    state: State
): State = {
  Command.process(
    "release with-defaults",
    Project
      .extract(state)
      .appendWithoutSession(
        Seq(
          releaseVersionBump := bump,
          releaseProcess := steps,
          releaseVersion := bumpedVersion(bump, state),
          releaseNextVersion := nextSnapshotVersion(releaseVersionBump.value, state)
        ),
        state
      )
  )
}

def vcs(state: State): sbtrelease.Vcs =
  Project
    .extract(state)
    .get(releaseVcs)
    .getOrElse(
      sys.error("Aborting release. Working directory is not a repository of a recognized VCS.")
    )

commands += Command.command("bumpPatch")(
  bump(sbtrelease.Version.Bump.Bugfix, releaseProcessBumpAndTag)
)
commands += Command.command("bumpMinor")(
  bump(sbtrelease.Version.Bump.Minor, releaseProcessBumpAndTag)
)
commands += Command.command("bumpMajor")(
  bump(sbtrelease.Version.Bump.Major, releaseProcessBumpAndTag)
)
commands += Command.command("bumpRelease")(
  bump(nextReleaseBump, releaseProcessBumpAndTag)
)
commands += Command.command("bumpSnapshot")(
  bump(nextReleaseBump, releaseProcessSnapshotBump)
)
