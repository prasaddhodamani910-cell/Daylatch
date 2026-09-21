# PLAY STORE RELEASE MASTER CHECKLIST
## Production-readiness checklist for an offline Android app

> **Purpose:** Use this document to turn the current Android project into a polished, secure, stable, production-ready app suitable for Google Play Store release.
>
> **Important:** Do not blindly change existing functionality. First inspect the project, understand the current architecture, then make only the changes required by this checklist.
>
> **Workflow:** Audit → Plan → Implement → Build → Test → Fix → Re-test → Release.
>
> **Rule:** Do not mark an item complete unless it has actually been checked or tested.

---

# 0. AGENT INSTRUCTIONS

- [ ] Read this entire file before making changes.
- [ ] Inspect the entire project structure first.
- [ ] Identify the Android framework/build system currently being used.
- [ ] Identify the app package/application ID.
- [ ] Identify min SDK and target SDK.
- [ ] Identify the current version name and version code.
- [ ] Identify the current database/local-storage system.
- [ ] Identify all dependencies/libraries.
- [ ] Identify all Android permissions.
- [ ] Identify all Activities, Services, Broadcast Receivers, Providers, and exported components.
- [ ] Identify all screens and navigation routes.
- [ ] Identify all user data stored by the app.
- [ ] Identify whether any network access currently exists.
- [ ] Identify analytics/crash-reporting/advertising SDKs.
- [ ] Identify debug/test/demo code.
- [ ] Identify TODO/FIXME items.
- [ ] Do not remove an existing feature merely to satisfy this checklist.
- [ ] Do not introduce unnecessary libraries.
- [ ] Do not introduce internet/network functionality into an offline-only app.
- [ ] Keep the app's existing intended functionality intact.
- [ ] Make production changes incrementally.
- [ ] Build and test after major changes.
- [ ] Keep a record of important changes.
- [ ] At the end, produce a release audit report listing PASS, FAIL, and NEEDS MANUAL CHECK.

---

# 1. PROJECT STRUCTURE & ARCHITECTURE

- [ ] Project opens/builds successfully.
- [ ] Project structure is understandable.
- [ ] Source code is organized logically.
- [ ] UI code is separated from business logic where appropriate.
- [ ] Data/storage logic is separated from UI where appropriate.
- [ ] No unnecessary duplicate code.
- [ ] No dead screens.
- [ ] No unreachable production code.
- [ ] No unused resources where practical.
- [ ] No obsolete development files included in the release.
- [ ] Naming is consistent.
- [ ] Classes/functions/variables have understandable names.
- [ ] No sensitive information is hard-coded.
- [ ] No test credentials are present.
- [ ] No development URLs/endpoints are present unless genuinely required.
- [ ] No localhost references remain in production functionality.
- [ ] No debug-only functionality is accidentally exposed.

---

# 2. APP IDENTITY

- [ ] Final application ID/package name is correct.
- [ ] Application ID is stable and must not be changed after release without a deliberate migration strategy.
- [ ] App display name is finalized.
- [ ] App label is correct.
- [ ] App icon is finalized.
- [ ] Adaptive icon is configured correctly.
- [ ] Icon looks good on different Android launchers.
- [ ] Icon does not contain unnecessary tiny text.
- [ ] Icon assets are properly licensed/owned.
- [ ] App theme/branding is consistent.
- [ ] Splash screen is intentional and professional.
- [ ] No default template branding remains.
- [ ] No Android Studio/Gradle/template placeholder names remain.

---

# 3. VERSIONING

- [ ] Version name is finalized.
- [ ] Version code is finalized.
- [ ] Version code is unique and incremented for every Play Store update.
- [ ] First public release uses an appropriate production version.
- [ ] Future versioning strategy is documented.
- [ ] Release notes can be generated for future updates.

---

# 4. OFFLINE FUNCTIONALITY

## Core requirement

- [ ] App's primary functionality works without internet.
- [ ] Test with Wi-Fi disabled.
- [ ] Test with mobile data disabled.
- [ ] Test in Airplane Mode.
- [ ] App does not unexpectedly display network errors when network is unnecessary.
- [ ] No feature falsely claims to synchronize with a server.
- [ ] No unnecessary INTERNET permission exists.
- [ ] No hidden network request is required for startup.
- [ ] App can launch normally offline.
- [ ] App can be closed and reopened offline.
- [ ] App can be used after device reboot while offline.
- [ ] Existing user data remains available offline.
- [ ] Local data is correctly saved.
- [ ] Local data is correctly loaded.
- [ ] Local data is not accidentally deleted during normal app updates.
- [ ] Empty local storage is handled correctly.
- [ ] Large amounts of local data are handled correctly.
- [ ] Corrupted/unexpected local data has a safe recovery path where practical.

---

# 5. LOCAL DATA & DATABASE

