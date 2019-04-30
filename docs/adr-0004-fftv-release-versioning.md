# ADR 4: FFTV Release Versioning 5/15/2019
## Context
We build several different versions of Firefox for purposes such as release, beta testing, and staged rollout. It's not obvious how the version numbers are related to these types of builds, and features have sometimes disappeared in rollback versions, so we document our strategy here.

Firefox TV releases are listed [https://github.com/mozilla-mobile/firefox-tv/releases](here). (These include LATs, which are not included in the changelog, and the changelog may include additional information, like reasons for re-releasing a version.)

As of the time of writing, the current release version is `3.9`.

## Decision
Firefox TV versioning is based off of [https://semver.org/](semantic versioning) of MAJOR.MINOR.PATCH, but reflects features rather than API compatibility.

Additionally, we also use alphanumeric suffixes to clearly differentiate between early test builds, releases, and re-releases.

Each release has a *tag* prefixed by `v`, such as `v3.8` and are listed in the [https://github.com/mozilla-mobile/firefox-tv/tags](Tags) page of the repo.

### Semantic Versioning
* MAJOR version changes signal significant changes to UI or functionality
* MINOR version changes are released every Sprint, unless they are skipped for release blockers
* PATCH version changes are for critical bug-fixes that cannot wait for the next Sprint.
* (LETTER-SUFFIX) reflects builds for our additional purposes that are detailed in following sections.

### Release
As of 3.8, public releases have no suffix, and are released using the staged rollout capability of the Amazon Developer portal.

### Live App Testing (-LAT1)
As part of our early testing, we create Live App Test (LAT) builds to send out candidate builds to our early testing groups before a release.

These have a `-LAT1` suffix, where the number is incremented per test build sent out per version. For example, the second test build for 3.5 would be `3.5-LAT2`.

This is first used in `3.3.0-LAT1`. These are used for testing, not general release.

#### Deprecated LAT versioning

Previously, the versioning was much more confusing. We wanted to preserve monotonic order versioning, so a LAT would have an additional number appended at the end of the *previous* version; for example, the second LAT testing the 3.2 release would be versioned `3.1.3.2`, because the last released version before `3.2` was `3.1.3`.

This deprecated LAT versioning was used between `2.1.0.1` and `3.1.3.2`.

### GeckoView (-GV)
Currently, there are two distinct web engines that Firefox for Fire TV can be build with: the system WebView or GeckoView. Since a build currently can only use one of these, when we make a build that uses the GeckoView engine, we need a separate suffix to differentiate it.

These GeckoView builds are suffixed with `-GV`.

This is first used in `3.4-GV`, but is used for testing and not released to the general population.

### Re-Release (-A)
There are two cases for re-release:
1) Rollback to a previous version due to critical bugs (e.g. rollback of 3.4 should be 3.3-A, although this is untested, and the platform may not allow decremented versioning, in which case, we would release the rollback as 3.4-A)
1) (deprecated) Release of a tested "staged rollout" build to the rest of the devices. (This is no longer used because staged rollout capability has been added to the app store.) This was monotonic because the "general population" devices had not been upgraded past this version.

This is a build that has already been released before, either to a portion of the population, or because in a subsequent release we needed to do a version bump in order to push out a release, but did not change the code.

These re-release builds are suffixed with a letter starting with `-A`, which is incremented with each re-release.

This is first used in `3.4-A`.

#### Deprecated re-release versioning
Before we started using letters to signify re-releases, we simply bumped the version number, so there are several versions that are simply re-releases of previous versions, but with different version numbers. These are listed below:

**3.1.3** is the same version as:
* 3.2.5
* 3.3
* 3.4-A
* 3.4-B

(and also listed in the changelog)

### Deprecated Split-Device Staged Rollout (-RO)
These split-device staged rollout releases were suffixed with a `-RO`, e.g. `3.7-RO`.

Before the staged rollout capability was added to the Amazon Developer portal, we handled staged rollout by releasing the newest version only to a single device, in this case the Firefox TV 4K Pendant (AFTN).

This versioning scheme was used between `3.5-RO` and `3.7-RO`.

### Status: Accepted

## Consequences
- If we need to re-release builds through a rollback, if the app store versioning enforces monotonic versioning, the re-release versioning may not apply, and may need to be bumped.
