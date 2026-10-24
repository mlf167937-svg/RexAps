# RexAps Kotlin refactor — validation report

## Cakupan dan hasil statis

- File Kotlin sumber di atas 15.000 byte sebelum perubahan: **48**.
- File Kotlin pada hasil: **595**.
- Deklarasi dipindahkan pada batas fungsi/metode utuh memakai Kotlin PSI; tidak ada pemotongan body berdasarkan rentang baris.
- Kotlin PSI syntax parse pada hasil terbaru: **0 error**.
- **Build Android belum terverifikasi.** Arsip sumber tidak menyertakan `gradlew`, `settings.gradle(.kts)`, atau `build.gradle(.kts)` modul; `./gradlew assembleDebug` tidak dapat dijalankan dari input ini.
- `build.log` yang diberikan berasal dari artefak refactor sebelumnya yang gagal compile; log tersebut bukan hasil build dari ZIP ini.

## Hasil per file asal

Ukuran dihitung dalam byte (ambang 15.000/10.000 byte desimal). Daftar deklarasi di bawah diambil dari isi file pecahan saat ini.

### `librex/rexarchive/internal/ArchiveEngine.kt`
- Sebelum: **17,687 byte**; sesudah/facade: **9,320 byte**; folder pecahan: `librex/rexarchive/internal/ArchiveEngine/`.
  - `ArchiveEngineActions.kt` — 4,219 byte, 73 baris; memuat: `ArchiveEngine.__splitImpl_createTar`, `ArchiveEngine.__splitImpl_createZip`.
  - `ArchiveEngineShared.kt` — 5,756 byte, 92 baris; memuat: `ArchiveEngine.__splitImpl_extractTar`, `ArchiveEngine.__splitImpl_extractZip`.

### `rexchat/MainActivity.kt`
- Sebelum: **15,318 byte**; sesudah/facade: **6,126 byte**; folder pecahan: `rexchat/MainActivity/`.
  - `MainActivityContent.kt` — 9,759 byte, 279 baris; memuat: `__splitImpl_SessionListScreen`.

### `rexchat/WaController.kt`
- Sebelum: **15,636 byte**; sesudah/facade: **7,914 byte**; folder pecahan: `rexchat/WaController/`.
  - `WaControllerActions.kt` — 7,782 byte, 204 baris; memuat: implementation internal.

### `rexcoder/model/RexModels.kt`
- Sebelum: **17,973 byte**; sesudah/facade: **10,222 byte**; folder pecahan: `rexcoder/model/RexModels/`.
  - `RexModelsActions.kt` — 2,755 byte, 71 baris; memuat: `RexCoderState.__splitImpl_submitTerminal`, `RexCoderState.__splitImpl_refreshWorkspace`, `RexCoderState.__splitImpl_saveDoc`, `RexCoderState.__splitImpl_removeGroup`, `RexCoderState.__splitImpl_deleteNode`.
  - `RexModelsContent.kt` — 294 byte, 8 baris; memuat: `__splitImpl_rememberRexCoderState`.
  - `RexModelsFiles.kt` — 1,357 byte, 25 baris; memuat: `RexCoderState.__splitImpl_openFile`, `RexCoderState.__splitImpl_createFolder`, `RexCoderState.__splitImpl_createFile`, `FileNode.__splitImpl_flatFiles`.
  - `RexModelsNav.kt` — 470 byte, 12 baris; memuat: `RexCoderState.__splitImpl_closeTab`.
  - `RexModelsShared.kt` — 8,998 byte, 216 baris; memuat: `RexCoderState.__splitImpl_connectSsh`, `RexCoderState.__splitImpl_openAt`, `RexCoderState.__splitImpl_search`, `RexCoderState.__splitImpl_renameNode`, `RexCoderState.__splitImpl_insertAtCursor`, `RexCoderState.__splitImpl_disconnectSsh`, `RexCoderState.__splitImpl_openSshLogin`, `RexCoderState.__splitImpl_setFontSize`, `RexCoderState.__splitImpl_setTerminalFont`, `RexCoderState.__splitImpl_trimTerminal`, `RexCoderState.__splitImpl_splitEditor`, `RexCoderState.__splitImpl_setTreeFont`, … (16 deklarasi).

### `rexcoder/ui/EditorArea.kt`
- Sebelum: **22,077 byte**; sesudah/facade: **7,472 byte**; folder pecahan: `rexcoder/ui/EditorArea/`.
  - `EditorAreaContent.kt` — 8,502 byte, 187 baris; memuat: `__splitImpl_CodeEditor`, `__splitImpl_ResizeHandle`.
  - `EditorAreaContent2.kt` — 8,227 byte, 182 baris; memuat: `__splitImpl_FontStepperContent`, `__splitImpl_EditorArea`, `__splitImpl_FontSizeChip`, `__splitImpl_EditorGroupView`, `__splitImpl_Welcome`.
  - `EditorAreaNav.kt` — 2,514 byte, 58 baris; memuat: `__splitImpl_Tab`.

### `rexcoder/ui/Panels.kt`
- Sebelum: **16,573 byte**; sesudah/facade: **8,700 byte**; folder pecahan: `rexcoder/ui/Panels/`.
  - `PanelsContent.kt` — 9,121 byte, 180 baris; memuat: `__splitImpl_TerminalView`, `__splitImpl_BottomPanel`.

### `rexcoder/ui/RexCoderApp.kt`
- Sebelum: **15,033 byte**; sesudah/facade: **8,859 byte**; folder pecahan: `rexcoder/ui/RexCoderApp/`.
  - `RexCoderAppContent.kt` — 7,035 byte, 146 baris; memuat: `__splitImpl_CommandPalette`, `__splitImpl_TitleBar`.

### `rexfox/RexFoxHome.kt`
- Sebelum: **17,857 byte**; sesudah/facade: **7,045 byte**; folder pecahan: `rexfox/RexFoxHome/`.
  - `RexFoxHomeContent.kt` — 7,074 byte, 154 baris; memuat: `__splitImpl_RexFoxHomeLandscape`.
  - `RexFoxHomeContent2.kt` — 6,005 byte, 156 baris; memuat: `__splitImpl_RexFoxHome`.