- [ ] Identify every piece of stored data.
- [ ] Use an appropriate local storage mechanism.
- [ ] Database/schema is finalized.
- [ ] Database migrations are handled.
- [ ] Existing user data survives app updates.
- [ ] Database initialization is reliable.
- [ ] Database queries are efficient.
- [ ] No blocking heavy database work on the main/UI thread.
- [ ] Duplicate records are prevented where appropriate.
- [ ] Invalid values are handled.
- [ ] Deleted data is actually deleted when intended.
- [ ] Reset/clear-data functionality works correctly if provided.
- [ ] Date/time data is stored consistently.
- [ ] Sorting/filtering remains correct after restart.
- [ ] App behaves correctly after many records/items.
- [ ] Database versioning is documented.
- [ ] Database corruption does not cause an unrecoverable crash.
- [ ] Backup/export is considered for important user-generated data.

---

# 6. DATA LOSS PROTECTION

- [ ] User changes are saved at the correct time.
- [ ] Closing the app unexpectedly does not unnecessarily lose recent data.
- [ ] Process death is tested.
- [ ] Device reboot is tested.
- [ ] App recreation/configuration changes are tested.
- [ ] Important destructive actions have confirmation where appropriate.
- [ ] Delete actions cannot be triggered accidentally by normal taps.
- [ ] Undo is provided where it improves safety.
- [ ] Export/import is considered if losing data would be especially harmful.
- [ ] No fake backup/sync claims are made.

---

# 7. SECURITY

- [ ] No passwords are hard-coded.
- [ ] No API keys/secrets are hard-coded.
- [ ] No private signing keys are stored in the project repository.
- [ ] No production credentials are included.
- [ ] No debug credentials are included.
- [ ] Sensitive data is not logged.
- [ ] Sensitive local data is protected where appropriate.
- [ ] User input is validated.
- [ ] Malformed input cannot crash the app.
- [ ] File paths/URI handling are safe.
- [ ] Intents are validated where necessary.
- [ ] Exported components are reviewed.
- [ ] Services are reviewed.
- [ ] Broadcast Receivers are reviewed.
- [ ] Content Providers are reviewed.
- [ ] Deep links/app links are reviewed if present.
- [ ] WebViews are reviewed if present.
- [ ] No unnecessary permissions exist.
- [ ] Release build does not expose debug information unnecessarily.
- [ ] Release logging is reviewed.
- [ ] Third-party libraries are reviewed.
- [ ] Dependencies are updated where safe and compatible.
- [ ] No known critical vulnerability is knowingly shipped.

---

# 8. ANDROID PERMISSIONS

For EVERY permission:

- [ ] Identify why it exists.
- [ ] Confirm the feature genuinely needs it.
- [ ] Remove unnecessary permissions.
- [ ] Test permission denial.
- [ ] Test permission revocation after granting.
- [ ] Handle users who permanently deny a permission.
- [ ] Explain permission usage when appropriate.
- [ ] Do not request permissions at startup unnecessarily.
- [ ] Request permissions as close as possible to the feature that needs them.
- [ ] Ensure Play Console declarations match actual behavior.

---

# 9. UI / UX GENERAL

- [ ] Every screen looks intentional.
- [ ] No placeholder UI.
- [ ] No default template UI.
- [ ] No overlapping elements.
- [ ] No clipped text.
- [ ] No clipped icons.
- [ ] No elements extending outside their intended containers.
- [ ] No broken scrolling.
- [ ] No accidental horizontal scrolling.
- [ ] Buttons are clearly interactive.
- [ ] Touch targets are sufficiently large.
- [ ] Spacing is consistent.
- [ ] Typography is consistent.
- [ ] Icon sizes are consistent.
- [ ] Corner radii are consistent.
- [ ] Elevation/shadows are consistent where used.
- [ ] Colors are consistent.
- [ ] UI hierarchy is clear.
- [ ] Important actions are easy to find.
- [ ] Destructive actions are visually understandable.
- [ ] Navigation is predictable.
- [ ] Back button works naturally.
- [ ] No dead-end screens.
- [ ] No confusing menus.
- [ ] No unnecessary animations.
- [ ] Animations are smooth.
- [ ] Animations can tolerate reduced-motion/accessibility settings where appropriate.

---

# 10. SYSTEM BARS & EDGE-TO-EDGE

- [ ] Status bar does not overlap important content.
- [ ] App title does not overlap the status bar.
- [ ] Icons/buttons do not overlap system areas.
- [ ] Navigation bar does not cover content.
- [ ] Edge-to-edge behavior is intentional.
- [ ] Insets are handled correctly.
- [ ] Keyboard does not cover important controls.
- [ ] Dialogs are positioned correctly.
- [ ] Bottom navigation is not hidden by system navigation.
- [ ] Test gesture navigation.
- [ ] Test 3-button navigation where practical.
- [ ] Test different display sizes.

