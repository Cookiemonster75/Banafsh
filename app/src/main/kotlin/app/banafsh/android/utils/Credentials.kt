package app.banafsh.android.utils

import android.content.Context
import android.os.CancellationSignal
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val executor = Executors.newCachedThreadPool()
private val coroutineScope = CoroutineScope(
    executor.asCoroutineDispatcher() + SupervisorJob() + CoroutineName("androidx-credentials-util")
)

private suspend inline fun <T> wrapper(
    crossinline block: (cont: CancellableContinuation<T>) -> Unit
): T = withContext(coroutineScope.coroutineContext) {
    runCatching {
        suspendCancellableCoroutine { cont ->
            runCatching {
                block(cont)
            }.exceptionOrNull()?.let {
                if (it is CancellationException) cont.cancel() else cont.resumeWithException(it)
            }
        }
    }.let {
        it.exceptionOrNull()?.printStackTrace()
        it.getOrThrow()
    }
}

suspend fun CredentialManager.upsert(
    context: Context,
    username: String,
    password: String
) = wrapper { cont ->
    createCredentialAsync(
        context = context,
        request = CreatePasswordRequest(
            id = username,
            password = password
        ),
        cancellationSignal = CancellationSignal().apply {
            setOnCancelListener { cont.cancel() }
        },
        executor = executor,
        callback = object :
            CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException> {
            override fun onError(e: CreateCredentialException) {
                when (e) {
                    is CreateCredentialCancellationException -> cont.cancel(e)
                    else -> cont.resumeWithException(e)
                }
            }

            override fun onResult(result: CreateCredentialResponse) = cont.resume(Unit)
        }
    )
}

suspend fun CredentialManager.get(context: Context) = wrapper { cont ->
    getCredentialAsync(
        context = context,
        request = GetCredentialRequest(listOf(GetPasswordOption())),
        cancellationSignal = CancellationSignal().apply {
            setOnCancelListener { cont.cancel() }
        },
        executor = executor,
        callback = object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
            override fun onError(e: GetCredentialException) {
                when (e) {
                    is GetCredentialCancellationException -> cont.cancel(e)
                    else -> cont.resumeWithException(e)
                }
            }

            override fun onResult(result: GetCredentialResponse) {
                val credential = runCatching { result.credential as? PasswordCredential }.getOrNull()
                cont.resume(credential)
            }
        }
    )
}