### `rexgit/RexGitComponents.kt`
- Sebelum: **106,222 byte**; sesudah/facade: **13,500 byte**; folder pecahan: `rexgit/RexGitComponents/`.
  - `RexGitComponentsActions.kt` — 3,005 byte, 89 baris; memuat: `__splitImpl_ActionBar`.
  - `RexGitComponentsAnimations.kt` — 3,307 byte, 101 baris; memuat: `__splitImpl_RexPushSuccessAnimation`.
  - `RexGitComponentsContent.kt` — 8,969 byte, 250 baris; memuat: `__splitImpl_ChangesPanel`, `__splitImpl_SearchField`, `__splitImpl_RexPushLoader`.
  - `RexGitComponentsContent2.kt` — 8,765 byte, 281 baris; memuat: `__splitImpl_WorkspaceCard`, `__splitImpl_StorageNotice`, `__splitImpl_GradientButton`, `__splitImpl_ChangeBadge`, `__splitImpl_InfoPill`, `__splitImpl_GuideCode`.
  - `RexGitComponentsContent3.kt` — 3,271 byte, 124 baris; memuat: `__splitImpl_Crumb`, `__splitImpl_IconTile`, `__splitImpl_ProgressSlot`, `__splitImpl_GuideHeading`, `__splitImpl_TappablePill`, `__splitImpl_GuideText`, `__splitImpl_ButtonSpinner`.
  - `RexGitComponentsDialogs.kt` — 9,524 byte, 219 baris; memuat: `__splitImpl_PushDialog`.
  - `RexGitComponentsDialogs2.kt` — 9,518 byte, 250 baris; memuat: `__splitImpl_GitDevDialog`.
  - `RexGitComponentsDialogs3.kt` — 6,820 byte, 171 baris; memuat: `__splitImpl_RexPushProgressDialog`.
  - `RexGitComponentsDialogs4.kt` — 6,900 byte, 197 baris; memuat: `__splitImpl_GithubDialog`.
  - `RexGitComponentsDialogs5.kt` — 8,490 byte, 238 baris; memuat: `__splitImpl_NameDialog`, `__splitImpl_ItemActionsSheet`, `__splitImpl_RexGitDialogFrame`.
  - `RexGitComponentsDialogs6.kt` — 6,490 byte, 201 baris; memuat: `__splitImpl_CreateSheet`, `__splitImpl_DeleteDialog`, `__splitImpl_SheetAction`, `__splitImpl_ErrorDialog`.
  - `RexGitComponentsFiles.kt` — 5,010 byte, 142 baris; memuat: `__splitImpl_FileRow`, `__splitImpl_fileStyle`, `__splitImpl_openAllFilesAccess`.
  - `RexGitComponentsHeader.kt` — 5,708 byte, 188 baris; memuat: `__splitImpl_HomeHeader`, `__splitImpl_RepositoryHeader`, `__splitImpl_HeaderIconButton`.
  - `RexGitComponentsLoading.kt` — 2,643 byte, 91 baris; memuat: `__splitImpl_EmptyState`.
  - `RexGitComponentsNav.kt` — 2,711 byte, 82 baris; memuat: `__splitImpl_SegmentedTabs`.
  - `RexGitComponentsProfile.kt` — 9,548 byte, 268 baris; memuat: `__splitImpl_AccountForm`, `__splitImpl_AccountRow`, `__splitImpl_AccountSwitcher`, `__splitImpl_accountSuffix`.
  - `RexGitComponentsRepository.kt` — 6,538 byte, 175 baris; memuat: `__splitImpl_RepositoryFiles`.
  - `RexGitComponentsRepository2.kt` — 7,639 byte, 205 baris; memuat: `__splitImpl_GithubRepositoryCard`, `__splitImpl_GitDevGuide`.
  - `RexGitComponentsRepository3.kt` — 6,459 byte, 195 baris; memuat: `__splitImpl_RepositoryCard`, `__splitImpl_RepositoryStatusPills`, `__splitImpl_RepositoryToolPills`, `__splitImpl_RexGitLogo`.
  - `RexGitComponentsShared.kt` — 891 byte, 31 baris; memuat: `__splitImpl_locationLabel`, `__splitImpl_formatSize`.

### `rexgit/RexGitEditor.kt`
- Sebelum: **31,739 byte**; sesudah/facade: **6,354 byte**; folder pecahan: `rexgit/RexGitEditor/`.
  - `RexGitEditorActions.kt` — 1,749 byte, 45 baris; memuat: `__splitImpl_rememberEditorActions`.
  - `RexGitEditorContent.kt` — 8,967 byte, 261 baris; memuat: `__splitImpl_EditorTextArea`, `__splitImpl_EditorFontControls`, `__splitImpl_EditorKeyChip`.
  - `RexGitEditorContent2.kt` — 1,671 byte, 51 baris; memuat: `__splitImpl_EditorToolButton`, `__splitImpl_EditorStatusText`.
  - `RexGitEditorDialogs.kt` — 3,608 byte, 79 baris; memuat: `__splitImpl_EditorDialogs`.
  - `RexGitEditorHeader.kt` — 3,225 byte, 100 baris; memuat: `__splitImpl_EditorHeader`.
  - `RexGitEditorRepository.kt` — 2,852 byte, 75 baris; memuat: `__splitImpl_rememberRexGitEditorState`.
  - `RexGitEditorShared.kt` — 3,145 byte, 86 baris; memuat: `RexGitEditorState.__splitImpl_goToLine`, `RexGitEditorState.__splitImpl_selectSearchMatch`, `RexGitEditorState.__splitImpl_searchMatches`, `RexGitEditorState.__splitImpl_onValueChange`, `RexGitEditorState.__splitImpl_insert`, `RexGitEditorState.__splitImpl_currentLine`, `RexGitEditorState.__splitImpl_selectAll`, `RexGitEditorState.__splitImpl_setDraftPx`.

### `rexgit/RexGitRepositoryHub.kt`
- Sebelum: **34,971 byte**; sesudah/facade: **8,905 byte**; folder pecahan: `rexgit/RexGitRepositoryHub/`.
  - `RexGitRepositoryHubPreview.kt` — 8,529 byte, 152 baris; memuat: `__splitImpl_RexGitReadmePreview`.
  - `RexGitRepositoryHubRepository.kt` — 12,149 byte, 174 baris; memuat: `__splitImpl_RexGitActionsPanel`.
  - `RexGitRepositoryHubRepository2.kt` — 9,507 byte, 129 baris; memuat: `__splitImpl_RexGitReleasePanel`.

### `rexgit/RexGitScreenLandscape.kt`
- Sebelum: **32,202 byte**; sesudah/facade: **3,769 byte**; folder pecahan: `rexgit/RexGitScreenLandscape/`.
  - `RexGitScreenLandscapeContent.kt` — 4,457 byte, 143 baris; memuat: `__splitImpl_EditorScreenLandscape`, `Modifier.__splitImpl_landscapeSideInsets`.
  - `RexGitScreenLandscapeDialogs.kt` — 3,919 byte, 123 baris; memuat: `__splitImpl_HomeScreenLandscape`.
  - `RexGitScreenLandscapeNav.kt` — 6,568 byte, 171 baris; memuat: `__splitImpl_GithubTabLandscape`, `__splitImpl_LocalTabLandscape`.
  - `RexGitScreenLandscapeProfile.kt` — 2,341 byte, 76 baris; memuat: `__splitImpl_GithubProfileRow`.
  - `RexGitScreenLandscapeRepository.kt` — 8,551 byte, 231 baris; memuat: `__splitImpl_RepositoryScreenLandscape`, `__splitImpl_RexGitScreenLandscape`.

### `rexgit/RexGitScreenPotrait.kt`
- Sebelum: **27,319 byte**; sesudah/facade: **2,927 byte**; folder pecahan: `rexgit/RexGitScreenPotrait/`.
  - `RexGitScreenPotraitContent.kt` — 2,479 byte, 89 baris; memuat: `__splitImpl_EditorScreenPortrait`.
  - `RexGitScreenPotraitDialogs.kt` — 2,104 byte, 70 baris; memuat: `__splitImpl_HomeScreenPortrait`.
  - `RexGitScreenPotraitNav.kt` — 8,734 byte, 242 baris; memuat: `__splitImpl_GithubTab`, `__splitImpl_LocalTab`.
  - `RexGitScreenPotraitRepository.kt` — 7,188 byte, 209 baris; memuat: `__splitImpl_RepositoryScreenPortrait`, `__splitImpl_RexGitScreenPortrait`.

### `rexgit/RexGitScreenShared.kt`
- Sebelum: **20,407 byte**; sesudah/facade: **2,384 byte**; folder pecahan: `rexgit/RexGitScreenShared/`.
  - `RexGitScreenSharedContent.kt` — 428 byte, 17 baris; memuat: `__splitImpl_screenIndex`.
  - `RexGitScreenSharedDialogs.kt` — 7,933 byte, 238 baris; memuat: `__splitImpl_RexGitScreenController`.
  - `RexGitScreenSharedDialogs2.kt` — 2,365 byte, 60 baris; memuat: `__splitImpl_rememberRexGitScreenHost`.

