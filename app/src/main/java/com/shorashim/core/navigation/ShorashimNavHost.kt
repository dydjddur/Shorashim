package com.shorashim.core.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shorashim.core.common.shorashimViewModelFactory
import com.shorashim.core.model.AuthState
import com.shorashim.data.repositories.AppContainer
import com.shorashim.feature.family.AddEditPersonRoute
import com.shorashim.feature.family.AddEditPersonViewModel
import com.shorashim.feature.family.CreateFamilyRoute
import com.shorashim.feature.family.CreateFamilyViewModel
import com.shorashim.feature.family.FamilyManagementRoute
import com.shorashim.feature.family.FamilyManagementViewModel
import com.shorashim.feature.family.MyFamiliesRoute
import com.shorashim.feature.family.MyFamiliesViewModel
import com.shorashim.feature.invitations.InvitationDeepLinkRoute
import com.shorashim.feature.invitations.InvitationDeepLinkViewModel
import com.shorashim.feature.invitations.InvitationsRoute
import com.shorashim.feature.invitations.InvitationsViewModel
import com.shorashim.feature.donations.DonationsScreen
import com.shorashim.feature.friends.FriendsRoute
import com.shorashim.feature.friends.FriendsViewModel
import com.shorashim.feature.help.HelpScreen
import com.shorashim.feature.profile.ProfileRoute
import com.shorashim.feature.profile.ProfileViewModel
import com.shorashim.feature.settings.SettingsRoute
import com.shorashim.feature.chat.ChatConversationRoute
import com.shorashim.feature.chat.ChatConversationViewModel
import com.shorashim.feature.chat.ChatListRoute
import com.shorashim.feature.chat.ChatListViewModel
import com.shorashim.feature.tehillim.TehillimRoute
import com.shorashim.feature.tehillim.TehillimViewModel