---

# 11. RESPONSIVE UI

Test:

- [ ] Small phone.
- [ ] Large phone.
- [ ] Different aspect ratios.
- [ ] Different screen densities.
- [ ] Portrait.
- [ ] Landscape if supported.
- [ ] Large font size.
- [ ] Display size changes.
- [ ] Foldable/tablet behavior if relevant.
- [ ] UI does not rely on hard-coded screen dimensions.
- [ ] Graphs/charts stay inside their intended containers.
- [ ] Images scale correctly.
- [ ] Text wraps correctly.
- [ ] Buttons remain accessible.

---

# 12. DARK MODE / LIGHT MODE

If supported:

- [ ] Light theme works.
- [ ] Dark theme works.
- [ ] Text remains readable.
- [ ] Icons remain visible.
- [ ] Charts remain readable.
- [ ] Dividers remain visible.
- [ ] Input fields remain readable.
- [ ] Dialogs remain readable.
- [ ] System bars match the theme appropriately.
- [ ] No hard-coded colors accidentally break dark mode.
- [ ] Images/assets are appropriate for both themes where necessary.

If dark mode is not supported:

- [ ] Confirm the app behaves acceptably under the device's theme settings.

---

# 13. TYPOGRAPHY & ACCESSIBILITY

- [ ] Text is readable.
- [ ] Font sizes are appropriate.
- [ ] Important text has sufficient contrast.
- [ ] Large font settings are tested.
- [ ] Text does not become clipped with larger fonts.
- [ ] Screen-reader labels exist where necessary.
- [ ] Decorative elements are not unnecessarily announced.
- [ ] Buttons have meaningful labels.
- [ ] Images have useful descriptions where necessary.
- [ ] Focus order is logical.
- [ ] Color is not the only way information is communicated.
- [ ] Interactive elements are sufficiently large.
- [ ] Accessibility services do not cause crashes.

---

# 14. NAVIGATION

- [ ] App launch screen is correct.
- [ ] Main navigation works.
- [ ] Back navigation works.
- [ ] Back from root screen behaves correctly.
- [ ] Deep navigation does not create broken states.
- [ ] Menus work.
- [ ] Three-dot menu works if present.
- [ ] About screen works.
- [ ] Credits screen works.
- [ ] Help/settings screens work if present.
- [ ] No screen can be reached that is unfinished.
- [ ] Navigation state survives appropriate lifecycle events.

---

# 15. ERROR HANDLING

Every important feature should have:

- [ ] Normal success state.
- [ ] Empty state.
- [ ] Invalid-input state.
- [ ] Failure state.
- [ ] Recovery path.
- [ ] User-friendly error message.
- [ ] No raw stack traces shown to users.
- [ ] No developer exception text shown to users.
- [ ] No unexplained blank screen.
- [ ] No infinite loading state.
- [ ] No crash when an operation fails.

---

# 16. INPUT VALIDATION

- [ ] Required fields are validated.
- [ ] Empty input is handled.
- [ ] Very long input is handled.
- [ ] Special characters are handled.
- [ ] Unicode text is handled.
- [ ] Numbers are validated.
- [ ] Invalid dates are handled.
- [ ] Invalid times are handled.
- [ ] Duplicate values are handled where appropriate.
- [ ] User cannot accidentally submit invalid data.
- [ ] Keyboard type matches expected input.
- [ ] Input fields have appropriate hints/labels.

---

# 17. LOADING / EMPTY / SUCCESS STATES

For every major screen:

- [ ] Loading state exists if loading is possible.
- [ ] Empty state exists.
- [ ] Success state exists.
- [ ] Error state exists.
- [ ] Retry/recovery exists where relevant.
- [ ] No UI flashes unnecessarily.
- [ ] No infinite spinner.
- [ ] No empty blank screen without explanation.

---

# 18. ANIMATIONS & PERFORMANCE

- [ ] Animations are smooth.
- [ ] Animations do not block interaction unnecessarily.
- [ ] No excessive animation.
- [ ] No animation causes crashes.
- [ ] Scrolling is smooth.
- [ ] Charts render efficiently.
- [ ] Large lists are efficient.
- [ ] Images are appropriately sized.
- [ ] No unnecessary image decoding.
- [ ] No obvious memory leaks.
- [ ] Heavy work is not performed on the UI thread.
- [ ] Startup is reasonably fast.
- [ ] App does not unnecessarily drain battery.
- [ ] App does not unnecessarily run in the background.

---

# 19. CRASH / ANR TESTING