### `rexgit/RexGitViewModel.kt`
- Sebelum: **47,834 byte**; sesudah/facade: **9,747 byte**; folder pecahan: `rexgit/RexGitViewModel/`.
  - `RexGitViewModelActions.kt` — 4,419 byte, 153 baris; memuat: `RexGitViewModel.__splitImpl_refresh`, `RexGitViewModel.__splitImpl_saveEditor`, `RexGitViewModel.__splitImpl_deleteEntry`, `RexGitViewModel.__splitImpl_refreshStatus`.
  - `RexGitViewModelDownload.kt` — 2,421 byte, 52 baris; memuat: `RexGitViewModel.__splitImpl_downloadGithubAsset`.
  - `RexGitViewModelEngine.kt` — 508 byte, 18 baris; memuat: `RexGitViewModel.__splitImpl_clearPushState`.
  - `RexGitViewModelFiles.kt` — 3,942 byte, 149 baris; memuat: `RexGitViewModel.__splitImpl_createFile`, `RexGitViewModel.__splitImpl_createFolder`, `RexGitViewModel.__splitImpl_openFile`, `RexGitViewModel.__splitImpl_openDirectory`.
  - `RexGitViewModelProfile.kt` — 7,438 byte, 216 baris; memuat: `RexGitViewModel.__splitImpl_publishAccounts`.
  - `RexGitViewModelRepository.kt` — 9,357 byte, 221 baris; memuat: `RexGitViewModel.__splitImpl_selectRepository`, `RexGitViewModel.__splitImpl_loadRepositoryGithubData`, `RexGitViewModel.__splitImpl_cloneRepository`, `RexGitViewModel.__splitImpl_searchGithubRepositories`.
  - `RexGitViewModelRepository2.kt` — 8,587 byte, 277 baris; memuat: `RexGitViewModel.__splitImpl_reloadRepository`, `RexGitViewModel.__splitImpl_backToRepositories`, `RexGitViewModel.__splitImpl_reloadGithub`, `RexGitViewModel.__splitImpl_fetchGithubRepositories`, `RexGitViewModel.__splitImpl_disconnectGithub`, `RexGitViewModel.__splitImpl_repositorySlug`.
  - `RexGitViewModelShared.kt` — 8,213 byte, 249 baris; memuat: `RexGitViewModel.__splitImpl_push`, `RexGitViewModel.__splitImpl_pull`, `RexGitViewModel.__splitImpl_loadInitial`, `RexGitViewModel.__splitImpl_readReadme`, `RexGitViewModel.__splitImpl_openParent`, `RexGitViewModel.__splitImpl_looksBinary`.

### `rexmanager/RexManagerScreen.kt`
- Sebelum: **51,929 byte**; sesudah/facade: **6,954 byte**; folder pecahan: `rexmanager/RexManagerScreen/`.
  - `RexManagerScreenContent.kt` — 9,343 byte, 163 baris; memuat: `__splitImpl_HomeScreen`.
  - `RexManagerScreenContent2.kt` — 9,254 byte, 196 baris; memuat: `__splitImpl_RexManagerScreen`.
  - `RexManagerScreenContent3.kt` — 7,343 byte, 180 baris; memuat: `__splitImpl_EditorScreen`.
  - `RexManagerScreenContent4.kt` — 9,264 byte, 217 baris; memuat: `__splitImpl_CompressScreen`, `__splitImpl_ArchiveScreen`, `__splitImpl_PermissionGate`, `__splitImpl_iconFor`.
  - `RexManagerScreenContent5.kt` — 2,031 byte, 49 baris; memuat: `__splitImpl_MediaScreen`, `__splitImpl_listArchive`, `__splitImpl_RexManagerApp`.
  - `RexManagerScreenFiles.kt` — 8,087 byte, 178 baris; memuat: `__splitImpl_FolderScreen`.
  - `RexManagerScreenHeader.kt` — 1,588 byte, 42 baris; memuat: `__splitImpl_RexTopBar`.
  - `RexManagerScreenPreview.kt` — 4,407 byte, 118 baris; memuat: `__splitImpl_AudioPreview`, `__splitImpl_ImagePreview`, `__splitImpl_VideoPreview`.
  - `RexManagerScreenShared.kt` — 4,368 byte, 107 baris; memuat: `__splitImpl_compress`, `__splitImpl_tokenColor`, `__splitImpl_formatSize`, `__splitImpl_walkTree`, `__splitImpl_hasAccess`.
  - `RexManagerScreenSidebar.kt` — 594 byte, 16 baris; memuat: `__splitImpl_drawerItemColors`.

### `rexmusic/RexLyricsOverlay.kt`
- Sebelum: **18,779 byte**; sesudah/facade: **8,999 byte**; folder pecahan: `rexmusic/RexLyricsOverlay/`.
  - `RexLyricsOverlayContent.kt` — 3,189 byte, 89 baris; memuat: `__splitImpl_VinylDisc`.
  - `RexLyricsOverlayFooter.kt` — 2,508 byte, 64 baris; memuat: `__splitImpl_LyricsFooter`.
  - `RexLyricsOverlayLyrics.kt` — 7,259 byte, 191 baris; memuat: `__splitImpl_SyncedLyrics`, `__splitImpl_RexLyricsOverlay`.

### `rexmusic/RexMusicComponents.kt`
- Sebelum: **28,043 byte**; sesudah/facade: **8,240 byte**; folder pecahan: `rexmusic/RexMusicComponents/`.
  - `RexMusicComponentsContent.kt` — 8,990 byte, 214 baris; memuat: `__splitImpl_ShelfCard`, `__splitImpl_AutoplayCard`, `__splitImpl_QuickTileCard`, `__splitImpl_SkeletonList`, `__splitImpl_CoverImage`.
  - `RexMusicComponentsContent2.kt` — 7,093 byte, 190 baris; memuat: `__splitImpl_ModeChip`, `__splitImpl_ArtistCard`, `__splitImpl_CoverThumb`, `__splitImpl_RexIconButton`, `__splitImpl_QuickGrid`, `__splitImpl_SectionTitle`, `__splitImpl_rememberPressScale`.
  - `RexMusicComponentsDownload.kt` — 3,110 byte, 73 baris; memuat: `__splitImpl_DownloadRow`, `__splitImpl_DownloadRing`.
  - `RexMusicComponentsHeader.kt` — 929 byte, 26 baris; memuat: `__splitImpl_TabHeader`.
  - `RexMusicComponentsLoading.kt` — 3,374 byte, 87 baris; memuat: `__splitImpl_ErrorCard`, `__splitImpl_EmptyState`.
  - `RexMusicComponentsPlayback.kt` — 2,703 byte, 75 baris; memuat: `__splitImpl_TrackRow`.
  - `RexMusicComponentsQueue.kt` — 2,335 byte, 64 baris; memuat: `__splitImpl_QueueRow`.
  - `RexMusicComponentsShared.kt` — 1,141 byte, 33 baris; memuat: `Drawable.__splitImpl_averageColor`, `__splitImpl_formatBytes`.

### `rexmusic/RexMusicScreen.kt`
- Sebelum: **40,219 byte**; sesudah/facade: **6,882 byte**; folder pecahan: `rexmusic/RexMusicScreen/`.
  - `RexMusicScreenContent.kt` — 28,036 byte, 872 baris; memuat: `__splitImpl_RexMusicContent`.
  - `RexMusicScreenPlayback.kt` — 7,428 byte, 191 baris; memuat: `__splitImpl_MiniPlayer`.

### `rexmusic/RexMusicScreenLandscape.kt`
- Sebelum: **32,852 byte**; sesudah/facade: **6,742 byte**; folder pecahan: `rexmusic/RexMusicScreenLandscape/`.
  - `RexMusicScreenLandscapeContent.kt` — 6,088 byte, 139 baris; memuat: `__splitImpl_AmbientBackdrop`.
  - `RexMusicScreenLandscapeContent2.kt` — 9,526 byte, 226 baris; memuat: `__splitImpl_RexMusicScreenLandscape`, `__splitImpl_DockItemView`.
  - `RexMusicScreenLandscapeContent3.kt` — 7,217 byte, 196 baris; memuat: `__splitImpl_DockIndicator`, `__splitImpl_RexBadge`, `__splitImpl_ContentStage`, `__splitImpl_DockStatusSignal`.
  - `RexMusicScreenLandscapeNav.kt` — 6,379 byte, 169 baris; memuat: `__splitImpl_NavigationDock`.
  - `RexMusicScreenLandscapePlayback.kt` — 2,976 byte, 79 baris; memuat: `__splitImpl_FloatingAudioDock`.

