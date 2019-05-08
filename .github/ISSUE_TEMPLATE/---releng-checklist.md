---
name: "✅ Releng checklist"
about: Keep track of the release activities
title: 'Releng checklist - vX.X'

---

##### Sprint Release Owner: _[name here]_

#### Tuesday #1

- [ ] Upgrade a-c

#### Wednesday #1

- [ ] Export strings (*if applicable*) ([instructions](https://github.com/mozilla-mobile/firefox-tv/wiki/Localization#exporting-strings-for-translation))
- [ ] Create GitHub pre-release build ([instructions](https://github.com/mozilla-mobile/firefox-tv/wiki/Releng-Checklist#final-builds))
- [ ] Release LAT ([instructions](https://developer.amazon.com/docs/app-testing/live-app-testing-getting-started.html))

#### Wednesday #2 (soft code freeze)

- [ ] Create PR for bumping version and updating changelog
  - Update the CHANGELOG: glance over the changelog to see if any major 
    features are missing and add the new release header (don't forget to add the diff links at the bottom)
  - Merge to master
- [ ] Cut release branch `releases/vX.X`
- [ ] Create GitHub release ([instructions](https://github.com/mozilla-mobile/firefox-tv/wiki/Releng-Checklist#final-builds))
- [ ] Release LAT ([instructions](https://developer.amazon.com/docs/app-testing/live-app-testing-getting-started.html))

#### Thursday #2

- [ ] [QA] run screenshot test and upload ([instructions](https://github.com/mozilla-mobile/firefox-tv/wiki/Localization#screenshots))
- [ ] Upgrade a-c

#### Friday #2 (hard code freeze)

- [ ] [Product, QA, eng] sign-off for release

#### Monday #3

- [ ] Submit app with staged rollout at 1% ([instructions](https://developer.amazon.com/docs/app-submission/submitting-apps-to-amazon-appstore.html))

#### Tuesday #3

- [ ] [QA] sign-off

- [ ] Bump rollout to 20%

#### Wednesday #3

- [ ] [QA] sign-off

- [ ] Bump rollout to 100%