- [ ] Test normal startup.
- [ ] Test repeated startup.
- [ ] Test rapid navigation.
- [ ] Test rapid tapping.
- [ ] Test repeated save/delete actions.
- [ ] Test empty database.
- [ ] Test large database.
- [ ] Test invalid input.
- [ ] Test permission denial.
- [ ] Test app background/foreground transitions.
- [ ] Test process death.
- [ ] Test device reboot.
- [ ] Test low-memory situations where practical.
- [ ] Test screen rotation where applicable.
- [ ] Test app update from previous version.
- [ ] Check for crashes.
- [ ] Check for ANRs.
- [ ] Fix all reproducible release-blocking crashes.

---

# 20. RELEASE BUILD CONFIGURATION

- [ ] Release build exists.
- [ ] Debug build is not being uploaded.
- [ ] Release signing is configured correctly.
- [ ] Signing credentials are stored securely.
- [ ] Keystore/upload credentials are backed up securely.
- [ ] Release build has correct application ID.
- [ ] Release build has correct version name.
- [ ] Release build has correct version code.
- [ ] Debug logging is removed/reduced appropriately.
- [ ] Debug flags are disabled.
- [ ] Test-only code is excluded.
- [ ] Development endpoints are excluded.
- [ ] Production resources are used.
- [ ] Release build is tested on a real device.

---

# 21. AAB / PLAY STORE PACKAGE

- [ ] Generate an Android App Bundle (AAB) for Play Store distribution.
- [ ] Verify the generated AAB is a release build.
- [ ] Verify package/application ID.
- [ ] Verify version code.
- [ ] Verify version name.
- [ ] Verify signing.
- [ ] Verify supported ABIs/configuration.
- [ ] Verify app starts after installation.
- [ ] Verify all major features work from the installed release build.
- [ ] Do not upload an accidentally generated debug artifact.

---

# 22. APP SIZE

- [ ] Check final AAB size.
- [ ] Remove unused large assets.
- [ ] Compress images appropriately.
- [ ] Remove unnecessary libraries.
- [ ] Avoid duplicate resources.
- [ ] Avoid unnecessarily large bundled files.
- [ ] Keep offline assets intentionally sized.
- [ ] Confirm startup remains fast.

---

# 23. DEPENDENCIES & LIBRARIES

For every dependency:

- [ ] Identify its purpose.
- [ ] Confirm it is actually used.
- [ ] Check version.
- [ ] Check compatibility with target Android version.
- [ ] Check licensing.
- [ ] Check security advisories where applicable.
- [ ] Remove unnecessary dependencies.
- [ ] Avoid adding a library for a problem that can be solved simply with existing project code.
- [ ] Ensure release build does not include development-only dependencies unnecessarily.

---

# 24. THIRD-PARTY SDKs

If any are present:

- [ ] Identify every SDK.
- [ ] Identify what data each SDK can collect.
- [ ] Identify network behavior.
- [ ] Identify permissions.
- [ ] Check license.
- [ ] Check privacy implications.
- [ ] Check Play Console disclosure requirements.
- [ ] Remove SDKs that are not actually needed.
- [ ] Ensure privacy policy accurately describes relevant data collection.

---

# 25. PRIVACY

Determine exactly:

- [ ] What data the app collects.
- [ ] What data the app stores.
- [ ] Where the data is stored.
- [ ] Whether data leaves the device.
- [ ] Whether analytics are used.
- [ ] Whether crash reporting is used.
- [ ] Whether advertisements are used.
- [ ] Whether third-party SDKs process data.
- [ ] Whether backups contain app data.
- [ ] Whether users can delete their data.
- [ ] Whether any account/login exists.
- [ ] Whether any identifiers are collected.

For an offline-only app, verify that your public claims accurately state that the relevant functionality/data stays on-device if that is genuinely true.

---

# 26. PRIVACY POLICY

- [ ] Determine whether a privacy policy is required.
- [ ] Create an accurate privacy policy if required.
- [ ] Include actual data practices.
- [ ] Do not claim "no data collection" if any SDK collects data.
- [ ] Do not claim "completely offline" if any feature requires internet.
- [ ] Keep the privacy policy accessible from an appropriate public location if required.
- [ ] Keep the policy consistent with Play Console declarations.
- [ ] Keep the policy consistent with the actual app.

---

# 27. PLAY STORE DATA SAFETY

Before completing Play Console's Data Safety section:

- [ ] Audit app code.
- [ ] Audit third-party SDKs.
- [ ] Audit network calls.
- [ ] Audit stored information.
- [ ] Identify collected data.
- [ ] Identify shared data.
- [ ] Identify data security practices.
- [ ] Ensure declarations match actual behavior.
- [ ] Re-check after every major dependency change.

Do not guess these answers.

---

# 28. ADS

If there are no ads:

- [ ] Ensure no advertising SDK is included.
- [ ] Play Console ad declaration reflects reality.

If ads exist:

- [ ] Identify advertising SDK.
- [ ] Review data collection.
- [ ] Review age/targeting requirements.
- [ ] Review consent requirements where applicable.
- [ ] Test ad placement.
- [ ] Ensure ads do not block essential functionality.
- [ ] Ensure ads are not misleading.

---

# 29. APP CONTENT

- [ ] App name is final.
- [ ] Short description is final.
- [ ] Full description is final.
- [ ] Category is appropriate.
- [ ] Tags/metadata are accurate.
- [ ] No keyword stuffing.
- [ ] No false claims.
- [ ] No fake reviews/testimonials.
- [ ] No misleading screenshots.
- [ ] No misleading functionality.
- [ ] No prohibited content.
- [ ] All included media is owned/licensed.
- [ ] Copyright/trademark issues have been reviewed.

---

# 30. APP STORE LISTING ASSETS

Prepare:

- [ ] App icon.
- [ ] Feature graphic if required.
- [ ] Phone screenshots.
- [ ] Tablet screenshots if applicable.
- [ ] Other device screenshots if applicable.
- [ ] Screenshots represent the real current app.
- [ ] Screenshots contain no private/personal information.
- [ ] Screenshots are readable.
- [ ] Screenshot text is truthful.
- [ ] Promotional graphics are professional.
- [ ] Branding is consistent.
- [ ] No copyrighted material without permission.

---

# 31. CONTACT INFORMATION

- [ ] Developer/contact email is valid.
- [ ] Email is accessible.
- [ ] Support method is decided.
- [ ] Support contact is included where appropriate.
- [ ] Privacy contact is available if required.
- [ ] Do not publish unnecessary personal information.

---

# 32. CONTENT RATING / TARGET AUDIENCE

- [ ] Complete Play Console content rating accurately.
- [ ] Select appropriate target audience.
- [ ] Review whether the app contains ads.
- [ ] Review whether the app contains user-generated content.
- [ ] Review sensitive/regulated features if any.
- [ ] Ensure age-related declarations match the actual app.

---

# 33. PLAY POLICY REVIEW

Before publishing:

- [ ] Review current Google Play Developer policies.
- [ ] Check app against relevant policy areas.
- [ ] Check privacy requirements.
- [ ] Check permissions requirements.
- [ ] Check target API requirements.
- [ ] Check metadata requirements.
- [ ] Check intellectual-property requirements.
- [ ] Check user-data requirements.
- [ ] Check any policy specific to the app category.
- [ ] Resolve policy issues before production release.

> **Important:** Google Play requirements change. Always verify the current requirements in Play Console/official Google Play documentation before the final submission.

---

# 34. INTERNAL TESTING

Create a clean release build and test it as if you were a normal user.

- [ ] Install from scratch.
- [ ] Launch.
- [ ] Complete first-use flow.
- [ ] Use every major feature.
- [ ] Save data.
- [ ] Close app.
- [ ] Reopen app.
- [ ] Verify data.
- [ ] Delete data.
- [ ] Recreate data.
- [ ] Test settings.
- [ ] Test menus.
- [ ] Test About.
- [ ] Test Credits.
- [ ] Test all buttons.
- [ ] Test all navigation.
- [ ] Test error cases.
- [ ] Test Airplane Mode.
- [ ] Test device reboot.

---

# 35. UPDATE TESTING

If a previous version exists:

- [ ] Install old version.
- [ ] Create realistic user data.
- [ ] Install new release over old version.
- [ ] Verify data remains.
- [ ] Verify database migration.
- [ ] Verify settings remain correct.
- [ ] Verify no unexpected reset.
- [ ] Verify all features continue working.
- [ ] Verify version number changes correctly.

---

# 36. FRESH INSTALL TESTING

- [ ] Uninstall old version.
- [ ] Install current release.
- [ ] Launch.
- [ ] Verify first-launch behavior.
- [ ] Verify default settings.
- [ ] Verify no old user data unexpectedly remains.
- [ ] Complete setup.
- [ ] Test all major features.

---

# 37. AIRPLANE MODE TEST

With Airplane Mode ON:

- [ ] App launches.
- [ ] Main screen works.
- [ ] Core features work.
- [ ] Data can be created.
- [ ] Data can be edited.
- [ ] Data can be deleted.
- [ ] Data remains after restart.
- [ ] No unnecessary network error appears.
- [ ] No feature falsely claims synchronization.
- [ ] App remains usable.

---

# 38. DEVICE REBOOT TEST

- [ ] Create data.
- [ ] Reboot device.
- [ ] Open app.
- [ ] Verify data.
- [ ] Verify settings.
- [ ] Verify UI.
- [ ] Verify no startup crash.

---

# 39. BATTERY / BACKGROUND BEHAVIOR

For an offline app:

- [ ] No unnecessary background service.
- [ ] No unnecessary wake locks.
- [ ] No unnecessary alarms.
- [ ] No unnecessary notifications.
- [ ] No continuous polling.
- [ ] No unnecessary background CPU work.
- [ ] App does not noticeably drain battery.
- [ ] Background behavior is documented where necessary.

---

# 40. NOTIFICATIONS

If notifications exist:

- [ ] Notifications are genuinely useful.
- [ ] Notification permission is requested appropriately where required.
- [ ] Notification channels are configured.
- [ ] Notification actions work.
- [ ] Notification text is correct.
- [ ] Notification does not expose sensitive information unnecessarily.
- [ ] User can control notification behavior where appropriate.
- [ ] Notifications do not spam users.

If notifications are not needed:

- [ ] Do not request notification permission unnecessarily.

---

# 41. DATE / TIME / LOCALE

- [ ] Dates display correctly.
- [ ] Times display correctly.
- [ ] Local timezone behavior is intentional.
- [ ] Date calculations are correct.
- [ ] Midnight/day-boundary behavior is tested.
- [ ] Locale-dependent formatting is appropriate.
- [ ] Numeric formatting is appropriate.
- [ ] App does not break when device language changes.

---

# 42. DATA EXPORT / IMPORT

Consider whether the app needs:

- [ ] Export data.
- [ ] Import data.
- [ ] Backup.
- [ ] Restore.
- [ ] Share/export functionality.

If implemented:

- [ ] Export works.
- [ ] Import works.
- [ ] Invalid files are rejected safely.
- [ ] Imported data is validated.
- [ ] Import does not overwrite data unexpectedly.
- [ ] User receives confirmation for destructive imports.
- [ ] Exported files do not unnecessarily expose sensitive information.

---

# 43. ABOUT & CREDITS

If included:

- [ ] About page works.
- [ ] App name is correct.
- [ ] App version is shown.
- [ ] Credits are accurate.
- [ ] Developer/creator attribution is accurate.
- [ ] Third-party licenses/attributions are included where required.
- [ ] Contact/support information is correct.
- [ ] No private information is exposed unnecessarily.

---

# 44. DEBUG CLEANUP

Search the project for:

- [ ] TODO
- [ ] FIXME
- [ ] DEBUG
- [ ] TEST
- [ ] SAMPLE
- [ ] MOCK
- [ ] PLACEHOLDER
- [ ] localhost
- [ ] test URLs
- [ ] test credentials
- [ ] fake data
- [ ] temporary buttons
- [ ] temporary screens
- [ ] developer menus
- [ ] verbose logging

Review every result and decide whether it belongs in production.

---

# 45. LOGGING

- [ ] Remove unnecessary verbose logging.
- [ ] Do not log passwords.
- [ ] Do not log tokens.
- [ ] Do not log private user information unnecessarily.
- [ ] Do not log complete database contents.
- [ ] Do not expose sensitive information through crash logs.
- [ ] Keep only useful production diagnostics.

---

# 46. FINAL CODE QUALITY REVIEW

- [ ] No obvious duplicated logic.
- [ ] No dead code in important areas.
- [ ] No unsafe casts that can easily crash.
- [ ] No ignored errors that matter.
- [ ] No swallowed exceptions hiding serious problems.
- [ ] No infinite loops.
- [ ] No obvious memory leaks.
- [ ] No hard-coded screen dimensions where inappropriate.
- [ ] No hard-coded user-specific data.
- [ ] No secrets in source code.
- [ ] Comments explain complex logic where useful.
- [ ] Code is maintainable for future updates.

---

# 47. FINAL UI WALKTHROUGH

Manually open EVERY screen.

For each screen verify:

- [ ] Correct title.
- [ ] Correct icon.
- [ ] Correct spacing.
- [ ] Correct colors.
- [ ] Correct typography.
- [ ] Correct buttons.
- [ ] Correct navigation.
- [ ] Correct system-bar handling.
- [ ] Correct dark/light behavior.
- [ ] Correct empty state.
- [ ] Correct error state.
- [ ] Correct data.
- [ ] No overlap.
- [ ] No clipping.
- [ ] No visual bugs.

---

# 48. FINAL FUNCTIONAL WALKTHROUGH

Perform the complete user journey:

1. [ ] Install app.
2. [ ] Launch app.
3. [ ] Complete first-use flow.
4. [ ] Use the primary feature.
5. [ ] Create data.
6. [ ] Edit data.
7. [ ] Delete data.
8. [ ] Close app.
9. [ ] Reopen app.
10. [ ] Verify data.
11. [ ] Change settings.
12. [ ] Restart device.
13. [ ] Reopen app.
14. [ ] Verify everything.
15. [ ] Test Airplane Mode.
16. [ ] Test error scenarios.
17. [ ] Test About/Credits.
18. [ ] Test every major screen.

---

# 49. RELEASE SIGNING SAFETY