### `rexmusic/RexMusicScreenPortrait.kt`
- Sebelum: **23,129 byte**; sesudah/facade: **7,102 byte**; folder pecahan: `rexmusic/RexMusicScreenPortrait/`.
  - `RexMusicScreenPortraitContent.kt` — 6,312 byte, 145 baris; memuat: `__splitImpl_PortraitAmbientBackdrop`.
  - `RexMusicScreenPortraitContent2.kt` — 9,336 byte, 246 baris; memuat: `__splitImpl_PortraitBottomChrome`, `__splitImpl_RexMusicScreenPortrait`, `__splitImpl_PortraitContentStage`.
  - `RexMusicScreenPortraitNav.kt` — 3,336 byte, 88 baris; memuat: `__splitImpl_PortraitNavigationIcon`.

### `rexmusic/RexMusicSync.kt`
- Sebelum: **85,539 byte**; sesudah/facade: **10,752 byte**; folder pecahan: `rexmusic/RexMusicSync/`.
  - `RexMusicSyncActions.kt` — 1,578 byte, 34 baris; memuat: `RexMusicSyncEngine.__splitImpl_createRoom`.
  - `RexMusicSyncContent.kt` — 16,797 byte, 356 baris; memuat: `__splitImpl_RexMusicSyncScreen`.
  - `RexMusicSyncContent2.kt` — 9,537 byte, 232 baris; memuat: `__splitImpl_SyncOverviewCard`, `__splitImpl_SyncConnectionCard`.
  - `RexMusicSyncContent3.kt` — 9,640 byte, 231 baris; memuat: `__splitImpl_SyncMemberCard`, `__splitImpl_SyncWaitingCard`, `__splitImpl_SyncOnlineSearchCard`, `__splitImpl_SyncOnlineResultCard`, `__splitImpl_SyncSectionHeading`.
  - `RexMusicSyncEngine.kt` — 1,288 byte, 22 baris; memuat: `RexMusicSyncEngine.__splitImpl_sendState`.
  - `RexMusicSyncHeader.kt` — 1,032 byte, 30 baris; memuat: `__splitImpl_SyncOfflineHeader`, `__splitImpl_SyncMembersHeader`, `__splitImpl_SyncQueueHeader`.
  - `RexMusicSyncLoading.kt` — 1,517 byte, 39 baris; memuat: `__splitImpl_SyncMembersEmptyCard`, `__splitImpl_SyncEmptyOfflineCard`.
  - `RexMusicSyncPlayback.kt` — 8,454 byte, 167 baris; memuat: `__splitImpl_SyncPlaybackCard`, `RexMusicSyncEngine.__splitImpl_playTrack`, `RexMusicSyncEngine.__splitImpl_syncLocalTrack`.
  - `RexMusicSyncPlayback2.kt` — 7,246 byte, 134 baris; memuat: `RexMusicSyncEngine.__splitImpl_reconcileLocalTrack`, `__splitImpl_SyncOfflineTrackCard`, `RexMusicSyncEngine.__splitImpl_togglePlayback`, `__splitImpl_SyncPlaybackEmptyCard`, `RexMusicSyncEngine.__splitImpl_distributeLocalTrack`, `RexMusicSyncEngine.__splitImpl_setMemberVolume`, `RexMusicSyncEngine.__splitImpl_trackFromJson`, `RexMusicSyncEngine.__splitImpl_setHostVolume`, `RexMusicSyncEngine.__splitImpl_trackJson`, `RexMusicSyncEngine.__splitImpl_stopLocalPlayback`.
  - `RexMusicSyncQueue.kt` — 4,528 byte, 80 baris; memuat: `__splitImpl_SyncQueueTrackCard`, `RexMusicSyncEngine.__splitImpl_addToQueue`, `RexMusicSyncEngine.__splitImpl_playQueued`, `__splitImpl_SyncEmptyQueueCard`, `RexMusicSyncEngine.__splitImpl_publishQueue`, `RexMusicSyncEngine.__splitImpl_sendQueue`.
  - `RexMusicSyncShared.kt` — 5,984 byte, 85 baris; memuat: `RexMusicSyncEngine.__splitImpl_handleClientMessage`.
  - `RexMusicSyncShared2.kt` — 7,209 byte, 112 baris; memuat: `RexMusicSyncEngine.__splitImpl_acceptPeer`, `RexMusicSyncEngine.__splitImpl_joinRoom`.
  - `RexMusicSyncShared3.kt` — 9,403 byte, 147 baris; memuat: `RexMusicSyncEngine.__splitImpl_receiveBundle`, `RexMusicSyncEngine.__splitImpl_broadcastTick`, `RexMusicSyncEngine.__splitImpl_sendBundle`, `RexMusicSyncEngine.__splitImpl_resumeRoomAtCurrentPosition`, `RexMusicSyncEngine.__splitImpl_detectIp`, `RexMusicSyncEngine.__splitImpl_bindContext`.
  - `RexMusicSyncShared4.kt` — 3,198 byte, 56 baris; memuat: `RexMusicSyncEngine.__splitImpl_broadcastMembers`, `RexMusicSyncEngine.__splitImpl_closeAll`, `RexMusicSyncEngine.__splitImpl_stopAll`, `RexMusicSyncEngine.__splitImpl_send`, `RexMusicSyncEngine.__splitImpl_monitorPeers`, `__splitImpl_formatRoomTime`, `RexMusicSyncEngine.__splitImpl_disconnect`.

### `rexmusic/RexMusicTabs.kt`
- Sebelum: **49,622 byte**; sesudah/facade: **7,765 byte**; folder pecahan: `rexmusic/RexMusicTabs/`.
  - `RexMusicTabsContent.kt` — 8,101 byte, 203 baris; memuat: `__splitImpl_MixHeroCard`, `LazyListScope.__splitImpl_offlineSection`, `__splitImpl_OfflineHeroCard`.
  - `RexMusicTabsContent2.kt` — 8,346 byte, 219 baris; memuat: `__splitImpl_SearchField`, `__splitImpl_PermissionCard`, `__splitImpl_ExploreTile`, `LazyListScope.__splitImpl_resultsSection`, `__splitImpl_HeroButton`, `__splitImpl_LibraryChip`, `LazyListScope.__splitImpl_exploreSection`.
  - `RexMusicTabsHeader.kt` — 1,512 byte, 37 baris; memuat: `__splitImpl_HomeHeader`.
  - `RexMusicTabsNav.kt` — 10,088 byte, 216 baris; memuat: `__splitImpl_PlaylistTab`.
  - `RexMusicTabsNav2.kt` — 4,679 byte, 140 baris; memuat: `__splitImpl_HomeTab`, `__splitImpl_LibraryTab`, `__splitImpl_SearchTab`.
  - `RexMusicTabsPlayback.kt` — 1,923 byte, 58 baris; memuat: `LazyListScope.__splitImpl_trackShelf`, `__splitImpl_TrackRowFor`.
  - `RexMusicTabsProfile.kt` — 1,103 byte, 31 baris; memuat: `LazyListScope.__splitImpl_historySection`.
  - `RexMusicTabsQueue.kt` — 5,405 byte, 118 baris; memuat: `__splitImpl_PlaylistDetailScreen`.
  - `RexMusicTabsQueue2.kt` — 8,489 byte, 189 baris; memuat: `__splitImpl_PlaylistGridCard`, `__splitImpl_PlaylistArtwork`, `__splitImpl_PlaylistDetailText`.
  - `RexMusicTabsQueue3.kt` — 2,720 byte, 66 baris; memuat: `__splitImpl_PlaylistEmptyState`, `__splitImpl_PlaylistArtworkTile`.
  - `RexMusicTabsShared.kt` — 2,752 byte, 69 baris; memuat: `LazyListScope.__splitImpl_onlineHome`, `__splitImpl_buildQuickTiles`, `LazyListScope.__splitImpl_artistShelf`.

