package contributors

import contributors.Contributors.LoadingStatus.*
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import tasks.*
import java.awt.event.ActionListener
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext

enum class Variant {
    BLOCKING,         // Request1Blocking
    BACKGROUND,       // Request2Background
    CALLBACKS,        // Request3Callbacks
    SUSPEND,          // Request4Coroutine
    CONCURRENT,       // Request5Concurrent
    NOT_CANCELLABLE,  // Request6NotCancellable
    PROGRESS,         // Request6Progress
    CHANNELS,         // Request7Channels
    RX,               // Request8Rx
    RX_PROGRESS       // Request9RxProgress
}
val setOfVideos = mutableSetOf<Video>()

interface Contributors: CoroutineScope {

    val job: Job

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    fun init() {
        // Start a new loading on 'load' click
        addLoadListener {
//            saveParams()
            loadContributors()
        }

        // Save preferences and exit on closing the window
        addOnWindowClosingListener {
            job.cancel()
//            saveParams()
            System.exit(0)
        }

        // Load stored params (user & password values)
//        loadInitialParams()
    }

    fun loadContributors() {
//        val (username, password, org, _) = getParams()
//        val req = RequestData(username, password, org)
        val (start, end) = getStartAndEnd()
        clearResults()
//        val service = createGitHubService(req.username, req.password)

        val startTime = System.currentTimeMillis()
//        when (getSelectedVariant()) {
//            BLOCKING -> { // Blocking UI thread
//                val users = loadContributorsBlocking(service, req)
//                updateResults(users, startTime)
//            }
//            BACKGROUND -> { // Blocking a background thread
//                loadContributorsBackground(service, req) { users ->
//                    SwingUtilities.invokeLater {
//                        updateResults(users, startTime)
//                    }
//                }
//            }
//            CALLBACKS -> { // Using callbacks
//                loadContributorsCallbacks(service, req) { users ->
//                    SwingUtilities.invokeLater {
//                        updateResults(users, startTime)
//                    }
//                }
//            }
//            SUSPEND -> { // Using coroutines
//                launch {
//                    val users = loadContributorsSuspend(service, req)
//                    updateResults(users, startTime)
//                }.setUpCancellation()
//            }
//            CONCURRENT -> { // Performing requests concurrently
//                launch {
//                    val users = loadContributorsConcurrent(service, req)
//                    updateResults(users, startTime)
//                }.setUpCancellation()
//            }
//            NOT_CANCELLABLE -> { // Performing requests in a non-cancellable way
//                launch {
//                    val users = loadContributorsNotCancellable(service, req)
//                    updateResults(users, startTime)
//                }.setUpCancellation()
//            }
//            PROGRESS -> { // Showing progress
//                launch(Dispatchers.Default) {
//                    loadContributorsProgress(service, req) { users, completed ->
//                        withContext(Dispatchers.Main) {
//                            updateResults(users, startTime, completed)
//                        }
//                    }
//                }.setUpCancellation()
//            }
//            CHANNELS -> {  // Performing requests concurrently and showing progress
//                launch(Dispatchers.Default) {
//                    loadContributorsChannels(service, req) { users, completed ->
//                        withContext(Dispatchers.Main) {
//                            updateResults(users, startTime, completed)
//                        }
//                    }
//                }.setUpCancellation()
//            }
//            RX -> {  // Using RxJava
//                loadContributorsReactive(service, req)
//                    .subscribe { users ->
//                        SwingUtilities.invokeLater {
//                            updateResults(users, startTime)
//                        }
//                    }.setUpCancellation()
//            }
//            RX_PROGRESS -> {  // Using RxJava and showing progress { users, completed ->
                loadVideosReactiveProgress(createVimeoService(),start..end).map {observableVideo->
                    observableVideo.subscribe(
                        {
                            SwingUtilities.invokeLater {
                                updateResults(it, startTime, false)
                            }
                        }, {
                            SwingUtilities.invokeLater {
                                println("Done/error. ${observableVideo.blockingSingle().id}")
                                textboxMessage("Error: ${it.message}", "error")
                                setLoadingStatus("error ${it.message}, ${(System.currentTimeMillis() - startTime).let{time->"${time / 1000}." + "${time % 1000 / 100} sec"}}",false)
                                setActionsStatus(newLoadingEnabled = true)
                            }
                        }, {
                            SwingUtilities.invokeLater {
                                println("Done.")
                                updateLoadingStatus(COMPLETED, startTime)
                                setActionsStatus(newLoadingEnabled = true)
                            }
                        }).setUpCancellation()
                }
            /* loadContributorsReactiveProgress(service, req)
                    .subscribe(
                        {
                        SwingUtilities.invokeLater {
                            updateResults(it, startTime, false)
                        }
                    }, {
                        SwingUtilities.invokeLater {
                            setLoadingStatus("error ${it.message}", false)
                            setActionsStatus(newLoadingEnabled = true)
                        }
                    }, {
                        SwingUtilities.invokeLater {
                            updateLoadingStatus(COMPLETED, startTime)
                            setActionsStatus(newLoadingEnabled = true)
                        }
                    }).setUpCancellation()*/
//            }
//        }
    }