- [ ] Release keystore exists.
- [ ] Keystore is stored securely.
- [ ] Passwords are not committed to source control.
- [ ] Backup exists in a secure location.
- [ ] Upload key credentials are protected.
- [ ] Only authorized people/agents can access signing credentials.
- [ ] Final AAB is signed correctly.
- [ ] Signing setup is documented privately.

> **NEVER put keystore passwords, API secrets, or private credentials into this public checklist or public repository.**

---

# 50. FINAL BUILD

Before building the final AAB:

- [ ] Clean project/build outputs as appropriate.
- [ ] Rebuild from a clean state.
- [ ] Confirm release configuration.
- [ ] Confirm version name.
- [ ] Confirm version code.
- [ ] Confirm application ID.
- [ ] Confirm signing.
- [ ] Confirm no debug configuration.
- [ ] Confirm no test data.
- [ ] Confirm no test credentials.
- [ ] Confirm no unnecessary permissions.
- [ ] Confirm dependencies.
- [ ] Generate final AAB.
- [ ] Install/test the corresponding release build.
- [ ] Archive the exact release source/build information.

---

# 51. PLAY CONSOLE PREPARATION

Prepare:

- [ ] Developer account.
- [ ] App creation.
- [ ] App name.
- [ ] Default language.
- [ ] App category.
- [ ] Short description.
- [ ] Full description.
- [ ] App icon.
- [ ] Feature graphic if required.
- [ ] Screenshots.
- [ ] Contact email.
- [ ] Privacy policy where required.
- [ ] Data Safety form.
- [ ] Content rating.
- [ ] Target audience.
- [ ] Ads declaration.
- [ ] App access information if applicable.
- [ ] Pricing.
- [ ] Countries/regions.
- [ ] Release artifact.
- [ ] Release notes.

---

# 52. STORE DESCRIPTION QUALITY

The description should clearly explain:

- [ ] What the app does.
- [ ] Who it is for.
- [ ] Main features.
- [ ] Offline functionality if applicable.
- [ ] Privacy/data behavior where useful.
- [ ] Any important limitations.
- [ ] No fake promises.
- [ ] No keyword stuffing.
- [ ] No misleading claims.
- [ ] No excessive repetition.

---

# 53. SCREENSHOT QUALITY

For every screenshot:

- [ ] Shows real app UI.
- [ ] Shows useful functionality.
- [ ] Text is readable.
- [ ] No personal data.
- [ ] No debug information.
- [ ] No unrelated apps.
- [ ] No misleading edits.
- [ ] Consistent visual style.
- [ ] Correct dimensions according to current Play requirements.
- [ ] Important features are represented.

---

# 54. RELEASE TRACK

Use an appropriate Play testing/release track:

- [ ] Internal testing considered.
- [ ] Closed testing considered.
- [ ] Open testing considered if useful/available.
- [ ] Production release planned.
- [ ] Current Play Console testing requirements checked.
- [ ] Required testing completed if applicable.
- [ ] Testers receive the correct build.
- [ ] Feedback is collected.
- [ ] Critical issues are fixed before production.

---

# 55. PRE-SUBMISSION FINAL AUDIT

Every item below must be TRUE:

- [ ] App builds successfully.
- [ ] Release AAB builds successfully.
- [ ] Release app installs.
- [ ] App launches.
- [ ] Core features work.
- [ ] Offline functionality works.
- [ ] Data persists.
- [ ] No critical crashes.
- [ ] No critical ANRs.
- [ ] UI is polished.
- [ ] No overlapping system bars.
- [ ] Charts/graphs stay inside their containers.
- [ ] Navigation works.
- [ ] Back button works.
- [ ] Permissions are minimal.
- [ ] Privacy information is accurate.
- [ ] Data Safety information is accurate.
- [ ] Store description is accurate.
- [ ] Screenshots are accurate.
- [ ] App icon is final.
- [ ] Version information is correct.
- [ ] Signing is correct.
- [ ] No secrets are exposed.
- [ ] No debug/test content remains.
- [ ] Licensing has been checked.
- [ ] Play policies have been checked.
- [ ] Current Play requirements have been checked.

---

# 56. MANUAL HUMAN CHECK

The coding agent must NOT be the only tester.

A human should personally verify:

- [ ] Does the app feel polished?
- [ ] Is anything confusing?
- [ ] Does anything feel slow?
- [ ] Does anything look unfinished?
- [ ] Is any text awkward?
- [ ] Is any button difficult to understand?
- [ ] Is the first launch understandable?
- [ ] Does the app feel consistent?
- [ ] Would a new user know what to do?
- [ ] Does the app behave correctly when used casually rather than according to a scripted test?
- [ ] Is there anything embarrassing or obviously unfinished?

---

# 57. FINAL RELEASE DECISION

Do NOT publish if:

- [ ] Critical crash exists.
- [ ] Data loss bug exists.
- [ ] Security issue exists.
- [ ] Privacy declaration is inaccurate.
- [ ] Play policy issue is unresolved.
- [ ] Release signing is uncertain.
- [ ] Core offline functionality is broken.
- [ ] Important UI is broken.
- [ ] App contains test/debug content.
- [ ] Store listing contains false claims.
- [ ] Required legal/licensing information is missing.

---

# 58. AFTER SUBMISSION

Once submitted:

- [ ] Monitor Play Console.
- [ ] Watch review/status messages.
- [ ] Respond to legitimate policy/review issues.
- [ ] Monitor crashes.
- [ ] Monitor ANRs.
- [ ] Monitor user feedback.
- [ ] Monitor reviews.
- [ ] Monitor uninstall/retention trends where available.
- [ ] Record bugs reported by users.
- [ ] Prioritize critical fixes.
- [ ] Prepare a maintenance/release schedule.

---

# 59. FUTURE UPDATE CHECKLIST

For every update:

- [ ] Read current Play requirements again.
- [ ] Update version code.
- [ ] Update version name when appropriate.
- [ ] Test existing functionality.
- [ ] Test new functionality.
- [ ] Test database migration.
- [ ] Test data preservation.
- [ ] Test offline behavior.
- [ ] Test release build.
- [ ] Update screenshots if UI changed.
- [ ] Update store description if functionality changed.
- [ ] Update privacy/data declarations if data practices changed.
- [ ] Update release notes.
- [ ] Build new AAB.
- [ ] Test AAB/release build.
- [ ] Upload through appropriate Play track.

---

# 60. FINAL AGENT REPORT

After completing the work, produce a report in this format:

## Project Audit

- Project build: PASS/FAIL
- Release build: PASS/FAIL
- AAB generation: PASS/FAIL
- Offline functionality: PASS/FAIL
- Data persistence: PASS/FAIL
- Database: PASS/FAIL
- Security: PASS/FAIL
- Permissions: PASS/FAIL
- UI/UX: PASS/FAIL
- Accessibility: PASS/FAIL
- Performance: PASS/FAIL
- Crash/ANR testing: PASS/FAIL
- Release signing: PASS/FAIL
- Privacy readiness: PASS/FAIL
- Play Store readiness: PASS/FAIL

## Changes Made

List every important change made.

## Issues Remaining

List every unresolved issue.

## Manual Checks Required

List anything that cannot be verified automatically and must be checked by the developer.

## Final Recommendation

Use one of:

- READY FOR FINAL HUMAN REVIEW
- NOT READY — FIX REQUIRED

Do not say "READY FOR PUBLISH" unless all required technical checks have actually passed and the remaining Play Console/legal/manual checks are explicitly identified.

---

# 61. IMPORTANT RULES FOR THIS PROJECT

1. **Do not break existing features while improving the app.**
2. **Do not add internet functionality to an offline app unless explicitly requested.**
3. **Do not add unnecessary permissions.**
4. **Do not expose secrets.**
5. **Do not fake privacy claims.**
6. **Do not fake store screenshots.**
7. **Do not leave debug functionality in production.**
8. **Do not assume Play Store requirements from memory. Verify current requirements before submission.**
9. **Do not mark a test PASS without actually performing it.**
10. **Do not delete user data during an update.**
11. **Do not change the application ID casually.**
12. **Protect the signing credentials.**
13. **Keep the release build reproducible and documented.**
14. **Prefer simple, maintainable solutions over unnecessary complexity.**
15. **Test on a real Android device.**
16. **Test with internet disabled.**
17. **Test fresh installation and upgrade installation.**
18. **Test every important screen manually.**
19. **Review every permission and third-party dependency.**
20. **Before submission, compare the actual app against the Play Store listing and privacy declarations.**

---

# 62. DEFINITION OF "PRODUCTION READY"

The app is considered technically production-ready only when:

- The release build installs successfully.
- The app launches successfully.
- Core functionality works reliably.
- Offline functionality works as intended.
- User data is stored and retrieved reliably.
- Important data is not accidentally lost.
- No known release-blocking crashes/ANRs remain.
- UI is polished and responsive.
- System bars/insets are handled correctly.
- Accessibility has been reviewed.
- Permissions are justified.
- Security has been reviewed.
- Privacy practices are understood and accurately documented.
- Release signing is configured safely.
- AAB has been generated successfully.
- Store assets are ready.
- Play Console information is accurate.
- Current Google Play requirements have been checked.
- Human testing has been completed.
- Remaining limitations are known and documented.

---

# END OF CHECKLIST

## Final principle

**Do not try to make the app "look ready." Make it actually ready.**

A professional release is:

**Stable + Secure + Private + Usable + Accessible + Fast + Tested + Properly documented + Compliant + Maintainable.**