### `rexmusic/RexOfflineManager.kt`
- Sebelum: **22,037 byte**; sesudah/facade: **9,640 byte**; folder pecahan: `rexmusic/RexOfflineManager/`.
  - `RexOfflineManagerDownload.kt` — 8,633 byte, 201 baris; memuat: `RexOfflineManager.__splitImpl_downloadFile`, `RexOfflineManager.__splitImpl_doDownload`, `RexOfflineManager.__splitImpl_download`.
  - `RexOfflineManagerLyrics.kt` — 2,136 byte, 52 baris; memuat: `RexOfflineManager.__splitImpl_writeLyrics`, `RexOfflineManager.__splitImpl_readOfflineLyrics`.
  - `RexOfflineManagerPlayback.kt` — 1,088 byte, 23 baris; memuat: implementation internal.
  - `RexOfflineManagerShared.kt` — 2,400 byte, 54 baris; memuat: `RexOfflineManager.__splitImpl_scan`.

### `rexmusic/RexPlayerController.kt`
- Sebelum: **41,341 byte**; sesudah/facade: **17,451 byte**; folder pecahan: `rexmusic/RexPlayerController/`.
  - `RexPlayerControllerActions.kt` — 516 byte, 16 baris; memuat: implementation internal.
  - `RexPlayerControllerEngine.kt` — 1,316 byte, 37 baris; memuat: implementation internal.
  - `RexPlayerControllerLoading.kt` — 992 byte, 28 baris; memuat: implementation internal.
  - `RexPlayerControllerLyrics.kt` — 1,758 byte, 48 baris; memuat: `RexPlayerController.__splitImpl_loadLyrics`, `RexPlayerController.__splitImpl_toggleLyrics`.
  - `RexPlayerControllerNetwork.kt` — 874 byte, 22 baris; memuat: `RexPlayerController.__splitImpl_requestFocus`.
  - `RexPlayerControllerPlayback.kt` — 9,667 byte, 222 baris; memuat: `RexPlayerController.__splitImpl_stopPlayer`, `RexPlayerController.__splitImpl_beginTrack`.
  - `RexPlayerControllerProfile.kt` — 903 byte, 24 baris; memuat: `RexPlayerController.__splitImpl_clearHistory`.
  - `RexPlayerControllerQueue.kt` — 5,210 byte, 140 baris; memuat: `RexPlayerController.__splitImpl_playPlaylist`, `RexPlayerController.__splitImpl_publishQueueInfo`, `RexPlayerController.__splitImpl_addToQueue`, `RexPlayerController.__splitImpl_createPlaylist`, `RexPlayerController.__splitImpl_addToPlaylist`, `RexPlayerController.__splitImpl_moveQueueToTop`, `RexPlayerController.__splitImpl_clearQueue`, `RexPlayerController.__splitImpl_removeFromPlaylist`, `RexPlayerController.__splitImpl_startQueue`, `RexPlayerController.__splitImpl_deletePlaylist`, `RexPlayerController.__splitImpl_removeFromQueue`, `RexPlayerController.__splitImpl_refreshPlaylists`.
  - `RexPlayerControllerShared.kt` — 9,280 byte, 241 baris; memuat: `RexPlayerController.__splitImpl_offlinePick`, `RexPlayerController.__splitImpl_playContextNext`.
  - `RexPlayerControllerShared2.kt` — 6,428 byte, 170 baris; memuat: `RexPlayerController.__splitImpl_popularPick`, `RexPlayerController.__splitImpl_abandonFocus`, `RexPlayerController.__splitImpl_pickFromArtist`, `RexPlayerController.__splitImpl_penalizeEarlySkip`, `RexPlayerController.__splitImpl_setOfflineMode`, `RexPlayerController.__splitImpl_recentKeys`, `RexPlayerController.__splitImpl_setAutoplay`, `RexPlayerController.__splitImpl_onOfflineChanged`, `RexPlayerController.__splitImpl_pauseInternal`, `RexPlayerController.__splitImpl_fail`, `RexPlayerController.__splitImpl_clearSearch`, `RexPlayerController.__splitImpl_playFromSearch`, … (16 deklarasi).

### `rexmusic/player/MusicPlayerCard.kt`
- Sebelum: **37,036 byte**; sesudah/facade: **7,391 byte**; folder pecahan: `rexmusic/player/MusicPlayerCard/`.
  - `MusicPlayerCardActions.kt` — 2,782 byte, 71 baris; memuat: `__splitImpl_BottomActions`, `__splitImpl_ActionPill`.
  - `MusicPlayerCardContent.kt` — 9,293 byte, 222 baris; memuat: `__splitImpl_AutoplayRow`, `__splitImpl_PlayPauseButton`, `__splitImpl_RepeatControl`, `__splitImpl_NeighborRow`, `__splitImpl_EqualizerBars`, `__splitImpl_NeighborSection`.
  - `MusicPlayerCardContent2.kt` — 2,491 byte, 66 baris; memuat: `__splitImpl_LikeButton`, `__splitImpl_ToggleControl`.
  - `MusicPlayerCardHeader.kt` — 3,202 byte, 104 baris; memuat: `__splitImpl_PlayerTopBar`.
  - `MusicPlayerCardPlayback.kt` — 5,266 byte, 124 baris; memuat: `__splitImpl_MusicPlayerCard`.
  - `MusicPlayerCardPlayback2.kt` — 8,179 byte, 192 baris; memuat: `__splitImpl_TrackInfo`, `__splitImpl_PlayerCover`.
  - `MusicPlayerCardPlayback3.kt` — 8,403 byte, 205 baris; memuat: `__splitImpl_SeekBar`, `__splitImpl_SeekSection`, `__splitImpl_PlayerControls`, `__splitImpl_PlayerError`, `__splitImpl_PlayerDetails`, `Modifier.__splitImpl_playerBackground`.

### `rexmusic/widget/RexMusicWidget.kt`
- Sebelum: **19,949 byte**; sesudah/facade: **9,415 byte**; folder pecahan: `rexmusic/widget/RexMusicWidget/`.
  - `RexMusicWidgetActions.kt` — 1,051 byte, 26 baris; memuat: `RexMusicWidget.Companion.__splitImpl_updateAll`.
  - `RexMusicWidgetLyrics.kt` — 896 byte, 19 baris; memuat: `RexMusicWidget.Companion.__splitImpl_lyricLines`.
  - `RexMusicWidgetShared.kt` — 9,506 byte, 209 baris; memuat: `RexMusicWidget.Companion.__splitImpl_render`, `WidgetArt.__splitImpl_roundedSquare`, `RexMusicWidget.Companion.__splitImpl_sourceLabel`, `RexMusicWidget.Companion.__splitImpl_shouldRender`, `WidgetArt.__splitImpl_averageColor`.
  - `RexMusicWidgetShared2.kt` — 1,838 byte, 38 baris; memuat: `RexMusicWidget.Companion.__splitImpl_openAppIntent`, `RexMusicWidget.Companion.__splitImpl_isWide`, `RexMusicWidget.Companion.__splitImpl_signature`.

### `rexnux/RexNuxInstaller.kt`
- Sebelum: **15,993 byte**; sesudah/facade: **7,511 byte**; folder pecahan: `rexnux/RexNuxInstaller/`.
  - `RexNuxInstallerDownload.kt` — 3,130 byte, 122 baris; memuat: `RexNuxInstaller.__splitImpl_installAlpine`.
  - `RexNuxInstallerShared.kt` — 5,974 byte, 198 baris; memuat: `RexNuxInstaller.__splitImpl_extractTarGz`.

### `rexnux/RexNuxScreen.kt`
- Sebelum: **23,567 byte**; sesudah/facade: **9,222 byte**; folder pecahan: `rexnux/RexNuxScreen/`.
  - `RexNuxScreenContent.kt` — 4,756 byte, 165 baris; memuat: `__splitImpl_RexNuxNoDistroContent`, `__splitImpl_RexNuxHomeScreen`.
  - `RexNuxScreenDownload.kt` — 7,316 byte, 238 baris; memuat: `__splitImpl_RexNuxInstalledContent`, `__splitImpl_RexNuxDownloadScreen`.
  - `RexNuxScreenSettings.kt` — 6,062 byte, 172 baris; memuat: `__splitImpl_RexNuxSettingsScreen`.