@Composable
fun ShorashimNavHost(
    navController: NavHostController,
    appContainer: AppContainer,
    innerPadding: PaddingValues,
    authState: AuthState = AuthState.Unauthenticated,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ShorashimDestination.FamilyTree.route,
        modifier = modifier.padding(innerPadding),
    ) {
        // Main Family Tab: Shows "המשפחות שלי"
        composable(ShorashimDestination.FamilyTree.route) {
            val viewModel: MyFamiliesViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    MyFamiliesViewModel(appContainer.familyRepository)
                },
            )
            MyFamiliesRoute(
                viewModel = viewModel,
                onNavigateToCreateFamily = {
                    navController.navigate(ShorashimRoutes.CREATE_FAMILY)
                },
                onNavigateToFamilyManagement = { familyId ->
                    navController.navigate(ShorashimRoutes.familyManagement(familyId))
                },
                onNavigateToInvitations = {
                    navController.navigate(ShorashimRoutes.INVITATIONS)
                },
            )
        }

        // Create Family Screen
        composable(ShorashimRoutes.CREATE_FAMILY) {
            val viewModel: CreateFamilyViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    CreateFamilyViewModel(appContainer.familyRepository)
                },
            )
            CreateFamilyRoute(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onFamilyCreated = { newFamilyId ->
                    navController.navigate(ShorashimRoutes.familyManagement(newFamilyId)) {
                        popUpTo(ShorashimDestination.FamilyTree.route)
                    }
                },
            )
        }

        // Family Management Screen
        composable(
            route = ShorashimRoutes.FAMILY_MANAGEMENT,
            arguments = listOf(
                navArgument("familyId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val familyId = backStackEntry.arguments?.getString("familyId").orEmpty()
            val viewModel: FamilyManagementViewModel = viewModel(
                key = "FamilyMgmt_$familyId",
                factory = shorashimViewModelFactory {
                    FamilyManagementViewModel(
                        familyId = familyId,
                        familyRepository = appContainer.familyRepository,
                        personRepository = appContainer.personRepository,
                        relationshipRepository = appContainer.relationshipRepository,
                        invitationRepository = appContainer.invitationRepository,
                        profileRepository = appContainer.profileRepository,
                        friendRepository = appContainer.friendRepository,
                        supabaseClient = appContainer.supabaseClient,
                    )
                },
            )
            FamilyManagementRoute(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddPerson = { fId ->
                    navController.navigate(ShorashimRoutes.addPerson(fId))
                },
                onNavigateToAddRelative = { fId, relation, anchorId ->
                    navController.navigate(ShorashimRoutes.addRelative(fId, relation.name, anchorId))
                },
                onNavigateToEditPerson = { fId, pId ->
                    navController.navigate(ShorashimRoutes.editPerson(fId, pId))
                },
                onNavigateToCreateFamily = {
                    navController.navigate(ShorashimRoutes.CREATE_FAMILY)
                },
                invitationRepository = appContainer.invitationRepository,
            )
        }

        // Add or Edit Person Screen
        composable(
            route = ShorashimRoutes.ADD_EDIT_PERSON,
            arguments = listOf(
                navArgument("familyId") { type = NavType.StringType },
                navArgument("personId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("relation") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("anchorId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val familyId = backStackEntry.arguments?.getString("familyId").orEmpty()
            val personId = backStackEntry.arguments?.getString("personId")
            val relation = backStackEntry.arguments?.getString("relation")
                ?.let { name -> com.shorashim.core.model.NewPersonRelation.entries.firstOrNull { it.name == name } }
            val anchorId = backStackEntry.arguments?.getString("anchorId")
            val viewModel: AddEditPersonViewModel = viewModel(
                key = "AddEditPerson_${familyId}_${personId ?: "new"}_${relation ?: "none"}_${anchorId ?: ""}",
                factory = shorashimViewModelFactory {
                    AddEditPersonViewModel(
                        familyId = familyId,
                        personId = personId,
                        personRepository = appContainer.personRepository,
                        relationshipRepository = appContainer.relationshipRepository,
                        relation = relation,
                        anchorId = anchorId,
                        familyRepository = appContainer.familyRepository,
                    )
                },
            )
            AddEditPersonRoute(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(ShorashimDestination.Memorials.route) {
            val viewModel: com.shorashim.feature.events.EventsViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    com.shorashim.feature.events.EventsViewModel(
                        eventsRepository = appContainer.eventsRepository,
                        familyRepository = appContainer.familyRepository,
                        personRepository = appContainer.personRepository,
                        notificationScheduler = appContainer.notificationScheduler,
                    )
                },
            )
            com.shorashim.feature.events.EventsRoute(
                viewModel = viewModel,
                onNavigateToMemorialTehillim = { deceasedName ->
                    navController.navigate(ShorashimRoutes.tehillim(deceasedName)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = ShorashimRoutes.TEHILLIM_WITH_PARAMS,
            arguments = listOf(
                navArgument("deceasedName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val deceasedName = backStackEntry.arguments?.getString("deceasedName")
            val viewModel: TehillimViewModel = viewModel(
                key = "Tehillim_${deceasedName.orEmpty()}",
                factory = shorashimViewModelFactory {
                    TehillimViewModel(
                        psalmsRepository = appContainer.psalmsRepository,
                        initialDeceasedName = deceasedName,
                    )
                },
            )
            TehillimRoute(viewModel = viewModel)
        }

        composable(ShorashimDestination.Tehillim.route) {
            val viewModel: TehillimViewModel = viewModel(
                key = "Tehillim_default",
                factory = shorashimViewModelFactory {
                    TehillimViewModel(appContainer.psalmsRepository)
                },
            )
            TehillimRoute(viewModel = viewModel)
        }

        composable(ShorashimRoutes.SETTINGS) {
            val viewModel: ProfileViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    ProfileViewModel(
                        profileRepository = appContainer.profileRepository,
                        familyRepository = appContainer.familyRepository,
                        friendRepository = appContainer.friendRepository,
                    )
                },
            )
            ProfileRoute(
                viewModel = viewModel,
                onLogout = onLogout,
                onNavigateToTree = { familyId ->
                    navController.navigate(ShorashimRoutes.familyManagement(familyId))
                },
            )
        }

        composable(ShorashimRoutes.PROFILE) {
            val viewModel: ProfileViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    ProfileViewModel(
                        profileRepository = appContainer.profileRepository,
                        familyRepository = appContainer.familyRepository,
                        friendRepository = appContainer.friendRepository,
                    )
                },
            )
            ProfileRoute(
                viewModel = viewModel,
                onLogout = onLogout,
                onNavigateToTree = { familyId ->
                    navController.navigate(ShorashimRoutes.familyManagement(familyId))
                },
            )
        }

        composable(ShorashimRoutes.FRIENDS) {
            val viewModel: FriendsViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    FriendsViewModel(
                        friendRepository = appContainer.friendRepository,
                        profileRepository = appContainer.profileRepository,
                    )
                },
            )
            val chatListViewModel: ChatListViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    ChatListViewModel(
                        chatRepository = appContainer.chatRepository,
                        friendRepository = appContainer.friendRepository,
                    )
                },
            )
            FriendsRoute(
                viewModel = viewModel,
                chatListViewModel = chatListViewModel,
                onNavigateToTree = { familyId ->
                    navController.navigate(ShorashimRoutes.viewCommunityTree(familyId))
                },
                onNavigateToChat = { otherUserId, otherUserName ->
                    navController.navigate(ShorashimRoutes.chatConversation(otherUserId, otherUserName))
                },
            )
        }

        // Dedicated E2EE Chat List Route
        composable(ShorashimRoutes.CHATS) {
            val viewModel: ChatListViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    ChatListViewModel(
                        chatRepository = appContainer.chatRepository,
                        friendRepository = appContainer.friendRepository,
                    )
                },
            )
            ChatListRoute(
                viewModel = viewModel,
                onOpenConversation = { otherUserId, otherUserName ->
                    navController.navigate(ShorashimRoutes.chatConversation(otherUserId, otherUserName))
                },
            )
        }

        // Dedicated E2EE Chat Conversation Route
        composable(
            route = ShorashimRoutes.CHAT_CONVERSATION,
            arguments = listOf(
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("otherUserName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId").orEmpty()
            val rawName = backStackEntry.arguments?.getString("otherUserName").orEmpty()
            val otherUserName = try { java.net.URLDecoder.decode(rawName, "UTF-8") } catch (_: Throwable) { rawName }
            val viewModel: ChatConversationViewModel = viewModel(
                key = "ChatConv_$otherUserId",
                factory = shorashimViewModelFactory {
                    ChatConversationViewModel(
                        otherUserId = otherUserId,
                        otherUserName = otherUserName,
                        chatRepository = appContainer.chatRepository,
                        invitationRepository = appContainer.invitationRepository,
                        familyRepository = appContainer.familyRepository,
                        supabaseClient = appContainer.supabaseClient,
                    )
                },
            )
            ChatConversationRoute(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenInvitation = { token ->
                    navController.navigate(ShorashimRoutes.invitationDeepLink(token))
                },
            )
        }

        composable(ShorashimRoutes.DONATIONS) {
            DonationsScreen()
        }

        composable(ShorashimRoutes.HELP) {
            HelpScreen()
        }

        // Phase 5: Pending Invitations list screen
        composable(ShorashimRoutes.INVITATIONS) {
            val viewModel: InvitationsViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    InvitationsViewModel(
                        invitationRepository = appContainer.invitationRepository,
                        familyRepository = appContainer.familyRepository,
                    )
                },
            )
            InvitationsRoute(
                viewModel = viewModel,
                onNavigateToFamily = { familyId ->
                    navController.navigate(ShorashimRoutes.familyManagement(familyId)) {
                        popUpTo(ShorashimDestination.FamilyTree.route)
                    }
                },
            )
        }

        // Phase 5: Invitation deep-link screen (shown after App Link opens the app)
        composable(
            route = ShorashimRoutes.INVITATION_DEEP_LINK,
            arguments = listOf(
                navArgument("token") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token").orEmpty()
            val viewModel: InvitationDeepLinkViewModel = viewModel(
                key = "InviteDeepLink_$token",
                factory = shorashimViewModelFactory {
                    InvitationDeepLinkViewModel(
                        token = token,
                        invitationRepository = appContainer.invitationRepository,
                    )
                },
            )
            InvitationDeepLinkRoute(
                viewModel = viewModel,
                onAccepted = { familyId ->
                    navController.navigate(ShorashimRoutes.familyManagement(familyId)) {
                        popUpTo(ShorashimDestination.FamilyTree.route)
                    }
                },
                onDeclined = { navController.popBackStack() },
                onNavigateHome = {
                    navController.navigate(ShorashimDestination.FamilyTree.route) {
                        popUpTo(ShorashimDestination.FamilyTree.route) { inclusive = true }
                    }
                },
            )
        }

        // Phase 7: Community / Public Shared Family Trees (Dedicated Bottom Bar Tab)
        composable(ShorashimDestination.Community.route) {
            val viewModel: com.shorashim.feature.community.CommunityTreesViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    com.shorashim.feature.community.CommunityTreesViewModel(appContainer.familyRepository)
                },
            )
            com.shorashim.feature.community.CommunityTreesRoute(
                viewModel = viewModel,
                onNavigateToTree = { familyId ->
                    navController.navigate(ShorashimRoutes.viewCommunityTree(familyId))
                },
            )
        }

        // Phase 7: View-Only Community Family Tree Screen
        composable(
            route = ShorashimRoutes.VIEW_COMMUNITY_TREE,
            arguments = listOf(
                navArgument("familyId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val familyId = backStackEntry.arguments?.getString("familyId").orEmpty()
            val viewModel: FamilyManagementViewModel = viewModel(
                key = "ViewCommunityTree_$familyId",
                factory = shorashimViewModelFactory {
                    FamilyManagementViewModel(
                        familyId = familyId,
                        familyRepository = appContainer.familyRepository,
                        personRepository = appContainer.personRepository,
                        relationshipRepository = appContainer.relationshipRepository,
                        invitationRepository = null,
                        profileRepository = appContainer.profileRepository,
                        supabaseClient = appContainer.supabaseClient,
                    )
                },
            )
            FamilyManagementRoute(
                viewModel = viewModel,
                isReadOnly = true,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddPerson = {},
                onNavigateToAddRelative = { _, _, _ -> },
                onNavigateToEditPerson = { _, _ -> },
                onNavigateToCreateFamily = {},
                invitationRepository = null,
            )
        }
    }
}