    private enum class LoadingStatus { COMPLETED, CANCELED, IN_PROGRESS }

    fun String.matchesVideoConstraint(): Boolean {
        return contains("greenius", ignoreCase = true)
    }
    @JvmName("updateResults1")
    private fun updateResults(
        video: Video,
        startTime: Long,
        completed: Boolean = true,
    ) {
        updateVideos(video)
        updateLoadingStatus((if (completed) COMPLETED else IN_PROGRESS), startTime)
        if (completed) {
            setActionsStatus(newLoadingEnabled = true)
        }
    }
    fun writeVideosToFile(){
        val outputFile = File("Results.txt").apply{createNewFile()}.toPath()
        outputFile.writeLinesToFile(setOfVideos.map { "$baseUrl${it.id}" })
    }

    // rewrite text
    @Throws(IOException::class)
    fun Path.writeLinesToFile(list: List<String>) {
        Files.write(
            this, list,
            StandardOpenOption.CREATE,
        )
    }
    private fun clearResults() {
        updateContributors(listOf())
        updateLoadingStatus(IN_PROGRESS)
        setActionsStatus(newLoadingEnabled = false)
    }

    private fun updateResults(
        users: List<User>,
        startTime: Long,
        completed: Boolean = true
    ) {
        updateContributors(users)
        updateLoadingStatus(if (completed) COMPLETED else IN_PROGRESS, startTime)
        if (completed) {
            setActionsStatus(newLoadingEnabled = true)
        }
    }

    private fun updateLoadingStatus(
        status: LoadingStatus,
        startTime: Long? = null
    ) {
        val time = if (startTime != null) {
            val time = (System.currentTimeMillis() - startTime).also{elapsed->
                videoCounter?.let{/*if(counter++ % 10 == 0)*/
                    println("Speed:                       ${it/(elapsed / 1000.0)} videos/sec")
                }
            }
            "${(time / 1000)}.${time % 1000 / 100} sec"
        } else ""

        val text = "Loading status: " +
                when (status) {
                    COMPLETED -> "completed in $time"
                    IN_PROGRESS -> "in progress $time"
                    CANCELED -> "canceled"
                }
        setLoadingStatus(text, status == IN_PROGRESS)
    }

    private fun Job.setUpCancellation() {
        // make active the 'cancel' button
        setActionsStatus(newLoadingEnabled = false, cancellationEnabled = true)

        val loadingJob = this

        // cancel the loading job if the 'cancel' button was clicked
        val listener = ActionListener {
            loadingJob.cancel()
            updateLoadingStatus(CANCELED)
        }
        addCancelListener(listener)

        // update the status and remove the listener after the loading job is completed
        launch {
            loadingJob.join()
            setActionsStatus(newLoadingEnabled = true)
            removeCancelListener(listener)
        }
    }

    private fun Disposable.setUpCancellation() {
        // make active the 'cancel' button
        setActionsStatus(newLoadingEnabled = false, cancellationEnabled = true)

        val loadingDisposable = this

        // cancel the loading job if the 'cancel' button was clicked
        var listener: ActionListener
        listener = ActionListener {
            loadingDisposable.dispose()
            updateLoadingStatus(CANCELED)
            setActionsStatus(newLoadingEnabled = true)
        }
        addCancelListener(listener)
    }

    fun loadInitialParams() {
        setParams(loadStoredParams())
    }

    fun saveParams() {
        val params = getParams()
        if (params.username.isEmpty() && params.password.isEmpty()) {
            removeStoredParams()
        }
        else {
            saveParams(params)
        }
    }

    fun getSelectedVariant(): Variant

    fun updateContributors(users: List<User>)

    fun setLoadingStatus(text: String, iconRunning: Boolean)

    fun setActionsStatus(newLoadingEnabled: Boolean, cancellationEnabled: Boolean = false)

    fun addCancelListener(listener: ActionListener)

    fun removeCancelListener(listener: ActionListener)

    fun addLoadListener(listener: () -> Unit)

    fun addOnWindowClosingListener(listener: () -> Unit)

    fun setParams(params: Params)

    fun getParams(): Params

    fun updateVideos(videos: List<Video>){}
    fun updateVideos(video: Video){}

    fun getStartAndEnd():Pair<Int,Int>{return Pair(1,1)}

}