### `rexpanel/FilesScreen.kt`
- Sebelum: **48,123 byte**; sesudah/facade: **7,171 byte**; folder pecahan: `rexpanel/FilesScreen/`.
  - `FilesScreenContent.kt` — 8,900 byte, 236 baris; memuat: `__splitImpl_GridTile`, `__splitImpl_TransferBar`, `__splitImpl_Breadcrumbs`, `__splitImpl_ZoomableImage`.
  - `FilesScreenContent2.kt` — 5,155 byte, 132 baris; memuat: `__splitImpl_SelectionBar`, `__splitImpl_StateMessage`, `__splitImpl_SkeletonRows`, `__splitImpl_QuickPlaces`.
  - `FilesScreenDialogs.kt` — 2,252 byte, 66 baris; memuat: `__splitImpl_NameDialog`, `__splitImpl_SheetAction`.
  - `FilesScreenDownload.kt` — 1,101 byte, 30 baris; memuat: `__splitImpl_DownloadProgress`.
  - `FilesScreenFiles.kt` — 11,343 byte, 244 baris; memuat: `__splitImpl_FileBrowser`.
  - `FilesScreenFiles2.kt` — 4,752 byte, 136 baris; memuat: `__splitImpl_FileRow`, `__splitImpl_FileThumb`, `__splitImpl_FilesScreen`.
  - `FilesScreenHeader.kt` — 4,541 byte, 102 baris; memuat: `__splitImpl_BrowserToolbar`.
  - `FilesScreenPlayback.kt` — 1,943 byte, 56 baris; memuat: `__splitImpl_VideoPlayer`.
  - `FilesScreenPreview.kt` — 9,625 byte, 239 baris; memuat: `__splitImpl_FilePreview`, `__splitImpl_PlayerPreview`, `__splitImpl_TextPreview`, `__splitImpl_ImagePreview`, `__splitImpl_NoPreview`.
  - `FilesScreenPreview2.kt` — 1,176 byte, 32 baris; memuat: `__splitImpl_PreviewError`.
  - `FilesScreenShared.kt` — 979 byte, 21 baris; memuat: `__splitImpl_styleOf`.

### `rexpanel/RemoteFiles.kt`
- Sebelum: **16,625 byte**; sesudah/facade: **9,377 byte**; folder pecahan: `rexpanel/RemoteFiles/`.
  - `RemoteFilesActions.kt` — 871 byte, 27 baris; memuat: `FileBrowserState.__splitImpl_deleteAll`.
  - `RemoteFilesContent.kt` — 1,184 byte, 29 baris; memuat: `SftpClient.__splitImpl_list`.
  - `RemoteFilesDownload.kt` — 508 byte, 15 baris; memuat: `SftpClient.__splitImpl_download`.
  - `RemoteFilesFiles.kt` — 930 byte, 21 baris; memuat: `__splitImpl_decodeFileSampled`.
  - `RemoteFilesShared.kt` — 6,434 byte, 167 baris; memuat: `__splitImpl_decodeSampled`, `FileBrowserState.__splitImpl_go`, `FileBrowserState.__splitImpl_thumbnail`, `__splitImpl_sftpMessage`, `__splitImpl_kindOf`, `__splitImpl_orient`, `FileBrowserState.__splitImpl_start`, `FileBrowserState.__splitImpl_mutate`.

### `rexpanel/RexPanelScreen.kt`
- Sebelum: **51,083 byte**; sesudah/facade: **6,380 byte**; folder pecahan: `rexpanel/RexPanelScreen/`.
  - `RexPanelScreenContent.kt` — 12,392 byte, 490 baris; memuat: `__splitImpl_ConnectForm`.
  - `RexPanelScreenContent2.kt` — 8,071 byte, 288 baris; memuat: `__splitImpl_TerminalPane`.
  - `RexPanelScreenContent3.kt` — 6,150 byte, 241 baris; memuat: `__splitImpl_MonitorSection`.
  - `RexPanelScreenContent4.kt` — 5,904 byte, 217 baris; memuat: `__splitImpl_RexPanelScreen`.
  - `RexPanelScreenContent5.kt` — 6,070 byte, 252 baris; memuat: `__splitImpl_GaugeCard`.
  - `RexPanelScreenContent6.kt` — 5,574 byte, 268 baris; memuat: `__splitImpl_KeyBar`, `__splitImpl_StatTile`, `__splitImpl_KeyChip`.
  - `RexPanelScreenHeader.kt` — 4,046 byte, 167 baris; memuat: `__splitImpl_PanelHeader`.
  - `RexPanelScreenNav.kt` — 3,106 byte, 114 baris; memuat: `__splitImpl_PanelTabs`.

### `rexpanel/RexPanelViewModel.kt`
- Sebelum: **21,875 byte**; sesudah/facade: **9,161 byte**; folder pecahan: `rexpanel/RexPanelViewModel/`.
  - `RexPanelViewModelEngine.kt` — 6,030 byte, 286 baris; memuat: `__splitImpl_parseSample`.
  - `RexPanelViewModelShared.kt` — 6,450 byte, 281 baris; memuat: `__splitImpl_buildMetrics`, `RexPanelViewModel.__splitImpl_connect`, `RexPanelViewModel.__splitImpl_startMonitor`.
  - `RexPanelViewModelShared2.kt` — 1,331 byte, 67 baris; memuat: `RexPanelViewModel.__splitImpl_sendText`.

### `rexspacex/RexSpaceXScreen.kt`
- Sebelum: **25,084 byte**; sesudah/facade: **7,986 byte**; folder pecahan: `rexspacex/RexSpaceXScreen/`.
  - `RexSpaceXScreenContent.kt` — 17,599 byte, 362 baris; memuat: `__splitImpl_RexSpaceXScreen`.

### `rextools/preview/PreviewScreenPortrait.kt`
- Sebelum: **26,304 byte**; sesudah/facade: **6,984 byte**; folder pecahan: `rextools/preview/PreviewScreenPortrait/`.
  - `PreviewScreenPortraitContent.kt` — 8,533 byte, 194 baris; memuat: `__splitImpl_SingleView`.
  - `PreviewScreenPortraitContent2.kt` — 9,223 byte, 223 baris; memuat: `__splitImpl_InputView`, `__splitImpl_GridTile`.
  - `PreviewScreenPortraitPreview.kt` — 5,041 byte, 115 baris; memuat: `__splitImpl_PreviewScreenPortrait`.

### `rextools/tiktok/TiktokStalkerScreenPortrait.kt`
- Sebelum: **27,869 byte**; sesudah/facade: **7,603 byte**; folder pecahan: `rextools/tiktok/TiktokStalkerScreenPortrait/`.
  - `TiktokStalkerScreenPortraitContent.kt` — 6,497 byte, 178 baris; memuat: `__splitImpl_TiktokStalkerScreenPortrait`.
  - `TiktokStalkerScreenPortraitContent2.kt` — 8,045 byte, 180 baris; memuat: `__splitImpl_StatusPanel`, `__splitImpl_HiddenScraper`, `__splitImpl_VideoTile`.
  - `TiktokStalkerScreenPortraitHeader.kt` — 3,847 byte, 85 baris; memuat: `__splitImpl_ProfileHeader`.
  - `TiktokStalkerScreenPortraitProfile.kt` — 5,984 byte, 126 baris; memuat: `__splitImpl_FullProfileCard`, `__splitImpl_BasicProfileCard`.

