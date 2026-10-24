# Refactor log

Baseline: original source archive. Files are rewritten only at complete Kotlin function-body boundaries.

## rexpanel/RexPanelScreen.kt

- Before: 51083 bytes; facade after: 6380 bytes; moved complete declarations: 10.
- Pieces: RexPanelScreenContent.kt (12392 bytes, 491 lines), RexPanelScreenContent2.kt (8071 bytes, 289 lines), RexPanelScreenContent3.kt (6150 bytes, 242 lines), RexPanelScreenContent4.kt (5904 bytes, 218 lines), RexPanelScreenContent5.kt (6070 bytes, 253 lines), RexPanelScreenContent6.kt (5574 bytes, 269 lines), RexPanelScreenHeader.kt (4046 bytes, 168 lines), RexPanelScreenNav.kt (3106 bytes, 115 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexpanel/FilesScreen.kt

- Before: 48123 bytes; facade after: 7171 bytes; moved complete declarations: 24.
- Pieces: FilesScreenFiles.kt (11343 bytes, 245 lines), FilesScreenFiles2.kt (4752 bytes, 137 lines), FilesScreenHeader.kt (4541 bytes, 103 lines), FilesScreenPreview.kt (9625 bytes, 240 lines), FilesScreenPreview2.kt (1176 bytes, 33 lines), FilesScreenContent.kt (8900 bytes, 237 lines), FilesScreenContent2.kt (5155 bytes, 133 lines), FilesScreenPlayback.kt (1943 bytes, 57 lines), FilesScreenDialogs.kt (2252 bytes, 67 lines), FilesScreenDownload.kt (1101 bytes, 31 lines), FilesScreenShared.kt (979 bytes, 22 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexpanel/RemoteFiles.kt

- Before: 16625 bytes; facade after: 9377 bytes; moved complete declarations: 14.
- Pieces: RemoteFilesContent.kt (1184 bytes, 30 lines), RemoteFilesShared.kt (6434 bytes, 168 lines), RemoteFilesFiles.kt (930 bytes, 22 lines), RemoteFilesActions.kt (871 bytes, 28 lines), RemoteFilesDownload.kt (508 bytes, 16 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexpanel/RexPanelViewModel.kt

- Before: 21875 bytes; facade after: 9161 bytes; moved complete declarations: 5.
- Pieces: RexPanelViewModelEngine.kt (6030 bytes, 287 lines), RexPanelViewModelShared.kt (6450 bytes, 282 lines), RexPanelViewModelShared2.kt (1331 bytes, 68 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## librex/rexarchive/internal/ArchiveEngine.kt

- Before: 17687 bytes; facade after: 9320 bytes; moved complete declarations: 4.
- Pieces: ArchiveEngineShared.kt (5756 bytes, 93 lines), ArchiveEngineActions.kt (4219 bytes, 74 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexfox/RexFoxHome.kt

- Before: 17857 bytes; facade after: 7045 bytes; moved complete declarations: 2.
- Pieces: RexFoxHomeContent.kt (7074 bytes, 155 lines), RexFoxHomeContent2.kt (6005 bytes, 157 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexmanager/RexManagerScreen.kt

- Before: 51929 bytes; facade after: 6954 bytes; moved complete declarations: 22.
- Pieces: RexManagerScreenContent.kt (9343 bytes, 164 lines), RexManagerScreenContent2.kt (9254 bytes, 197 lines), RexManagerScreenContent3.kt (7343 bytes, 181 lines), RexManagerScreenContent4.kt (9264 bytes, 218 lines), RexManagerScreenContent5.kt (2031 bytes, 50 lines), RexManagerScreenFiles.kt (8087 bytes, 179 lines), RexManagerScreenPreview.kt (4407 bytes, 119 lines), RexManagerScreenShared.kt (4368 bytes, 108 lines), RexManagerScreenHeader.kt (1588 bytes, 43 lines), RexManagerScreenSidebar.kt (594 bytes, 17 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rextools/tiktok/TiktokStalkerScreenPortrait.kt

- Before: 27869 bytes; facade after: 7603 bytes; moved complete declarations: 7.
- Pieces: TiktokStalkerScreenPortraitContent.kt (6497 bytes, 179 lines), TiktokStalkerScreenPortraitContent2.kt (8045 bytes, 181 lines), TiktokStalkerScreenPortraitProfile.kt (5984 bytes, 127 lines), TiktokStalkerScreenPortraitHeader.kt (3847 bytes, 86 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rextools/ui/RexUi.kt

- Before: 41286 bytes; facade after: 10783 bytes; moved complete declarations: 32.
- Pieces: RexUiContent.kt (7453 bytes, 173 lines), RexUiContent2.kt (9641 bytes, 224 lines), RexUiContent3.kt (9178 bytes, 221 lines), RexUiContent4.kt (8883 bytes, 229 lines), RexUiLoading.kt (4643 bytes, 108 lines), RexUiActions.kt (1809 bytes, 45 lines), RexUiProfile.kt (1856 bytes, 53 lines), RexUiAnimations.kt (1124 bytes, 31 lines), RexUiShared.kt (1664 bytes, 45 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rextools/preview/PreviewScreenPortrait.kt

- Before: 26304 bytes; facade after: 6984 bytes; moved complete declarations: 4.
- Pieces: PreviewScreenPortraitContent.kt (8533 bytes, 195 lines), PreviewScreenPortraitContent2.kt (9223 bytes, 224 lines), PreviewScreenPortraitPreview.kt (5041 bytes, 116 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexnux/RexNuxInstaller.kt

- Before: 15993 bytes; facade after: 7511 bytes; moved complete declarations: 2.
- Pieces: RexNuxInstallerShared.kt (5974 bytes, 199 lines), RexNuxInstallerDownload.kt (3130 bytes, 123 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexnux/RexNuxScreen.kt

- Before: 23567 bytes; facade after: 9222 bytes; moved complete declarations: 5.
- Pieces: RexNuxScreenSettings.kt (6062 bytes, 173 lines), RexNuxScreenDownload.kt (7316 bytes, 239 lines), RexNuxScreenContent.kt (4756 bytes, 166 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexcoder/model/RexModels.kt

- Before: 17973 bytes; facade after: 10222 bytes; moved complete declarations: 33.
- Pieces: RexModelsShared.kt (8998 bytes, 217 lines), RexModelsActions.kt (2755 bytes, 72 lines), RexModelsFiles.kt (1357 bytes, 26 lines), RexModelsNav.kt (470 bytes, 13 lines), RexModelsContent.kt (294 bytes, 9 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexcoder/ui/Panels.kt

- Before: 16573 bytes; facade after: 8700 bytes; moved complete declarations: 2.
- Pieces: PanelsContent.kt (9121 bytes, 181 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexcoder/ui/RexCoderApp.kt

- Before: 15033 bytes; facade after: 8859 bytes; moved complete declarations: 2.
- Pieces: RexCoderAppContent.kt (7035 bytes, 147 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexcoder/ui/EditorArea.kt

- Before: 22077 bytes; facade after: 7472 bytes; moved complete declarations: 8.
- Pieces: EditorAreaContent.kt (8502 bytes, 188 lines), EditorAreaContent2.kt (8227 bytes, 183 lines), EditorAreaNav.kt (2514 bytes, 59 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexgit/RexGitComponents.kt

- Before: 106222 bytes; facade after: 13500 bytes; moved complete declarations: 50.
- Pieces: RexGitComponentsDialogs.kt (9524 bytes, 220 lines), RexGitComponentsDialogs2.kt (9518 bytes, 251 lines), RexGitComponentsDialogs3.kt (6820 bytes, 172 lines), RexGitComponentsDialogs4.kt (6900 bytes, 198 lines), RexGitComponentsDialogs5.kt (8490 bytes, 239 lines), RexGitComponentsDialogs6.kt (6490 bytes, 202 lines), RexGitComponentsRepository.kt (6538 bytes, 176 lines), RexGitComponentsRepository2.kt (7639 bytes, 206 lines), RexGitComponentsRepository3.kt (6459 bytes, 196 lines), RexGitComponentsProfile.kt (9548 bytes, 269 lines), RexGitComponentsContent.kt (8969 bytes, 251 lines), RexGitComponentsContent2.kt (8765 bytes, 282 lines), RexGitComponentsContent3.kt (3271 bytes, 125 lines), RexGitComponentsAnimations.kt (3307 bytes, 102 lines), RexGitComponentsHeader.kt (5708 bytes, 189 lines), RexGitComponentsActions.kt (3005 bytes, 90 lines), RexGitComponentsFiles.kt (5010 bytes, 143 lines), RexGitComponentsNav.kt (2711 bytes, 83 lines), RexGitComponentsLoading.kt (2643 bytes, 92 lines), RexGitComponentsShared.kt (891 bytes, 32 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexgit/RexGitScreenShared.kt

- Before: 20407 bytes; facade after: 2384 bytes; moved complete declarations: 3.
- Pieces: RexGitScreenSharedDialogs.kt (7933 bytes, 239 lines), RexGitScreenSharedDialogs2.kt (2365 bytes, 61 lines), RexGitScreenSharedContent.kt (428 bytes, 18 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexgit/RexGitEditor.kt

- Before: 31739 bytes; facade after: 6354 bytes; moved complete declarations: 17.
- Pieces: RexGitEditorContent.kt (8967 bytes, 262 lines), RexGitEditorContent2.kt (1671 bytes, 52 lines), RexGitEditorDialogs.kt (3608 bytes, 80 lines), RexGitEditorRepository.kt (2852 bytes, 76 lines), RexGitEditorHeader.kt (3225 bytes, 101 lines), RexGitEditorActions.kt (1749 bytes, 46 lines), RexGitEditorShared.kt (3145 bytes, 87 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexgit/RexGitScreenLandscape.kt

- Before: 32202 bytes; facade after: 3769 bytes; moved complete declarations: 8.
- Pieces: RexGitScreenLandscapeRepository.kt (8551 bytes, 232 lines), RexGitScreenLandscapeContent.kt (4457 bytes, 144 lines), RexGitScreenLandscapeDialogs.kt (3919 bytes, 124 lines), RexGitScreenLandscapeNav.kt (6568 bytes, 172 lines), RexGitScreenLandscapeProfile.kt (2341 bytes, 77 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexgit/RexGitScreenPotrait.kt

- Before: 27319 bytes; facade after: 2927 bytes; moved complete declarations: 6.
- Pieces: RexGitScreenPotraitNav.kt (8734 bytes, 243 lines), RexGitScreenPotraitRepository.kt (7188 bytes, 210 lines), RexGitScreenPotraitContent.kt (2479 bytes, 90 lines), RexGitScreenPotraitDialogs.kt (2104 bytes, 71 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexgit/RexGitRepositoryHub.kt

- Before: 34971 bytes; facade after: 8905 bytes; moved complete declarations: 3.
- Pieces: RexGitRepositoryHubRepository.kt (12149 bytes, 175 lines), RexGitRepositoryHubRepository2.kt (9507 bytes, 130 lines), RexGitRepositoryHubPreview.kt (8529 bytes, 153 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexgit/RexGitViewModel.kt

- Before: 47834 bytes; facade after: 9747 bytes; moved complete declarations: 34.
- Pieces: RexGitViewModelShared.kt (8213 bytes, 250 lines), RexGitViewModelRepository.kt (9357 bytes, 222 lines), RexGitViewModelRepository2.kt (8587 bytes, 278 lines), RexGitViewModelProfile.kt (7438 bytes, 217 lines), RexGitViewModelDownload.kt (2421 bytes, 53 lines), RexGitViewModelActions.kt (4419 bytes, 154 lines), RexGitViewModelFiles.kt (3942 bytes, 150 lines), RexGitViewModelEngine.kt (508 bytes, 19 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexwarp/RexWarpVpnService.kt

- Before: 15706 bytes; facade after: 9300 bytes; moved complete declarations: 7.
- Pieces: RexWarpVpnServiceShared.kt (5102 bytes, 100 lines), RexWarpVpnServiceEngine.kt (1099 bytes, 25 lines), RexWarpVpnServiceActions.kt (1147 bytes, 26 lines), RexWarpVpnServiceLoading.kt (939 bytes, 27 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexwarp/ui/RexWarpSpeedTestScreenPotriat.kt

- Before: 21856 bytes; facade after: 6591 bytes; moved complete declarations: 5.
- Pieces: RexWarpSpeedTestScreenPotriatContent.kt (9288 bytes, 115 lines), RexWarpSpeedTestScreenPotriatContent2.kt (8738 bytes, 130 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexwarp/ui/RexWarpSpeedTestScreenLandscape.kt

- Before: 22294 bytes; facade after: 6593 bytes; moved complete declarations: 5.
- Pieces: RexWarpSpeedTestScreenLandscapeContent.kt (9288 bytes, 115 lines), RexWarpSpeedTestScreenLandscapeContent2.kt (9176 bytes, 140 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexwarp/ui/components/RexWarpSpeedTestHistorySection.kt

- Before: 18334 bytes; facade after: 5606 bytes; moved complete declarations: 1.
- Pieces: RexWarpSpeedTestHistorySectionProfile.kt (13384 bytes, 227 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexwarp/ui/components/RexWarpGraph.kt

- Before: 15148 bytes; facade after: 2544 bytes; moved complete declarations: 1.
- Pieces: RexWarpGraphContent.kt (13398 bytes, 248 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexwarp/ui/components/RexWarpDiagnosticsPanel.kt

- Before: 17217 bytes; facade after: 8496 bytes; moved complete declarations: 1.
- Pieces: RexWarpDiagnosticsPanelContent.kt (9492 bytes, 174 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexspacex/RexSpaceXScreen.kt

- Before: 25084 bytes; facade after: 7986 bytes; moved complete declarations: 1.
- Pieces: RexSpaceXScreenContent.kt (17599 bytes, 363 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## ui/RexLocalAccountScreen.kt

- Before: 82437 bytes; facade after: 13506 bytes; moved complete declarations: 63.
- Pieces: RexLocalAccountScreenDialogs.kt (9906 bytes, 201 lines), RexLocalAccountScreenDialogs2.kt (7917 bytes, 189 lines), RexLocalAccountScreenDialogs3.kt (4718 bytes, 134 lines), RexLocalAccountScreenProfile.kt (9150 bytes, 214 lines), RexLocalAccountScreenProfile2.kt (9683 bytes, 240 lines), RexLocalAccountScreenProfile3.kt (9439 bytes, 226 lines), RexLocalAccountScreenProfile4.kt (2111 bytes, 61 lines), RexLocalAccountScreenContent.kt (9135 bytes, 208 lines), RexLocalAccountScreenContent2.kt (9388 bytes, 247 lines), RexLocalAccountScreenNav.kt (4397 bytes, 105 lines), RexLocalAccountScreenShared.kt (9367 bytes, 230 lines), RexLocalAccountScreenShared2.kt (3077 bytes, 79 lines), RexLocalAccountScreenFiles.kt (773 bytes, 21 lines), RexLocalAccountScreenActions.kt (662 bytes, 22 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## ui/RexApsApp.kt

- Before: 48000 bytes; facade after: 7046 bytes; moved complete declarations: 19.
- Pieces: RexApsAppDialogs.kt (8576 bytes, 217 lines), RexApsAppDialogs2.kt (2654 bytes, 68 lines), RexApsAppContent.kt (5844 bytes, 154 lines), RexApsAppContent2.kt (9181 bytes, 254 lines), RexApsAppContent3.kt (9193 bytes, 232 lines), RexApsAppContent4.kt (6747 bytes, 217 lines), RexApsAppNav.kt (3070 bytes, 73 lines), RexApsAppHeader.kt (3602 bytes, 102 lines), RexApsAppProfile.kt (2782 bytes, 83 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## ui/RexTheme.kt

- Before: 16731 bytes; facade after: 12420 bytes; moved complete declarations: 6.
- Pieces: RexThemeShared.kt (4564 bytes, 75 lines), RexThemeActions.kt (256 bytes, 7 lines), RexThemeContent.kt (403 bytes, 11 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## ui/RexAppsScreenPortrait.kt

- Before: 30953 bytes; facade after: 9256 bytes; moved complete declarations: 11.
- Pieces: RexAppsScreenPortraitContent.kt (8984 bytes, 214 lines), RexAppsScreenPortraitContent2.kt (8785 bytes, 245 lines), RexAppsScreenPortraitContent3.kt (2896 bytes, 80 lines), RexAppsScreenPortraitProfile.kt (3086 bytes, 86 lines), RexAppsScreenPortraitHeader.kt (3406 bytes, 96 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## ui/settings/RexThemeSettings.kt

- Before: 22214 bytes; facade after: 981 bytes; moved complete declarations: 1.
- Pieces: RexThemeSettingsSettings.kt (21677 bytes, 348 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexmusic/RexMusicScreenPortrait.kt

- Before: 23129 bytes; facade after: 7102 bytes; moved complete declarations: 5.
- Pieces: RexMusicScreenPortraitContent.kt (6312 bytes, 146 lines), RexMusicScreenPortraitContent2.kt (9336 bytes, 247 lines), RexMusicScreenPortraitNav.kt (3336 bytes, 89 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexmusic/RexMusicSync.kt

- Before: 85539 bytes; facade after: 10752 bytes; moved complete declarations: 50.
- Pieces: RexMusicSyncContent.kt (16797 bytes, 357 lines), RexMusicSyncContent2.kt (9537 bytes, 233 lines), RexMusicSyncContent3.kt (9640 bytes, 232 lines), RexMusicSyncShared.kt (5984 bytes, 86 lines), RexMusicSyncShared2.kt (7209 bytes, 113 lines), RexMusicSyncShared3.kt (9403 bytes, 148 lines), RexMusicSyncShared4.kt (3198 bytes, 57 lines), RexMusicSyncPlayback.kt (8454 bytes, 168 lines), RexMusicSyncPlayback2.kt (7246 bytes, 135 lines), RexMusicSyncQueue.kt (4528 bytes, 81 lines), RexMusicSyncActions.kt (1578 bytes, 35 lines), RexMusicSyncEngine.kt (1288 bytes, 23 lines), RexMusicSyncLoading.kt (1517 bytes, 40 lines), RexMusicSyncHeader.kt (1032 bytes, 31 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexmusic/RexMusicScreenLandscape.kt

- Before: 32852 bytes; facade after: 6742 bytes; moved complete declarations: 9.
- Pieces: RexMusicScreenLandscapeContent.kt (6088 bytes, 140 lines), RexMusicScreenLandscapeContent2.kt (9526 bytes, 227 lines), RexMusicScreenLandscapeContent3.kt (7217 bytes, 197 lines), RexMusicScreenLandscapeNav.kt (6379 bytes, 170 lines), RexMusicScreenLandscapePlayback.kt (2976 bytes, 80 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexmusic/RexPlayerController.kt

- Before: 41341 bytes; facade after: 17451 bytes; moved complete declarations: 66.
- Pieces: RexPlayerControllerPlayback.kt (9667 bytes, 223 lines), RexPlayerControllerShared.kt (9280 bytes, 242 lines), RexPlayerControllerShared2.kt (6428 bytes, 171 lines), RexPlayerControllerLyrics.kt (1758 bytes, 49 lines), RexPlayerControllerEngine.kt (1316 bytes, 38 lines), RexPlayerControllerQueue.kt (5210 bytes, 141 lines), RexPlayerControllerLoading.kt (992 bytes, 29 lines), RexPlayerControllerNetwork.kt (874 bytes, 23 lines), RexPlayerControllerProfile.kt (903 bytes, 25 lines), RexPlayerControllerActions.kt (516 bytes, 17 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexmusic/RexMusicScreen.kt

- Before: 40219 bytes; facade after: 6882 bytes; moved complete declarations: 2.
- Pieces: RexMusicScreenContent.kt (28036 bytes, 873 lines), RexMusicScreenPlayback.kt (7428 bytes, 192 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexmusic/RexOfflineManager.kt

- Before: 22037 bytes; facade after: 9640 bytes; moved complete declarations: 9.
- Pieces: RexOfflineManagerDownload.kt (8633 bytes, 202 lines), RexOfflineManagerShared.kt (2400 bytes, 55 lines), RexOfflineManagerLyrics.kt (2136 bytes, 53 lines), RexOfflineManagerPlayback.kt (1088 bytes, 24 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexmusic/RexMusicComponents.kt

- Before: 28043 bytes; facade after: 8240 bytes; moved complete declarations: 21.
- Pieces: RexMusicComponentsDownload.kt (3110 bytes, 74 lines), RexMusicComponentsPlayback.kt (2703 bytes, 76 lines), RexMusicComponentsContent.kt (8990 bytes, 215 lines), RexMusicComponentsContent2.kt (7093 bytes, 191 lines), RexMusicComponentsQueue.kt (2335 bytes, 65 lines), RexMusicComponentsLoading.kt (3374 bytes, 88 lines), RexMusicComponentsHeader.kt (929 bytes, 27 lines), RexMusicComponentsShared.kt (1141 bytes, 34 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexmusic/player/MusicPlayerCard.kt

- Before: 37036 bytes; facade after: 7391 bytes; moved complete declarations: 20.
- Pieces: MusicPlayerCardPlayback.kt (5266 bytes, 125 lines), MusicPlayerCardPlayback2.kt (8179 bytes, 193 lines), MusicPlayerCardPlayback3.kt (8403 bytes, 206 lines), MusicPlayerCardHeader.kt (3202 bytes, 105 lines), MusicPlayerCardContent.kt (9293 bytes, 223 lines), MusicPlayerCardContent2.kt (2491 bytes, 67 lines), MusicPlayerCardActions.kt (2782 bytes, 72 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexmusic/RexLyricsOverlay.kt

- Before: 18779 bytes; facade after: 8999 bytes; moved complete declarations: 4.
- Pieces: RexLyricsOverlayLyrics.kt (7259 bytes, 192 lines), RexLyricsOverlayContent.kt (3189 bytes, 90 lines), RexLyricsOverlayFooter.kt (2508 bytes, 65 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexmusic/widget/RexMusicWidget.kt

- Before: 19949 bytes; facade after: 9415 bytes; moved complete declarations: 11.
- Pieces: RexMusicWidgetShared.kt (9506 bytes, 210 lines), RexMusicWidgetShared2.kt (1838 bytes, 39 lines), RexMusicWidgetLyrics.kt (896 bytes, 20 lines), RexMusicWidgetActions.kt (1051 bytes, 27 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexmusic/RexMusicTabs.kt

- Before: 49622 bytes; facade after: 7765 bytes; moved complete declarations: 27.
- Pieces: RexMusicTabsNav.kt (10088 bytes, 217 lines), RexMusicTabsNav2.kt (4679 bytes, 141 lines), RexMusicTabsQueue.kt (5405 bytes, 119 lines), RexMusicTabsQueue2.kt (8489 bytes, 190 lines), RexMusicTabsQueue3.kt (2720 bytes, 67 lines), RexMusicTabsContent.kt (8101 bytes, 204 lines), RexMusicTabsContent2.kt (8346 bytes, 220 lines), RexMusicTabsHeader.kt (1512 bytes, 38 lines), RexMusicTabsShared.kt (2752 bytes, 70 lines), RexMusicTabsProfile.kt (1103 bytes, 32 lines), RexMusicTabsPlayback.kt (1923 bytes, 59 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexchat/WaController.kt

- Before: 15636 bytes; facade after: 7914 bytes; moved complete declarations: 1.
- Pieces: WaControllerActions.kt (7782 bytes, 205 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.

## rexchat/MainActivity.kt

- Before: 15318 bytes; facade after: 6126 bytes; moved complete declarations: 1.
- Pieces: MainActivityContent.kt (9759 bytes, 280 lines).
- Facade still contains original signatures and delegates each moved body to an internal implementation.


## Summary

- Original large files examined: 48
- Whole function/method bodies moved: 644
- Files with no safe complete-function extraction: 0

## Syntax parser check

- Kotlin files parsed: 595
- PSI syntax errors: 0
