package app.banafsh.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import app.banafsh.android.ui.components.themed.Header
import app.banafsh.android.ui.components.themed.IconButton
import app.banafsh.android.ui.screens.settings.REPO_NAME
import app.banafsh.android.ui.screens.settings.REPO_OWNER
import app.banafsh.android.ui.screens.settings.SettingsEntry
import app.banafsh.android.ui.screens.settings.SettingsGroup
import app.banafsh.android.ui.theme.Appearance
import app.banafsh.android.ui.theme.BuiltInFontFamily
import app.banafsh.android.ui.theme.LocalAppearance
import app.banafsh.android.ui.theme.Typography
import app.banafsh.android.ui.theme.defaultDarkPalette
import app.banafsh.android.ui.theme.defaultLightPalette
import app.banafsh.android.utils.createFile
import app.banafsh.android.utils.secondary
import app.banafsh.android.utils.semiBold
import app.banafsh.android.utils.shareFile
import app.banafsh.android.utils.toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BugReporterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val exceptionMessage = intent.getStringExtra("exception_message")
        val threadName = intent.getStringExtra("thread_name")

        val deviceBrand = Build.BRAND
        val deviceModel = Build.MODEL
        val sdkLevel = Build.VERSION.SDK_INT

        val currentDateTime = Calendar.getInstance().time
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDateTime = formatter.format(currentDateTime)

        val appVersion = BuildConfig.VERSION_NAME

        val combinedTextBuilder = StringBuilder()
        combinedTextBuilder
            .append("Banafsh Version:\t").append(appVersion).append('\n')
            .append("Phone Brand:\t").append(deviceBrand).append('\n')
            .append("Phone Model:\t").append(deviceModel).append('\n')
            .append("Sdk Version:\t").append(sdkLevel).append('\n')
            .append("Thread:\t").append(threadName).append('\n')
            .append("Time:\t").append(formattedDateTime).append('\n')
            .append("-------------- Beginning of crush -------------").append('\n')
            .append(exceptionMessage)
            .append("-------------- Ending of crush ----------------")

        setContent {
            val isDark = isSystemInDarkTheme()
            val colorPalette = if (isDark) defaultDarkPalette else defaultLightPalette
            val textStyle = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                color = colorPalette.text
            )
            val typography = Typography(
                xxs = textStyle.copy(fontSize = 12.sp),
                xs = textStyle.copy(fontSize = 14.sp),
                s = textStyle.copy(fontSize = 16.sp),
                m = textStyle.copy(fontSize = 18.sp),
                l = textStyle.copy(fontSize = 20.sp),
                xxl = textStyle.copy(fontSize = 32.sp),
                fontFamily = BuiltInFontFamily.System
            )
            val appearance = Appearance(
                colorPalette = colorPalette,
                typography = typography,
                thumbnailShapeCorners = 8.dp
            )

            CompositionLocalProvider(
                LocalAppearance provides appearance
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorPalette.surface),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val uriHandler = LocalUriHandler.current

                    Header(title = stringResource(R.string.crash_report))
                    SettingsGroup(stringResource(R.string.crash_report)) {
                        LogEntry(
                            title = stringResource(R.string.crash_report),
                            body = combinedTextBuilder.toString(),
                            trailingContent = {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        icon = R.drawable.share_social,
                                        color = colorPalette.text,
                                        onClick = {
                                            this@BugReporterActivity.startActivity(
                                                Intent.createChooser(
                                                    Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        type = "text/plain"
                                                        putExtra(
                                                            Intent.EXTRA_TEXT,
                                                            combinedTextBuilder.toString()
                                                        )
                                                    },
                                                    null
                                                )
                                            )
                                        },
                                        modifier = Modifier
                                            .padding(all = 4.dp)
                                            .size(17.dp)
                                    )
                                    IconButton(
                                        icon = R.drawable.copy,
                                        color = colorPalette.text,
                                        onClick = {
                                            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText(
                                                "error msg",
                                                combinedTextBuilder.toString()
                                            )
                                            clipboard.setPrimaryClip(clip)
                                            toast(getString(R.string.crash_report_copied))
                                        },
                                        modifier = Modifier
                                            .padding(all = 4.dp)
                                            .size(17.dp)
                                    )
                                }
                            }
                        )

                        SettingsEntry(
                            title = stringResource(R.string.crash_report_send_file),
                            text = stringResource(R.string.crash_report_send_file_description),
                            onClick = {
                                val dayFormat = SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                )
                                val dateFormatted = dayFormat.format(currentDateTime)
                                val bugReport = createFile(
                                    this@BugReporterActivity,
                                    "Bug Report",
                                    "bug_report-$dateFormatted.txt",
                                    combinedTextBuilder.toString()
                                )
                                shareFile(this@BugReporterActivity, bugReport, "text/*")
                            },
                            trailingContent = {
                                IconButton(
                                    icon = R.drawable.share_social,
                                    color = colorPalette.text,
                                    enabled = false,
                                    onClick = {},
                                    modifier = Modifier
                                        .padding(all = 4.dp)
                                        .size(17.dp)
                                )
                            }
                        )
                    }

                    SettingsGroup(title = stringResource(R.string.contact)) {
                        SettingsEntry(
                            title = stringResource(R.string.report_bug),
                            text = stringResource(R.string.report_bug_description),
                            onClick = {
                                uriHandler.openUri(
                                    @Suppress("ktlint:standard:max-line-length")
                                    "https://github.com/$REPO_OWNER/$REPO_NAME/issues/new?assignees=&labels=bug&template=bug_report.yaml"
                                )
                            }
                        )

                        SettingsEntry(
                            title = stringResource(R.string.request_feature),
                            text = stringResource(R.string.redirect_github),
                            onClick = {
                                uriHandler.openUri(
                                    @Suppress("ktlint:standard:max-line-length")
                                    "https://github.com/$REPO_OWNER/$REPO_NAME/issues/new?assignees=&labels=enhancement&template=feature_request.md"
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogEntry(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) = Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
        .padding(start = 32.dp, end = 16.dp)
        .padding(vertical = 16.dp)
        .fillMaxWidth()
) {
    val (colorPalette, typography) = LocalAppearance.current

    Column(modifier = Modifier.weight(1f)) {
        BasicText(
            text = title,
            style = typography.xs.semiBold.copy(color = colorPalette.text)
        )

        BasicTextField(
            value = body,
            onValueChange = {},
            readOnly = true,
            textStyle = typography.xs.semiBold.secondary,
            maxLines = 10
        )
    }

    trailingContent?.invoke()
}