### `rextools/ui/RexUi.kt`
- Sebelum: **41,286 byte**; sesudah/facade: **10,783 byte**; folder pecahan: `rextools/ui/RexUi/`.
  - `RexUiActions.kt` — 1,809 byte, 44 baris; memuat: `__splitImpl_RexQuickAction`.
  - `RexUiAnimations.kt` — 1,124 byte, 30 baris; memuat: `Modifier.__splitImpl_shimmer`.
  - `RexUiContent.kt` — 7,453 byte, 172 baris; memuat: `__splitImpl_RexPrimaryButton`, `__splitImpl_RexScreen`.
  - `RexUiContent2.kt` — 9,641 byte, 223 baris; memuat: `__splitImpl_RexField`, `__splitImpl_RexButtonBase`, `__splitImpl_RexToastHost`, `__splitImpl_RexStat`.
  - `RexUiContent3.kt` — 9,178 byte, 220 baris; memuat: `__splitImpl_RexBackground`, `__splitImpl_RexFilterChip`, `__splitImpl_RexPanel`, `__splitImpl_RexIconTile`, `__splitImpl_RexTheme`, `__splitImpl_RexImage`.
  - `RexUiContent4.kt` — 8,883 byte, 228 baris; memuat: `__splitImpl_RexSkeletonPanel`, `__splitImpl_RexEqualizer`, `__splitImpl_RexPhaseSwitch`, `__splitImpl_RexRoundButton`, `__splitImpl_RexBackButton`, `__splitImpl_RexPill`, `__splitImpl_RexHero`, `__splitImpl_RexTonalButton`, `__splitImpl_RexOverline`, `__splitImpl_RexOutlinedButton`, `__splitImpl_RexSkeleton`, `__splitImpl_hairline`.
  - `RexUiLoading.kt` — 4,643 byte, 107 baris; memuat: `__splitImpl_RexEmptyState`, `__splitImpl_RexErrorBanner`, `__splitImpl_RexErrorSlot`.
  - `RexUiProfile.kt` — 1,856 byte, 52 baris; memuat: `__splitImpl_RexAvatar`.
  - `RexUiShared.kt` — 1,664 byte, 44 baris; memuat: `Modifier.__splitImpl_entrance`, `Modifier.__splitImpl_pressScale`.

### `rexwarp/RexWarpVpnService.kt`
- Sebelum: **15,706 byte**; sesudah/facade: **9,300 byte**; folder pecahan: `rexwarp/RexWarpVpnService/`.
  - `RexWarpVpnServiceActions.kt` — 1,147 byte, 25 baris; memuat: implementation internal.
  - `RexWarpVpnServiceEngine.kt` — 1,099 byte, 24 baris; memuat: `RexWarpVpnService.__splitImpl_onTunnelState`.
  - `RexWarpVpnServiceLoading.kt` — 939 byte, 26 baris; memuat: `RexWarpVpnService.__splitImpl_handleFailure`.
  - `RexWarpVpnServiceShared.kt` — 5,102 byte, 99 baris; memuat: `RexWarpVpnService.__splitImpl_startConnect`, `RexWarpVpnService.__splitImpl_sample`, `RexWarpVpnService.__splitImpl_pauseReason`.

### `rexwarp/ui/RexWarpSpeedTestScreenLandscape.kt`
- Sebelum: **22,294 byte**; sesudah/facade: **6,593 byte**; folder pecahan: `rexwarp/ui/RexWarpSpeedTestScreenLandscape/`.
  - `RexWarpSpeedTestScreenLandscapeContent.kt` — 9,288 byte, 114 baris; memuat: `__splitImpl_SpeedTestGauge`, `__splitImpl_SpeedTestHero`.
  - `RexWarpSpeedTestScreenLandscapeContent2.kt` — 9,176 byte, 139 baris; memuat: `__splitImpl_TestProgressPanel`, `__splitImpl_RexWarpSpeedTestScreenLandscape`, `__splitImpl_PrivacyPanel`.

### `rexwarp/ui/RexWarpSpeedTestScreenPotriat.kt`
- Sebelum: **21,856 byte**; sesudah/facade: **6,591 byte**; folder pecahan: `rexwarp/ui/RexWarpSpeedTestScreenPotriat/`.
  - `RexWarpSpeedTestScreenPotriatContent.kt` — 9,288 byte, 114 baris; memuat: `__splitImpl_SpeedTestGauge`, `__splitImpl_SpeedTestHero`.
  - `RexWarpSpeedTestScreenPotriatContent2.kt` — 8,738 byte, 129 baris; memuat: `__splitImpl_TestProgressPanel`, `__splitImpl_RexWarpSpeedTestScreenPortrait`, `__splitImpl_PrivacyPanel`.

### `rexwarp/ui/components/RexWarpDiagnosticsPanel.kt`
- Sebelum: **17,217 byte**; sesudah/facade: **8,496 byte**; folder pecahan: `rexwarp/ui/components/RexWarpDiagnosticsPanel/`.
  - `RexWarpDiagnosticsPanelContent.kt` — 9,492 byte, 173 baris; memuat: `__splitImpl_RexWarpDiagnosticsPanel`.

### `rexwarp/ui/components/RexWarpGraph.kt`
- Sebelum: **15,148 byte**; sesudah/facade: **2,544 byte**; folder pecahan: `rexwarp/ui/components/RexWarpGraph/`.
  - `RexWarpGraphContent.kt` — 13,398 byte, 247 baris; memuat: `__splitImpl_RexWarpGraph`.

### `rexwarp/ui/components/RexWarpSpeedTestHistorySection.kt`
- Sebelum: **18,334 byte**; sesudah/facade: **5,606 byte**; folder pecahan: `rexwarp/ui/components/RexWarpSpeedTestHistorySection/`.
  - `RexWarpSpeedTestHistorySectionProfile.kt` — 13,384 byte, 226 baris; memuat: `__splitImpl_RexWarpSpeedTestHistorySection`.

### `ui/RexAppsScreenPortrait.kt`
- Sebelum: **30,953 byte**; sesudah/facade: **9,256 byte**; folder pecahan: `ui/RexAppsScreenPortrait/`.
  - `RexAppsScreenPortraitContent.kt` — 8,984 byte, 213 baris; memuat: `__splitImpl_ProFeaturedCard`, `__splitImpl_ProAmbientBackdrop`.
  - `RexAppsScreenPortraitContent2.kt` — 8,785 byte, 244 baris; memuat: `__splitImpl_ProPortraitHome`, `__splitImpl_RexAppsScreenPortrait`, `__splitImpl_ProNavItem`, `__splitImpl_ProAppTile`.
  - `RexAppsScreenPortraitContent3.kt` — 2,896 byte, 79 baris; memuat: `__splitImpl_ProSearchBar`, `__splitImpl_ProPortraitNav`.
  - `RexAppsScreenPortraitHeader.kt` — 3,406 byte, 95 baris; memuat: `__splitImpl_ProHomeHeader`, `__splitImpl_ProSectionHeader`.
  - `RexAppsScreenPortraitProfile.kt` — 3,086 byte, 85 baris; memuat: `__splitImpl_ProPortraitProfile`.

### `ui/RexApsApp.kt`
- Sebelum: **48,000 byte**; sesudah/facade: **7,046 byte**; folder pecahan: `ui/RexApsApp/`.
  - `RexApsAppContent.kt` — 5,844 byte, 153 baris; memuat: `__splitImpl_FeaturedCard`.
  - `RexApsAppContent2.kt` — 9,181 byte, 253 baris; memuat: `__splitImpl_ThemeTile`, `__splitImpl_SocialLinkCard`.
  - `RexApsAppContent3.kt` — 9,193 byte, 231 baris; memuat: `__splitImpl_AppCard`, `__splitImpl_HeroCard`, `__splitImpl_ThemePicker`.
  - `RexApsAppContent4.kt` — 6,747 byte, 216 baris; memuat: `__splitImpl_StatusPill`, `__splitImpl_StatCard`, `__splitImpl_HeroStatChip`, `Modifier.__splitImpl_pressable`, `__splitImpl_AppIcon`, `Modifier.__splitImpl_entrance`, `__splitImpl_accentBrush`.
  - `RexApsAppDialogs.kt` — 8,576 byte, 216 baris; memuat: `__splitImpl_RexApsApp`.
  - `RexApsAppDialogs2.kt` — 2,654 byte, 67 baris; memuat: `__splitImpl_ThemeSheet`.
  - `RexApsAppHeader.kt` — 3,602 byte, 101 baris; memuat: `__splitImpl_HomeHeader`, `__splitImpl_SectionHeader`.
  - `RexApsAppNav.kt` — 3,070 byte, 72 baris; memuat: `__splitImpl_ThemeTabContent`.
  - `RexApsAppProfile.kt` — 2,782 byte, 82 baris; memuat: `__splitImpl_SpinningAvatar`.

