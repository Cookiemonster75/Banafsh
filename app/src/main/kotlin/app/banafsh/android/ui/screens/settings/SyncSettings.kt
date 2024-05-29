package app.banafsh.android.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import app.banafsh.android.Database
import app.banafsh.android.Dependencies
import app.banafsh.android.R
import app.banafsh.android.lib.compose.persist.persistList
import app.banafsh.android.lib.core.ui.LocalAppearance
import app.banafsh.android.lib.providers.piped.Piped
import app.banafsh.android.lib.providers.piped.models.Instance
import app.banafsh.android.models.PipedSession
import app.banafsh.android.transaction
import app.banafsh.android.ui.components.themed.CircularProgressIndicator
import app.banafsh.android.ui.components.themed.ConfirmationDialog
import app.banafsh.android.ui.components.themed.DefaultDialog
import app.banafsh.android.ui.components.themed.DialogTextButton
import app.banafsh.android.ui.components.themed.IconButton
import app.banafsh.android.ui.components.themed.TextField
import app.banafsh.android.ui.screens.Route
import app.banafsh.android.utils.center
import app.banafsh.android.utils.get
import app.banafsh.android.utils.semiBold
import app.banafsh.android.utils.upsert
import io.ktor.http.Url
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Route
@Composable
fun SyncSettings() {
    val coroutineScope = rememberCoroutineScope()

    val (colorPalette, typography) = LocalAppearance.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val pipedSessions by Database.pipedSessions().collectAsState(initial = listOf())

    var linkingPiped by remember { mutableStateOf(false) }
    if (linkingPiped) DefaultDialog(
        onDismiss = { linkingPiped = false },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var isLoading by rememberSaveable { mutableStateOf(false) }
        var hasError by rememberSaveable { mutableStateOf(false) }
        var successful by remember { mutableStateOf(false) }

        when {
            successful -> BasicText(
                text = stringResource(R.string.piped_session_created_successfully),
                style = typography.xs.semiBold.center,
                modifier = Modifier.padding(all = 24.dp)
            )

            hasError -> BasicText(
                text = stringResource(R.string.error_piped_link),
                style = typography.xs.semiBold.center,
                modifier = Modifier.padding(all = 24.dp)
            )

            isLoading -> CircularProgressIndicator(modifier = Modifier.padding(all = 8.dp))

            else -> Column(modifier = Modifier.fillMaxWidth()) {
                var instances: List<Instance> by persistList(tag = "settings/sync/piped/instances")
                var loadingInstances by rememberSaveable { mutableStateOf(true) }
                var selectedInstance: Int? by rememberSaveable { mutableStateOf(null) }
                var username by rememberSaveable { mutableStateOf("") }
                var password by rememberSaveable { mutableStateOf("") }
                var canSelect by rememberSaveable { mutableStateOf(false) }
                var instancesUnavailable by rememberSaveable { mutableStateOf(false) }
                var customInstance: String? by rememberSaveable { mutableStateOf(null) }

                LaunchedEffect(Unit) {
                    Piped.getInstances()?.getOrNull()?.let {
                        selectedInstance = null
                        instances = it
                        canSelect = true
                    } ?: run { instancesUnavailable = true }
                    loadingInstances = false
                    runCatching {
                        Dependencies.credentialManager.get(context)?.let {
                            username = it.id
                            password = it.password
                        }
                    }.getOrNull()
                }

                BasicText(
                    text = stringResource(R.string.piped),
                    style = typography.m.semiBold
                )

                if (customInstance == null) ValueSelectorSettingsEntry(
                    title = stringResource(R.string.instance),
                    selectedValue = selectedInstance,
                    values = instances.indices.toImmutableList(),
                    onValueSelected = { selectedInstance = it },
                    valueText = { idx ->
                        idx?.let { instances.getOrNull(it)?.name }
                            ?: if (instancesUnavailable) stringResource(R.string.error_piped_instances_unavailable)
                            else stringResource(R.string.click_to_select)
                    },
                    isEnabled = !instancesUnavailable && canSelect,
                    usePadding = false,
                    trailingContent = if (loadingInstances) {
                        { CircularProgressIndicator() }
                    } else null
                )
                SwitchSettingsEntry(
                    title = stringResource(R.string.custom_instance),
                    text = null,
                    isChecked = customInstance != null,
                    onCheckedChange = { customInstance = if (customInstance == null) "" else null },
                    usePadding = false
                )
                customInstance?.let { instance ->
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = instance,
                        onValueChange = { customInstance = it },
                        hintText = stringResource(R.string.base_api_url),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    hintText = stringResource(R.string.username),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    hintText = stringResource(R.string.password),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        autoCorrect = false,
                        keyboardType = KeyboardType.Password
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            password()
                        }
                )
                Spacer(modifier = Modifier.height(16.dp))
                DialogTextButton(
                    text = stringResource(R.string.login),
                    primary = true,
                    enabled = (customInstance?.isNotBlank() == true || selectedInstance != null) &&
                            username.isNotBlank() && password.isNotBlank(),
                    onClick = {
                        @Suppress("Wrapping") // thank you ktlint
                        (customInstance?.let {
                            runCatching {
                                Url(it)
                            }.getOrNull() ?: runCatching {
                                Url("https://$it")
                            }.getOrNull()
                        } ?: selectedInstance?.let { instances[it].apiBaseUrl })?.let { url ->
                            coroutineScope.launch {
                                isLoading = true
                                val session = Piped.login(
                                    apiBaseUrl = url,
                                    username = username,
                                    password = password
                                )?.getOrNull()
                                isLoading = false
                                if (session == null) {
                                    hasError = true
                                    return@launch
                                }

                                transaction {
                                    Database.insert(
                                        PipedSession(
                                            apiBaseUrl = session.apiBaseUrl,
                                            username = username,
                                            token = session.token
                                        )
                                    )
                                }

                                successful = true

                                runCatching {
                                    Dependencies.credentialManager.upsert(
                                        context = context,
                                        username = username,
                                        password = password
                                    )
                                }

                                linkingPiped = false
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    var deletingPipedSession: Int? by rememberSaveable { mutableStateOf(null) }
    if (deletingPipedSession != null) ConfirmationDialog(
        text = stringResource(R.string.confirm_delete_piped_session),
        onDismiss = {
            deletingPipedSession = null
        },
        onConfirm = {
            deletingPipedSession?.let {
                transaction { Database.delete(pipedSessions[it]) }
            }
        }
    )

    SettingsCategoryScreen(title = stringResource(R.string.sync)) {
        SettingsDescription(text = stringResource(R.string.sync_description))

        SettingsGroup(title = stringResource(R.string.piped)) {
            SettingsEntry(
                title = stringResource(R.string.add_account),
                text = stringResource(R.string.add_account_description),
                onClick = { linkingPiped = true }
            )
            SettingsEntry(
                title = stringResource(R.string.learn_more),
                text = stringResource(R.string.learn_more_description),
                onClick = { uriHandler.openUri("https://github.com/TeamPiped/Piped/blob/master/README.md") }
            )
        }
        SettingsGroup(title = stringResource(R.string.piped_sessions)) {
            pipedSessions.fastForEachIndexed { i, session ->
                SettingsEntry(
                    title = session.username,
                    text = session.apiBaseUrl.toString(),
                    onClick = { },
                    trailingContent = {
                        IconButton(
                            onClick = { deletingPipedSession = i },
                            icon = R.drawable.delete,
                            color = colorPalette.text
                        )
                    }
                )
            }
        }
    }
}
