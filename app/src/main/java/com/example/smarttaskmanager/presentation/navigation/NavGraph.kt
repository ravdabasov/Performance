package com.example.smarttaskmanager.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.smarttaskmanager.presentation.screen.addedit.AddEditTaskScreen
import com.example.smarttaskmanager.presentation.screen.chat.TaskChatScreen
import com.example.smarttaskmanager.presentation.screen.chat.TeamChatScreen
import com.example.smarttaskmanager.presentation.screen.completed.CompletedTasksScreen
import com.example.smarttaskmanager.presentation.screen.dashboard.DashboardScreen
import com.example.smarttaskmanager.presentation.screen.statistics.StatisticsScreen
import com.example.smarttaskmanager.presentation.screen.tasklist.TaskListScreen

/** Bütün naviqasiya route-larının mərkəzləşdirilmiş tərifi - typo riskini aradan qaldırır. */
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object TaskList : Screen("task_list")
    data object Completed : Screen("completed")
    data object Statistics : Screen("statistics")
    data object ChatList : Screen("chat_list")
    data object AddTask : Screen("add_task")
    data class TaskChat(val taskId: Long) : Screen("task_chat/$taskId") {
        companion object {
            const val ROUTE_PATTERN = "task_chat/{taskId}"
        }
    }
    data class EditTask(val taskId: Long) : Screen("edit_task/$taskId") {
        companion object {
            const val ROUTE_PATTERN = "edit_task/{taskId}"
        }
    }
}

@Composable
fun SmartTaskNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route,
    onNavigateToTab: (String) -> Unit
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToTaskList = { onNavigateToTab(Screen.TaskList.route) },
                onAddTask = { navController.navigate(Screen.AddTask.route) },
                onTaskClick = { taskId -> navController.navigate(Screen.EditTask(taskId).route) }
            )
        }

        composable(Screen.TaskList.route) {
            TaskListScreen(
                onAddTask = { navController.navigate(Screen.AddTask.route) },
                onTaskClick = { taskId -> navController.navigate(Screen.EditTask(taskId).route) }
            )
        }

        composable(Screen.Completed.route) {
            CompletedTasksScreen()
        }

        composable(Screen.Statistics.route) {
            StatisticsScreen()
        }

        composable(Screen.ChatList.route) {
            TeamChatScreen()
        }

        composable(Screen.AddTask.route) {
            AddEditTaskScreen(
                onDone = { navController.popBackStack() },
                onOpenChat = { taskId -> navController.navigate(Screen.TaskChat(taskId).route) }
            )
        }

        composable(
            route = Screen.EditTask.ROUTE_PATTERN,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) {
            AddEditTaskScreen(
                onDone = { navController.popBackStack() },
                onOpenChat = { taskId -> navController.navigate(Screen.TaskChat(taskId).route) }
            )
        }

        composable(
            route = Screen.TaskChat.ROUTE_PATTERN,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) {
            TaskChatScreen(onBack = { navController.popBackStack() })
        }
    }
}