### `ui/RexLocalAccountScreen.kt`
- Sebelum: **82,437 byte**; sesudah/facade: **13,506 byte**; folder pecahan: `ui/RexLocalAccountScreen/`.
  - `RexLocalAccountScreenActions.kt` — 662 byte, 21 baris; memuat: `RexProfileController.__splitImpl_deleteVideo`.
  - `RexLocalAccountScreenContent.kt` — 9,135 byte, 207 baris; memuat: `__splitImpl_PermissionGate`, `__splitImpl_BannerView`, `__splitImpl_FeedItem`.
  - `RexLocalAccountScreenContent2.kt` — 9,388 byte, 246 baris; memuat: `__splitImpl_RexVideo`, `__splitImpl_AddTile`, `__splitImpl_MediaOption`, `__splitImpl_Aurora`, `__splitImpl_MediaThumb`, `__splitImpl_PermissionStep`, `__splitImpl_RexGrid`, `__splitImpl_StatItem`, `__splitImpl_GlowBadge`, `__splitImpl_GlassCard`, `__splitImpl_Reveal`.
  - `RexLocalAccountScreenDialogs.kt` — 9,906 byte, 200 baris; memuat: `__splitImpl_CropDialog`.
  - `RexLocalAccountScreenDialogs2.kt` — 7,917 byte, 188 baris; memuat: `__splitImpl_StoryComposeDialog`, `__splitImpl_EditProfileSheet`.
  - `RexLocalAccountScreenDialogs3.kt` — 4,718 byte, 133 baris; memuat: `__splitImpl_RexProfileOverlays`, `__splitImpl_VideoFeed`.
  - `RexLocalAccountScreenFiles.kt` — 773 byte, 20 baris; memuat: `__splitImpl_decodeFileSampled`.
  - `RexLocalAccountScreenNav.kt` — 4,397 byte, 104 baris; memuat: `__splitImpl_ProfileTabs`, `__splitImpl_SegTabs`.
  - `RexLocalAccountScreenProfile.kt` — 9,150 byte, 213 baris; memuat: `__splitImpl_StoryViewer`, `__splitImpl_StoryRingAvatar`.
  - `RexLocalAccountScreenProfile2.kt` — 9,683 byte, 239 baris; memuat: `RexProfileController.__splitImpl_applyCrop`, `__splitImpl_StoryTile`, `__splitImpl_RexLocalAccountScreen`, `__splitImpl_AvatarContent`, `__splitImpl_ProfileIdentity`.
  - `RexLocalAccountScreenProfile3.kt` — 9,439 byte, 225 baris; memuat: `__splitImpl_rememberRexProfileController`, `__splitImpl_buildCropReq`, `__splitImpl_ProfileStats`, `__splitImpl_readAccount`, `__splitImpl_RexProfileRoot`, `__splitImpl_ProfileActions`, `RexProfileController.__splitImpl_addStoryVideo`, `__splitImpl_writeAccount`, `__splitImpl_renderCrop`.
  - `RexLocalAccountScreenProfile4.kt` — 2,111 byte, 60 baris; memuat: `RexProfileController.__splitImpl_saveProfile`, `RexProfileController.__splitImpl_postStory`, `RexProfileController.__splitImpl_clearAvatar`, `RexProfileController.__splitImpl_deleteStory`.
  - `RexLocalAccountScreenShared.kt` — 9,367 byte, 229 baris; memuat: `__splitImpl_trimVideo`, `__splitImpl_extractMotionVideo`, `RexProfileController.__splitImpl_addVideo`, `__splitImpl_applyExif`, `__splitImpl_loadStories`, `__splitImpl_decodeSampled`, `Modifier.__splitImpl_pressable`, `RexProfileController.__splitImpl_load`, `__splitImpl_videoFrameAt`, `RexProfileController.__splitImpl_handlePick`.
  - `RexLocalAccountScreenShared2.kt` — 3,077 byte, 78 baris; memuat: `RexProfileController.__splitImpl_clearBanner`, `__splitImpl_videoDurationOf`, `__splitImpl_downscale`, `__splitImpl_hasStorageAccess`, `__splitImpl_loadVideos`, `__splitImpl_rotate90`, `VSrc.__splitImpl_bind`, `VSrc.__splitImpl_open`, `RexProfileController.__splitImpl_toast`.

### `ui/RexTheme.kt`
- Sebelum: **16,731 byte**; sesudah/facade: **12,420 byte**; folder pecahan: `ui/RexTheme/`.
  - `RexThemeActions.kt` — 256 byte, 6 baris; memuat: `RexThemeStore.__splitImpl_save`.
  - `RexThemeContent.kt` — 403 byte, 10 baris; memuat: implementation internal.
  - `RexThemeShared.kt` — 4,564 byte, 74 baris; memuat: `RexPaletteSpec.__splitImpl_toColorScheme`, `RexPaletteSpec.Companion.__splitImpl_of`, `RexThemeOption.__splitImpl_labelToThemeId`, `RexThemeStore.__splitImpl_load`.

### `ui/settings/RexThemeSettings.kt`
- Sebelum: **22,214 byte**; sesudah/facade: **981 byte**; folder pecahan: `ui/settings/RexThemeSettings/`.
  - `RexThemeSettingsSettings.kt` — 21,677 byte, 347 baris; memuat: `__splitImpl_RexThemeSettings`.

## Outlier ukuran yang masih perlu ditangani

Saya tidak memotong composable/fungsi monolitik di tengah kode demi memenuhi batas ukuran. Karena itu hasil ini lebih konservatif, tetapi **belum 100% memenuhi** target 10.000 byte / 300 baris untuk setiap pecahan. Deklarasi berikut perlu decomposisi manual per section/use case:

- Pecahan yang melebihi 10.000 byte atau 300 baris: **10**.
  - `rexmusic/RexMusicScreen/RexMusicScreenContent.kt` — 28,036 byte, 872 baris.
  - `ui/settings/RexThemeSettings/RexThemeSettingsSettings.kt` — 21,677 byte, 347 baris.
  - `rexspacex/RexSpaceXScreen/RexSpaceXScreenContent.kt` — 17,599 byte, 362 baris.
  - `rexmusic/RexMusicSync/RexMusicSyncContent.kt` — 16,797 byte, 356 baris.
  - `rexwarp/ui/components/RexWarpGraph/RexWarpGraphContent.kt` — 13,398 byte, 247 baris.
  - `rexwarp/ui/components/RexWarpSpeedTestHistorySection/RexWarpSpeedTestHistorySectionProfile.kt` — 13,384 byte, 226 baris.
  - `rexpanel/RexPanelScreen/RexPanelScreenContent.kt` — 12,392 byte, 490 baris.
  - `rexgit/RexGitRepositoryHub/RexGitRepositoryHubRepository.kt` — 12,149 byte, 174 baris.
  - `rexpanel/FilesScreen/FilesScreenFiles.kt` — 11,343 byte, 244 baris.
  - `rexmusic/RexMusicTabs/RexMusicTabsNav.kt` — 10,088 byte, 216 baris.

- File asal >15.000 byte yang masih lebih dari 15.000 byte sebagai file utama: **1**.
  - `rexmusic/RexPlayerController.kt` — 41,341 → 17,451 byte.

## Kesimpulan

Paket ini berisi semua source yang tersedia pada arsip input beserta folder pecahan, dan lolos pemeriksaan sintaks Kotlin. Ini **bukan build Android yang terverifikasi hijau**: pemeriksaan tipe/dependency, `assembleDebug`, dan verifikasi UI runtime belum bisa dilakukan tanpa konfigurasi Gradle serta resource/project lengkap.
